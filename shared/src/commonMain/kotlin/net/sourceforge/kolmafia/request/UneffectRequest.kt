package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.maximizer.SkillRequiredItemForEffect
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop UneffectRequest HTTP and response-sync hub.
 *
 * The optional collaborators keep the request usable by small tests while allowing production
 * callers to get preflight checks, remedy retrieval, local state sync, and session logging.
 */
open class UneffectRequest(
    private val client: HttpClient,
    private val effectManager: EffectManager? = null,
    private val inventoryManager: InventoryManager? = null,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
    private val asdonMartinRequest: AsdonMartinRequest = AsdonMartinRequest(client),
    private val retrieveItem: (suspend (Int) -> Boolean)? = null,
    private val passwordHash: () -> String = { "" },
    private val hasSkill: (String) -> Boolean = { false },
) {
    private val currentEffectRemovals = mutableSetOf<Int>()

    /** Compatibility entry point; all callers should receive desktop routing. */
    open suspend fun uneffect(effectId: Int): Result<Unit> = remove(effectId)

    /** Unified routed removal with desktop active/removable/intrinsic preflight. */
    open suspend fun remove(effectId: Int): Result<Unit> {
        if (!isRemovable(effectId)) {
            return Result.failure(IllegalStateException("${effectName(effectId)} is unremovable."))
        }
        val active = effectManager?.state?.value?.effects?.firstOrNull { it.id == effectId }
        if (effectManager != null && active == null) {
            return Result.failure(IllegalStateException("You don't have that effect."))
        }
        if (active?.isIntrinsic == true && !isRemovableIntrinsic(effectId, hasSkill)) {
            return Result.failure(
                IllegalStateException("${active.name} is intrinsic and cannot be removed."),
            )
        }
        if (!currentEffectRemovals.add(effectId)) {
            return Result.failure(IllegalStateException("Recursive uneffect request for $effectId"))
        }

        return try {
            when {
                isAsdon(effectId) -> removeAsdon(effectId)
                isShruggable(effectId) -> submitShrug(effectId)
                else -> submitRemedy(effectId)
            }
        } finally {
            currentEffectRemovals.remove(effectId)
        }
    }

    private suspend fun submitShrug(effectId: Int): Result<Unit> =
        submit(
            path = "charsheet.php",
            effectId = effectId,
            fields = parameters {
                append("action", "unbuff")
                append("ajax", "1")
                append("whichbuff", effectId.toString())
            },
        )

    private suspend fun removeAsdon(effectId: Int): Result<Unit> {
        val result = asdonMartinRequest.clearDrive(effectId - ASDON_FIRST)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        processResults(effectId, result.getOrDefault(""))
        registerRequest("campground.php?preaction=undrive", effectId, sessionLogger, inventoryManager)
        return Result.success(Unit)
    }

    private suspend fun submitRemedy(effectId: Int): Result<Unit> {
        if (UneffectRemovableMaps.needsCocoa(effectId)) {
            return Result.failure(
                IllegalStateException(
                    "${effectName(effectId)} can be removed only with hot Dreadsylvanian cocoa.",
                ),
            )
        }
        val hasAncient = inventoryCount(UneffectRemovableMaps.ANCIENT_CURE_ALL) > 0
        if (!hasAncient && inventoryCount(UneffectRemovableMaps.REMEDY) <= 0) {
            val retrieved = retrieveItem?.invoke(UneffectRemovableMaps.REMEDY) ?: true
            if (!retrieved) {
                return Result.failure(IllegalStateException("Unable to retrieve a soft green echo eyedrop antidote."))
            }
        }
        return submit(
            path = "uneffect.php",
            effectId = effectId,
            fields = parameters {
                append("using", "Yep.")
                append("whicheffect", effectId.toString())
                append("pwd", passwordHash())
            },
        )
    }

    private suspend fun submit(path: String, effectId: Int, fields: Parameters): Result<Unit> =
        try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/$path",
                formParameters = fields,
            )
            if (!response.status.isSuccess()) {
                Result.failure(IllegalStateException("HTTP ${response.status.value}"))
            } else {
                val body = response.bodyAsText()
                processResults(effectId, body)
                val query = fields.entries().flatMap { (key, values) ->
                    values.map { "$key=$it" }
                }.joinToString("&")
                registerRequest("$path?$query", effectId, sessionLogger, inventoryManager)
                Result.success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    /** Successful uneffect responses are authoritative even when KoL omits "Effect removed." */
    open fun processResults(effectId: Int, responseText: String?) {
        if (responseText == null) return
        effectManager?.removeEffect(effectId)
        if (effectId == INIGOS || effectId == CRAFT_TEA ||
            (effectId == GARISH && preferences?.getBoolean("autoGarish", false) != true)
        ) {
            ConcoctionDatabase.markRefreshNeeded()
        }
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    companion object {
        const val TIMER_FIRST = 873
        const val TIMER_LAST = 882
        const val ASDON_FIRST = 2308
        const val ASDON_LAST = 2316
        const val INIGOS = 716
        const val GARISH = 918
        const val CRAFT_TEA = 1989
        const val CITIZEN_OF_A_ZONE = 2822

        private val ALWAYS_SHRUGGABLE = setOf(
            1003, // Just the Best Anapests
            1492, // Reassured
            1515, // Hare-brained
            2128, // Record Hunger
            2129, // Drunk and Avuncular
            2131, // Shriek of the Weasel
            2132, // Power Man
            2133, // Lucky Struck
            2134, // Ministrations in the Dark
            2135, // Superdrifting
            2147, // Eldritch Attunement
            2600, 2601, 2602, // cartographic effects
        )

        fun isRemovable(effectId: Int): Boolean = UneffectRemovableMaps.isRemovable(effectId)

        fun isTimer(effectId: Int): Boolean = effectId in TIMER_FIRST..TIMER_LAST

        fun isAsdon(effectId: Int): Boolean = effectId in ASDON_FIRST..ASDON_LAST

        fun isShruggable(effectId: Int): Boolean {
            if (isTimer(effectId) || effectId in ALWAYS_SHRUGGABLE) return true
            val effect = EffectDatabase.getById(effectId) ?: return false
            if (effect.isSong()) return true
            val skillName = UneffectSkillEffectMap.effectToSkill(effect.name) ?: return false
            val skill = SkillDefinitionDatabase.getByName(skillName) ?: return false
            return skill.duration > 0 &&
                "passive" !in skill.tags &&
                SkillRequiredItemForEffect.requiredItem(skill.id, effectId) == -1
        }

        fun isRemovableIntrinsic(effectId: Int, hasSkill: (String) -> Boolean = { false }): Boolean =
            effectId == CITIZEN_OF_A_ZONE ||
                UneffectRemovableMaps.getUneffectSkill(effectId, hasSkill).isNotEmpty()

        fun effectName(effectId: Int): String =
            EffectDatabase.getById(effectId)?.name ?: "Effect #$effectId"

        /**
         * RequestLogger hook. Explicit [knownEffectId] supports POST bodies whose fields are not in
         * the URL; ordinary browser visits are parsed from whicheffect/whichbuff.
         */
        fun registerRequest(
            location: String,
            knownEffectId: Int? = null,
            sessionLogger: SessionLogger? = null,
            inventoryManager: InventoryManager? = null,
        ): Boolean {
            val relative = location.substringAfterLast("$KOL_BASE_URL/").trimStart('/')
            if (!relative.startsWith("uneffect.php") &&
                !relative.startsWith("charsheet.php") &&
                !(relative.startsWith("campground.php") && relative.contains("preaction=undrive"))
            ) {
                return false
            }
            if (!relative.contains('?') && knownEffectId == null) return true
            val effectId = knownEffectId
                ?: Regex("""(?:whicheffect|whichbuff)=(\d+)""")
                    .find(relative)?.groupValues?.get(1)?.toIntOrNull()
                ?: return true

            if (relative.startsWith("uneffect.php") && isRemovable(effectId)) {
                val ancient = inventoryManager?.state?.value?.items
                    ?.get(UneffectRemovableMaps.ANCIENT_CURE_ALL)?.quantity ?: 0
                inventoryManager?.consumeItemLocally(
                    if (ancient > 0) UneffectRemovableMaps.ANCIENT_CURE_ALL
                    else UneffectRemovableMaps.REMEDY,
                    1,
                )
            }
            sessionLogger?.appendRawLine("uneffect ${effectName(effectId)}")
            return true
        }
    }
}

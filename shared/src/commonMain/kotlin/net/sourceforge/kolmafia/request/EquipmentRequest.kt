package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemEquippedSync

open class EquipmentRequest(
    private val client: HttpClient,
    private val characterRequest: CharacterRequest? = null,
    private val character: KoLCharacter? = null,
    private val questDatabase: QuestDatabase? = null,
) {
    companion object {
        private val OUTFIT_SELECT_PATTERN = Regex(
            """<select\s+name=whichoutfit>(.*?)</select>""",
            RegexOption.DOT_MATCHES_ALL
        )
        private val OUTFIT_OPTION_PATTERN = Regex(
            """<option\s+value=['"]?(-?\d+)['"]?[^>]*>([^<]+)</option>"""
        )
        private val OUTFIT_ID_COMMENT = Regex("""<!--\s*outfitid:\s*(\d+)\s*-->""", RegexOption.IGNORE_CASE)
        val MANAGE_ENTRY_PATTERN = Regex(
            """name=name(\d+)\s+value="([^"]*)".*?<center><b>Contents:</b></cente[rR]>(.*?)</td>""",
            RegexOption.DOT_MATCHES_ALL
        )
    }

    /** Wear a static or custom outfit by KoL outfit id. */
    open suspend fun wearOutfit(outfitId: Int): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/inv_equip.php",
            formParameters = Parameters.build {
                append("which", "2")
                append("action", "outfit")
                append("whichoutfit", outfitId.toString())
                append("ajax", "1")
            }
        )
        val body = response.bodyAsText()
        if (body.contains("You put on", ignoreCase = true)) {
            syncCharacterEquipment()
            Result.success(Unit)
        } else {
            Result.failure(Exception("Outfit wear failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Birthday suit — unequip all gear. */
    open suspend fun unequipAll(): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/inv_equip.php",
            formParameters = Parameters.build {
                append("which", "2")
                append("action", "unequipall")
                append("ajax", "1")
            }
        )
        syncCharacterEquipment()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
        if (slot in EquipmentSlot.CODPIECE_SLOTS) {
            return equipCodpieceGem(itemId, slot)
        }
        return try {
            client.submitForm(
                url = "$KOL_BASE_URL/inv_equip.php",
                formParameters = Parameters.build {
                    append("which", "2")
                    append("action", "equip")
                    append("whichitem", itemId.toString())
                    append("slot", slot.apiKey)
                    append("ajax", "1")
                }
            )
            syncCharacterEquipment()
            QuestItemEquippedSync.apply(itemId, questDatabase)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun unequipSlot(slot: EquipmentSlot): Result<Unit> {
        if (slot in EquipmentSlot.CODPIECE_SLOTS) {
            return unequipCodpieceSlot(slot)
        }
        return try {
            client.submitForm(
                url = "$KOL_BASE_URL/inv_equip.php",
                formParameters = Parameters.build {
                    append("action", "unequip")
                    append("type", slot.apiKey)
                }
            )
            syncCharacterEquipment()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop `inventory.php?action=docodpiece` — opens codpiece gem editor (choice 1588). */
    open suspend fun openCodpieceEditor(): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/inventory.php") {
            parameter("action", "docodpiece")
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Codpiece editor open failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun equipCodpieceGem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
        if (slot !in EquipmentSlot.CODPIECE_SLOTS) {
            return Result.failure(Exception("Not a codpiece slot"))
        }
        if (!ModifierDatabase.isCodpieceGem(itemId)) {
            return Result.failure(Exception("Not a codpiece gem"))
        }
        val gemName = ItemDatabase.getById(itemId)?.name
        val alreadyEquipped = character?.state?.value?.equipment?.get(slot)
        if (gemName != null && gemName.equals(alreadyEquipped, ignoreCase = true)) {
            return Result.success(Unit)
        }
        return try {
            openCodpieceEditor().getOrThrow()
            val response = client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = Parameters.build {
                    append("whichchoice", "1588")
                    append("option", "1")
                    append("which", codpieceSlotNumber(slot).toString())
                    append("iid", itemId.toString())
                }
            )
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Codpiece equip failed"))
            }
            syncCharacterEquipment()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun unequipCodpieceSlot(slot: EquipmentSlot): Result<Unit> {
        if (slot !in EquipmentSlot.CODPIECE_SLOTS) {
            return Result.failure(Exception("Not a codpiece slot"))
        }
        val alreadyEmpty = character?.state?.value?.equipment?.get(slot).isNullOrBlank()
        if (alreadyEmpty) {
            return Result.success(Unit)
        }
        return try {
            openCodpieceEditor().getOrThrow()
            val response = client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = Parameters.build {
                    append("whichchoice", "1588")
                    append("option", "2")
                    append("which", codpieceSlotNumber(slot).toString())
                }
            )
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Codpiece unequip failed"))
            }
            syncCharacterEquipment()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun codpieceSlotNumber(slot: EquipmentSlot): Int = when (slot) {
        EquipmentSlot.CODPIECE1 -> 1
        EquipmentSlot.CODPIECE2 -> 2
        EquipmentSlot.CODPIECE3 -> 3
        EquipmentSlot.CODPIECE4 -> 4
        EquipmentSlot.CODPIECE5 -> 5
        else -> throw IllegalArgumentException("Not a codpiece slot: $slot")
    }

    /** Parse outfit dropdown from equipment page HTML. */
    open suspend fun fetchOutfitOptions(): List<Pair<Int, String>> {
        return try {
            val html = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("which", "2")
            }.bodyAsText()
            parseOutfitOptions(html)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseOutfitOptions(html: String): List<Pair<Int, String>> {
        val select = OUTFIT_SELECT_PATTERN.find(html)?.groupValues?.get(1) ?: return emptyList()
        return OUTFIT_OPTION_PATTERN.findAll(select).mapNotNull { m ->
            val id = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val label = m.groupValues[2].trim()
            id to label
        }.toList()
    }

    open suspend fun syncCharacterEquipment() {
        val req = characterRequest ?: return
        val char = character ?: return
        req.fetchCharacterState().onSuccess { char.updateFromApiResponse(it) }
    }

    /** Save current equipment as a custom outfit (desktop `outfit save <name>`). */
    open suspend fun saveCustomOutfit(name: String): Result<Int> {
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/inv_equip.php",
                formParameters = Parameters.build {
                    append("which", "2")
                    append("action", "customoutfit")
                    append("outfitname", name)
                    append("ajax", "1")
                }
            )
            val body = response.bodyAsText()
            if (!body.contains("Your custom outfit has been saved", ignoreCase = true)) {
                Result.failure(Exception("Outfit save failed"))
            } else {
                val id = OUTFIT_ID_COMMENT.find(body)?.groupValues?.get(1)?.toIntOrNull()
                    ?: return Result.failure(Exception("No outfit id in response"))
                syncCharacterEquipment()
                Result.success(-id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

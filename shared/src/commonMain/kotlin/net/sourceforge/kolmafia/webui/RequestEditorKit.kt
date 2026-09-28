package net.sourceforge.kolmafia.webui

import net.sourceforge.kolmafia.adventure.choice.ChoiceAdventures
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [RequestEditorKit] — HTML decorate pipeline for Relay Browser responses.
 */
object RequestEditorKit {
    var preferences: Preferences? = null

    fun getFeatureRichHTML(
        location: String,
        text: String,
        addComplexFeatures: Boolean = true,
        preferences: Preferences? = RequestEditorKit.preferences,
    ): String {
        if (text.isEmpty()) return ""
        if (location.startsWith("api.php")) return text
        val buffer = StringBuilder(text.replace("<body><head>", "<head>"))
        applyPageAdjustments(location, buffer, addComplexFeatures, preferences)
        applyGlobalAdjustments(location, buffer, addComplexFeatures, preferences)
        return buffer.toString()
    }

    fun applyPageAdjustments(
        location: String,
        buffer: StringBuilder,
        addComplexFeatures: Boolean,
        preferences: Preferences? = RequestEditorKit.preferences,
    ) {
        if (location.startsWith("charpane.php")) {
            if (addComplexFeatures) CharPaneDecorator.decorate(buffer)
            return
        }
        if (location.contains("menu.php")) {
            TopMenuDecorator.decorate(buffer, location)
            return
        }

        when {
            location.startsWith("account.php") -> {
                replaceOnce(buffer, "Manage Subscriptions", "Manage Subscriptions (this will not work in KoLmafia)")
            }
            location.startsWith("adventure.php") -> {
                StationaryButtonDecorator.decorate(location, buffer)
            }
            location.startsWith("ascend.php") -> {
                ValhallaDecorator.decorateGashJump(location, buffer)
            }
            location.startsWith("basement.php") -> {
                BasementDecorator.decorate(buffer)
            }
            location.startsWith("beerpong.php") -> {
                BeerPongDecorator.decorate(buffer)
            }
            location.startsWith("bigisland.php") || location.startsWith("postwarisland.php") -> {
                IslandDecorator.decorateBigIsland(location, buffer)
            }
            location.startsWith("choice.php") -> {
                StationaryButtonDecorator.decorate(location, buffer)
                addChoiceSpoilers(location, buffer)
                val choiceId = Regex("""whichchoice=(\d+)""")
                    .find(location)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Regex("""name=["']?whichchoice["']?[^>]*value=["']?(\d+)""", RegexOption.IGNORE_CASE)
                        .find(buffer)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (choiceId == 392) MemoriesDecorator.decorateElements(392, buffer)
                MemoriesDecorator.decorateElementsResponse(buffer)
                if (choiceId != null && choiceId in 1308..1313) {
                    ClanFortuneDecorator.decorateQuestion(buffer, preferences)
                    ClanFortuneDecorator.decorateAnswer(buffer, preferences)
                }
                if (buffer.indexOf("Symbology") >= 0 || buffer.indexOf("_villainLair") >= 0 ||
                    buffer.indexOf("Villain") >= 0
                ) {
                    VillainLairDecorator.decorate(buffer, preferences)
                }
                UseLinkDecorator.decorate(location, buffer, preferences)
            }
            location.startsWith("clan_hobopolis.php") -> {
                HobopolisDecorator.decorate(location, buffer)
            }
            location.startsWith("clan_viplounge.php") -> {
                ClanFortuneDecorator.decorateQuestion(buffer, preferences)
                ClanFortuneDecorator.decorateAnswer(buffer, preferences)
            }
            location.startsWith("fight.php") -> {
                FightDecorator.decorateEndOfFight(buffer)
                StationaryButtonDecorator.decorate(location, buffer)
                FightDecorator.decorateMonster(buffer)
                FightDecorator.decorateLocation(buffer)
                val monster = Regex("""<!-- Monster: ([^>]+) -->""")
                    .find(buffer)?.groupValues?.getOrNull(1)
                    ?: Regex("""<span id=["']monname["'][^>]*>([^<]+)</span>""", RegexOption.IGNORE_CASE)
                        .find(buffer)?.groupValues?.getOrNull(1)
                NemesisDecorator.decorateRaverFight(buffer, monster, preferences)
                UseLinkDecorator.decorate(location, buffer, preferences)
            }
            location.startsWith("inventory.php") || location.startsWith("inv_use.php") ||
                location.startsWith("craft.php") || location.startsWith("storage.php") -> {
                UseItemDecorator.decorate(location, buffer)
                UseLinkDecorator.decorate(location, buffer, preferences)
            }
            location.startsWith("mining.php") -> {
                MineDecorator.decorate(buffer)
            }
            location.startsWith("place.php") && location.contains("villain") -> {
                VillainLairDecorator.decorate(buffer, preferences)
            }
        }
    }

    fun applyGlobalAdjustments(
        location: String,
        buffer: StringBuilder,
        addComplexFeatures: Boolean,
        preferences: Preferences? = RequestEditorKit.preferences,
    ) {
        if (addComplexFeatures) {
            insertBefore(buffer, "</head>", """<script language="Javascript" src="/basics.js"></script>""")
            insertBefore(buffer, "</head>", """<link rel="stylesheet" href="/basics.1.css" />""")
            if (preferences?.getBoolean("relayAddsChatLinks", true) != false) {
                insertBefore(buffer, "</head>", """<script language="Javascript" src="/ircm_extend.4.js"></script>""")
            }
            if (preferences?.getBoolean("relayBrowserNoFocus", false) != true) {
                insertBefore(buffer, "</head>", """<script language="Javascript" src="/onfocus.1.js"></script>""")
            }
        }
        if (location.startsWith("charpane.php") || location.contains("menu.php")) return

        if (addComplexFeatures) {
            if (location.contains("fight.php")) {
                insertBefore(buffer, "</html>", """<script src="/combatfilter.1.js"></script>""")
                insertBefore(buffer, "</html>", """<script src="/macrohelper.6.js"></script>""")
            }
            if (location.contains("volcanomaze.php")) {
                insertBefore(buffer, "</html>", """<script src="/volcanomaze.5.js"></script>""")
            }
            if (location.contains("basement.php")) {
                insertBefore(buffer, "</html>", """<script src="/basement.js"></script>""")
            }
        }

        // Chat / CLI frame polish: ensure title + basics already present
        if (location.endsWith("chat.html") || location.endsWith("cli.html")) {
            if (buffer.indexOf("mafia-chat-cli") < 0) {
                insertBefore(buffer, "</body>", """<span class="mafia-chat-cli"></span>""")
            }
        }
    }

    fun addChoiceSpoilers(location: String, buffer: StringBuilder) {
        val choiceId = Regex("""whichchoice=(\d+)""")
            .find(location)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("""name=["']?whichchoice["']?[^>]*value=["']?(\d+)""", RegexOption.IGNORE_CASE)
                .find(buffer)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return
        val spoilers = ChoiceAdventures.choiceSpoilers(choiceId) ?: return
        val options = spoilers.options.mapNotNull { it?.name }.filter { it.isNotBlank() }
        if (options.isEmpty() && spoilers.name.isBlank()) return
        val html = buildString {
            append("""<div class="mafia-choice-spoilers" style="margin:4px;padding:4px;border:1px solid #888;background:#ffd;">""")
            append("<b>").append(spoilers.name.ifBlank { "Spoilers" }).append(":</b><ul>")
            options.forEach { append("<li>").append(it).append("</li>") }
            append("</ul></div>")
        }
        val formIdx = buffer.indexOf("<form")
        if (formIdx >= 0) {
            buffer.insert(formIdx, html)
        } else {
            insertBefore(buffer, "</body>", html)
        }
    }

    fun insertBefore(buffer: StringBuilder, marker: String, insertion: String) {
        val idx = buffer.indexOf(marker)
        if (idx >= 0) buffer.insert(idx, insertion)
    }

    fun replaceOnce(buffer: StringBuilder, old: String, new: String) {
        val idx = buffer.indexOf(old)
        if (idx >= 0) {
            buffer.replace(idx, idx + old.length, new)
        }
    }

    fun replaceAll(buffer: StringBuilder, old: String, new: String) {
        var idx = buffer.indexOf(old)
        while (idx >= 0) {
            buffer.replace(idx, idx + old.length, new)
            idx = buffer.indexOf(old, idx + new.length)
        }
    }
}

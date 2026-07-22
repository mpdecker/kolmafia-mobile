package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ConsequenceAction
import net.sourceforge.kolmafia.data.EffectEnchantmentParser
import net.sourceforge.kolmafia.data.ItemEnchantmentParser
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierExpression
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ConsequenceManager.fireAction] group/bracket resolution for consequence prefs. */
object ConsequenceActionResolver {

    private val GROUP_PATTERN = Regex("""\$(\d)""")
    private val EXPR_PATTERN = Regex("""\[(.+?)\]""")
    private val COMMA_NUMERIC = Regex("""^[\d,]+$""")

    data class Context(
        val ascensionNumber: Int = 0,
        val expressionContext: ExpressionContext = ExpressionContext.EMPTY,
    )

    fun resolveReplacement(template: String, match: MatchResult): String =
        substituteGroups(template, match)

    fun resolveValue(
        rawValue: String,
        match: MatchResult,
        expressionContext: ExpressionContext = ExpressionContext.EMPTY,
    ): String {
        var text = substituteGroups(rawValue, match)
        text = evaluateBracketExpressions(text, expressionContext)
        return text
    }

    fun fireAction(
        action: ConsequenceAction,
        match: MatchResult,
        preferences: Preferences,
        context: Context = Context(),
        itemSpec: String? = null,
        effectSpec: String? = null,
        html: String? = null,
    ) {
        when (action) {
            is ConsequenceAction.SetString -> {
                val value = match.groupValues.getOrNull(action.groupIndex) ?: return
                preferences.setString(action.key, value)
            }
            is ConsequenceAction.SetLiteral -> {
                val raw = resolveLiteralValue(action.value, match) ?: return
                val value = substituteGroups(raw, match)
                applyPref(preferences, action.key, value)
            }
            is ConsequenceAction.SetBoolean ->
                preferences.setBoolean(action.key, action.value)
            is ConsequenceAction.SetAscensions ->
                preferences.setInt(action.key, context.ascensionNumber)
            is ConsequenceAction.SetExpressionValue -> {
                val value = resolveValue(action.rawValue, match, context.expressionContext)
                applyPref(preferences, action.key, value)
            }
            is ConsequenceAction.SetItemMods -> {
                val spec = itemSpec ?: return
                val source = html ?: return
                val mods = ItemEnchantmentParser.parseItemEnchantments(source)
                ModifierDatabase.overrideModifier("Item", spec, mods)
                preferences.setString(action.key, mods)
            }
            is ConsequenceAction.SetEffectMods -> {
                val spec = effectSpec ?: return
                val source = html ?: return
                val mods = EffectEnchantmentParser.parseEffectEnchantments(source)
                ModifierDatabase.overrideModifier("Effect", spec, mods)
                preferences.setString(action.key, mods)
            }
            is ConsequenceAction.ReturnReplacement -> {
                // Name replacement is handled by MonsterConsequenceSync, not fireAction.
            }
        }
    }

    fun applyPref(preferences: Preferences, key: String, value: String) {
        val trimmed = value.trim()
        val asDouble = trimmed.toDoubleOrNull()
        if (asDouble != null && asDouble == asDouble.toLong().toDouble()) {
            preferences.setInt(key, asDouble.toLong().toInt())
        } else {
            preferences.setString(key, trimmed)
        }
    }

    internal fun resolveLiteralValue(value: String, match: MatchResult): String? = when (value) {
        "monstername" -> {
            val id = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
            MonsterDatabase.getById(id)?.name
        }
        else -> value
    }

    internal fun substituteGroups(text: String, match: MatchResult): String {
        return GROUP_PATTERN.replace(text) { m ->
            val group = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            match.groupValues.getOrNull(group) ?: ""
        }
    }

    internal fun evaluateBracketExpressions(
        text: String,
        expressionContext: ExpressionContext = ExpressionContext.EMPTY,
    ): String {
        val buff = StringBuilder()
        var lastIndex = 0
        for (m in EXPR_PATTERN.findAll(text)) {
            buff.append(text, lastIndex, m.range.first)
            val inner = m.groupValues[1]
            val evalInner = if (COMMA_NUMERIC.matches(inner)) inner.replace(",", "") else inner
            val result = ModifierExpression.evaluate("[$evalInner]", expressionContext)
            val replacement = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                result.toString()
            }
            buff.append(replacement)
            lastIndex = m.range.last + 1
        }
        buff.append(text.substring(lastIndex))
        return buff.toString()
    }
}

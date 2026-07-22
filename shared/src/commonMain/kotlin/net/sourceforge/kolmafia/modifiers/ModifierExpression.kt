package net.sourceforge.kolmafia.modifiers

import kotlin.math.*
import net.sourceforge.kolmafia.util.RomanNumerals

/**
 * Recursive-descent evaluator for KoLmafia modifier expressions.
 *
 * Grammar (subset of desktop's expression language):
 *   expr   ::= add_sub
 *   add_sub::= mul_div (('+' | '-') mul_div)*
 *   mul_div::= pow    (('*' | '/') pow)*
 *   pow    ::= unary  ('^' unary)?
 *   unary  ::= '-'? atom
 *   atom   ::= '(' expr ')' | number | variable | function_call
 *   variable ::= single uppercase letter
 *   function_call ::= name '(' args... ')'
 *
 * Numeric functions: min, max, ceil, floor, abs, sqrt, roman
 * String-arg functions: effect, skill, pref, loc, zone, env, path, class, fam,
 *                       famattr, mainhand, res, mod, interact
 */
class ModifierExpression(private val src: String) {
    private var pos = 0

    fun evaluate(ctx: ExpressionContext): Double =
        try { parseAddSub(ctx) } catch (_: Exception) { 0.0 }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun peek(): Char? = if (pos < src.length) src[pos] else null
    private fun spaces() { while (pos < src.length && src[pos] == ' ') pos++ }

    private fun parseAddSub(ctx: ExpressionContext): Double {
        var v = parseMulDiv(ctx)
        spaces()
        while (true) v = when (peek()) {
            '+' -> { pos++; v + parseMulDiv(ctx) }
            '-' -> { pos++; v - parseMulDiv(ctx) }
            else -> return v
        }.also { spaces() }
    }

    private fun parseMulDiv(ctx: ExpressionContext): Double {
        var v = parsePow(ctx)
        spaces()
        while (true) v = when (peek()) {
            '*' -> { pos++; v * parsePow(ctx) }
            '/' -> { pos++; val d = parsePow(ctx); if (d != 0.0) v / d else 0.0 }
            else -> return v
        }.also { spaces() }
    }

    private fun parsePow(ctx: ExpressionContext): Double {
        val base = parseUnary(ctx)
        spaces()
        return if (peek() == '^') { pos++; base.pow(parseUnary(ctx)) } else base
    }

    private fun parseUnary(ctx: ExpressionContext): Double {
        spaces()
        return if (peek() == '-') { pos++; -parseAtom(ctx) } else parseAtom(ctx)
    }

    private fun parseAtom(ctx: ExpressionContext): Double {
        spaces()
        return when {
            peek() == '(' -> {
                pos++
                val v = parseAddSub(ctx)
                spaces()
                if (peek() == ')') pos++
                v
            }
            peek()?.isDigit() == true ||
                (peek() == '.' && src.getOrNull(pos + 1)?.isDigit() == true) ->
                parseNumber()
            peek()?.isLetter() == true -> parseWord(ctx)
            else -> { if (pos < src.length) pos++; 0.0 }
        }
    }

    private fun parseNumber(): Double {
        val start = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
        return src.substring(start, pos).toDoubleOrNull() ?: 0.0
    }

    private fun parseWord(ctx: ExpressionContext): Double {
        val start = pos
        while (pos < src.length && (src[pos].isLetterOrDigit() || src[pos] == '_')) pos++
        val word = src.substring(start, pos)

        // Single uppercase letter = context variable
        if (word.length == 1 && word[0].isUpperCase()) return ctx.variable(word[0])

        // Multi-letter monster tokens (KW/KV/KC) before function-call check
        ctx.multiLetterVariable(word)?.let { return it }

        // Must be followed by '(' to be a function
        spaces()
        if (peek() != '(') return 0.0
        pos++  // consume '('

        val result = callFunction(word.lowercase(), ctx)
        spaces()
        if (peek() == ')') pos++
        return result
    }

    private fun callFunction(name: String, ctx: ExpressionContext): Double = when (name) {
        // ── Arithmetic — numeric args ─────────────────────────────────────────
        "min"    -> { val a = parseAddSub(ctx); skipComma(); val b = parseAddSub(ctx); min(a, b) }
        "max"    -> { val a = parseAddSub(ctx); skipComma(); val b = parseAddSub(ctx); max(a, b) }
        "ceil"   -> ceil(parseAddSub(ctx))
        "floor"  -> floor(parseAddSub(ctx))
        "abs"    -> abs(parseAddSub(ctx))
        "sqrt"   -> sqrt(parseAddSub(ctx).coerceAtLeast(0.0))
        "lt"     -> { val a = parseAddSub(ctx); skipComma(); val b = parseAddSub(ctx); compare(a, b) { x, y -> x < y } }
        "lte"    -> { val a = parseAddSub(ctx); skipComma(); val b = parseAddSub(ctx); compare(a, b) { x, y -> x <= y } }
        "gt"     -> { val a = parseAddSub(ctx); skipComma(); val b = parseAddSub(ctx); compare(a, b) { x, y -> x > y } }
        "gte"    -> { val a = parseAddSub(ctx); skipComma(); val b = parseAddSub(ctx); compare(a, b) { x, y -> x >= y } }
        "eq"     -> { val a = parseAddSub(ctx); skipComma(); val b = parseAddSub(ctx); compare(a, b) { x, y -> x == y } }

        // ── Game-state — string args (unquoted) ───────────────────────────────
        "effect"   -> ctx.effectTurns(readStringArg())
        "skill"    -> if (ctx.hasSkill(readStringArg())) 1.0 else 0.0
        "equipped" -> ctx.equippedValue(readStringArg())
        "pref"     -> {
            val prefName = readStringArg()
            val contains = if (peek() == ',') {
                skipComma()
                readStringArg()
            } else {
                null
            }
            ctx.prefValue(prefName, contains)
        }
        "loc"      -> if (ctx.locContains(readStringArg())) 1.0 else 0.0
        "zone"     -> if (ctx.zoneContains(readStringArg())) 1.0 else 0.0
        "env"      -> if (ctx.envContains(readStringArg())) 1.0 else 0.0
        "path"     -> if (ctx.pathContains(readStringUntilCloseParen())) 1.0 else 0.0
        "class"    -> if (ctx.classContains(readStringArg())) 1.0 else 0.0
        "interact" -> if (!ctx.isRestricted) 1.0 else 0.0
        "mod" -> {
            val stat = readStringArg()
            val item = if (peek() == ',') { skipComma(); readStringArg() } else null
            ctx.modValue(stat, item?.ifBlank { null })
        }
        "fam" -> ctx.famValue(readStringArg())
        "famattr" -> ctx.famattrValue(readStringArg())
        "mainhand" -> ctx.mainhandValue(readStringArg())
        "res" -> ctx.resValue(readStringArg())
        "stripcommas" -> readStringArg().replace(",", "").toDoubleOrNull() ?: 0.0
        "roman" -> RomanNumerals.parse(readStringArg()).toDouble()
        else -> { readStringArg(); 0.0 }
    }

    private fun skipComma() { spaces(); if (peek() == ',') pos++; spaces() }

    private fun compare(a: Double, b: Double, predicate: (Double, Double) -> Boolean): Double =
        if (predicate(a, b)) 1.0 else 0.0

    /** Reads an unquoted string arg up to the next unbalanced ')' or ','. */
    private fun readStringArg(): String {
        spaces()
        // Strip surrounding quotes if present
        if (peek() == '"') {
            pos++
            val start = pos
            while (pos < src.length && src[pos] != '"') pos++
            val s = src.substring(start, pos)
            if (pos < src.length) pos++  // consume closing "
            return s
        }
        val sb = StringBuilder()
        var depth = 0
        while (pos < src.length) {
            when {
                src[pos] == '(' -> { depth++; sb.append(src[pos++]) }
                src[pos] == ')' -> if (depth > 0) { depth--; sb.append(src[pos++]) } else break
                src[pos] == ',' && depth == 0 -> break
                else -> sb.append(src[pos++])
            }
        }
        return sb.toString().trim()
    }

    /** Desktop path()/class()/env() args may contain commas — read until closing ')'. */
    private fun readStringUntilCloseParen(): String {
        spaces()
        val sb = StringBuilder()
        while (pos < src.length && src[pos] != ')') {
            sb.append(src[pos++])
        }
        return sb.toString().trim()
    }

    companion object {
        /** Evaluate a bracketed expression string like "[3+3*min(1,effect(X))]". */
        fun evaluate(bracketedExpr: String, ctx: ExpressionContext): Double {
            val inner = bracketedExpr.trim().removePrefix("[").removeSuffix("]")
            return ModifierExpression(inner).evaluate(ctx)
        }
    }
}

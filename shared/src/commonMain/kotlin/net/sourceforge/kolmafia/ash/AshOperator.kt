package net.sourceforge.kolmafia.ash

import kotlin.math.pow

enum class AshOperator(val symbol: String) {
    ADD("+"), SUB("-"), MUL("*"), DIV("/"), REM("%"), POW("**"),
    EQ("=="), NEQ("!="), LT("<"), LE("<="), GT(">"), GE(">="),
    AND("&&"), OR("||"), NOT("!"),
    BAND("&"), BOR("|"), BXOR("^"), BNOT("~"),
    NEGATE("-"),
    PRE_INC("++"), PRE_DEC("--"), POST_INC("++"), POST_DEC("--");

    fun apply(left: AshValue, right: AshValue): AshValue {
        val bothNumeric = (left.type == AshType.INT || left.type == AshType.FLOAT) &&
                          (right.type == AshType.INT || right.type == AshType.FLOAT)
        val useFloat = bothNumeric &&
                       (left.type == AshType.FLOAT || right.type == AshType.FLOAT)
        return when (this) {
            ADD -> when {
                left.type == AshType.STRING || right.type == AshType.STRING ->
                    AshValue.of(left.toString() + right.toString())
                useFloat -> AshValue.of(left.toDouble() + right.toDouble())
                else -> AshValue.of(left.toLong() + right.toLong())
            }
            SUB -> if (useFloat) AshValue.of(left.toDouble() - right.toDouble())
                   else AshValue.of(left.toLong() - right.toLong())
            MUL -> if (useFloat) AshValue.of(left.toDouble() * right.toDouble())
                   else AshValue.of(left.toLong() * right.toLong())
            DIV -> if (useFloat) {
                if (right.toDouble() == 0.0) throw ScriptException("Division by zero")
                AshValue.of(left.toDouble() / right.toDouble())
            } else {
                if (right.toLong() == 0L) throw ScriptException("Division by zero")
                AshValue.of(left.toLong() / right.toLong())
            }
            REM -> if (useFloat) {
                AshValue.of(left.toDouble() % right.toDouble())
            } else {
                if (right.toLong() == 0L) throw ScriptException("Modulo by zero")
                AshValue.of(left.toLong() % right.toLong())
            }
            POW -> AshValue.of(left.toDouble().pow(right.toDouble()))
            EQ -> {
                val bothNumericTypes = (left.type == AshType.INT || left.type == AshType.FLOAT) &&
                                       (right.type == AshType.INT || right.type == AshType.FLOAT)
                val bothBool = left.type == AshType.BOOLEAN && right.type == AshType.BOOLEAN
                when {
                    bothNumericTypes -> AshValue.of(left.toDouble() == right.toDouble())
                    bothBool -> AshValue.of(left.toBoolean() == right.toBoolean())
                    else -> AshValue.of(left.toString() == right.toString())
                }
            }
            NEQ -> {
                val bothNumericTypes = (left.type == AshType.INT || left.type == AshType.FLOAT) &&
                                       (right.type == AshType.INT || right.type == AshType.FLOAT)
                val bothBool = left.type == AshType.BOOLEAN && right.type == AshType.BOOLEAN
                when {
                    bothNumericTypes -> AshValue.of(left.toDouble() != right.toDouble())
                    bothBool -> AshValue.of(left.toBoolean() != right.toBoolean())
                    else -> AshValue.of(left.toString() != right.toString())
                }
            }
            LT -> numericCompare(left, right) { a, b -> a < b }
            LE -> numericCompare(left, right) { a, b -> a <= b }
            GT -> numericCompare(left, right) { a, b -> a > b }
            GE -> numericCompare(left, right) { a, b -> a >= b }
            AND -> AshValue.of(left.toBoolean() && right.toBoolean())
            OR -> AshValue.of(left.toBoolean() || right.toBoolean())
            BAND -> AshValue.of(left.toLong() and right.toLong())
            BOR -> AshValue.of(left.toLong() or right.toLong())
            BXOR -> AshValue.of(left.toLong() xor right.toLong())
            else -> throw ScriptException("Operator $symbol is not binary")
        }
    }

    fun applyUnary(operand: AshValue): AshValue = when (this) {
        NOT -> AshValue.of(!operand.toBoolean())
        NEGATE -> when (operand.type) {
            AshType.FLOAT -> AshValue.of(-operand.toDouble())
            else -> AshValue.of(-operand.toLong())
        }
        BNOT -> AshValue.of(operand.toLong().inv())
        PRE_INC, POST_INC -> when (operand.type) {
            AshType.FLOAT -> AshValue.of(operand.toDouble() + 1.0)
            else -> AshValue.of(operand.toLong() + 1L)
        }
        PRE_DEC, POST_DEC -> when (operand.type) {
            AshType.FLOAT -> AshValue.of(operand.toDouble() - 1.0)
            else -> AshValue.of(operand.toLong() - 1L)
        }
        else -> throw ScriptException("Operator $symbol is not unary")
    }

    private fun numericCompare(
        left: AshValue, right: AshValue, cmp: (Double, Double) -> Boolean
    ): AshValue = AshValue.of(cmp(left.toDouble(), right.toDouble()))
}

package net.sourceforge.kolmafia.request

/**
 * Desktop [DwarfFactoryRequest.DwarfNumberTranslator] — base-7 digit cipher solver
 * (Phases 2646–2660).
 */
class DwarfNumberTranslator(digits: String = "-------") {
    private val digitMap = mutableMapOf<Char, Int>()
    private val charMap = mutableMapOf<Int, Char>()
    private val numbers = mutableListOf<String>()
    private val knownDigits = mutableListOf<Char>()
    private val rolls = mutableListOf<String>()
    private val permutations = mutableSetOf<String>()
    private var numberCount = 0
    private var rollCount = 0

    init {
        for (i in digits.indices) {
            if (i >= 7) break
            val digit = digits[i]
            if (digit.isLetter()) mapCharacter(digit.uppercaseChar(), i)
        }
    }

    private fun mapCharacter(code: Char, value: Int) {
        digitMap[code] = value
        charMap[value] = code
    }

    fun digitString(): String = buildString {
        for (i in 0 until 7) {
            append(charMap[i] ?: '-')
        }
    }

    fun valid(): Boolean = digitMap.size == 7

    fun parseNumber(string: String): Int {
        var number = 0
        for (ch in string) {
            val value = digitMap[ch] ?: return -1
            number = number * 7 + value
        }
        return number
    }

    private fun addNewDigit(ch: Char) {
        if (ch !in knownDigits) knownDigits.add(ch)
    }

    fun addNumber(number: String) {
        if (numbers.any { it == number }) return
        numbers.add(number)
        for (ch in number) addNewDigit(ch)
    }

    fun analyzeNumbers() {
        if (numbers.size == numberCount) return
        numberCount = numbers.size

        val matches = Array(2) { CharArray(8) }
        val counts = IntArray(2)

        for (value in numbers) {
            if (value.length < 3) continue
            val d1 = value[0]
            val d2 = value[1]
            val d3 = value[2]
            var off = 0
            for (j in matches.indices) {
                val match = matches[j][0]
                if (match == 0.toChar() || match == d1) {
                    off = j
                    break
                }
            }
            val digits = matches[off]
            digits[0] = d1
            for (k in 1 until digits.size) {
                val match = digits[k]
                if (match == d2) break
                if (match == 0.toChar()) {
                    digits[k] = d2
                    counts[off]++
                    break
                }
            }
            for (k in 1 until digits.size) {
                val match = digits[k]
                if (match == d3) break
                if (match == 0.toChar()) {
                    digits[k] = d3
                    counts[off]++
                    break
                }
            }
        }

        val oneOffset = when {
            counts[0] >= 3 -> 0
            counts[1] >= 3 -> 1
            else -> -1
        }
        if (oneOffset == -1) return
        mapCharacter(matches[oneOffset][0], 1)
        val twoOffset = 1 - oneOffset
        if (counts[twoOffset] == 0) return
        mapCharacter(matches[twoOffset][0], 2)
        mapCharacter(matches[twoOffset][1], 0)
    }

    fun addRoll(roll: String) {
        if (roll.length != 8) return
        if (rolls.any { it == roll }) return
        addNewDigit(roll[0])
        addNewDigit(roll[1])
        addNewDigit(roll[3])
        addNewDigit(roll[4])
        rolls.add(roll)
    }

    fun analyzeRolls() {
        if (rolls.size == rollCount) return
        rollCount = rolls.size
        matchDigitPermutations()
    }

    private fun matchDigitPermutations() {
        if (digitMap.size == 7) return
        if (knownDigits.size != 7) return
        if (permutations.isEmpty()) generatePermutations("")
        for (roll in rolls) {
            if (permutations.size <= 1) break
            checkPermutations(roll)
        }
        if (permutations.size == 1) saveSoloPermutation()
    }

    private fun generatePermutations(prefix: String) {
        val index = prefix.length
        if (index == 7) {
            permutations.add(prefix)
            return
        }
        charMap[index]?.let {
            generatePermutations(prefix + it)
            return
        }
        for (i in 0 until 7) {
            val rune = knownDigits[i]
            if (prefix.indexOf(rune) != -1) continue
            val known = digitMap[rune]
            if (known != null && known != index) continue
            generatePermutations(prefix + rune)
        }
    }

    private fun checkPermutations(roll: String) {
        if (roll.length != 8) return
        val d1 = roll[0]
        val d2 = roll[1]
        val d3 = roll[3]
        val d4 = roll[4]
        val high = roll[6] - '0'
        val low = roll[7] - '0'
        val value = high * 7 + low
        permutations.removeAll { !validPermutation(it, d1, d2, d3, d4, value) }
    }

    private fun validPermutation(
        permutation: String,
        d1: Char,
        d2: Char,
        d3: Char,
        d4: Char,
        value: Int,
    ): Boolean {
        val i1 = permutation.indexOf(d1)
        val total = if (d1 == d2 && i1 == 0) {
            49
        } else {
            val i2 = permutation.indexOf(d2)
            i1 * 7 + i2
        }
        val i3 = permutation.indexOf(d3)
        val i4 = permutation.indexOf(d4)
        return total - (i3 * 7 + i4) == value
    }

    private fun saveSoloPermutation() {
        val digits = permutations.firstOrNull() ?: return
        for (i in 0 until 7) mapCharacter(digits[i], i)
    }
}

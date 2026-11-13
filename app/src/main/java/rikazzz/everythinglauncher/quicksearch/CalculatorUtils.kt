package rikazzz.everythinglauncher.quicksearch

import java.util.Locale

/**
 * Math expression evaluator based on quick-search.
 * Handles +, -, *, /, (), %, decimals, and percentage phrases like "15% of 200" or "20% off 50".
 */
object CalculatorUtils {
    private val percentPhraseRegex =
        Regex(
            pattern = """^\s*([+-]?\d+(?:\.\d+)?)\s*%\s*(off|of)\s*([+-]?\d+(?:\.\d+)?)\s*$""",
            option = RegexOption.IGNORE_CASE
        )

    private val letterMultiplyRegex = Regex("""(?<=[\d.)])\s*[xX]\s*(?=[\d.(])""")

    fun isMathExpression(query: String): Boolean {
        val trimmed = normalizeLetterMultiply(query.trim())
        if (trimmed.isEmpty()) return false
        if (isPercentPhrase(trimmed)) return true

        val hasOperator =
            trimmed.contains('+') ||
                trimmed.contains('-') ||
                trimmed.contains('*') ||
                trimmed.contains('/') ||
                trimmed.contains('×') ||
                trimmed.contains('÷') ||
                trimmed.contains('·') ||
                trimmed.contains('(') ||
                trimmed.contains(')') ||
                trimmed.contains('%')

        if (!hasOperator) return false

        val validChars =
            trimmed.all { char ->
                char.isDigit() ||
                    char == '+' ||
                    char == '-' ||
                    char == '*' ||
                    char == '/' ||
                    char == '(' ||
                    char == ')' ||
                    char == '.' ||
                    char == ' ' ||
                    char == '×' ||
                    char == '÷' ||
                    char == '·' ||
                    char == '%'
            }

        return validChars && trimmed.length >= 2
    }

    fun evaluateExpression(expression: String): String? =
        try {
            evaluatePercentPhrase(expression)?.let { percentResult ->
                return formatResult(percentResult)
            }
            val cleaned = cleanExpression(expression)
            val result = evaluate(cleaned)
            formatResult(result)
        } catch (e: Exception) {
            null
        }

    private fun normalizeLetterMultiply(expression: String): String = letterMultiplyRegex.replace(expression, "*")

    private fun isPercentPhrase(query: String): Boolean = percentPhraseRegex.matches(query)

    private fun evaluatePercentPhrase(expression: String): Double? {
        val match = percentPhraseRegex.matchEntire(expression) ?: return null
        val percent = match.groupValues[1].toDoubleOrNull() ?: return null
        val operation = match.groupValues[2].lowercase(Locale.US)
        val baseValue = match.groupValues[3].toDoubleOrNull() ?: return null

        val percentValue = baseValue * (percent / 100.0)
        return when (operation) {
            "off" -> baseValue - percentValue
            "of" -> percentValue
            else -> null
        }
    }

    private fun cleanExpression(expression: String): String =
        normalizeLetterMultiply(expression)
            .replace("×", "*")
            .replace("÷", "/")
            .replace("·", "*")
            .replace(" ", "")
            .trim()

    private fun evaluate(expression: String): Double {
        var index = 0

        fun parseNumber(): Double {
            var numStr = ""
            while (index < expression.length &&
                (expression[index].isDigit() || expression[index] == '.')
            ) {
                numStr += expression[index]
                index++
            }
            return numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number")
        }

        lateinit var parseExpression: () -> Double

        fun parseFactor(): Double {
            if (index >= expression.length) {
                throw IllegalArgumentException("Unexpected end of expression")
            }

            return when (expression[index]) {
                '(' -> {
                    index++
                    val result = parseExpression()
                    if (index >= expression.length || expression[index] != ')') {
                        throw IllegalArgumentException("Missing closing parenthesis")
                    }
                    index++
                    result
                }
                '-' -> {
                    index++
                    -parseFactor()
                }
                '+' -> {
                    index++
                    parseFactor()
                }
                else -> parseNumber()
            }
        }

        fun parseTerm(): Double {
            var result = parseFactor()
            while (index < expression.length) {
                when (expression[index]) {
                    '*' -> {
                        index++
                        result *= parseFactor()
                    }
                    '/' -> {
                        index++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        result /= divisor
                    }
                    else -> break
                }
            }
            return result
        }

        parseExpression = {
            var result = parseTerm()
            while (index < expression.length) {
                when (expression[index]) {
                    '+' -> {
                        index++
                        result += parseTerm()
                    }
                    '-' -> {
                        index++
                        result -= parseTerm()
                    }
                    else -> break
                }
            }
            result
        }

        val result = parseExpression()
        if (index < expression.length) {
            throw IllegalArgumentException("Unexpected character at position $index")
        }
        return result
    }

    private fun formatResult(result: Double): String {
        val rounded = String.format(Locale.US, "%.4f", result)
        return rounded.trimEnd('0').trimEnd('.')
    }
}

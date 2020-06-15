package calculator.lexer.Token

abstract class UnaryOperatorToken: OperatorToken() {
    abstract val verbose: String
    companion object {
        private val UNARYMINUS = UnaryMinus()
        private val UNARYPLUS = UnaryPlus()
        private val SIN = Sin()

        private val OPERATORS: Array<Token> = arrayOf(UNARYMINUS, UNARYPLUS, SIN)

        // Checks if the passed string is a binary operator
        fun assert(string: String): Boolean = OPERATORS.any { it.value == string }

        fun acquire(string: String): Token = OPERATORS.first { it.value == string }
    }
}

data class Sin(override val value: String = "sin", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "SIN"): UnaryOperatorToken()
data class UnaryMinus(override val value: String = "-", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "-"): UnaryOperatorToken()
data class UnaryPlus(override val value: String = "+", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = ""): UnaryOperatorToken()
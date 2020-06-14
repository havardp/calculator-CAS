package calculator.lexer

// TODO: add constant token class
// TODO: Add missing data classes
abstract class Token{
    abstract val value: String
}

abstract class ParenthesisToken: OperatorToken(){
    abstract override val value: String
    companion object {
        private val LPAR = LeftParenthesisToken()
        private val RPAR = RightParenthesisToken()

        private val OPERATORS: Array<Token> = arrayOf(LPAR, RPAR)

        // Checks if the passed string is a binary operator
        fun assert(string: String): Boolean = OPERATORS.any { it.value == string }

        fun acquire(string: String): Token = OPERATORS.first { it.value == string }
    }
}
data class LeftParenthesisToken(override val value: String = "(", override val precedence: Int = -1): ParenthesisToken()
data class RightParenthesisToken(override val value: String = ")", override val precedence: Int = -1): ParenthesisToken()

abstract class OperatorToken: Token(){
    abstract val precedence: Int
}

abstract class BinaryOperatorToken: OperatorToken() {
    abstract val verbose: String
    companion object {
        private val PLUS = Plus()
        private val MULT = Mult()

        private val OPERATORS: Array<Token> = arrayOf(
                PLUS,
                MULT
        )

        // Checks if the passed string is a binary operator
        fun assert(string: String): Boolean = OPERATORS.any { it.value == string }

        fun acquire(string: String): Token = OPERATORS.first { it.value == string }
        // ^ = 2
        // + - = 0
    }
}
data class Plus(override val value: String = "+", override val precedence: Int = 0, override val verbose: String = "SUM"): BinaryOperatorToken()
data class Mult(override val value: String = "*", override val precedence: Int = 1, override val verbose: String = "MULTIPLY"): BinaryOperatorToken()

data class OperandToken(override val value: String): Token() {
    companion object {
        // Checks if the passed string is an operand
        fun assert(str: String) : Boolean = str.toDoubleOrNull() is Double
    }
}

data class VariableToken(override val value: String): Token() {
    companion object {
        // Checks if the passed string is an operand
        fun assert(str: String) : Boolean = str == "x"
    }
}

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

data class EOFToken(override val value: String): Token()
package calculator.lexer.Token

abstract class UnaryOperatorToken: OperatorToken() {
    abstract val verbose: String
    companion object {
        private val UNARYMINUS = UnaryMinus()
        private val UNARYPLUS = UnaryPlus()
        private val SIN = Sin()
        private val ARCSIN = ArcSin()
        private val COS = Cos()
        private val ARCCOS = ArcCos()
        private val TAN = Tan()
        private val ARCTAN = ArcTan()
        private val SQRT = Sqrt()
        private val ABS = Abs()
        private val DEG = Deg()
        private val RAD = Rad()
        private val CEIL = Ceil()
        private val FLOOR = Floor()
        private val ROUND = Round()

        private val OPERATORS: Array<Token> = arrayOf(
                UNARYMINUS,
                UNARYPLUS,
                SIN,
                ARCSIN,
                COS,
                ARCCOS,
                TAN,
                ARCTAN,
                SQRT,
                ABS,
                DEG,
                RAD,
                CEIL,
                FLOOR,
                ROUND
        )

        // Checks if the passed string is a binary operator
        fun assert(string: String): Boolean = OPERATORS.any { it.value == string }

        fun acquire(string: String): Token = OPERATORS.first { it.value == string }
    }
}

data class UnaryMinus(override val value: String = "-", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "-"): UnaryOperatorToken()
data class UnaryPlus(override val value: String = "+", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = ""): UnaryOperatorToken()

data class Sin(override val value: String = "sin", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "SIN"): UnaryOperatorToken()
data class ArcSin(override val value: String = "arcsin", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "ARCSIN"): UnaryOperatorToken()
data class Cos(override val value: String = "cos", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "COS"): UnaryOperatorToken()
data class ArcCos(override val value: String = "arccos", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "ARCCOS"): UnaryOperatorToken()
data class Tan(override val value: String = "tan", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "TAN"): UnaryOperatorToken()
data class ArcTan(override val value: String = "arctan", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "ARCTAN"): UnaryOperatorToken()

data class Sqrt(override val value: String = "sqrt", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "SQRT"): UnaryOperatorToken()
data class Abs(override val value: String = "abs", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "ABS"): UnaryOperatorToken()
data class Deg(override val value: String = "deg", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "TO_DEGREES"): UnaryOperatorToken()
data class Rad(override val value: String = "rad", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "TO_RADIANS"): UnaryOperatorToken()
data class Ceil(override val value: String = "ceil", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "CEIL"): UnaryOperatorToken()
data class Floor(override val value: String = "floor", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "FLOOR"): UnaryOperatorToken()
data class Round(override val value: String = "round", override val precedence: Int = Int.MAX_VALUE, override val verbose: String = "ROUND"): UnaryOperatorToken()
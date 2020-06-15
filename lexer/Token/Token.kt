package calculator.lexer.Token

// TODO: add constant token class
// TODO: Add missing data classes
abstract class Token{
    abstract val value: String
}

// parent of parenthesis, binary operator and unary operator
abstract class OperatorToken: Token(){
    abstract val precedence: Int
}

data class EOFToken(override val value: String): Token()
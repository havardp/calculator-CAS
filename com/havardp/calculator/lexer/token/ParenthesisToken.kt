package com.havardp.calculator.lexer.token

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
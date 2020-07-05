package com.havardp.calculator.lexer.token

data class OperandToken(override val value: String): Token() {
    companion object {
        // Checks if the passed string is an operand
        fun assert(str: String) : Boolean = str.toDoubleOrNull() is Double || str == "e" || str == "pi"
    }
}
package calculator.lexer

import com.example.linearmath.expressionCalculator.NotAnOperatorException

// TODO: add constant token class
// TODO: Maybe do tokens as in old code, with a seperate token class for each operator/operand, and operate method in them
abstract class Token(val value: String, val precedence: Int? = null){
    override fun toString() : String {
        return value
    }
}

class ParenthesisToken(value: String, precedence: Int): Token(value, precedence) {
    companion object {
        // Checks if the passed string is a parenthesis operator
        fun assert(str: String) : Boolean {
            if (str == "(" || str == ")") return true
            return false
        }
    }
}

class BinaryOperatorToken(value: String, precedence: Int): Token(value, precedence) {
    companion object {
        // Checks if the passed string is a binary operator
        fun assert(str: String): Boolean {
            if (str matches Regex("[+\\-*/^]")) return true
            return false
        }

        fun precedence(str: String): Int {
            if (str == "*" || str == "/") return 1
            if (str == "+" || str == "-") return 0
            if (str == "^") return 2
            throw NotAnOperatorException("Tried to get precedence of BinaryOperatorToken, but the string parameter matches none of those operators.")
        }
    }
}

class OperandToken(value: String): Token(value) {
    companion object {
        // Checks if the passed string is an operand
        fun assert(str: String) : Boolean {
            if(str.toDoubleOrNull() is Double) return true
            return false
        }
    }
}

class VariableToken(value: String): Token(value) {
    companion object {
        // Checks if the passed string is an operand
        fun assert(str: String) : Boolean {
            if(str == "x" || str == "y") return true
            return false
        }
    }
}

class UnaryOperatorToken(value: String, precedence: Int): Token(value, precedence) {
    companion object {
        // Checks if the passed string is a unary operator
        // TODO: add more functions
        fun assert(str: String) : Boolean {
            if (str matches Regex("\\+|-|sin|cos")) return true
            return false
        }
    }
}

class EOFToken(value: String): Token(value)
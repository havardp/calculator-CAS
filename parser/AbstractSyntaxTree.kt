package calculator.parser

import calculator.lexer.Token

abstract class AbstractSyntaxTree(val token: Token)

class BinaryOperatorNode(token: Token, private val left: AbstractSyntaxTree, private val right: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token($left, $right)"
    }
}

class UnaryOperatorNode(token: Token, private val middle: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token($middle)"
    }
}

class OperandNode(token: Token): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token"
    }
}

class VariableNode(token: Token): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token"
    }
}


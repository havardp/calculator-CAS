package calculator.parser

import calculator.interpreter.Visitor
import calculator.lexer.Token

abstract class AbstractSyntaxTree(val token: Token){
    abstract override fun toString(): String
    abstract fun accept(visitor: Visitor)
}

class BinaryOperatorNode(token: Token, val left: AbstractSyntaxTree, val right: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token($left, $right)"
    }
    override fun accept(visitor: Visitor){
        visitor.visit(this)
    }
}

class UnaryOperatorNode(token: Token, val middle: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token($middle)"
    }
    override fun accept(visitor: Visitor){
        visitor.visit(this)
    }
}

class OperandNode(token: Token): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token"
    }
    override fun accept(visitor: Visitor){
        visitor.visit(this)
    }
}

class VariableNode(token: Token): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "$token"
    }
    override fun accept(visitor: Visitor){
        visitor.visit(this)
    }
}


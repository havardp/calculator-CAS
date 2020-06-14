package calculator.parser

import com.example.expressionCalculator.calculator.interpreter.NodeVisitor
import calculator.lexer.Token

abstract class AbstractSyntaxTree(val token: Token){
    abstract override fun toString(): String
    abstract fun accept(visitor: NodeVisitor)
}

class BinaryOperatorNode(token: Token, val left: AbstractSyntaxTree, val right: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "${token.value}($left, $right)"
    }
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}

class UnaryOperatorNode(token: Token, val middle: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun toString() : String {
        return "${token.value}($middle)"
    }
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}

class OperandNode(token: Token): AbstractSyntaxTree(token){
    override fun toString() : String {
        return token.value
    }
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}

class VariableNode(token: Token): AbstractSyntaxTree(token){
    override fun toString() : String {
        return token.value
    }
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}


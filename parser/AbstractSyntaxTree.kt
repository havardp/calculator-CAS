package calculator.parser

import calculator.interpreter.NodeVisitor
import calculator.lexer.Token.Token

abstract class AbstractSyntaxTree(val token: Token){
    abstract fun accept(visitor: NodeVisitor)
}

class BinaryOperatorNode(token: Token, val left: AbstractSyntaxTree, val right: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}

class UnaryOperatorNode(token: Token, val middle: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}

class OperandNode(token: Token): AbstractSyntaxTree(token){
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}

class VariableNode(token: Token): AbstractSyntaxTree(token){
    override fun accept(visitor: NodeVisitor){
        visitor.visit(this)
    }
}


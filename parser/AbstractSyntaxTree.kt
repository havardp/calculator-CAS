package calculator.parser

import calculator.interpreter.NodeVisitor.NodeVisitor
import calculator.interpreter.NodeVisitor.PrettyPrintVisitor
import calculator.interpreter.NodeVisitor.PrintGraphTreeVisitor
import calculator.interpreter.NodeVisitor.RewriteVisitor
import calculator.lexer.Token.Token

abstract class AbstractSyntaxTree(val token: Token){
    abstract fun accept(visitor: RewriteVisitor): AbstractSyntaxTree // the rewrite visitor which recursively returns the nodes to build the tree
    abstract fun accept(visitor: PrettyPrintVisitor): String
    abstract fun accept(visitor: PrintGraphTreeVisitor) // all other visitors which doesnt return anything
}

class BinaryOperatorNode(token: Token, val left: AbstractSyntaxTree, val right: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun accept(visitor: RewriteVisitor): AbstractSyntaxTree{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrettyPrintVisitor): String{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrintGraphTreeVisitor){
        visitor.visit(this)
    }
}

class UnaryOperatorNode(token: Token, val middle: AbstractSyntaxTree): AbstractSyntaxTree(token){
    override fun accept(visitor: RewriteVisitor): AbstractSyntaxTree{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrettyPrintVisitor): String{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrintGraphTreeVisitor){
        visitor.visit(this)
    }
}

class OperandNode(token: Token): AbstractSyntaxTree(token){
    override fun accept(visitor: RewriteVisitor): AbstractSyntaxTree{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrettyPrintVisitor): String{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrintGraphTreeVisitor){
        visitor.visit(this)
    }
}

class VariableNode(token: Token): AbstractSyntaxTree(token){
    override fun accept(visitor: RewriteVisitor): AbstractSyntaxTree{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrettyPrintVisitor): String{
        return visitor.visit(this)
    }
    override fun accept(visitor: PrintGraphTreeVisitor){
        visitor.visit(this)
    }
}


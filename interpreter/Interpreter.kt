package calculator.interpreter

import calculator.parser.*
import calculator.interpreter.NodeVisitor.*
import java.util.*

class Interpreter(tree: AbstractSyntaxTree) {
    private val treeStack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()

    init {
        treeStack.push(tree)
    }

    fun printGraphTree(ast: AbstractSyntaxTree): String{
        val printGraphVisitor = PrintGraphTreeVisitor() // prints the graph
        ast.accept(printGraphVisitor)
        return printGraphVisitor.getGraph()
    }

    private fun prettyPrint(ast: AbstractSyntaxTree): String{
        val prettyPrintVisitor = PrettyPrintVisitor() // prints the expression in infix form.
        return ast.accept(prettyPrintVisitor)
    }

    fun rewrite() {
        val rewriteVisitor = RewriteVisitor()
        var rewrittenTree = treeStack.peek().accept(rewriteVisitor)

        while(!rewrittenTree.equals(treeStack.peek())){
            treeStack.push(rewrittenTree)
            rewriteVisitor.resetFinished()
            rewrittenTree = treeStack.peek().accept(rewriteVisitor)
        }

        for(t in treeStack) println(prettyPrint(t))
        println(printGraphTree(treeStack.peek()))
    }
}


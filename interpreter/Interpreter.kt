package calculator.interpreter

import calculator.parser.*
import calculator.interpreter.NodeVisitor.*
import java.util.*

class Interpreter(private val tree: AbstractSyntaxTree) {
    private val treeStack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()
    private var currentTree: AbstractSyntaxTree? = null

    init {
        treeStack.push(tree)
    }

    fun printGraphTree(): String{
        val printGraphVisitor = PrintGraphTreeVisitor() // prints the graph
        tree.accept(printGraphVisitor)
        return printGraphVisitor.getGraph()
    }

    fun printFlatTree(): String{
        val printFlatTreeVisitor = PrintFlatTreeVisitor() // prints the flat tree
        tree.accept(printFlatTreeVisitor)
        return printFlatTreeVisitor.getFlatTree()
    }

    fun prettyPrint(ast: AbstractSyntaxTree): String{
        val prettyPrintVisitor = PrettyPrintVisitor() // prints the expression in infix form.
        ast.accept(prettyPrintVisitor)
        return prettyPrintVisitor.prettyPrint()
    }

    fun rewrite() {
        var rewriteVisitor = RewriteVisitor()
        currentTree = treeStack.peek().accept(rewriteVisitor)
        while(prettyPrint(treeStack.peek()) != currentTree?.let { prettyPrint(it) }){
            treeStack.push(currentTree)
            rewriteVisitor = RewriteVisitor()
            currentTree = treeStack.peek().accept(rewriteVisitor)
        }

        for(t in treeStack) println(prettyPrint(t))
    }
}


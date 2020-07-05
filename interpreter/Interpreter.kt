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

    // TODO: fun interpret, if quadratic, solve it, else, rewrite the tree.

    fun rewrite() {
        val rewriteVisitor = RewriteVisitor()
        println(printGraphTree(treeStack.peek()))
        println(prettyPrint((treeStack.peek()))

        )
        var rewrittenTree = treeStack.peek().accept(rewriteVisitor)
        var counter = 0

        while(!rewrittenTree.equals(treeStack.peek())){
            treeStack.push(rewrittenTree)
            rewriteVisitor.finished = false

            // prints the tree and pretty print
            //println(printGraphTree(rewrittenTree))
            println(prettyPrint((rewrittenTree)))

            rewrittenTree = treeStack.peek().accept(rewriteVisitor)

            // just so the program stops whenever we have an infinite loop in rewrite visitor
            // There shouldn't be one but just in case
            counter++
            //println(printGraphTree(rewrittenTree))
            if(counter > 80) {
                println("counter greater than 1000, loop in code rewrite visitor probably")
                break
            }
        }
        // TODO with explanation, for( until treeStack.size -1) get pretty print and explanation, continue if pretty print is identical ASSOCIATIVITY
        for(t in treeStack) {
            //println(prettyPrint(t))
            // loop through it like this to
        }
    }
}


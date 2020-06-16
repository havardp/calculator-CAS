package calculator.interpreter

import calculator.parser.*
import calculator.interpreter.NodeVisitor.*
import java.math.BigDecimal

class Interpreter(private val parser: Parser) {
    private val tree: AbstractSyntaxTree = parser.parse()

    fun interpret(): BigDecimal{
        // Visit nodes with type checker
        // If no variables and no equal, do this
        val arithmeticVisitor = ArithmeticVisitor() // visitor which evaluates the tree (only works for arithmetic so far)
        tree.accept(arithmeticVisitor)
        return arithmeticVisitor.getVal()

        // elseif variable, rewrite
        // elseif equal, evaluate both sides, IF EQUAL IS NOT ROOT, there is a problem, throw error
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

    fun prettyPrint(): String{
        val prettyPrintVisitor = PrettyPrintVisitor() // prints the expression in infix form.
        tree.accept(prettyPrintVisitor)
        return prettyPrintVisitor.prettyPrint()
    }
}


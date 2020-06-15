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
        // elseif equal, evaluate both sides
    }

    fun printGraphTree(): String{
        val printGraphVisitor = PrintGraphTreeVisitor() // visitor which prints the tree
        tree.accept(printGraphVisitor)
        return printGraphVisitor.getGraph()
    }

    fun printFlatTree(): String{
        val printFlatTreeVisitor = PrintFlatTreeVisitor() // visitor which prints the tree
        tree.accept(printFlatTreeVisitor)
        return printFlatTreeVisitor.getFlatTree()
    }
}


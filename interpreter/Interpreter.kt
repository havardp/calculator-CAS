package calculator.interpreter

import calculator.parser.*
import calculator.interpreter.NodeVisitor.*
import java.math.BigDecimal

class Interpreter(private val parser: Parser) {
    fun interpret(): BigDecimal{
        val arithmeticVisitor = ArithmeticVisitor() // visitor which evaluates the tree (only works for arithmetic so far)
        val printGraphVisitor = PrintGraphTreeVisitor() // visitor which prints the tree
        val printFlatTreeVisitor = PrintFlatTreeVisitor() // visitor which prints the tree
        val tree = parser.parse()
        tree.accept(arithmeticVisitor)
        tree.accept(printGraphVisitor)
        tree.accept(printFlatTreeVisitor)
        println(printGraphVisitor.getGraph())
        println("Flat tree of abstract syntax tree")
        println(printFlatTreeVisitor.str)
        println("\nEvaluation of tree")
        return arithmeticVisitor.getVal()
    }
}


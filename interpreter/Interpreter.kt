package calculator.interpreter

import calculator.parser.*
import java.math.BigDecimal

class Interpreter(private val parser: Parser) {
    fun interpret(): BigDecimal{
        val arithmeticVisitor = ArithmeticVisitor() // visitor which evaluates the tree (only works for arithmetic so far)
        val printGraphVisitor = PrintGraphVisitor() // visitor which prints the tree
        val printFlatTreeVisitor = PrintFlatTreeVisitor() // visitor which prints the tree
        val tree = parser.parse()
        tree.accept(arithmeticVisitor)
        tree.accept(printGraphVisitor)
        tree.accept(printFlatTreeVisitor)
        println(printGraphVisitor.getGraph())
        println(printFlatTreeVisitor.str)
        return arithmeticVisitor.getVal()
    }
}


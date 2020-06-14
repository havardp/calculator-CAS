package calculator.interpreter

import calculator.parser.*
import com.example.expressionCalculator.calculator.interpreter.ArithmeticVisitor
import java.math.BigDecimal

class Interpreter(private val parser: Parser) {
    fun interpret(): BigDecimal{
        val visitor = ArithmeticVisitor()
        val tree = parser.parse()
        tree.accept(visitor)
        return visitor.getVal()
    }
}


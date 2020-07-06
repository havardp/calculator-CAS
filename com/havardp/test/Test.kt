package main.com.havardp.test

import com.havardp.calculator.interpreter.Interpreter
import com.havardp.calculator.lexer.Lexer
import com.havardp.calculator.parser.Parser
import kotlin.test.assertEquals
import org.junit.Test

class Test {
    private fun evaluate(str: String): String{
        val lexer = Lexer(str)
        val parser = Parser(lexer)
        val interpreter = Interpreter(parser)
        interpreter.interpret()
        return interpreter.getPrettyPrintedResult()
    }

    @Test
    fun evaluateOperand(){
        assertEquals("4", evaluate("4"))
        assertEquals("0.1", evaluate("0.1"))
        assertEquals("-1", evaluate("-1"))
        assertEquals("-1.1", evaluate("-1.1"))
    }

    @Test
    fun evaluateVariable(){
        assertEquals("x", evaluate("x"))
    }

    @Test
    fun evaluateConstant(){
        assertEquals("3.1415926535", evaluate("pi"))
        assertEquals("2.7182818284", evaluate("e"))
    }

    @Test
    fun evaluateUnary(){
        assertEquals("-1", evaluate("-(3-2)"))
        assertEquals("1", evaluate("+1"))

        assertEquals("0", evaluate("sin(0)"))
        assertEquals("1", evaluate("sin(pi/2)"))
        assertEquals("1", evaluate("arcsin(sin(1))"))
        assertEquals("1", evaluate("sin(arcsin(1))"))
        assertEquals("1", evaluate("cos(0)"))
        assertEquals("0", evaluate("cos(pi/2)"))
        assertEquals("1", evaluate("arccos(cos(1))"))
        assertEquals("1", evaluate("cos(arccos(1))"))
        assertEquals("0", evaluate("tan(0)"))
        assertEquals("1", evaluate("tan(pi/4)"))
        assertEquals("1", evaluate("arctan(tan(1))"))
        assertEquals("1", evaluate("tan(arctan(1))"))

        assertEquals("2", evaluate("sqrt(4)"))
        assertEquals("1", evaluate("abs(-1)"))
        assertEquals("90", evaluate("deg(pi/2)"))
        assertEquals("0", evaluate("rad(0)"))
        assertEquals("2", evaluate("ceil(1.1)"))
        assertEquals("1", evaluate("floor(1.1)"))
        assertEquals("1", evaluate("round(1.4)"))
        assertEquals("2", evaluate("round(1.6)"))
    }

    @Test
    fun evaluateBinary(){
        assertEquals("4", evaluate("2+2"))
        assertEquals("2", evaluate("4-2"))
        assertEquals("6", evaluate("2*3"))
        assertEquals("2", evaluate("4/2"))
        assertEquals("16", evaluate("2^4"))
        assertEquals("1", evaluate("3%2"))
    }

    @Test
    fun evaluateMixedArithmetic(){
        assertEquals("100.5", evaluate("2+3-4*2+16-2/4+88"))
        assertEquals("\\frac{2}{x}", evaluate("(1+1)/x"))
    }

    @Test
    fun evaluateAlgebra(){
        assertEquals("2 \\cdot x", evaluate("x+x"))
        assertEquals("2 - 6 \\cdot x + 3 \\cdot sin(x)", evaluate("2+(2+x)+2x+2sin(x)+-3x*3*x/x+3x-(3x+2)+sin(x)"))
    }

    @Test
    fun evaluateAlgebraEquality(){
        assertEquals("x = 2", evaluate("2x-4=0"))
        assertEquals("x = -3.5", evaluate("2x-3x+3+4+3x/(2x)+2x=3-x-3x+2+4x"))
        assertEquals("x = 2", evaluate("x^5-3=29"))
    }
}
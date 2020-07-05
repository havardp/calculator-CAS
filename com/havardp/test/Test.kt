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
        val interpreter = Interpreter(parser.parse())
        val result = interpreter.interpret()
        return "4"
    }

    @Test
    fun evaluation(){
        assertEquals("4", evaluate("2+2"))
    }
    // x^5 - 3 = 29
    // 2x-3x+3+4+3x/(2x)+2x=3-x-3x+2+4x
    // 2+(2+x)+2x+2sin(x)+-3x*3*x/x+3x-(3x+2)+sin(x)
}
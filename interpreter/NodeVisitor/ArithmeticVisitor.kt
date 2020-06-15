package calculator.interpreter.NodeVisitor

import calculator.exception.InvalidSyntaxException
import calculator.exception.NotAnOperatorException
import calculator.lexer.Token.*
import calculator.parser.BinaryOperatorNode
import calculator.parser.OperandNode
import calculator.parser.UnaryOperatorNode
import calculator.parser.VariableNode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.math.*

class ArithmeticVisitor: NodeVisitor() {
    private val stack: Stack<BigDecimal> = Stack<BigDecimal>()
    private val PRECISION = 4
    private val CONTEXT = MathContext(PRECISION, RoundingMode.HALF_UP)

    fun getVal(): BigDecimal {
        return stack.pop()
    }

    override fun visit(node: BinaryOperatorNode) {
        node.left.accept(this)
        node.right.accept(this)
        val b = stack.pop()
        val a = stack.pop()

        when(node.token){
            is Plus -> stack.push(a.plus(b))
            is Minus -> stack.push(a.minus(b))
            is Multiplication -> stack.push(a.multiply(b, CONTEXT))
            is Divide -> {
                try{
                    stack.push(a.divide(b, CONTEXT))
                }catch(e: ArithmeticException){
                    throw InvalidSyntaxException("Tried to divide by zero")
                }
            }
            is Power -> stack.push(a.toDouble().pow(b.toDouble()).toBigDecimal())
            is Modulus -> stack.push(a.remainder(b, CONTEXT))
            else -> throw NotAnOperatorException("Tried to visit and operate on binary operator, but token was not binary operator")
        }
    }
    override fun visit(node: UnaryOperatorNode){
        node.middle.accept(this)
        val a = stack.pop()
        when(node.token){
            is UnaryMinus -> stack.push(a.negate())
            is UnaryPlus -> stack.push(a)
            is Sin -> stack.push(sin(a.toDouble()).toBigDecimal())
            is ArcSin -> stack.push(asin(a.toDouble()).toBigDecimal())
            is Cos -> stack.push(cos(a.toDouble()).toBigDecimal())
            is ArcCos -> stack.push(acos(a.toDouble()).toBigDecimal())
            is Tan -> stack.push(tan(a.toDouble()).toBigDecimal())
            is ArcTan -> stack.push(atan(a.toDouble()).toBigDecimal())
            is Sqrt -> stack.push(sqrt(a.toDouble()).toBigDecimal())
            is Abs -> stack.push(abs(a.toDouble()).toBigDecimal())
            is Deg -> stack.push(Math.toDegrees(a.toDouble()).toBigDecimal())
            is Rad -> stack.push(Math.toRadians(a.toDouble()).toBigDecimal())
            is Ceil -> stack.push(ceil(a.toDouble()).toBigDecimal())
            is Floor -> stack.push(floor(a.toDouble()).toBigDecimal())
            is Round -> stack.push(round(a.toDouble()).toBigDecimal())
            else -> throw NotAnOperatorException("Tried to visit and operate on unary operator, but token was not unary operator")
        }
    }

    override fun visit(node: OperandNode) {
        stack.push(node.token.value.toBigDecimal())
    }

    override fun visit(node: VariableNode) {
        // haven't yet implemented variable visiting, maybe do it in another visitor class?
    }
}
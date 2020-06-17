package calculator.interpreter.NodeVisitor

import calculator.exception.InvalidSyntaxException
import calculator.exception.NotAnOperatorException
import calculator.lexer.Token.*
import calculator.parser.*
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

// Preorder traversal??
class RewriteVisitor: NodeVisitor() {
    private var finished = false
    private val PRECISION = 4
    private val CONTEXT = MathContext(PRECISION, RoundingMode.HALF_UP)

    fun resetFinished(){
        finished = false
    }

    override fun visit(node: BinaryOperatorNode): AbstractSyntaxTree {
        if(finished) return node
        return rewriteBinary(node.token, node.left, node.right) }

    override fun visit(node: UnaryOperatorNode): AbstractSyntaxTree{
        if(finished) return node
        return rewriteUnary(node.token, node.middle)
    }

    override fun visit(node: OperandNode): AbstractSyntaxTree {
        return node
    }

    override fun visit(node: VariableNode): AbstractSyntaxTree {
        return node
    }

    private fun rewriteUnary(token: Token, middle: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        if(middle.token is OperandToken) return evaluateUnary(token, middle.token)

        finished = false
        val middle = middle.accept(this)
        return UnaryOperatorNode(token, middle)
    }

    private fun rewriteBinary(token: Token, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        if(token is Plus){
            // x + 0 -> x
            if (right.token.value == "0") return left
            // 0 + x -> x
            else if(left.token.value == "0") return right
        }
        else if(token is Minus){
            // x - x = 0
            if(left.equals(right)) return OperandNode(OperandToken("0"))
            // x - 0 -> x
            else if (right.token.value == "0") return left
            // 0 - x -> -x
            else if(left.token.value == "0") {
                if(right.token is OperandToken) return evaluateUnary(UnaryMinus(), right.token) // if right is operand, we can just negate it right away
                return UnaryOperatorNode(UnaryMinus() ,right)
            }
        }
        else if(token is Multiplication){
            // TODO: need to check that the expression is equal, not that the value is
            if(left.token is OperandToken && right is BinaryOperatorNode){
                // x*y/x -> y
                if(right.token is Divide && right.right.equals(left)) return right.left
            } else if(right.token is OperandToken && left is BinaryOperatorNode){
                // y/x*x -> y
                if(left.token is Divide && left.right.equals(right)) return left.left
            }
            // 1 * x -> x
            else if(left.token.value == "1") return right
            // x * 1 -> x
            else if(right.token.value == "1") return left
            // 0 * x -> 0 and x * 0 -> 0
            else if(left.token.value == "0" || right.token.value == "0") return OperandNode(OperandToken("0"))
        }
        else if(token is Divide){
            // 0 / x -> 0 TODO: need to make sure x expression is not 0 aswell
            if(left.token.value == "0") return OperandNode(OperandToken("0"))
        }
        else if(token is Power){

        }
        if(left.token is OperandToken && right.token is OperandToken) return evaluateBinary(token, left.token, right.token)
        // TODO: if left is operand and right is variable (or vice versa), non terminating rewrite

        // If we have come here and not returned, then we visit child
        finished = false
        val leftVisited = left.accept(this)
        val rightVisited = right.accept(this)
        return BinaryOperatorNode(token, leftVisited, rightVisited)
    }

    private fun evaluateUnary(operator: Token, middle: OperandToken): AbstractSyntaxTree {
        val operand = middle.value.toBigDecimal()
        val result = when(operator){
            is UnaryMinus -> operand.negate()
            is UnaryPlus -> operand
            is Sin -> sin(operand.toDouble()).toBigDecimal()
            is ArcSin -> asin(operand.toDouble()).toBigDecimal()
            is Cos -> cos(operand.toDouble()).toBigDecimal()
            is ArcCos -> acos(operand.toDouble()).toBigDecimal()
            is Tan -> tan(operand.toDouble()).toBigDecimal()
            is ArcTan -> atan(operand.toDouble()).toBigDecimal()
            is Sqrt -> sqrt(operand.toDouble()).toBigDecimal()
            is Abs -> abs(operand.toDouble()).toBigDecimal()
            is Deg -> Math.toDegrees(operand.toDouble()).toBigDecimal()
            is Rad -> Math.toRadians(operand.toDouble()).toBigDecimal()
            is Ceil -> ceil(operand.toDouble()).toBigDecimal()
            is Floor -> floor(operand.toDouble()).toBigDecimal()
            is Round -> round(operand.toDouble()).toBigDecimal()
            else -> throw NotAnOperatorException("Tried to visit and operate on unary operator, but token was not unary operator")
        }
        return OperandNode(OperandToken(result.toString()))
    }

    private fun evaluateBinary(operator: Token, left: OperandToken, right: OperandToken): AbstractSyntaxTree {
        val operand1 = left.value.toBigDecimal()
        val operand2 = right.value.toBigDecimal()
        val result = when(operator){
            is Plus -> operand1.plus(operand2)
            is Minus -> operand1.minus(operand2)
            is Multiplication -> operand1.multiply(operand2, CONTEXT)
            is Divide -> {
                try{
                    operand1.divide(operand2, CONTEXT)
                }catch(e: ArithmeticException){
                    throw InvalidSyntaxException("Tried to divide by zero")
                }
            }
            is Power -> operand1.toDouble().pow(operand2.toDouble()).toBigDecimal()
            is Modulus -> operand1.remainder(operand2, CONTEXT)
            else -> throw NotAnOperatorException("Tried to visit and binary operate on node, but node was not binary operator")
        }
        return OperandNode(OperandToken(result.toString()))
    }
}
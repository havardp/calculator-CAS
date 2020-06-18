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
    private var nonTerminatingRewrite = false
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

    // exp is expression, basically a abstract syntax tree, a node
    // op is operand
    // var is variable
    private fun rewriteBinary(token: Token, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        if(token is Plus){
            // exp + 0 -> exp
            if (right.token.value == "0") return left
            // 0 + exp -> exp
            else if(left.token.value == "0") return right
            // exp+-op -> exp-op
            else if(right.token is OperandToken && right.token.value.toBigDecimal() < 0.toBigDecimal())
                return BinaryOperatorNode(Minus(), left, evaluateUnary(UnaryMinus(), right.token))
            // var + var -> 2 * var
            else if(left.token is VariableToken && right.token is VariableToken) {
                return BinaryOperatorNode(Multiplication(), OperandNode(OperandToken("2")), left)
            }
            else if(left is UnaryOperatorNode && left.token is UnaryMinus && right.token !is UnaryMinus) return BinaryOperatorNode(Minus(), right, left.middle)
        }
        else if(token is Minus){
            // exp - exp = 0
            if(left.equals(right)) return OperandNode(OperandToken("0"))
            // exp - 0 -> exp
            else if (right.token.value == "0") return left
            // 0 - exp -> -exp
            else if(left.token.value == "0") {
                if(right.token is OperandToken) return evaluateUnary(UnaryMinus(), right.token) // if right is operand, we can just negate it right away
                return UnaryOperatorNode(UnaryMinus() ,right)
            }
            else if(left is BinaryOperatorNode && right.token is OperandToken){
                if(left.token is Plus && left.left.token is VariableToken && left.right.token is OperandToken){
                    // (var + op) - op -> var + (op - op)
                    return BinaryOperatorNode(left.token, left.left, BinaryOperatorNode(token, left.right, right))
                }
                else if(left.token is Plus && left.left.token is OperandToken && left.right.token is VariableToken){
                    // (op + var) - op -> var + (op - op)
                    return BinaryOperatorNode(left.token, left.right, BinaryOperatorNode(token, left.left, right))
                }
                if(left.token is Minus && left.left.token is VariableToken && left.right.token is OperandToken){
                    // (var - op) - op -> var + (-op - op)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(left.token, UnaryOperatorNode(UnaryMinus(), right), left.right))
                }
                else if(left.token is Minus && left.left.token is OperandToken && left.right.token is VariableToken){
                    // (op - var) - op -> (op - op) - var
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(left.token, left.left, right), left.right)
                }
            }
            else if(right is BinaryOperatorNode && left.token is OperandToken){
                if(right.token is Plus && right.left.token is VariableToken && right.right.token is OperandToken){
                    // op1 - (var + op2) -> -var + (op1 - op2)
                    return BinaryOperatorNode(right.token, UnaryOperatorNode(UnaryMinus(), right.left), BinaryOperatorNode(token, left, right.right))
                }
                else if(right.token is Plus && right.left.token is OperandToken && right.right.token is VariableToken){
                    // op1 - (op2 + var) -> -var + (op1 - op2)
                    return BinaryOperatorNode(right.token, UnaryOperatorNode(UnaryMinus(), right.right), BinaryOperatorNode(token, left, right.left))
                }
                if(right.token is Minus && right.left.token is VariableToken && right.right.token is OperandToken){
                    // op1 - (var - op2) -> -var + (op1 + op2)
                    return BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), right.left), BinaryOperatorNode(Plus(), left, right.right))
                }
                else if(right.token is Minus && right.left.token is OperandToken && right.right.token is VariableToken){
                    // op1 - (op2 - var) -> var + (op1 + op2)
                    return BinaryOperatorNode(Plus(), right.right, BinaryOperatorNode(token, left, right.left))
                }
            }
        }
        else if(token is Multiplication){
            // TODO: need to check that the expression is equal, not that the value is
            if(left.token is OperandToken && right is BinaryOperatorNode){
                // exp1*exp2/exp1 -> exp2
                if(right.token is Divide && right.right.equals(left)) return right.left
            } else if(right.token is OperandToken && left is BinaryOperatorNode){
                // exp2/exp1*exp1 -> exp
                if(left.token is Divide && left.right.equals(right)) return left.left
            }
            // 1 * exp -> exp
            else if(left.token.value == "1") return right
            // exp * 1 -> exp
            else if(right.token.value == "1") return left
            // 0 * exp -> 0 and exp * 0 -> 0
            else if(left.token.value == "0" || right.token.value == "0") return OperandNode(OperandToken("0"))
        }
        else if(token is Divide){
            // 0 / exp -> 0 TODO: need to make sure x expression is not 0 aswell
            if(left.token.value == "0") return OperandNode(OperandToken("0"))
        }
        else if(token is Power){

        }
        if(left.token is OperandToken && right.token is OperandToken) return evaluateBinary(token, left.token, right.token)

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
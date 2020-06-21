package calculator.interpreter.NodeVisitor

import calculator.exception.InvalidSyntaxException
import calculator.exception.NotAnOperatorException
import calculator.lexer.Token.*
import calculator.parser.*
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

// Preorder traversal, because we want to rewrite before we evaluate
// for example 3*1/3 should be rewritten, not evaluated, or else it equals 0.99... and not 1, which it should be
class RewriteVisitor: NodeVisitor() {
    private var finished = false
    private val PRECISION = 4
    private val CONTEXT = MathContext(PRECISION, RoundingMode.HALF_UP)

    fun resetFinished(){
        finished = false
    }

    override fun visit(node: BinaryOperatorNode): AbstractSyntaxTree {
        if (finished) return node
        val rewrittenNode = when (node.token) {
            is Plus -> rewritePlus(node.token, node.left, node.right)
            is Minus -> rewriteMinus(node.token, node.left, node.right)
            is Multiplication -> rewriteMultiplication(node.token, node.left, node.right)
            is Divide -> rewriteDivide(node.token, node.left, node.right)
            is Power -> rewritePower(node.token, node.left, node.right)
            else -> throw NotAnOperatorException("tried to handle non binary operator as binary operator")
        }
        // If finished is true here that means that we did a rewrite in the code above, thus we return the rewritten node
        if(finished) return rewrittenNode
        // if we couldn't rewrite, and both child nodes are operand, then we evaluate it
        if(node.left.token is OperandToken && node.right.token is OperandToken) return evaluateBinary(node.token, node.left.token, node.right.token)
        // else visit the children and return
        return BinaryOperatorNode(node.token, node.left.accept(this), node.right.accept(this))
    }

    override fun visit(node: UnaryOperatorNode): AbstractSyntaxTree{
        if(finished) return node
        val rewrittenNode = rewriteUnary(node.token, node.middle)

        // If finished is true here that means that we did a rewrite in the code above, thus we return the rewritten node
        if(finished) return rewrittenNode
        // if we couldn't rewrite, and child node is operand, then we evaluate it
        if(node.middle.token is OperandToken) return evaluateUnary(node.token, node.middle.token)
        // else visit child and return
        return UnaryOperatorNode(node.token, node.middle.accept(this))
    }

    override fun visit(node: OperandNode): AbstractSyntaxTree {
        return node
    }

    override fun visit(node: VariableNode): AbstractSyntaxTree {
        return node
    }

    // exp is expression, basically a abstract syntax tree, a node
    // op is operand
    // var is variable
    private fun rewritePlus(token: Token, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true

        // exp + 0 -> exp
        if (right.token.value == "0") return left
        // 0 + exp -> exp
        if(left.token.value == "0") return right
        // exp + exp -> 2 * exp (not operand)
        if(left !is OperandNode && left.equals(right))
            return BinaryOperatorNode(Multiplication(), OperandNode(OperandToken("2")), left)
        // exp + -exp -> exp - exp
        if(right is UnaryOperatorNode && right.token is UnaryMinus && right.middle !is UnaryOperatorNode) return BinaryOperatorNode(Minus(), left, right.middle)

        // handles commutativity and associativity
        if(left is BinaryOperatorNode){
            if (left.token is Plus){
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || right.token is VariableToken && left.left.token !is VariableToken && left.right.token is VariableToken){
                    // (exp + op) + op -> exp + (op + op)
                    // (exp + var) + var -> exp + (var + var)
                    return BinaryOperatorNode(token, left.left, BinaryOperatorNode(token, left.right, right))
                }
                if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.token is VariableToken && left.left.token is VariableToken && left.right.token !is VariableToken)){
                    // (op + exp) + op -> exp + (op + op)
                    // (var + exp) + var -> exp + (var + var)
                    return BinaryOperatorNode(token, left.right, BinaryOperatorNode(token, left.left, right))
                }
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right))
                    // (exp1 + exp2 * exp3) + exp3 -> exp1 + (exp2 * exp3 + exp3)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Plus(), left.right, right))
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right.right))
                    // (exp1 + exp2 * exp3) + exp3 * exp4 -> exp1 + (exp2 * exp3 + exp3 * exp4)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Plus(), left.right, right))
            }
            else if (left.token is Minus){
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || right.token is VariableToken && left.left.token !is VariableToken && left.right.token is VariableToken){
                    // (exp - op) + op -> exp + (op - op)
                    // (exp - var) + var -> exp + (var - var)
                    return BinaryOperatorNode(token, left.left, BinaryOperatorNode(left.token, right, left.right))
                }
                if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.token is VariableToken && left.left.token is VariableToken && left.right.token !is VariableToken)){
                    // (op - exp) + op -> (op + op) - exp
                    // (var - exp) + var -> (var + var) - exp
                    return BinaryOperatorNode(left.token, BinaryOperatorNode(token, right, left.left), left.right)
                }
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right))
                    // (exp1 - exp2 * exp3) + exp3 -> exp1 + (-exp2 * exp3 + exp3)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), left.right), right))
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right.right))
                // (exp1 - exp2 * exp3) + exp3 * exp4 -> exp1 + (exp3 * exp4 - exp2 * exp3)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), right, left.right))
            }
        }
        if(right is BinaryOperatorNode){
            if (right.token is Plus){
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || left.token is VariableToken && right.left.token !is VariableToken && right.right.token is VariableToken){
                    // op + (exp + op) -> exp + (op + op)
                    // var + (exp + var) -> exp + (var + var)
                    return BinaryOperatorNode(token, right.left, BinaryOperatorNode(token, right.right, left))
                }
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.token is VariableToken && right.left.token is VariableToken && right.right.token !is VariableToken)){
                    // op + (op + exp) -> exp + (op + op)
                    // var + (var + exp) -> exp + (var + var)
                    return BinaryOperatorNode(token, right.right, BinaryOperatorNode(token, right.left, left))
                }

            }
            if (right.token is Minus){
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || left.token is VariableToken && right.left.token !is VariableToken && right.right.token is VariableToken){
                    // op + (exp - op) -> exp + (op - op)
                    // var + (exp - var) -> exp + (var - var)
                    return BinaryOperatorNode(token, right.left, BinaryOperatorNode(right.token, left, right.right))
                }
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.token is VariableToken && right.left.token is VariableToken && right.right.token !is VariableToken)){
                    // op + (op - exp) -> (op + op) - exp
                    // var + (var - exp) -> (var + var) - exp
                    return BinaryOperatorNode(right.token, BinaryOperatorNode(token, left, right.left), right.right)
                }
            }
        }

        // Distributivity
        if (left is BinaryOperatorNode && right is BinaryOperatorNode){
            if(left.token is Multiplication && right.token is Multiplication){
                // exp1 * exp2 + exp1 * exp3 -> exp1(exp2 + exp3)
                if(left.left.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Plus(), left.right, right.right))
                // exp1 * exp2 + exp3 * exp1 -> exp1(exp2 + exp3)
                if(left.left.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Plus(), left.right, right.left))
                // exp2 * exp1 + exp1 * exp2 -> exp1(exp2 + exp3)
                if(left.right.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Plus(), left.left, right.right))
                // exp1 * exp2 + exp3 * exp1 -> exp1(exp2 + exp3)
                if(left.right.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Plus(), left.left, right.left))
            }
        }
        // exp1 * exp2 + exp1 -> exp1 * exp2 + exp1 * 1 (so that we can use distributivity rules above)
        if(left is BinaryOperatorNode && left.token is Multiplication && right !is OperandNode){
            println("${token.value} , ${left.token.value} , ${right.token.value}")
            if(left.right.equals(right)) return BinaryOperatorNode(token, left, BinaryOperatorNode(Multiplication(), right, OperandNode(OperandToken("1"))))
            if(left.left.equals(right)) return BinaryOperatorNode(token, left, BinaryOperatorNode(Multiplication(), right, OperandNode(OperandToken("1"))))
        }
        // exp1 + exp2 * exp1 -> exp1 * exp2 + exp1 * 1 (so that we can use distributivity rules above)
        if(right is BinaryOperatorNode && right.token is Multiplication && left !is OperandNode){
            if(right.right.equals(left)) return BinaryOperatorNode(token, right, BinaryOperatorNode(Multiplication(), left, OperandNode(OperandToken("1"))))
            if(right.left.equals(left)) return BinaryOperatorNode(token, right, BinaryOperatorNode(Multiplication(), left, OperandNode(OperandToken("1"))))
        }

        // exp * var + exp + var -> exp * var + var + exp
        if(right.token is VariableToken && left is BinaryOperatorNode && left.token is Plus && left.left is BinaryOperatorNode && left.left.token is Multiplication){
            if(left.left.right.token is VariableToken) {
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), left.left, right), left.right)
            }
        }
        // exp * var + exp + exp * var -> exp * var + exp * var + exp COMMUTATIVITY
        if(left is BinaryOperatorNode && left.token is Plus && right is BinaryOperatorNode && right.token is Multiplication && left.left is BinaryOperatorNode && left.left.token is Multiplication){
            if(right.right.token is VariableToken && left.left.right.token is VariableToken)
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), left.left, right), left.right)
        }

        // unary - var||op -> -exp + var||op
        // move unary to the right so that we can move it up the tree with the code below
        if((right is OperandNode || right is VariableNode)
                        && (left.token is Multiplication || left.token is Divide || left is UnaryOperatorNode && (left.token !is UnaryMinus || left.middle is UnaryOperatorNode)))
            return BinaryOperatorNode(Plus(), right, left)

        // exp + unary + exp -> exp + exp + unary
        // exp + -unary + exp -> exp + exp +- unary
        // commutativity, move unary operators to the top of the binary tree, to more easily evaluate
        if(left is BinaryOperatorNode && left.token is Plus){
            if(right !is UnaryOperatorNode || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode)){
                if(left.right is UnaryOperatorNode ||
                        (left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right is UnaryOperatorNode))
                   return BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), left.left, right), left.right)
            }
        }
        // exp - unary + exp -> exp + exp - unary
        // exp - -unary + exp -> exp + exp -- unary
        if(left is BinaryOperatorNode && left.token is Minus){
            if(right !is UnaryOperatorNode || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode)){
                if(left.right is UnaryOperatorNode ||
                        (left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right is UnaryOperatorNode))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left.left, right), left.right)
            }
        }

        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteMinus(token: Minus, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        // exp - exp = 0
        if(left.equals(right)) return OperandNode(OperandToken("0"))
        // exp - 0 -> exp
        else if (right.token.value == "0") return left
        // 0 - exp -> -exp
        else if(left.token.value == "0") {
            if(right.token is OperandToken) return evaluateUnary(UnaryMinus(), right.token) // if right is operand, we can just negate it right away
            return UnaryOperatorNode(UnaryMinus() ,right)
        }
        // exp - -exp -> exp + exp
        if(right is UnaryOperatorNode && right.token is UnaryMinus && right.middle !is UnaryOperatorNode) return BinaryOperatorNode(Plus(), left, right.middle)

        if(left is BinaryOperatorNode){
            if (left.token is Plus){
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || right.token is VariableToken && left.left.token !is VariableToken && left.right.token is VariableToken){
                    // (exp + op) - op -> exp + (op - op)
                    // (exp + var) - var -> exp + (var - var)
                    return BinaryOperatorNode(left.token, left.left, BinaryOperatorNode(token, left.right, right))
                }
                else if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.token is VariableToken && left.left.token is VariableToken && left.right.token !is VariableToken)){
                    // (op + exp) - op -> exp + (op - op)
                    // (var + exp) - var -> exp + (var - var)
                    return BinaryOperatorNode(left.token, left.right, BinaryOperatorNode(token, left.left, right))
                }
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right))
                    // (exp1 + exp2 * exp3) - exp3 -> exp1 + (exp2 * exp3 - exp3)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), left.right, right))
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right.right))
                    // (exp1 + exp2 * exp3) - exp3 * exp4 -> exp1 + (exp2 * exp3 - exp3 * exp4)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), left.right, right))
            }
            else if (left.token is Minus){
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || right.token is VariableToken && left.left.token !is VariableToken && left.right.token is VariableToken){
                    // (exp - op) - op -> exp + (-op - op)
                    // (exp - var) - var -> exp + (-var - var)
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(left.token, UnaryOperatorNode(UnaryMinus(), right), left.right))
                }
                else if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.token is VariableToken && left.left.token is VariableToken && left.right.token !is VariableToken)){
                    // (op - exp) - op -> (op - op) - exp
                    // (var - exp) - var -> (var - var) - exp
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(left.token, left.left, right), left.right)
                }
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right))
                    // (exp1 - exp2 * exp3) - exp3 -> exp1 - (exp2 * exp3 + exp3)
                    return BinaryOperatorNode(Minus(), left.left, BinaryOperatorNode(Plus(), left.right, right))
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right.equals(right.right))
                    // (exp1 - exp2 * exp3) - exp3 * exp4 -> exp1 - (exp3 * exp4 + exp2 * exp3)
                    return BinaryOperatorNode(Minus(), left.left, BinaryOperatorNode(Plus(), left.right, right))
            }
        }
        if(right is BinaryOperatorNode){
            if (right.token is Plus){
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || left.token is VariableToken && right.left.token !is VariableToken && right.right.token is VariableToken){
                    // op - (exp + op) -> -exp + (op - op)
                    // var - (exp + var) -> -exp + (var - var)
                    return BinaryOperatorNode(right.token, UnaryOperatorNode(UnaryMinus(), right.left), BinaryOperatorNode(token, left, right.right))
                }
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.token is VariableToken && right.left.token is VariableToken && right.right.token !is VariableToken)){
                    // op - (op + exp) -> -exp + (op - op)
                    // var - (var + exp) -> -exp + (var - var)
                    return BinaryOperatorNode(right.token, UnaryOperatorNode(UnaryMinus(), right.right), BinaryOperatorNode(token, left, right.left))
                }
            }
            else if (right.token is Minus){
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || left.token is VariableToken && right.left.token !is VariableToken && right.right.token is VariableToken){
                    // op - (exp - op) -> -exp + (op + op)
                    // var - (exp - var) -> -exp + (var + var)
                    return BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), right.left), BinaryOperatorNode(Plus(), left, right.right))
                }
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.token is VariableToken && right.left.token is VariableToken && right.right.token !is VariableToken)){
                    // op - (op - exp) -> exp + (op - op)
                    // var - (var - exp) -> exp + (var - var)
                    return BinaryOperatorNode(Plus(), right.right, BinaryOperatorNode(token, left, right.left))
                }
            }
        }

        // Distributivity
        if (left is BinaryOperatorNode && right is BinaryOperatorNode){
            if(left.token is Multiplication && right.token is Multiplication){
                // exp1 * exp2 - exp1 * exp3 -> exp1(exp2 - exp3)
                if(left.left.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Minus(), left.right, right.right))
                // exp1 * exp2 - exp3 * exp1 -> exp1(exp2 - exp3)
                if(left.left.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Minus(), left.right, right.left))
                // exp2 * exp1 - exp1 * exp2 -> exp1(exp2 - exp3)
                if(left.right.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Minus(), left.left, right.right))
                // exp1 * exp2 - exp3 * exp1 -> exp1(exp2 - exp3)
                if(left.right.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Minus(), left.left, right.left))
            }
        }

        // exp1 * exp2 - exp1 -> exp1 * exp2 - exp1 * 1 (so that we can use distributivity rules above)
        if(left is BinaryOperatorNode && left.token is Multiplication && right !is OperandNode){
            if(left.right.equals(right)) return BinaryOperatorNode(token, left, BinaryOperatorNode(Multiplication(), right, OperandNode(OperandToken("1"))))
            if(left.left.equals(right)) return BinaryOperatorNode(token, left, BinaryOperatorNode(Multiplication(), right, OperandNode(OperandToken("1"))))
        }
        // exp1 - exp2 * exp1 -> exp1 * 1 - exp1 * exp2 (so that we can use distributivity rules above)
        if(right is BinaryOperatorNode && right.token is Multiplication && left !is OperandNode){
            if(right.right.equals(left)) return BinaryOperatorNode(token, BinaryOperatorNode(Multiplication(), left, OperandNode(OperandToken("1"))), right)

            //exp1 + exp2 - exp3 * exp2 -> exp1 + (exp2 - exp3 * exp2)
            if(left is BinaryOperatorNode && left.token is Plus && right.right.equals(left.right))
                return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), left.right, right))


            if(right.left.equals(left)) return BinaryOperatorNode(token, BinaryOperatorNode(Multiplication(), left, OperandNode(OperandToken("1"))), right)
        }

        // exp * var + exp - var -> exp * var - var + exp
        if(right.token is VariableToken && left is BinaryOperatorNode && left.token is Plus && left.left is BinaryOperatorNode && left.left.token is Multiplication){
            if(left.left.right.token is VariableToken) {
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Minus(), left.left, right), left.right)
            }
        }
        // exp * var + exp + exp * var -> exp * var + exp * var + exp COMMUTATIVITY
        if(left is BinaryOperatorNode && left.token is Plus && right is BinaryOperatorNode && right.token is Multiplication && left.left is BinaryOperatorNode && left.left.token is Multiplication){
            if(right.right.token is VariableToken && left.left.right.token is VariableToken)
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Minus(), left.left, right), left.right)
        }

        // unary - var||op -> -exp + var||op
        // move unary to the right so that we can move it up the tree with the code below
        if((right is OperandNode || right is VariableNode)
                        && (left.token is Multiplication || left.token is Divide || left is UnaryOperatorNode && (left.token !is UnaryMinus || left.middle is UnaryOperatorNode)))
            return BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(),right), left)

        // exp + unary - exp -> exp - exp + unary
        // exp + -unary - exp -> exp - exp + -unary
        // commutativity, move unary operators to the top of the binary tree, to more easily evaluate
        if(left is BinaryOperatorNode && left.token is Plus){
            if(right !is UnaryOperatorNode || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode)){
                if(left.right is UnaryOperatorNode ||
                        (left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right is UnaryOperatorNode))
                    return BinaryOperatorNode(Plus(), BinaryOperatorNode(Minus(), left.left, right), left.right)
            }

        }
        // exp - unary - exp -> exp - exp - unary
        // exp - -unary - exp -> exp - exp - -unary
        if(left is BinaryOperatorNode && left.token is Minus){
            if(right !is UnaryOperatorNode || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode)){
                if (left.right is UnaryOperatorNode ||
                        (left.right is BinaryOperatorNode && left.right.token is Multiplication && left.right.right is UnaryOperatorNode))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left.left, right), left.right)
            }
        }

        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteMultiplication(token: Multiplication, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        if(left.token is OperandToken && right is BinaryOperatorNode){
            // exp1*exp2/exp1 -> exp2
            if(right.token is Divide && right.right.equals(left)) return right.left
        } else if(right.token is OperandToken && left is BinaryOperatorNode){
            // exp2/exp1*exp1 -> exp
            if(left.token is Divide && left.right.equals(right)) return left.left
        }
        if ((left.token is VariableToken || left.token is UnaryOperatorToken) && right.token is OperandToken) return BinaryOperatorNode(token, right, left)
        // (negative op) * exp -> -((positive op) * exp)
        if(left.token is OperandToken && left.token.value.toBigDecimal() < 0.toBigDecimal())
            return UnaryOperatorNode(UnaryMinus(), BinaryOperatorNode(token, UnaryOperatorNode(UnaryMinus(), left), right))
        // 1 * exp -> exp
        else if(left.token.value == "1") return right
        // exp * 1 -> exp
        else if(right.token.value == "1") return left
        // 0 * exp -> 0 and exp * 0 -> 0
        else if(left.token.value == "0" || right.token.value == "0") return OperandNode(OperandToken("0"))
        // Commutativity
        if(left is BinaryOperatorNode){
            if (left.token is Multiplication){
                if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.token is VariableToken && left.left.token is VariableToken && left.right.token !is VariableToken)){
                    // (op * exp) * op -> exp * (op * op)
                    // (var * exp) * var -> exp * (var * var)
                    // and more, because we rewrite operand to left
                    return BinaryOperatorNode(token, left.right, BinaryOperatorNode(token, left.left, right))
                }
            }
        }
        if(right is BinaryOperatorNode){
            if (right.token is Multiplication){
                if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.token is VariableToken && right.left.token is VariableToken && right.right.token !is VariableToken)){
                    // op * (op * exp) -> exp * (op * op)
                    // var * (var * exp) -> exp * (var * var)
                    // and more, because we rewrite operand to left
                    return BinaryOperatorNode(token, right.right, BinaryOperatorNode(token, right.left, left))
                }
            }
        }
        // exp1^exp2 * exp1^exp3 -> exp1^(exp2 + exp3)
        if(left is BinaryOperatorNode && left.token is Power && right is BinaryOperatorNode && right.token is Power){
            if(left.left.equals(right.left)) return BinaryOperatorNode(Power(), left.left, BinaryOperatorNode(Plus(), left.right, right.right))
        }
        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteDivide(token: Divide, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        // 1 / 1 / exp -> exp
        if(left.token.value == "1" && right is BinaryOperatorNode && right.token is Divide && right.left.token.value == "1") return right.right
        // 0 / exp -> 0
        if(left.token.value == "0") return OperandNode(OperandToken("0"))
        // TODO handle division of numbers with expressions involved, for example 3x/6 -> either x/2 or 0.5x
        // (exp1 * exp2) / exp1 -> exp2
        if(left is BinaryOperatorNode && left.token is Multiplication){
            if(left.left.equals(right)) return left.right
            if(left.right.equals(right)) return left.left
        }
        // exp1 / (exp1 * exp2) -> 1 / exp2
        if(right is BinaryOperatorNode && right.token is Multiplication){
            if(right.left.equals(left)) return BinaryOperatorNode(Divide(), OperandNode(OperandToken("1")), right.right)
            if(right.right.equals(left)) return BinaryOperatorNode(Divide(), OperandNode(OperandToken("1")), right.left)
        }
        // (exp1 * exp2) / (exp1 * exp3) -> exp2 / exp3
        if(right is BinaryOperatorNode && left is BinaryOperatorNode && left.token is Multiplication && right.token is Multiplication){
            if(left.left.equals(right.left)) return BinaryOperatorNode(Divide(), left.right, right.right)
            if(left.left.equals(right.right)) return BinaryOperatorNode(Divide(), left.right, right.left)
            if(left.right.equals(right.left)) return BinaryOperatorNode(Divide(), left.left, right.right)
            if(left.right.equals(right.right)) return BinaryOperatorNode(Divide(), left.left, right.left)
        }
        // ((exp1 * exp2) * exp3) / exp2 -> exp1 * exp3
        if(left is BinaryOperatorNode && left.token is Multiplication && left.left is BinaryOperatorNode && left.left.token is Multiplication){
            if(left.left.left.equals(right)) return BinaryOperatorNode(Multiplication(), left.left.right, left.right)
            if(left.left.right.equals(right)) return BinaryOperatorNode(Multiplication(), left.left.left, left.right)
        }
        // ((exp1 * exp2) * exp3) / (exp2 * exp4) -> (exp1 * exp3) / exp4
        if(left is BinaryOperatorNode && left.token is Multiplication
                && left.left is BinaryOperatorNode && left.left.token is Multiplication
                && right is BinaryOperatorNode && right.token is Multiplication){
            if(left.left.left.equals(right.right)) return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.right, left.right), right.left)
            if(left.left.left.equals(right.left)) return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.right, left.right), right.right)
            if(left.left.right.equals(right.right)) return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.left, left.right), right.left)
            if(left.left.right.equals(right.left)) return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.left, left.right), right.right)

        }
        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewritePower(token: Power, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteUnary(token: Token, middle: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        // +exp -> exp
        if(token is UnaryPlus) return middle
        // --exp -> exp
        if(token is UnaryMinus && middle is UnaryOperatorNode && middle.token is UnaryMinus) return middle.middle

        // Inverses
        if(middle is UnaryOperatorNode
                && ((token is Sin && middle.token is ArcSin)
                    || (token is ArcSin && middle.token is Sin)
                    || (token is Cos && middle.token is ArcCos)
                    || (token is ArcCos && middle.token is Cos)
                    || (token is Tan && middle.token is ArcTan)
                    || (token is ArcTan && middle.token is Tan)))
            return middle.middle
        if(token is Sqrt && middle is BinaryOperatorNode && middle.token is Power && middle.right.token.value == "2")
            return UnaryOperatorNode(Abs(), middle.left)

        finished = false
        return UnaryOperatorNode(token, middle)
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
        finished = true
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
        finished = true
        return OperandNode(OperandToken(result.toString()))
    }
}
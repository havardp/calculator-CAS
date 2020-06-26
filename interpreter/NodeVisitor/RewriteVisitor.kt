package calculator.interpreter.NodeVisitor

import calculator.exception.InvalidSyntaxException
import calculator.exception.NotAnOperatorException
import calculator.interpreter.Interpreter
import calculator.lexer.Token.*
import calculator.parser.*
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

// Preorder traversal, because we want to rewrite before we evaluate
// for example 3*1/3 should be rewritten, not evaluated, or else it equals 0.99... and not 1, which it should be
// todo: either post or pre order. post order is less errors, since we can evaluate first. pre order is more precise, since 3*(1/3) -> 1 instead of 0.99...
class RewriteVisitor: NodeVisitor() {
    private var finished = false
    private val PRECISION = 4
    private val CONTEXT = MathContext(PRECISION, RoundingMode.HALF_UP)

    fun resetFinished(){
        finished = false
    }

    override fun visit(node: BinaryOperatorNode): AbstractSyntaxTree {
        // if equal is child of equal, then there was >1 equal in the equation, and it is not valid
        if(node.token is Equal && (node.left.token is Equal || node.right.token is Equal)) throw InvalidSyntaxException("Cannot have more than one equal operator")
        // if equal is child of another node, then a equal operator was inside a parenthesis and is not the root of the tree
        if(node.left.token is Equal || node.right.token is Equal) throw InvalidSyntaxException("Cannot have equal operator in parenthesis")

        // if this is right child and left child did a change then we return
        if (finished) return node

        /*// visit left and right and return if they did a change
        val left = node.left.accept(this)
        val right = node.right.accept(this)
        if(finished) return BinaryOperatorNode(node.token, left, right)*/

        // try to rewrite the binary operator
        val rewrittenNode = when (node.token) {
            is Plus -> rewritePlus(node.token, node.left, node.right)
            is Minus -> rewriteMinus(node.token, node.left, node.right)
            is Multiplication -> rewriteMultiplication(node.token, node.left, node.right)
            is Divide -> rewriteDivide(node.token, node.left, node.right)
            is Power -> rewritePower(node.token, node.left, node.right)
            is Equal -> rewriteEqual(node.token, node.left, node.right)
            else -> throw NotAnOperatorException("tried to handle non binary operator as binary operator")
        }
        // if we did a change, return the rewritten node
        if(finished) return rewrittenNode

        // if both child nodes are operand, then we evaluate it
        if(node.left.token is OperandToken && node.right.token is OperandToken) return evaluateBinary(node.token, node.left.token, node.right.token)

        // else visit the children and return
        return BinaryOperatorNode(node.token, node.left.accept(this), node.right.accept(this))
    }

    override fun visit(node: UnaryOperatorNode): AbstractSyntaxTree{
        if(node.middle.token is Equal) throw InvalidSyntaxException("Cannot have equal operator in a unary operator")

        if(finished) return node

        // if child node is operand, then we evaluate it
        if(node.middle.token is OperandToken) return evaluateUnary(node.token, node.middle.token)

        val rewrittenNode = rewriteUnary(node.token, node.middle)
        // If finished is true here that means that we did a rewrite in the code above, thus we return the rewritten node
        if(finished) return rewrittenNode

        // else visit child and return
        return UnaryOperatorNode(node.token, node.middle.accept(this))
    }

    override fun visit(node: OperandNode): AbstractSyntaxTree {
        return node
    }

    override fun visit(node: VariableNode): AbstractSyntaxTree {
        return node
    }

    // todo visit constant node, return the appropriate operand node, hm can't do this if i do post order
    // might have a button that's like decimal approximation, and just never evaluate constant here
    // when lexing euler number, should only be valid if index is 0, or index == pointer or whatever

    private fun rewriteEqual(token: Equal, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true

        // naive check for if it is a quadratic equation, should probably do this outside and have another visitor or something which handles it, for now it will do here
        // only actually checks if it is ordered before, also this doesn't account for unary minus before x^2 nor operand before x^2
        if(left is BinaryOperatorNode && left.left is BinaryOperatorNode
                && left.left.left is BinaryOperatorNode && left.left.left.token is Power && left.left.left.left is VariableNode && left.left.left.right.token.value == "2"
                && (left.left.right is VariableNode || (left.left.right is BinaryOperatorNode && left.left.right.token is Multiplication && left.left.right.right is VariableNode))
                && left.right is OperandNode && right is OperandNode && right.token.value == "0"){
            println("this is quadratic equation")
        }
        if(left is BinaryOperatorNode
                && left.left is BinaryOperatorNode && left.left.token is Power && left.left.left is VariableNode && left.left.right.token.value == "2"
                && left.right is OperandNode && right is OperandNode && right.token.value == "0") {
            println("this is also quadratic equation, without x factor")
        }


        // variable on both sides
        if(left.containsVariable() && right.containsVariable()){

        }

        // variable only on left side
        if(left.containsVariable()){
            if(left is BinaryOperatorNode){
                if(left.token is Plus){
                    if(!left.left.containsVariable())
                        return BinaryOperatorNode(Equal(), left.right, BinaryOperatorNode(Minus(), right, left.left))
                }
            }
        }

        // variable only on right side
        if(right.containsVariable()){

        }

        finished = false
        return BinaryOperatorNode(Equal(), left, right)
    }

    // exp is expression, basically a abstract syntax tree, a node
    // op is operand
    // var is variable
    private fun rewritePlus(token: Token, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true

        /**
         *   IDENTITY: remove redundant plus zero
         *       +       ->      exp
         *    exp  0     ->
         */
        if (right.token.value == "0") return left

        /**
         *   IDENTITY: remove redundant plus zero
         *       +       ->      exp
         *     0  exp    ->
         */
        if(left.token.value == "0") return right

        /**
         *   DISTRIBUTION: Adds two equal expressions together (not operands)
         *        +        ->        *
         *   exp1  exp1    ->      2  exp1
         */
        if(left !is OperandNode && left.equals(right))
            return BinaryOperatorNode(Multiplication(), OperandNode(OperandToken("2")), left)

        /**
         *   change unary minus to actual minus
         *       +          ->         -
         *    exp1 (-)      ->    exp1  exp2
         *        exp2
         */
        if(right is UnaryOperatorNode && right.token is UnaryMinus && right.middle !is UnaryOperatorNode)
            return BinaryOperatorNode(Minus(), left, right.middle)

        /**
         *   ADDITIVE INVERSE
         *       +          ->        0
         *    (-) exp1
         *   exp1
         */
        if(left is UnaryOperatorNode && left.token is UnaryMinus && left.middle.equals(right))
            return OperandNode(OperandToken("0"))

        /**
         *   Change plus negative op to minus positive op
         *       +          ->        -
         *    exp *         ->    exp   *
         *   neg-op exp     ->     pos-op exp
         */
        if(right is BinaryOperatorNode && right.token is Multiplication
                && right.left is OperandNode && right.left.token is OperandToken && right.left.token.value.toBigDecimal() < 0.toBigDecimal())
            return BinaryOperatorNode(Minus(), left, BinaryOperatorNode(Multiplication(), evaluateUnary(UnaryMinus(), right.left.token), right.right))

        /** COMMUTATIVITY && ASSOCIATIVITY */
        if(left is BinaryOperatorNode){
            if (left.token is Plus){

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *   where exp1 = exp3 && exp2 != exp3
                 *         +            ->           +
                 *      +    exp1       ->        +    exp2
                 *  exp2  exp3          ->   exp1  exp3
                 */
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || (right.equals(left.right) && !right.equals(left.left)))
                    return BinaryOperatorNode(token, BinaryOperatorNode(token, left.right, right), left.left)

                /**
                 *   COMMUTATIVE && ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *   where exp2 = exp3 && exp1 != exp2
                 *           +             ->           +
                 *        +    exp1        ->        +    exp3
                 *   exp2  exp3            ->   exp2  exp1
                 */
                if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.equals(left.left) && !right.equals(left.right)))
                    return BinaryOperatorNode(token, BinaryOperatorNode(token, left.left, right), left.right)

                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1 and none of the factors in left.left is equal to factors in left.right
                 *           +             ->           +
                 *        +    exp1        ->      exp2   +
                 *   exp2   *              ->            *  exp1
                 *      exp3 exp4          ->       exp3  exp4
                 */
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && right.token !is Multiplication && right !is OperandNode
                        && (left.right.right.equals(right) || left.right.left.equals(right))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.left)
                                    || left.left.left.equals(left.right.right)
                                    || left.left.right.equals(left.right.left)
                                    || left.left.right.equals(left.right.right))))
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Plus(), left.right, right))

                if(right is BinaryOperatorNode && right.token is Multiplication
                        && !left.right.equals(left.left) && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)) {
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            +
                     *       +         *          ->       exp2   +
                     *   exp1 exp2 exp3 exp4      ->         exp1   *
                     *                            ->             exp3 exp4
                     */
                    if(left.left.equals(right.right) || left.left.equals(right.left))
                        return BinaryOperatorNode(Plus(), left.right, BinaryOperatorNode(Plus(), left.left, right))

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            +
                     *       +         *          ->       exp1   +
                     *   exp1 exp2 exp3 exp4      ->         exp2   *
                     *                            ->             exp3 exp4
                     */
                    if(left.right.equals(right.right) || left.right.equals(right.left))
                        return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Plus(), left.right, right))
                }

                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes (and there isn't one between between left.left and left.right)
                 *            +             ->           +
                 *        +        *        ->      exp1     +
                 *   exp1   *  exp2  exp3   ->          *         *
                 *     exp4  exp5           ->     exp4  exp5 exp2 exp3
                 */
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && (left.right.right.equals(right.right) || left.right.left.equals(right.right)
                                || left.right.right.equals(right.left) || left.right.left.equals(right.left))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.right)
                                || left.left.left.equals(left.right.left)
                                || left.left.right.equals(left.right.right)
                                || left.left.right.equals(left.right.left))))
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Plus(), left.right, right))

            }
            else if (left.token is Minus){
                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *   where exp3 = exp1 && exp2 != exp1
                 *         +            ->           +
                 *       -    exp1      ->       exp2    -
                 *  exp2  exp3          ->           exp1  exp3
                 */
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || (right.equals(left.right) && !right.equals(left.left)))
                    return BinaryOperatorNode(token, left.left, BinaryOperatorNode(left.token, right, left.right))

                /**
                 *   COMMUTATIVE && ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *           +            ->           -
                 *        -    exp1       ->        +    exp3
                 *   exp2  exp3           ->   exp1   exp2
                 */
                if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.equals(left.left) && !right.equals(left.right)))
                    return BinaryOperatorNode(left.token, BinaryOperatorNode(token, right, left.left), left.right)

                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1
                 *           +             ->           +
                 *        -    exp1        ->      exp2   -
                 *   exp2   *              ->           exp1  *
                 *      exp3 exp4          ->            exp3  exp4
                 */
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && right.token !is Multiplication && right !is OperandNode
                        && (left.right.right.equals(right) || left.right.left.equals(right))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.left)
                                || left.left.left.equals(left.right.right)
                                || left.left.right.equals(left.right.left)
                                || left.left.right.equals(left.right.right))))
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), right, left.right))

                if(right is BinaryOperatorNode && right.token is Multiplication
                        && !left.right.equals(left.left) && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)){
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            -
                     *       -         *          ->         +    exp2
                     *   exp1 exp2 exp3 exp4      ->    exp1   *
                     *                            ->       exp3 exp4
                     */
                    if(left.left.equals(right.right) || left.left.equals(right.left))
                        return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left.left, right), left.right)

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            +
                     *       -         *          ->       exp1   -
                     *   exp1 exp2 exp3 exp4      ->           *  exp2
                     *                            ->       exp3 exp4
                     */
                    if(left.right.equals(right.right) || left.right.equals(right.left))
                        return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), right, left.right))
                }

                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes
                 *            +             ->           +
                 *        -        *        ->      exp1     -
                 *   exp1   *  exp2  exp3   ->          *         *
                 *     exp4  exp5           ->     exp2  exp3 exp4 exp5
                 */
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && (left.right.right.equals(right.right) || left.right.left.equals(right.right)
                                || left.right.right.equals(right.left) || left.right.left.equals(right.left))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.right)
                                || left.left.left.equals(left.right.left)
                                || left.left.right.equals(left.right.right)
                                || left.left.right.equals(left.right.left))))
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), right, left.right))
            }
        }

        /** COMMUTATIVITY && ASSOCIATIVITY */
        if(right is BinaryOperatorNode){
            if (right.token is Plus){
                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp3 && exp1 != exp2
                 *         +             ->           +
                 *    exp1   +           ->     exp2    +
                 *       exp2  exp3      ->         exp1  exp3
                 */
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || (left.equals(right.right) && !left.equals(right.left)))
                    return BinaryOperatorNode(token, right.left, BinaryOperatorNode(token, right.right, left))

                /**
                 *   ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp2 && exp1 != exp3
                 *         +             ->           +
                 *    exp1   +           ->        +   exp3
                 *       exp2  exp3      ->    exp1  exp2
                 */
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.equals(right.left) && !left.equals(right.right)))
                    return BinaryOperatorNode(token, BinaryOperatorNode(token, left, right.left), right.right)

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1 and none of the factors in right.left is equal to factors in right.right
                 *           +              ->           +
                 *       exp1  +            ->      exp2   +
                 *         exp2  *          ->          exp1 *
                 *           exp3  exp4     ->           exp3  exp4
                 */
                if(right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && left.token !is Multiplication && left !is OperandNode
                        && (right.right.right.equals(left) || right.right.left.equals(left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.left)
                                || right.left.left.equals(right.right.right)
                                || right.left.right.equals(right.right.left)
                                || right.left.right.equals(right.right.right))))
                    return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Plus(), left, right.right))

                if(left is BinaryOperatorNode && left.token is Multiplication && !left.right.equals(left.left)
                        && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)){
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            +
                     *       *          +         ->        exp4  +
                     *   exp1 exp2  exp3 exp4     ->          exp3   *
                     *                            ->             exp1 exp2
                     */
                    if(right.left.equals(left.right) || right.left.equals(left.left))
                        return BinaryOperatorNode(Plus(),  right.right, BinaryOperatorNode(Plus(), right.left, left))

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            +
                     *       *         +          ->       exp3   +
                     *   exp1 exp2 exp3 exp4      ->          exp4 *
                     *                            ->           exp3 exp4
                     */
                    if(right.right.equals(left.right) || right.right.equals(left.left))
                        return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Plus(), right.right, left))
                }

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes (and there isn't one between between right.left and right.right)
                 *             +               ->           +
                 *       *           +         ->      exp3     +
                 *   exp1  exp2  exp3  *       ->          *         *
                 *                 exp4 exp5   ->     exp1  exp2 exp4 exp5
                 */
                if(left is BinaryOperatorNode && left.token is Multiplication
                        && right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && (right.right.right.equals(left.right) || right.right.left.equals(left.right)
                                || right.right.right.equals(left.left) || right.right.left.equals(left.left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.right)
                                || right.left.left.equals(right.right.left)
                                || right.left.right.equals(right.right.right)
                                || right.left.right.equals(right.right.left))))
                    return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Plus(), left, right.right))
            }
            if (right.token is Minus){

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp3 && exp1 != exp2
                 *         +             ->           +
                 *    exp1   -           ->     exp2    -
                 *       exp2  exp3      ->         exp1  exp3
                 */
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || (left.equals(right.right) && !left.equals(right.left)))
                    return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Minus(), left, right.right))

                /**
                 *   ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp2 && exp1 != exp3
                 *         +             ->           -
                 *    exp1   -           ->        +   exp3
                 *       exp2  exp3      ->    exp1  exp2
                 */
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.equals(right.left) && !left.equals(right.right)))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left, right.left), right.right)

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1 and none of the factors in right.left is equal to factors in right.right
                 *           +              ->           +
                 *       exp1  -            ->      exp2   -
                 *         exp2  *          ->          exp1 *
                 *           exp3  exp4     ->           exp3  exp4
                 */
                if(right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && left !is OperandNode && left.token !is Multiplication
                        && (right.right.right.equals(left) || right.right.left.equals(left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.left)
                                || right.left.left.equals(right.right.right)
                                || right.left.right.equals(right.right.left)
                                || right.left.right.equals(right.right.right))))
                    return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Minus(), left, right.right))

                if(left is BinaryOperatorNode && left.token is Multiplication
                        && !left.right.equals(left.left) && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)){
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            -
                     *       *          -         ->          +  exp4
                     *   exp1 exp2  exp3 exp4     ->        exp3   *
                     *                            ->           exp1 exp2
                     */
                    if(right.left.equals(left.right) || right.left.equals(left.left))
                        return BinaryOperatorNode(Minus(),  BinaryOperatorNode(Plus(), right.left, left), right.right)

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            +               ->            +
                     *       *         -          ->       exp3   -
                     *   exp1 exp2 exp3 exp4      ->            *  exp4
                     *                            ->        exp1 exp2
                     */
                    if(right.right.equals(left.right) || right.right.equals(left.left))
                        return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Minus(), left, right.right))
                }

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes (and there isn't one between between right.left and right.right)
                 *             +               ->           +
                 *       *           -         ->      exp3     -
                 *   exp1  exp2  exp3  *       ->          *         *
                 *                 exp4 exp5   ->     exp1  exp2 exp4 exp5
                 */
                if(left is BinaryOperatorNode && left.token is Multiplication
                        && right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && (right.right.right.equals(left.right) || right.right.left.equals(left.right)
                                || right.right.right.equals(left.left) || right.right.left.equals(left.left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.right)
                                || right.left.left.equals(right.right.left)
                                || right.left.right.equals(right.right.right)
                                || right.left.right.equals(right.right.left))))
                    return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Minus(), left, right.right))
            }
        }

        /**  Distributivity: Distributes a common factor out
         *             +               ->           *
         *       *           *         ->      exp     +
         *   exp  exp   exp  exp       ->           exp  exp
         */
        if (left is BinaryOperatorNode && right is BinaryOperatorNode){
            if(left.token is Multiplication && right.token is Multiplication){
                if(left.left.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Plus(), left.right, right.right))
                if(left.left.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Plus(), left.right, right.left))
                if(left.right.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Plus(), left.left, right.right))
                if(left.right.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Plus(), left.left, right.left))
            }
        }

        /**  IDENTITY: Multiplies by one so that we can use distributivity laws above
         *          +           ->            +
         *       *    exp1      ->        *        *
         *   exp2  exp3         ->   exp2  exp3  1  exp1
         */
        if(left is BinaryOperatorNode && left.token is Multiplication && right !is OperandNode)
            if(left.right.equals(right) || left.left.equals(right))
                return BinaryOperatorNode(token, left, BinaryOperatorNode(Multiplication(), OperandNode(OperandToken("1")), right))

        /**  IDENTITY: Multiplies by one so that we can use distributivity laws above
         *          +           ->              +
         *     exp1     *       ->        *          *
         *         exp2  exp3   ->     1   exp1  exp2  exp3
         */
        if(right is BinaryOperatorNode && right.token is Multiplication && left !is OperandNode)
            if(right.right.equals(left) || right.left.equals(left))
                return BinaryOperatorNode(token, BinaryOperatorNode(Multiplication(), OperandNode(OperandToken("1")), left), right)

        /**
         *  COMMUTATIVITY: move unary||multiplication||divide operators to the right so we can move it to the top with the code below
         *       +       ->        +
         *  exp1   exp2  ->   exp2  exp1
         */
        if((left.token is Multiplication || left.token is Divide || left is UnaryOperatorNode && (left.token !is UnaryMinus || left.middle is UnaryOperatorNode))
                && !(right.token is Multiplication || right.token is Divide || right is UnaryOperatorNode && (right.token !is UnaryMinus || right.middle is UnaryOperatorNode)))
            return BinaryOperatorNode(Plus(), right, left)

        /**
         *  COMMUTATIVITY: move multiplication operator to the right if it contains unary operator and the other one does not
         *          +            ->           +
         *      *        *       ->     *           *
         *  exp1 exp2 exp3 exp4  ->  exp3 exp4  exp1 exp2
         */
        if(left is BinaryOperatorNode && left.token is Multiplication && left.right is UnaryOperatorNode
                && right is BinaryOperatorNode && right.token is Multiplication && right.right !is UnaryOperatorNode && right.left !is UnaryOperatorNode)
            return BinaryOperatorNode(Plus(), right, left)

        /**
         *   COMMUTATIVITY AND ASSOCIATIVITY: Move unary operators (or multiplication with unary) up the tree
         *          +         ->          +
         *       +     exp1   ->       +      exp3
         *   exp2   exp3      ->   exp2 exp1
         */
        if(left is BinaryOperatorNode && left.token is Plus
                && ((right !is UnaryOperatorNode
                        || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode))
                        && (right.token !is Multiplication || (right as BinaryOperatorNode).right !is UnaryOperatorNode))
                && (left.right is UnaryOperatorNode
                        || (left.right is BinaryOperatorNode
                        && (left.right.token is Multiplication && !( left.left is BinaryOperatorNode && left.left.right.equals(left.right.right)))
                        && left.right.right is UnaryOperatorNode)))
                    return BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), left.left, right), left.right)


        /**
         *   COMMUTATIVITY AND ASSOCIATIVITY: Move unary operators (or multiplication with unary) up the tree
         *          +         ->          -
         *       -     exp1   ->       +      exp3
         *   exp2   exp3      ->   exp2 exp1
         */
        if(left is BinaryOperatorNode && left.token is Plus
                && ((right !is UnaryOperatorNode
                        || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode))
                        && (right.token !is Multiplication || (right as BinaryOperatorNode).right !is UnaryOperatorNode))
                && (left.right is UnaryOperatorNode
                        || (left.right is BinaryOperatorNode
                        && (left.right.token is Multiplication && !( left.left is BinaryOperatorNode && left.left.right.equals(left.right.right)))
                        && left.right.right is UnaryOperatorNode)))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left.left, right), left.right)

        //TODO this is (2+x)+(3+x), need to do this 4 times, so that we check +- -+ and -- as well
        /**
         *   COMMUTATIVITY AND ASSOCIATIVITY: move operands or variables closer together
         *             +             ->           +
         *       +          +        ->        +     exp1
         *  exp1  exp2 exp3  exp4    ->     +    exp2
         *                              exp3 exp4
         */
        if (left is BinaryOperatorNode && left.token is Plus && right is BinaryOperatorNode && right.token is Plus){
            if(left.right is OperandNode
                    && (right.left is OperandNode && right.right !is OperandNode
                            || right.right is OperandNode && right.left !is OperandNode))
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), right, left.right), left.left)
            if(left.left is OperandNode
                    && (right.left is OperandNode && right.right !is OperandNode
                            || right.right is OperandNode && right.left !is OperandNode))
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), right, left.left), left.right)
        }

        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteMinus(token: Minus, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true

        /**
         *   IDENTITY: remove redundant plus zero
         *       -       ->      exp
         *    exp  0     ->
         */
        if (right.token.value == "0") return left

        /**
         *   IDENTITY: remove redundant plus zero
         *       -       ->     (-)
         *     0  exp    ->     exp
         */
        if(left.token.value == "0") {
            if(right.token is OperandToken) return evaluateUnary(UnaryMinus(), right.token) // if right is operand, we can just negate it right away
            return UnaryOperatorNode(UnaryMinus() ,right)
        }

        /**
         *   Subtracts two equal expressions together (not operands)
         *        -        ->        0
         *   exp1  exp1    ->
         */
        if(left.equals(right)) return OperandNode(OperandToken("0"))

        /**
         *   change unary minus to plus
         *       -          ->         +
         *    exp1 (-)      ->    exp1  exp2
         *        exp2
         */
        if(right is UnaryOperatorNode && right.token is UnaryMinus && left !is UnaryOperatorNode)
            return BinaryOperatorNode(Plus(), left, right.middle)

        /**
         *   Change minus negative op to plus positive op
         *       -          ->        +
         *    exp *         ->    exp   *
         *   neg-op exp     ->     pos-op exp
         */
        if(right is BinaryOperatorNode && right.token is Multiplication
                && right.left is OperandNode && right.left.token is OperandToken && right.left.token.value.toBigDecimal() < 0.toBigDecimal())
            return BinaryOperatorNode(Plus(), left, BinaryOperatorNode(Multiplication(), evaluateUnary(UnaryMinus(), right.left.token), right.right))

        /** COMMUTATIVITY && ASSOCIATIVITY */
        if(left is BinaryOperatorNode){
            if (left.token is Plus){

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *   where exp1 = exp3 && exp2 != exp3
                 *         -            ->           +
                 *      +    exp1       ->      exp2   -
                 *  exp2  exp3          ->         exp3  exp1
                 */
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || (right.equals(left.right) && !right.equals(left.left)))
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), left.right, right))

                /**
                 *   COMMUTATIVE && ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *   where exp2 = exp3 && exp1 != exp2
                 *           -             ->           +
                 *        +    exp1        ->      exp3  -
                 *   exp2  exp3            ->        exp2  exp1
                 */
                if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.equals(left.left) && !right.equals(left.right)))
                    return BinaryOperatorNode(Plus(), left.right, BinaryOperatorNode(Minus(), left.left, right))

                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1 and none of the factors in left.left is equal to factors in left.right
                 *           -             ->           +
                 *        +    exp1        ->      exp2   -
                 *   exp2   *              ->            *  exp1
                 *      exp3 exp4          ->       exp3  exp4
                 */
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && right.token !is Multiplication && right !is OperandNode
                        && (left.right.right.equals(right) || left.right.left.equals(right))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.left)
                                || left.left.left.equals(left.right.right)
                                || left.left.right.equals(left.right.left)
                                || left.left.right.equals(left.right.right))))
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), left.right, right))

                if(right is BinaryOperatorNode && right.token is Multiplication
                        && !left.right.equals(left.left) && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)){
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            +
                     *       +         *          ->       exp2   -
                     *   exp1 exp2 exp3 exp4      ->         exp1   *
                     *                            ->             exp3 exp4
                     */
                    if (left.left.equals(right.right) || left.left.equals(right.left))
                        return BinaryOperatorNode(Plus(), left.right, BinaryOperatorNode(Minus(), left.left, right) )

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            +
                     *       +         *          ->       exp1   -
                     *   exp1 exp2 exp3 exp4      ->         exp2   *
                     *                            ->             exp3 exp4
                     */
                    if(left.right.equals(right.right) || left.right.equals(right.left))
                        return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), left.right, right))
                }

                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes (and there isn't one between between left.left and left.right)
                 *            -             ->           +
                 *        +        *        ->      exp1     -
                 *   exp1   *  exp2  exp3   ->          *         *
                 *     exp4  exp5           ->     exp4  exp5 exp2 exp3
                 */
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && (left.right.right.equals(right.right) || left.right.left.equals(right.right)
                                || left.right.right.equals(right.left) || left.right.left.equals(right.left))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.right)
                                || left.left.left.equals(left.right.left)
                                || left.left.right.equals(left.right.right)
                                || left.left.right.equals(left.right.left))))
                    return BinaryOperatorNode(Plus(), left.left, BinaryOperatorNode(Minus(), left.right, right))
            }
            else if (left.token is Minus){
                /**
                 *   ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *   where exp3 = exp1 && exp2 != exp1
                 *         -            ->           -
                 *       -    exp1      ->       exp2    +
                 *  exp2  exp3          ->           exp3 exp1
                 */
                if((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || (right.equals(left.right) && !right.equals(left.left)))
                    return BinaryOperatorNode(Minus(), left.left, BinaryOperatorNode(Plus(), left.right, right))

                /**
                 *   COMMUTATIVE && ASSOCIATIVITY: move variable and operands together so we can evaluate them
                 *           -            ->           -
                 *        -    exp1       ->        -    exp3
                 *   exp2  exp3           ->   exp2   exp1
                 */
                if((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                        || (right.equals(left.left) && !right.equals(left.right)))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left.left, right), left.right)

                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1
                 *           -             ->           -
                 *        -    exp1        ->      exp2   +
                 *   exp2   *              ->           exp1  *
                 *      exp3 exp4          ->            exp3  exp4
                 */
                if(left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && right.token !is Multiplication && right !is OperandNode
                        && (left.right.right.equals(right) || left.right.left.equals(right))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.left)
                                || left.left.left.equals(left.right.right)
                                || left.left.right.equals(left.right.left)
                                || left.left.right.equals(left.right.right))))
                    return BinaryOperatorNode(Minus(), left.left, BinaryOperatorNode(Plus(), right, left.right))

                if(right is BinaryOperatorNode && right.token is Multiplication && !left.right.equals(left.left)
                        && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)){
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            -
                     *       -         *          ->         -    exp2
                     *   exp1 exp2 exp3 exp4      ->    exp1   *
                     *                            ->       exp3 exp4
                     */
                    if(left.left.equals(right.right) || left.left.equals(right.left))
                        return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left.left, right), left.right)

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            -
                     *       -         *          ->       exp1   +
                     *   exp1 exp2 exp3 exp4      ->            *  exp2
                     *                            ->        exp3 exp4
                     */
                    if(left.right.equals(right.right) || left.right.equals(right.left))
                        return BinaryOperatorNode(Minus(), left.left, BinaryOperatorNode(Plus(), right, left.right))
                }


                /**
                 *   ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes
                 *            -             ->           -
                 *        -        *        ->      exp1     +
                 *   exp1   *  exp2  exp3   ->          *         *
                 *     exp4  exp5           ->     exp4  exp5 exp2 exp3
                 */
                if(right is BinaryOperatorNode && right.token is Multiplication
                        && left.right is BinaryOperatorNode && left.right.token is Multiplication
                        && (left.right.right.equals(right.right) || left.right.left.equals(right.right)
                                || left.right.right.equals(right.left) || left.right.left.equals(right.left))
                        && (left.left.token !is Multiplication
                                || !((left.left as BinaryOperatorNode).left.equals(left.right.right)
                                || left.left.left.equals(left.right.left)
                                || left.left.right.equals(left.right.right)
                                || left.left.right.equals(left.right.left))))
                    return BinaryOperatorNode(Minus(), left.left, BinaryOperatorNode(Plus(), right, left.right))
            }
        }

        /** COMMUTATIVITY && ASSOCIATIVITY */
        if(right is BinaryOperatorNode){
            if (right.token is Plus){
                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp3 && exp1 != exp2
                 *         -             ->           -
                 *    exp1   +           ->        -     exp2
                 *       exp2  exp3      ->   exp1  exp3
                 */
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || (left.equals(right.right) && !left.equals(right.left)))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left, right.right), right.left)


                /**
                 *   ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp2 && exp1 != exp3
                 *         -             ->           -
                 *    exp1   +           ->        -   exp3
                 *       exp2  exp3      ->    exp1  exp2
                 */
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.equals(right.left) && !left.equals(right.right)))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left, right.left), right.right)

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1 and none of the factors in right.left is equal to factors in right.right
                 *           -              ->           -
                 *       exp1  +            ->        -    exp2
                 *         exp2  *          ->    exp1  *
                 *           exp3  exp4     ->     exp3  exp4
                 */
                if(right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && left.token !is Multiplication && left !is OperandNode
                        && (right.right.right.equals(left) || right.right.left.equals(left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.left)
                                || right.left.left.equals(right.right.right)
                                || right.left.right.equals(right.right.left)
                                || right.left.right.equals(right.right.right))))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left, right.right), right.left)

                if(left is BinaryOperatorNode && left.token is Multiplication
                        && !left.right.equals(left.left) && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)){
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            -
                     *       *          +         ->        -       exp4
                     *   exp1 exp2  exp3 exp4     ->      *  exp3
                     *                            ->  exp1 exp2
                     */
                    if(right.left.equals(left.right) || right.left.equals(left.left))
                        return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left, right.left), right.right)

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            -
                     *       *          +         ->        -       exp3
                     *   exp1 exp2  exp3 exp4     ->      *  exp4
                     *                            ->  exp1 exp2
                     */
                    if(right.right.equals(left.right) || right.right.equals(left.left))
                        return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left, right.right), right.left)
                }

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes (and there isn't one between between right.left and right.right)
                 *             -               ->                 -
                 *       *           +         ->            -       exp3
                 *   exp1  exp2  exp3  *       ->       *         *
                 *                 exp4 exp5   ->  exp1  exp2 exp4 exp5
                 */
                if(left is BinaryOperatorNode && left.token is Multiplication
                        && right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && (right.right.right.equals(left.right) || right.right.left.equals(left.right)
                                || right.right.right.equals(left.left) || right.right.left.equals(left.left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.right)
                                || right.left.left.equals(right.right.left)
                                || right.left.right.equals(right.right.right)
                                || right.left.right.equals(right.right.left))))
                    return BinaryOperatorNode(Plus(), right.left, BinaryOperatorNode(Plus(), left, right.right))
            }
            else if (right.token is Minus){
                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp3 && exp1 != exp2
                 *         -             ->           -
                 *    exp1   -           ->       +      exp2
                 *       exp2  exp3      ->   exp1  exp3
                 */
                if((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || (left.equals(right.right) && !left.equals(right.left)))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left, right.right), right.left)


                /**
                 *   ASSOCIATIVITY: move variable and operands together so we can evaluate them (also if expressions are equal)
                 *   where exp1 = exp2 && exp1 != exp3
                 *         -             ->           +
                 *    exp1   -           ->        -   exp3
                 *       exp2  exp3      ->    exp1  exp2
                 */
                else if((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.equals(right.left) && !left.equals(right.right)))
                    return BinaryOperatorNode(Plus(), BinaryOperatorNode(Minus(), left, right.left), right.right)

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where exp3 = exp1 || exp4 = exp1 and none of the factors in right.left is equal to factors in right.right
                 *           -              ->           -
                 *       exp1  -            ->        +     exp2
                 *         exp2  *          ->    exp1  *
                 *           exp3  exp4     ->     exp3  exp4
                 */
                if(right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && left.token !is Multiplication && left !is OperandNode
                        && (right.right.right.equals(left) || right.right.left.equals(left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.left)
                                || right.left.left.equals(right.right.right)
                                || right.left.right.equals(right.right.left)
                                || right.left.right.equals(right.right.right))))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left, right.right), right.left)

                if(left is BinaryOperatorNode && left.token is Multiplication
                        && !left.right.equals(left.left) && !right.left.equals(right.right)
                        && !(left.left is OperandNode && left.right is OperandNode || right.left is OperandNode && right.right is OperandNode)) {
                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            +
                     *       *          -         ->          -  exp4
                     *   exp1 exp2  exp3 exp4     ->        *  exp3
                     *                            ->    exp1 exp2
                     */
                    if(right.left.equals(left.right) || right.left.equals(left.left))
                        return BinaryOperatorNode(Plus(),  BinaryOperatorNode(Minus(), left, right.left), right.right)

                    /**  COMMUTATIVITY AND ASSOCIATIVITY: move equal expressions closer together
                     *            -               ->            -
                     *       *         -          ->        +       exp3
                     *   exp1 exp2 exp3 exp4      ->      *  exp4
                     *                            ->  exp1 exp2
                     */
                    if(right.right.equals(left.right) || right.right.equals(left.left))
                        return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left, right.right), right.left)
                }

                /**
                 *   COMMUTATIVITY && ASSOCIATIVITY: move equal expressions together so we can use distributive laws on them
                 *   Where there is a common factor among the two multiplication nodes (and there isn't one between between right.left and right.right)
                 *             -               ->                 -
                 *       *           -         ->            +         exp3
                 *   exp1  exp2  exp3  *       ->       *         *
                 *                 exp4 exp5   ->  exp1  exp2 exp4 exp5
                 */
                if(left is BinaryOperatorNode && left.token is Multiplication
                        && right.right is BinaryOperatorNode && right.right.token is Multiplication
                        && (right.right.right.equals(left.right) || right.right.left.equals(left.right)
                                || right.right.right.equals(left.left) || right.right.left.equals(left.left))
                        && (right.left.token !is Multiplication
                                || !((right.left as BinaryOperatorNode).left.equals(right.right.right)
                                || right.left.left.equals(right.right.left)
                                || right.left.right.equals(right.right.right)
                                || right.left.right.equals(right.right.left))))
                    return BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), left, right.right), right.left)

            }
        }

        /**  Distributivity: Distributes a common factor out
         *             -               ->           *
         *       *           *         ->      exp     -
         *   exp  exp   exp  exp       ->           exp  exp
         */
        if (left is BinaryOperatorNode && right is BinaryOperatorNode){
            if(left.token is Multiplication && right.token is Multiplication){
                if(left.left.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Minus(), left.right, right.right))
                if(left.left.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.left, BinaryOperatorNode(Minus(), left.right, right.left))
                if(left.right.equals(right.left)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Minus(), left.left, right.right))
                if(left.right.equals(right.right)) return BinaryOperatorNode(Multiplication(), left.right, BinaryOperatorNode(Minus(), left.left, right.left))
            }
        }

        /**  IDENTITY: Multiplies by one so that we can use distributivity laws above
         *          -           ->            -
         *       *    exp1      ->        *        *
         *   exp2  exp3         ->   exp2  exp3  1  exp1
         */
        if(left is BinaryOperatorNode && left.token is Multiplication && right !is OperandNode)
            if(left.right.equals(right) || left.left.equals(right))
                return BinaryOperatorNode(Minus(), left, BinaryOperatorNode(Multiplication(), OperandNode(OperandToken("1")), right))

        /**  IDENTITY: Multiplies by one so that we can use distributivity laws above
         *          -           ->            -
         *     exp1     *       ->        *        *
         *         exp2  exp3   ->   exp2  exp3  1   exp1
         */
        if(right is BinaryOperatorNode && right.token is Multiplication && left !is OperandNode)
            if(right.right.equals(left) || right.left.equals(left))
                return BinaryOperatorNode(Minus(), BinaryOperatorNode(Multiplication(), OperandNode(OperandToken("1")), left), right)

        /**
         *  COMMUTATIVITY: move unary||multiplication||divide operators to the right so we can move it to the top with the code below
         *       -       ->        +
         *  exp1   exp2  ->     (-) exp1
         *                     exp2
         */
        if((left.token is Multiplication || left.token is Divide || left is UnaryOperatorNode && (left.token !is UnaryMinus || left.middle is UnaryOperatorNode))
                && !(right.token is Multiplication || right.token is Divide || right is UnaryOperatorNode && (right.token !is UnaryMinus || right.middle is UnaryOperatorNode)))
            return BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(),right), left)

        /**
         *  COMMUTATIVITY: move multiplication operator to the right if it contains unary operator and the other one does not
         *          -            ->           +
         *      *        *       ->     (-)           *
         *  exp1 exp2 exp3 exp4  ->      *        exp1 exp2
         *                           exp3 exp4
         */
        if(left is BinaryOperatorNode && left.token is Multiplication && left.right is UnaryOperatorNode
                && right is BinaryOperatorNode && right.token is Multiplication && right.right !is UnaryOperatorNode && right.left !is UnaryOperatorNode)
            return BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), right), left)

        /**
         *   COMMUTATIVITY AND ASSOCIATIVITY: Move unary operators (or multiplication with unary) up the tree
         *          -         ->          +
         *       +     exp1   ->       -      exp3
         *   exp2   exp3      ->   exp2 exp1
         */
        if(left is BinaryOperatorNode && left.token is Plus
                && ((right !is UnaryOperatorNode
                        || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode))
                        && (right.token !is Multiplication || (right as BinaryOperatorNode).right !is UnaryOperatorNode))
                && (left.right is UnaryOperatorNode
                        || (left.right is BinaryOperatorNode
                        && (left.right.token is Multiplication && !( left.left is BinaryOperatorNode && left.left.right.equals(left.right.right)))
                        && left.right.right is UnaryOperatorNode)))
            return BinaryOperatorNode(Plus(), BinaryOperatorNode(Minus(), left.left, right), left.right)

        /**
         *   COMMUTATIVITY AND ASSOCIATIVITY: Move unary operators (or multiplication with unary) up the tree
         *          -         ->          -
         *       -     exp1   ->       -      exp3
         *   exp2   exp3      ->   exp2 exp1
         */
        if(left is BinaryOperatorNode && left.token is Plus
                && ((right !is UnaryOperatorNode
                        || (right.token is UnaryMinus && right.middle !is UnaryOperatorNode))
                        && (right.token !is Multiplication || (right as BinaryOperatorNode).right !is UnaryOperatorNode))
                && (left.right is UnaryOperatorNode
                        || (left.right is BinaryOperatorNode
                        && (left.right.token is Multiplication && !( left.left is BinaryOperatorNode && left.left.right.equals(left.right.right)))
                        && left.right.right is UnaryOperatorNode)))
            return BinaryOperatorNode(Minus(), BinaryOperatorNode(Minus(), left.left, right), left.right)

        //TODO this is (2+x)-(3+x), need to do this 4 times, so that we check +- -+ and -- as well
        /**
         *   COMMUTATIVITY AND ASSOCIATIVITY: move operands or variables closer together
         *             -             ->           +
         *       +          +        ->        -     exp1
         *  exp1  exp2 exp3  exp4    ->   exp2   +
         *                                   exp3 exp4
         */
        if (left is BinaryOperatorNode && left.token is Plus && right is BinaryOperatorNode && right.token is Plus){
            if(left.right is OperandNode
                    && (right.left is OperandNode && right.right !is OperandNode
                            || right.right is OperandNode && right.left !is OperandNode))
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Minus(), left.right, right), left.left)
            if(left.left is OperandNode
                    && (right.left is OperandNode && right.right !is OperandNode
                            || right.right is OperandNode && right.left !is OperandNode))
                return BinaryOperatorNode(Plus(), BinaryOperatorNode(Minus(), left.left, right), left.right)
        }

        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteMultiplication(token: Multiplication, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true

        /**
         *   CANCELLATION: factor under and over divide cancels out
         *   where exp1 = exp3
         *            *          ->          exp2
         *       exp1   /        ->
         *         exp2  exp3    ->
         */
        if(right is BinaryOperatorNode && right.token is Divide && right.right.equals(left))
            return right.left

        /**
         *   CANCELLATION: factor under and over divide cancels out
         *   where exp1 = exp3
         *            *          ->          exp2
         *         /    exp1     ->
         *    exp2  exp3         ->
         */
        if(left is BinaryOperatorNode && left.token is Divide && left.right.equals(right))
            return left.left


        /**
         *   CANCELLATION: Move operand to the left (x2 -> 2x)
         *            *          ->      *
         *        exp1 exp2      ->   exp2 exp1
         */
        if (left !is OperandNode && right is OperandNode)
            return BinaryOperatorNode(token, right, left)

        /**
         *   IDENTITY: remove redundant 1
         *            *          ->      exp1
         *          1  exp1      ->
         */
        if(left.token.value == "1") return right

        /**
         *   IDENTITY: remove redundant 1
         *            *          ->      exp1
         *        exp1  1        ->
         */
        if(right.token.value == "1") return left

        /**
         *   IDENTITY: if left or right child is 0, expression is 0
         *            *          ->      0
         *        exp1  0        ->
         */
        if(left.token.value == "0" || right.token.value == "0") return OperandNode(OperandToken("0"))

        /**
         *   COMMUTATIVITY: if left or right child is 0, expression is 0
         *            *          ->      0
         *        exp1  0        ->
         */
        if((right !is UnaryOperatorNode || right.token is Minus && right.middle is UnaryOperatorNode)
                && (left is UnaryOperatorNode && (left.token !is UnaryMinus || left.middle is UnaryOperatorNode)))
            return BinaryOperatorNode(Multiplication(), right, left)

        /**
         *   COMMUTATIVITY: move equal expressions together
         *   where exp2 = exp1 && exp1 != exp3
         *            *          ->         *
         *         *    exp1     ->      *     exp3
         *     exp2  exp3        ->  exp2 exp1
         */
        if(left is BinaryOperatorNode && left.token is Multiplication
                && ((right.token is OperandToken && left.left.token is OperandToken && left.right.token !is OperandToken)
                    || (right.equals(left.left) && !right.equals(left.right))))
                    return BinaryOperatorNode(Multiplication(), BinaryOperatorNode(Multiplication(), left.left, right), left.right)

        /**
         *   COMMUTATIVITY: move equal expressions together
         *   where exp2 = exp1 && exp1 != exp3
         *            *          ->         *
         *         *    exp1     ->      *     exp2
         *     exp2  exp3        ->  exp3 exp1
         */
        if(left is BinaryOperatorNode && left.token is Multiplication
                && ((right.token is OperandToken && left.left.token !is OperandToken && left.right.token is OperandToken)
                        || (!right.equals(left.left) && right.equals(left.right))))
            return BinaryOperatorNode(Multiplication(), BinaryOperatorNode(Multiplication(), left.right, right), left.left)

        /**
         *   COMMUTATIVITY: move equal expressions together
         *   where exp2 = exp1 && exp1 != exp3
         *            *          ->         *
         *        exp1  *        ->     exp3  *
         *         exp2  exp3    ->        exp2 exp1
         */
        if(right is BinaryOperatorNode && right.token is Multiplication
                && ((left.token is OperandToken && right.left.token is OperandToken && right.right.token !is OperandToken)
                        || (left.equals(right.left) && !left.equals(right.right))))
            return BinaryOperatorNode(Multiplication(), right.right, BinaryOperatorNode(Multiplication(), right.left, left))

        /**
         *   COMMUTATIVITY: move equal expressions together
         *   where exp2 = exp1 && exp1 != exp3
         *            *          ->         *
         *        exp1  *        ->     exp2  *
         *         exp2  exp3    ->        exp3 exp1
         */
        if(right is BinaryOperatorNode && right.token is Multiplication
                && ((left.token is OperandToken && right.left.token !is OperandToken && right.right.token is OperandToken)
                        || (!left.equals(right.left)) && left.equals(right.right)))
            return BinaryOperatorNode(Multiplication(), right.left, BinaryOperatorNode(Multiplication(), right.right, left))

        /**
         *   For example 3^x * 3^(sin(x)) -> 3^(x+sin(x))
         *   where exp1 = exp3
         *             *            ->         ^
         *        ^        ^        ->     exp1   +
         *    exp1 exp2 exp3 exp4   ->         exp2 exp4
         */
        if(left is BinaryOperatorNode && left.token is Power && right is BinaryOperatorNode && right.token is Power && left.left.equals(right.left))
                return BinaryOperatorNode(Power(), left.left, BinaryOperatorNode(Plus(), left.right, right.right))

        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteDivide(token: Divide, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true
        /**
         *   equivalent simplification, 1/1/exp -> exp
         *            /          ->      exp
         *         1     /       ->
         *            1     exp
         */
        if(left.token.value == "1" && right is BinaryOperatorNode && right.token is Divide && right.left.token.value == "1")
            return right.right

        /**
         *   0 divided by anything is zero (unless the denominator is 0, then it's undefined, don't think i want to handle that though)
         *            /          ->      0
         *         0    exp      ->
         */
        if(left.token.value == "0")
            return OperandNode(OperandToken("0"))

        /**
         *   We cancel a factor above and below the divide. Or if there's two operands we do the calculation, so 2x/3 -> 0.6667x
         *            /           ->      exp3
         *         *    exp1      ->
         *     exp2 exp3
         */
        if(left is BinaryOperatorNode && left.token is Multiplication){
            if(left.left.equals(right))
                return left.right
            if(left.right.equals(right))
                return left.left

            if(right.token is OperandToken && left.left.token is OperandToken && left.right !is OperandNode)
                return BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.left.token, right.token), left.right)
            if(right.token is OperandToken && left.right.token is OperandToken && left.left !is OperandNode)
                return BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.right.token, right.token), left.left)
        }

        /**
         *   We cancel a factor above and below the divide. Or if there's two operands we do the calculation, so 2/3x -> 0.6667 / x
         *            /           ->        /
         *       exp1    *        ->     1     exp3
         *            exp2 exp3   ->
         */
        if(right is BinaryOperatorNode && right.token is Multiplication){
            if(right.left.equals(left))
                return BinaryOperatorNode(Divide(), OperandNode(OperandToken("1")), right.right)
            if(right.right.equals(left))
                return BinaryOperatorNode(Divide(), OperandNode(OperandToken("1")), right.left)

            if(left.token is OperandToken && right.left.token is OperandToken && right.right !is OperandNode)
                return BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.token, right.left.token), right.right)
            if(left.token is OperandToken && right.right.token is OperandToken && right.left !is OperandNode)
                return BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(),left.token, right.right.token), right.left)
        }

        /**
         *   We cancel a factor above and below the divide. Or if there's two operands we do the calculation, so 2sin(x)/3x -> 0.6667 * sin(x) / x
         *             /            ->         /
         *        *        *        ->     exp2 exp3
         *    exp1 exp2 exp3 exp4   ->
         */
        if(right is BinaryOperatorNode && left is BinaryOperatorNode && left.token is Multiplication && right.token is Multiplication){
            if(left.left.equals(right.left)) return BinaryOperatorNode(Divide(), left.right, right.right)
            if(left.left.equals(right.right)) return BinaryOperatorNode(Divide(), left.right, right.left)
            if(left.right.equals(right.left)) return BinaryOperatorNode(Divide(), left.left, right.right)
            if(left.right.equals(right.right)) return BinaryOperatorNode(Divide(), left.left, right.left)

            if(left.left.token is OperandToken && left.right !is OperandNode && right.left.token is OperandToken && right.right !is OperandNode)
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.left.token, right.left.token), left.right), right.right)
            if(left.left.token is OperandToken && left.right !is OperandNode && right.left !is OperandNode && right.right.token is OperandToken)
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.left.token, right.right.token), left.right), right.left)
            if(left.left !is OperandNode && left.right.token is OperandToken && right.left.token is OperandToken && right.right !is OperandNode)
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.right.token, right.left.token), left.left), right.right)
            if(left.left !is OperandNode && left.right.token is OperandToken && right.left !is OperandNode && right.right.token is OperandToken)
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.right.token, right.right.token), left.left), right.left)
        }

        /**
         *   We cancel a factor above and below the divide. Or if there's two operands we do the calculation
         *             /             ->         *
         *         *      exp1       ->     exp2  exp3
         *       *  exp2             ->
         *   exp3 exp4
         */
        if(left is BinaryOperatorNode && left.token is Multiplication && left.left is BinaryOperatorNode && left.left.token is Multiplication){
            if(left.left.left.equals(right))
                return BinaryOperatorNode(Multiplication(), left.left.right, left.right)
            if(left.left.right.equals(right))
                return BinaryOperatorNode(Multiplication(), left.left.left, left.right)

            if(left.left.left.token is OperandToken && left.left.right !is OperandNode && left.right !is OperandNode && right.token is OperandToken)
                return BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.left.left.token, right.token), BinaryOperatorNode(Multiplication(), left.left.right, left.right))
            if(left.left.left !is OperandNode && left.left.right.token is OperandToken && left.right !is OperandNode && right.token is OperandToken)
                return BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.left.right.token, right.token), BinaryOperatorNode(Multiplication(), left.left.left, left.right))
        }

        /**
         *   We cancel a factor above and below the divide. Or if there's two operands we do the calculation
         *              /              ->            /
         *         *         *         ->        *       exp2
         *       *  exp1 exp2 exp3     ->    exp1 exp4
         *   exp4 exp5
         */
        if(left is BinaryOperatorNode && left.token is Multiplication
                && left.left is BinaryOperatorNode && left.left.token is Multiplication
                && right is BinaryOperatorNode && right.token is Multiplication){
            if(left.left.left.equals(right.right))
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.right, left.right), right.left)
            if(left.left.left.equals(right.left))
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.right, left.right), right.right)
            if(left.left.right.equals(right.right))
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.left, left.right), right.left)
            if(left.left.right.equals(right.left))
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), left.left.left, left.right), right.right)

            if(left.left.left.token is OperandToken && left.left.right !is OperandNode && left.right !is OperandNode && right.left.token is OperandToken && right.right !is OperandNode)
                return BinaryOperatorNode(Divide(), BinaryOperatorNode(Multiplication(), evaluateBinary(Divide(), left.left.left.token, right.left.token), BinaryOperatorNode(Multiplication(), left.left.right, left.right)), right.right)
        }

        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewritePower(token: Power, left: AbstractSyntaxTree, right: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true

        /**
         *   anything to the power of 0 is 1
         *          ^      ->      1
         *       exp  0    ->
         */
        if(right.token.value == "0") return OperandNode(OperandToken("1"))

        /**
         *   anything to the power of 1 is itself
         *          ^      ->      exp
         *       exp  1    ->
         */
        if(right.token.value == "1") return left

        finished = false
        return BinaryOperatorNode(token, left, right)
    }

    private fun rewriteUnary(token: Token, middle: AbstractSyntaxTree): AbstractSyntaxTree {
        finished = true

        /**
         *   Remove redundant unary plus
         *          (+)      ->      exp
         *          exp      ->
         */
        if(token is UnaryPlus) return middle

        /**
         *   DOUBLE NEGATION: minus minus is plus
         *          (-)      ->      exp
         *          (-)      ->
         *          exp      ->
         */
        if(token is UnaryMinus && middle is UnaryOperatorNode && middle.token is UnaryMinus) return middle.middle

        /**
         *   DOUBLE NEGATION: minus minus is plus
         *          (-)      ->      *
         *           *       ->   -op exp
         *         op exp    ->
         */
        if(token is UnaryMinus && middle is BinaryOperatorNode && middle.token is Multiplication && middle.left.token is OperandToken)
                return BinaryOperatorNode(Multiplication(), evaluateUnary(UnaryMinus(), middle.left.token), middle.right)


        /** INVERSES: for example sin(arcsin(x)) -> x */
        if(middle is UnaryOperatorNode
                && ((token is Sin && middle.token is ArcSin)
                    || (token is ArcSin && middle.token is Sin)
                    || (token is Cos && middle.token is ArcCos)
                    || (token is ArcCos && middle.token is Cos)
                    || (token is Tan && middle.token is ArcTan)
                    || (token is ArcTan && middle.token is Tan)))
            return middle.middle

        /**
         *   sqrt(exp^2) -> |exp|
         *           sqrt     ->      abs
         *            ^       ->      exp
         *         exp  2     ->
         */
        if(token is Sqrt && middle is BinaryOperatorNode && middle.token is Power && middle.right.token.value == "2")
            return UnaryOperatorNode(Abs(), middle.left)

        finished = false
        return UnaryOperatorNode(token, middle)
    }

    private fun evaluateUnary(operator: Token, middle: OperandToken): AbstractSyntaxTree {
        // todo, probably just turn it to double instead, since it requires it being double, no point in having it big decimal
        val operand = middle.value.toBigDecimal()

        /** Evaluates the unary operator */
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

        /** evalutes the binary operator */
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
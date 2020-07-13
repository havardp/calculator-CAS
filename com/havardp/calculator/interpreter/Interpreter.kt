package com.havardp.calculator.interpreter

import com.havardp.calculator.parser.*
import com.havardp.calculator.interpreter.nodeVisitor.*
import com.havardp.calculator.lexer.Lexer
import com.havardp.calculator.lexer.token.*
import com.havardp.exception.*
import java.math.BigDecimal
import java.util.*

class Interpreter(parser: Parser) {
    private var currentTree = parser.parse()
    private var rewrittenTree = currentTree

    /** Used for debugging, prints the graph */
    fun printGraphTree(ast: AbstractSyntaxTree): String{
        val visitor = PrintGraphTreeVisitor()
        ast.accept(visitor)
        return visitor.getGraph()
    }

    /** Used for debugging, pretty prints the equation, about the same as a user would've inputted */
    fun debugPrettyPrint(ast: AbstractSyntaxTree): String{
        val visitor = PrettyPrintVisitor()
        return ast.accept(visitor)
    }

    private fun prettyPrint(ast: AbstractSyntaxTree): String{
        val visitor = PrettyPrintLatexVisitor()
        return ast.accept(visitor)
    }

    // returns the result object from rewrite, or if it is a quadratic equation, the result object quadratic
    fun interpret(): Result {
        val result: OrdinaryResult = rewrite()
        val quadraticResult = isQuadraticEquation(rewriteToQuadratic(currentTree))
        return quadraticResult ?: result
    }

    private fun rewrite(): OrdinaryResult {
        /** the visitor instance that lets us rewrite the tree */
        val rewriteVisitor = RewriteVisitor()
        /** latex print of what the user inputted */
        val input = prettyPrint(currentTree)
        /** A list of all the rewritings */
        val solveSteps = Stack<String>()
        /** Used for preventing crashes */
        var counter = 0

        /** Stops when rewritten and current tree are identical, when rewrite visitor couldn't do more changes */
        do {
            currentTree = rewrittenTree

            /** add the change to the solution */
            solveSteps.push(prettyPrint(currentTree))

            /** Do a rewrite */
            rewriteVisitor.finished = false
            try {
                rewrittenTree = currentTree.accept(rewriteVisitor)
            } catch (e: ArithmeticErrorException){
                println(e.message)
                break
            } catch (e: InvalidSyntaxException){
                println(e.message)
                break
            }

            /** prevents crash if there's somehow a non terminating loop in the rewrite visitor */
            counter++
            if(counter > 1000) {
                println("counter greater than 80, loop in code rewrite visitor probably")
                break
            }
        } while(!rewrittenTree.equals(currentTree))

        val explanationSteps = rewriteVisitor.explanationSteps
        if(solveSteps.size != explanationSteps.size)
            throw InterpreterErrorException("The size of the explanations and rewrites differs")

        val result = prettyPrint(rewrittenTree)
        return OrdinaryResult(input, result, solveSteps, explanationSteps)
    }



    /** Analyses the tree, if it is a quadratic equation, then we call evaluate quadratic with the corresponding a b c values */
    private fun isQuadraticEquation(node: AbstractSyntaxTree): QuadraticResult? {
        var a: BigDecimal? = null
        var b: BigDecimal? = null
        var c: BigDecimal? = null

        if(node is BinaryOperatorNode && node.token is Equal && node.right.token.value == "0"){
            val left = node.left

            if(left is BinaryOperatorNode){
                /** Determine the value of the constant factor */
                if(left.token is Minus && left.right is OperandNode)
                    c = left.right.token.value.toBigDecimal().negate()
                else if(left.token is Plus && left.right is OperandNode)
                    c = left.right.token.value.toBigDecimal()
                else return null

                if(left.left is BinaryOperatorNode){
                    /** determine the value of the x factor */
                    if(left.left.token is Plus && left.left.right is VariableNode)
                        b = 1.toBigDecimal()
                    else if(left.left.token is Minus && left.left.right is VariableNode)
                        b = (-1).toBigDecimal()
                    else if(left.left.token is Plus && left.left.right is UnaryOperatorNode && left.left.right.token is UnaryMinus && left.left.right.middle is VariableNode)
                        b = (-1).toBigDecimal()
                    else if(left.left.token is Minus && left.left.right is UnaryOperatorNode && left.left.right.token is UnaryMinus && left.left.right.middle is VariableNode)
                        b = 1.toBigDecimal()
                    else if(left.left.right is BinaryOperatorNode && left.left.right.token is Multiplication){
                        if(left.left.token is Plus && left.left.right.right is VariableNode && left.left.right.left is OperandNode)
                            b = left.left.right.left.token.value.toBigDecimal()
                        else if(left.left.token is Minus && left.left.right.right is VariableNode && left.left.right.left is OperandNode)
                            b = left.left.right.left.token.value.toBigDecimal().negate()
                    }
                    else return null

                    /** Determines the value of the x^2 factor */
                    if(left.left.left is BinaryOperatorNode) {
                        if(left.left.left.token is Power && left.left.left.right.token.value == "2" && left.left.left.left is VariableNode)
                            a = 1.toBigDecimal()
                        if(left.left.left.token is Multiplication && left.left.left.left is OperandNode
                                && left.left.left.right is BinaryOperatorNode && left.left.left.right.token is Power
                                && left.left.left.right.right.token.value == "2" && left.left.left.right.left is VariableNode)
                            a = left.left.left.left.token.value.toBigDecimal()
                    }
                    else if(left.left.left is UnaryOperatorNode && left.left.left.token is UnaryMinus && left.left.left.middle is BinaryOperatorNode)
                        if(left.left.left.middle.token is Power && left.left.left.middle.left is VariableNode && left.left.left.middle.right.token.value == "2")
                            a = (-1).toBigDecimal()

                }
            }
        }

        if(a != null && b != null && c != null)
            return solveQuadraticEquation(a.toString(), b.toString(), c.toString(), node)

        return null
    }

    /**
     * rewrites to an expected form, for example after rewrite() has been called, it is probably on the form ax^2+bx=c, but we want ax^2+bx-c=0
     * Also if b node is written before a node for example, we move them
     */
    private fun rewriteToQuadratic(node: AbstractSyntaxTree): AbstractSyntaxTree {
        /** calls the isQuadraticEquation with a small rewrite that just makes sure the tree is in the expected form */
        if(node is BinaryOperatorNode && node.token is Equal){

            /** if x node is before x^2 node we move them */
            if(node.left is BinaryOperatorNode && (node.left.left is VariableNode || (node.left.left is UnaryOperatorNode && node.left.left.middle is VariableNode))){
                if(node.left.token is Plus){
                    if(node.right.token.value.toBigDecimal() < 0.toBigDecimal())
                        return BinaryOperatorNode(Equal(), BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), node.left.right, node.left.left), OperandNode(OperandToken(node.right.token.value.toBigDecimal().negate().toString()))), OperandNode(OperandToken("0")))
                    else
                        return BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), node.left.right, node.left.left), node.right), OperandNode(OperandToken("0")))
                }
                if(node.left.token is Minus){
                    if(node.right.token.value.toBigDecimal() < 0.toBigDecimal())
                        return BinaryOperatorNode(Equal(), BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), node.left.right), node.left.left), OperandNode(OperandToken(node.right.token.value.toBigDecimal().negate().toString()))), OperandNode(OperandToken("0")))
                    else
                        return BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), node.left.right), node.left.left), node.right), OperandNode(OperandToken("0")))
                }
            }

            /** Since rewrite visitor moves operands to the right of equality, we just move it back here */
            else{
                if(node.right.token.value.toBigDecimal() < 0.toBigDecimal())
                    return BinaryOperatorNode(Equal(), BinaryOperatorNode(Plus(), node.left, OperandNode(OperandToken(node.right.token.value.toBigDecimal().negate().toString()))), OperandNode(OperandToken("0")))
                else
                    return BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), node.left, node.right), OperandNode(OperandToken("0")))
            }
        }
        return node
    }

    private fun solveQuadraticEquation(a: String, b: String, c: String, tree: AbstractSyntaxTree): QuadraticResult {
        val input = prettyPrint(tree)
        val quadraticFormula = "\\frac{-$b \\pm \\sqrt{$b^2 - 4 \\cdot $a \\cdot $c}}{2 \\cdot $a}"
        val root1 = Interpreter(Parser(Lexer("(-$b+sqrt($b^2-4*$a*$c))/(2*$a)"))).interpret()
        val root2 = Interpreter(Parser(Lexer("(-$b-sqrt($b^2-4*$a*$c))/(2*$a)"))).interpret()
        if(root1 is OrdinaryResult && root2 is OrdinaryResult)
            return QuadraticResult(input, quadraticFormula, root1, root2)
        else
            throw InterpreterErrorException("root of quadratic is not ordinary result")
    }
}


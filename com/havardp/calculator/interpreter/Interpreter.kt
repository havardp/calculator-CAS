package com.havardp.calculator.interpreter

import com.havardp.calculator.parser.*
import com.havardp.calculator.interpreter.nodeVisitor.*
import com.havardp.calculator.lexer.Lexer
import com.havardp.calculator.lexer.token.*
import java.math.BigDecimal
import java.util.*

class Interpreter(parser: Parser) {
    private val treeStack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()
    private val rewriteVisitor = RewriteVisitor()
    private val result = Result()

    init {
        val tree = parser.parse()
        treeStack.push(tree)
    }

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
        val visitor = PrettyPrintLatexVisitor() // prints the expression in infix form.
        return ast.accept(visitor)
    }

    fun interpret(): Result {
        result.input = prettyPrint(treeStack.peek())
        rewrite()

        result.result = prettyPrint(treeStack.peek())
        return result
    }

    private fun rewrite() {
        var rewrittenTree = treeStack.peek().accept(rewriteVisitor)
        var counter = 0

        while(!rewrittenTree.equals(treeStack.peek())){
            treeStack.push(rewrittenTree)
            rewriteVisitor.finished = false

            /** add the change to the solution */
            result.solveSteps.add(prettyPrint(rewrittenTree))

            /** Do another rewrite */
            rewrittenTree = treeStack.peek().accept(rewriteVisitor)

            /** code below prevents crash if there's somehow a non terminating loop in the rewrite visitor */
            counter++
            if(counter > 200) {
                println("counter greater than 80, loop in code rewrite visitor probably")
                break
            }
        }

        /** calls the isQuadraticEquation with a small rewrite that just makes sure the tree is in the expected form */
        if(rewrittenTree is BinaryOperatorNode && rewrittenTree.token is Equal){
            val node = rewrittenTree

            /** if x node is before x^2 node we move them */
            if(node.left is BinaryOperatorNode && (node.left.left is VariableNode || (node.left.left is UnaryOperatorNode && node.left.left.middle is VariableNode))){
                if(node.left.token is Plus){
                    if(node.right.token.value.toBigDecimal() < 0.toBigDecimal())
                        isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), node.left.right, node.left.left), OperandNode(OperandToken(node.right.token.value.toBigDecimal().negate().toString()))), OperandNode(OperandToken("0"))))
                    else
                        isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), node.left.right, node.left.left), node.right), OperandNode(OperandToken("0"))))
                }
                if(node.left.token is Minus){
                    if(node.right.token.value.toBigDecimal() < 0.toBigDecimal())
                        isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Plus(), BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), node.left.right), node.left.left), OperandNode(OperandToken(node.right.token.value.toBigDecimal().negate().toString()))), OperandNode(OperandToken("0"))))
                    else
                        isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), node.left.right), node.left.left), node.right), OperandNode(OperandToken("0"))))
                }
            }

            /** Since rewrite visitor moves operands to the right of equality, we just move it back here */
            else{
                if(node.right.token.value.toBigDecimal() < 0.toBigDecimal())
                    isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Plus(), node.left, OperandNode(OperandToken(node.right.token.value.toBigDecimal().negate().toString()))), OperandNode(OperandToken("0"))))
                else
                    isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), node.left, node.right), OperandNode(OperandToken("0"))))
            }
        }
    }

    /** Analyses the tree, if it is a quadratic equation, then we call evaluate quadratic with the corresponding a b c values */
    private fun isQuadraticEquation(node: AbstractSyntaxTree) {
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
                else return

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
                    else return

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
        if(a != null && b != null && c != null) solveQuadraticEquation(a.toString(), b.toString(), c.toString())
    }

    private fun solveQuadraticEquation(a: String, b: String, c: String) {
        result.isQuadratic = true
        result.quadraticFormula = "\\frac{-$b \\pm \\sqrt{$b^2 - 4 \\cdot $a \\cdot $c}}{2 \\cdot $a}"
        val root1 = Interpreter(Parser(Lexer("(-$b+sqrt($b^2-4*$a*$c))/(2*$a)"))).interpret()
        val root2 = Interpreter(Parser(Lexer("(-$b-sqrt($b^2-4*$a*$c))/(2*$a)"))).interpret()
        result.root1 = root1
        result.root2 = root2
    }
}


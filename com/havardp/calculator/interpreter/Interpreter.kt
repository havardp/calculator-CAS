package com.havardp.calculator.interpreter

import com.havardp.calculator.parser.*
import com.havardp.calculator.interpreter.nodeVisitor.*
import com.havardp.calculator.lexer.token.*
import java.math.BigDecimal
import java.util.*

class Interpreter(parser: Parser) {
    val treeStack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()
    private val rewriteVisitor = RewriteVisitor()
    //TODO: input variable, set when interpret is called, solution variable, set when rewrite is done (or in quadratic as well)
    // maybe make a data class Result, that has all info, and return it on interpreter.interpret

    init {
        val tree = parser.parse()
        treeStack.push(tree)
    }

    fun printGraphTree(ast: AbstractSyntaxTree): String{
        val visitor = PrintGraphTreeVisitor() // prints the graph
        ast.accept(visitor)
        return visitor.getGraph()
    }

    fun debugPrettyPrint(ast: AbstractSyntaxTree): String{
        val visitor = PrettyPrintVisitor() // prints the expression in infix form.
        return ast.accept(visitor)
    }

    fun prettyPrint(ast: AbstractSyntaxTree): String{
        val visitor = PrettyPrintLatexVisitor() // prints the expression in infix form.
        return ast.accept(visitor)
    }

    fun getPrettyPrintedResult(): String{
        return prettyPrint(treeStack.peek())
    }

    fun interpret() {
        rewrite()
    }

    private fun rewrite() {
        var rewrittenTree = treeStack.peek().accept(rewriteVisitor)
        var counter = 0

        while(!rewrittenTree.equals(treeStack.peek())){
            treeStack.push(rewrittenTree)
            rewriteVisitor.finished = false
            rewrittenTree = treeStack.peek().accept(rewriteVisitor)

            // just so the program stops whenever we have an infinite loop in rewrite visitor
            counter++
            if(counter > 80) {
                println("counter greater than 80, loop in code rewrite visitor probably")
                break
            }
        }

        /** calls the isQuadraticEquation with a small rewrite that just makes sure the tree is in the form i expect */
        if(rewrittenTree is BinaryOperatorNode && rewrittenTree.token is Equal){
            val node = rewrittenTree

            /** if x node is before x^2 node we move them */
            if(node.left is BinaryOperatorNode && (node.left.left is VariableNode || (node.left.left is UnaryOperatorNode && node.left.left.middle is VariableNode))){
                if(node.left.token is Plus)
                    isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), node.left.right, node.left.left), node.right), OperandNode(OperandToken("0"))))
                if(node.left.token is Minus)
                    isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), BinaryOperatorNode(Plus(), UnaryOperatorNode(UnaryMinus(), node.left.right), node.left.left), node.right), OperandNode(OperandToken("0"))))
            }

            /** Since rewrite visitor moves operands to the right of equality, we just move it back here */
            else
                isQuadraticEquation(BinaryOperatorNode(Equal(), BinaryOperatorNode(Minus(), node.left, node.right), OperandNode(OperandToken("0"))))
        }
    }

    // Should call this after rewrite, then we have a standard form for the input, and can evaluate more expressions
    // then we just say the input is equal to the result of the rewrite, so we dont move operands to the right and then back etc.
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

        if(a != null && b != null && c != null) solveQuadraticEquation(a, b, c)
    }

    private fun solveQuadraticEquation(a: BigDecimal, b: BigDecimal, c: BigDecimal) {
        println("a: $a, b: $b, c: $c")
    }
}


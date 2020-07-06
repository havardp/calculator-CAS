package com.havardp.calculator.interpreter

import com.havardp.calculator.parser.*
import com.havardp.calculator.interpreter.nodeVisitor.*
import java.util.*

class Interpreter(parser: Parser) {
    val treeStack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()
    private val rewriteVisitor = RewriteVisitor()

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

    fun getExplanation(){
        TODO("not yet implemented, need to have an explanation or step by step stack in rewrite visitor")
    }

    // TODO: fun interpret, if quadratic, solve it, else, rewrite the tree.
    fun interpret() {
        /** if quadratic equation */
        /*// naive check for if it is a quadratic equation, should probably do this outside and have another visitor or something which handles it, for now it will do here
        // only actually checks if it is ordered, also this doesn't account for unary minus before x^2 nor operand before x^2

        if(left is BinaryOperatorNode && left.left is BinaryOperatorNode
                && left.left.left is BinaryOperatorNode && left.left.left.token is Power && left.left.left.left is VariableNode && left.left.left.right.token.value == "2"
                && (left.left.right is VariableNode || (left.left.right is BinaryOperatorNode && left.left.right.token is Multiplication && left.left.right.right is VariableNode))
                && left.right is OperandNode && right is OperandNode && right.token.value == "0"){
            println("this is quadratic equation")
            return BinaryOperatorNode(token, left, right)
        }
        if(left is BinaryOperatorNode
                && left.left is BinaryOperatorNode && left.left.token is Power && left.left.left is VariableNode && left.left.right.token.value == "2"
                && left.right is OperandNode && right is OperandNode && right.token.value == "0") {
            println("this is also quadratic equation, without x factor")
            return BinaryOperatorNode(token, left, right)
        }*/

        // else
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
    }
}


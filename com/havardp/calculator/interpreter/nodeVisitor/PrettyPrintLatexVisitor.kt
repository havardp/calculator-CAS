package com.havardp.calculator.interpreter.nodeVisitor

import com.havardp.calculator.lexer.token.*
import com.havardp.calculator.parser.*
import java.lang.ArithmeticException
import java.math.RoundingMode

/** Prints an abstract syntax tree to latex format */
class PrettyPrintLatexVisitor: NodeVisitor(){
    override fun visit(node: BinaryOperatorNode): String {
        if(node.token is Divide)
            return "\\frac{${node.left.accept(this)}}{${node.right.accept(this)}}"
        if(node.token is Power)
            return "${node.left.accept(this)}^{${node.right.accept(this)}}"
        if(node.token is BinaryOperatorToken && node.right.token is BinaryOperatorToken
                && ((node.token is Minus && node.token.precedence == node.right.token.precedence)
                        || node.token.precedence > node.right.token.precedence))
            return "${node.left.accept(this)} ${node.token.verbose} (${node.right.accept(this)})"

        return "${node.left.accept(this)} ${(node.token as BinaryOperatorToken).verbose} ${node.right.accept(this)}"
    }

    override fun visit(node: UnaryOperatorNode): String {
        if(node.token is Sqrt) return "\\sqrt{${node.middle.accept(this)}}"
        return "${(node.token as UnaryOperatorToken).value}(${node.middle.accept(this)})"
    }

    override fun visit(node: OperandNode): String {
        return try {
            /**if there is at least five 0's in the start of the decimal, we round, so like 0.000000321 -> 0 or 0.999999321321 -> 1 */
            node.token.value.toBigDecimal().setScale(5, RoundingMode.HALF_UP).toBigIntegerExact().toString()
        }catch (e: ArithmeticException){
            node.token.value
        }
    }

    override fun visit(node: VariableNode): String = node.token.value
    override fun visit(node: ImaginaryNode): String = node.token.value
}
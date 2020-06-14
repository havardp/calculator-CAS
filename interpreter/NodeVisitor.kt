package com.example.expressionCalculator.calculator.interpreter

import calculator.lexer.Mult
import calculator.lexer.Plus
import calculator.parser.BinaryOperatorNode
import calculator.parser.OperandNode
import calculator.parser.UnaryOperatorNode
import calculator.parser.VariableNode
import java.math.BigDecimal
import java.util.*
import kotlin.math.sin

// https://en.wikipedia.org/wiki/Visitor_pattern implementation
abstract class NodeVisitor {
    abstract fun visit(node: BinaryOperatorNode)
    abstract fun visit(node: UnaryOperatorNode)
    abstract fun visit(node: OperandNode)
    abstract fun visit(node: VariableNode)
}

class ArithmeticVisitor: NodeVisitor() {
    private val stack: Stack<BigDecimal> = Stack<BigDecimal>()

    fun getVal(): BigDecimal {
        return stack.pop()
    }

    override fun visit(node: BinaryOperatorNode) {
        node.left.accept(this)
        node.right.accept(this)
        val a = stack.pop()
        val b = stack.pop()

        when(node.token){
            is Plus -> stack.push(a+b)
            is Mult -> stack.push(a*b)
        }
    }
    override fun visit(node: UnaryOperatorNode){
        node.middle.accept(this)
        val a = stack.pop()
        stack.push(sin(a.toDouble()).toBigDecimal())
    }

    override fun visit(node: OperandNode) {
        stack.push(node.token.value.toBigDecimal())
    }

    override fun visit(node: VariableNode) {
        // haven't yet implemented variable visiting, maybe do it in another visitor class?
    }
}

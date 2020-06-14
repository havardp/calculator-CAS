package calculator.interpreter

import calculator.parser.*
import java.math.BigDecimal
import java.util.*
import kotlin.math.sin

abstract class NodeVisitor {
    abstract fun visit(node: BinaryOperatorNode)
    abstract fun visit(node: UnaryOperatorNode)
    abstract fun visit(node: OperandNode)
    abstract fun visit(node: VariableNode)
}

class Visitor: NodeVisitor() {
    private val stack: Stack<BigDecimal> = Stack<BigDecimal>()

    fun getVal(): BigDecimal{
        return stack.pop()
    }

    override fun visit(node: BinaryOperatorNode) {
        node.left.accept(this)
        node.right.accept(this)
        val a = stack.pop()
        val b = stack.pop()
        stack.push(a+b)
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
        //stack.push(node)
    }

}


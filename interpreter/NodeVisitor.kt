package calculator.interpreter

import calculator.lexer.Token.*
import calculator.parser.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import java.util.function.BinaryOperator
import kotlin.math.pow
import kotlin.math.sin

// https://en.wikipedia.org/wiki/Visitor_pattern implementation
// https://en.wikipedia.org/wiki/Double_dispatch#Visitor_pattern
// TODO: Add type checker visitor? like check which visitor to use, arithmetic, variable etc..
// TODO: Add rewrite visitor, node transformation

abstract class NodeVisitor {
    abstract fun visit(node: BinaryOperatorNode)
    abstract fun visit(node: UnaryOperatorNode)
    abstract fun visit(node: OperandNode)
    abstract fun visit(node: VariableNode)
}

// Only working properly for simple graph, as soon as multi digit numbers, or deep graphs are introduced, it doesn't display properly
class PrintGraphVisitor: NodeVisitor(){
    var list = arrayListOf<String>()
    var indentation = 12 //How many spaces of indentation there should be on a given line
    var level = 0        //Which "level" in the graph we are at

    fun getGraph(): String {
        var str = "Graph of abstract syntax tree\n"
        for(e in list){
            str += e
            str += "\n"
        }
        return str
    }

    override fun visit(node: BinaryOperatorNode) {
        if(list.size <= level) {
            list.add("")
            for(i in 0 until indentation) list[level] += " "
        }
        list[level] += node.token.value + "   "
        level++
        indentation -= 2
        node.left.accept(this)
        indentation += 4
        node.right.accept(this)
        indentation -= 2
        level--
    }
    override fun visit(node: UnaryOperatorNode){
        if(list.size <= level) {
            list.add("")
            for(i in 0 until indentation) list[level] += " "
        }
        list[level] += node.token.value + "   "
        level++
        node.middle.accept(this)
        level--
    }

    override fun visit(node: OperandNode) {
        if(list.size <= level) {
            list.add("")
            for(i in 0 until indentation) list[level] += " "
        }
        list[level] += node.token.value + "   "
    }

    override fun visit(node: VariableNode) {
        if(list.size <= level) {
            list.add("")
            for(i in 0 until indentation) list[level] += " "
        }
        list[level] += node.token.value + "   "
    }
}

// Only working properly for simple graph, as soon as multi digit numbers, or deep graphs are introduced, it doesn't display properly
class PrintFlatTreeVisitor: NodeVisitor(){
    var str = ""

    override fun visit(node: BinaryOperatorNode) {
        str += "${(node.token as BinaryOperatorToken).verbose}("
        node.left.accept(this)
        str += ", "
        node.right.accept(this)
        str +=")"
    }
    override fun visit(node: UnaryOperatorNode){
        str += "${(node.token as BinaryOperatorToken).verbose}("
        node.middle.accept(this)
        str += ")"
    }

    override fun visit(node: OperandNode) {
        str += node.token.value
    }

    override fun visit(node: VariableNode) {
        str += node.token.value
    }
}

class ArithmeticVisitor: NodeVisitor() {
    private val stack: Stack<BigDecimal> = Stack<BigDecimal>()
    private val PRECISION = 4
    private val CONTEXT = MathContext(PRECISION, RoundingMode.HALF_UP)

    fun getVal(): BigDecimal {
        return stack.pop()
    }

    override fun visit(node: BinaryOperatorNode) {
        node.left.accept(this)
        node.right.accept(this)
        val b = stack.pop()
        val a = stack.pop()

        when(node.token){
            is Plus -> stack.push(a.plus(b))
            is Minus -> stack.push(a.minus(b))
            is Multiplication -> stack.push(a.multiply(b, CONTEXT))
            is Divide -> stack.push(a.divide(b, CONTEXT))
            is Power -> stack.push(a.toDouble().pow(b.toDouble()).toBigDecimal())
            is Modulus -> stack.push(a.remainder(b, CONTEXT))
        }
    }
    override fun visit(node: UnaryOperatorNode){
        node.middle.accept(this)
        val a = stack.pop()
        when(node.token){
            is Sin -> stack.push(sin(a.toDouble()).toBigDecimal())
            is UnaryMinus -> stack.push(a.negate())
        }
    }

    override fun visit(node: OperandNode) {
        stack.push(node.token.value.toBigDecimal())
    }

    override fun visit(node: VariableNode) {
        // haven't yet implemented variable visiting, maybe do it in another visitor class?
    }
}

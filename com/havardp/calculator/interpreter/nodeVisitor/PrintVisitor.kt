package com.havardp.calculator.interpreter.nodeVisitor

import com.havardp.calculator.lexer.token.*
import com.havardp.calculator.parser.*
import java.lang.ArithmeticException
import java.math.RoundingMode
import java.util.*

// Only working properly for simple graph, as soon as multi digit numbers, or deep graphs are introduced, it doesn't display properly
// mostly used for debugging purposes
class PrintGraphTreeVisitor: NodeVisitor(){
    private var list = arrayListOf<String>()
    private var indentation = 20 //How many spaces of indentation there should be on a given line
    private var level = 0        //Which "level" in the graph we are at

    fun getGraph(): String {
        var str = "Graph of abstract syntax tree\n"
        for(e in list){
            str += e
            str += "\n"
        }
        return str
    }

    private fun createNewLevel(length: Int){
        if(list.size <= level) {
            list.add("")
            for(i in 0 until indentation+ 1 - length) list[level] += " "
        }
    }

    override fun visit(node: BinaryOperatorNode) {
        createNewLevel(node.token.value.length)
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
        createNewLevel(node.token.value.length)
        list[level] += node.token.value + "   "
        level++
        node.middle.accept(this)
        level--
    }

    override fun visit(node: OperandNode) {
        createNewLevel(node.token.value.length)
        list[level] += node.token.value + "   "
    }

    override fun visit(node: VariableNode) {
        createNewLevel(node.token.value.length)
        list[level] += node.token.value + "   "
    }
}

// TODO: add latex pretty print visitor, mostly \frac{left}{right}
// it should have space between every token, to avoid stuff like \cdotsin(x) for example, instead \cdot sin(x)
// and use \cdot instead of multiplication
class PrettyPrintVisitor: NodeVisitor(){
    override fun visit(node: BinaryOperatorNode): String {
        if(node.token is BinaryOperatorToken && node.right.token is BinaryOperatorToken
                && (((node.token is Minus || node.token is Divide) && node.token.precedence == node.right.token.precedence)
                        || node.token.precedence > node.right.token.precedence))
            return "${node.left.accept(this)}${node.token.value}(${node.right.accept(this)})"

        return "${node.left.accept(this)}${(node.token as BinaryOperatorToken).value}${node.right.accept(this)}"
    }

    override fun visit(node: UnaryOperatorNode): String {
        if(node.token is UnaryPlus || node.token is UnaryMinus) return "(${(node.token as UnaryOperatorToken).value}${node.middle.accept(this)})"
        return "${(node.token as UnaryOperatorToken).value}(${node.middle.accept(this)})"
    }

    override fun visit(node: OperandNode): String {
        return try {
            node.token.value.toBigDecimal().setScale(5, RoundingMode.HALF_UP).toBigIntegerExact().toString()
        }catch (e: ArithmeticException){
            node.token.value
        }
    }

    override fun visit(node: VariableNode): String = node.token.value
}
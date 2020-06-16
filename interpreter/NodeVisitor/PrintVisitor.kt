package calculator.interpreter.NodeVisitor

import calculator.lexer.Token.BinaryOperatorToken
import calculator.lexer.Token.OperatorToken
import calculator.lexer.Token.UnaryOperatorToken
import calculator.parser.BinaryOperatorNode
import calculator.parser.OperandNode
import calculator.parser.UnaryOperatorNode
import calculator.parser.VariableNode
import java.util.*

// Only working properly for simple graph, as soon as multi digit numbers, or deep graphs are introduced, it doesn't display properly
class PrintGraphTreeVisitor: NodeVisitor(){
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

    fun createNewLevel(length: Int){
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


class PrettyPrintVisitor: NodeVisitor(){
    private val parentStack: Stack<OperatorToken> = Stack<OperatorToken>()

    override fun visit(node: BinaryOperatorNode): String {
        val str: String = if(parentStack.size != 0 && parentStack.peek().precedence >= (node.token as BinaryOperatorToken).precedence){
            parentStack.push(node.token)
            "(${node.left.accept(this)}${node.token.value}${node.right.accept(this)})"
        }else{
            parentStack.push(node.token as OperatorToken)
            "${node.left.accept(this)}${node.token.value}${node.right.accept(this)}"
        }
        parentStack.pop()
        return str
    }
    override fun visit(node: UnaryOperatorNode): String {
        return "${(node.token as UnaryOperatorToken).verbose}(${node.middle.accept(this)})"
    }

    override fun visit(node: OperandNode): String {
       return node.token.value
    }

    override fun visit(node: VariableNode): String {
        return node.token.value
    }
}
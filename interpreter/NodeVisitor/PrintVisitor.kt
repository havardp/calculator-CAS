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

    fun createNewLevel(){
        if(list.size <= level) {
            list.add("")
            for(i in 0 until indentation) list[level] += " "
        }
    }

    override fun visit(node: BinaryOperatorNode) {
        createNewLevel()
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
        createNewLevel()
        list[level] += node.token.value + "   "
        level++
        node.middle.accept(this)
        level--
    }

    override fun visit(node: OperandNode) {
        createNewLevel()
        list[level] += node.token.value + "   "
    }

    override fun visit(node: VariableNode) {
        createNewLevel()
        list[level] += node.token.value + "   "
    }
}

class PrintFlatTreeVisitor: NodeVisitor(){
    private var str = "Flat tree of abstract syntax tree\n"

    fun getFlatTree(): String {
        return str
    }

    override fun visit(node: BinaryOperatorNode) {
        str += "${(node.token as BinaryOperatorToken).verbose}("
        node.left.accept(this)
        str += ", "
        node.right.accept(this)
        str +=")"
    }
    override fun visit(node: UnaryOperatorNode){
        str += "${(node.token as UnaryOperatorToken).verbose}("
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


class PrettyPrintVisitor: NodeVisitor(){
    private var str = "Flat tree of abstract syntax tree\n"
    private val parentStack: Stack<OperatorToken> = Stack<OperatorToken>()

    fun prettyPrint(): String {
        return str
    }

    override fun visit(node: BinaryOperatorNode) {
        if(parentStack.size != 0 && parentStack.peek().precedence >= (node.token as BinaryOperatorToken).precedence){
            parentStack.push(node.token)
            str += "("
            node.left.accept(this)
            str += "${(node.token as BinaryOperatorToken).value}"
            node.right.accept(this)
            str += ")"
        }else{
            parentStack.push(node.token as OperatorToken)
            node.left.accept(this)
            str += "${(node.token as BinaryOperatorToken).value}"
            node.right.accept(this)
        }
        parentStack.pop()
    }
    override fun visit(node: UnaryOperatorNode){
        str += "${(node.token as UnaryOperatorToken).verbose}("
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
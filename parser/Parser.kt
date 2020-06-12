package calculator.parser

import calculator.exception.InvalidSyntaxException
import calculator.lexer.*
import java.util.Stack

class Parser(private val lexer: Lexer){
    private var currentToken : Token
    private val stack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()
    private val operatorStack: Stack<Token> = Stack<Token>()

    init {
        currentToken = lexer.getNextToken()
    }

    fun parse(): AbstractSyntaxTree{
        while(currentToken !is EOFToken){
            parseToken()
            nextToken()
        }

        return extractAST()
    }

    // finishes the algorithm and returns the root node from the stack
    private fun extractAST(): AbstractSyntaxTree {
        while(!operatorStack.empty()){
            createOperatorNode()
        }

        // TODO: error check if stack actually only has one node
        return stack.pop()
    }

    private fun nextToken(){
        currentToken = lexer.getNextToken()
    }

    // creates a operator node
    private fun createOperatorNode(){
        if(operatorStack.peek() is BinaryOperatorToken){
            val operator = operatorStack.pop()
            val node2 = stack.pop()
            val node1 = stack.pop()
            stack.push(BinaryOperatorNode(operator, node1, node2))
        } else if(operatorStack.peek() is UnaryOperatorToken){
            val operator = operatorStack.pop()
            val node1 = stack.pop()
            stack.push(UnaryOperatorNode(operator, node1))
        }
        else throw InvalidSyntaxException("$currentToken is where in the parsing we got the error")
    }

    // parses a single token, and decides what to do with it
    private fun parseToken(){
        if(currentToken is OperandToken){
            stack.push(OperandNode(currentToken))
        }

        else if(currentToken is VariableToken){
            stack.push(VariableNode(currentToken))
        }

        else if(currentToken is UnaryOperatorToken || currentToken.value == "("){
            operatorStack.push(currentToken)
        }

        else if(currentToken.value == ")"){
            while(operatorStack.peek().value != "(") {
                createOperatorNode()
            }
            operatorStack.pop()
        }

        else if(currentToken is BinaryOperatorToken){
            while(operatorStack.isNotEmpty() && stackHasPrecedence()){
                createOperatorNode()
            }
            operatorStack.push(currentToken)
        }
    }

    private fun stackHasPrecedence(): Boolean {
        val leftAssociative = currentToken.precedence == operatorStack.peek().precedence
        val comparePrecedence = currentToken.precedence!! < operatorStack.peek().precedence!!
        val notParenthesis = operatorStack.peek().value != "("

        return comparePrecedence || leftAssociative && notParenthesis
    }
}
package calculator.parser

import calculator.exception.InvalidSyntaxException
import calculator.lexer.*
import java.util.Stack

// implementation of shunting yard algorithm
class Parser(private val lexer: Lexer){
    private var currentToken : Token = lexer.getNextToken()
    private val nodeStack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()
    private val operatorStack: Stack<Token> = Stack<Token>()

    fun parse(): AbstractSyntaxTree{
        while(currentToken !is EOFToken){
            parseToken()
            currentToken = lexer.getNextToken()
        }

        return extractAST()
    }

    // finishes the algorithm and returns the root node from the nodeStack
    private fun extractAST(): AbstractSyntaxTree {
        while(!operatorStack.empty()){
            createOperatorNode()
        }

        if(nodeStack.size != 1) throw InvalidSyntaxException("Couldn't parse to a single tree, syntax is wrong")
        return nodeStack.pop()
    }

    // creates a operator node
    private fun createOperatorNode(){
        if(operatorStack.peek() is BinaryOperatorToken){
            val operator = operatorStack.pop()
            val node2 = nodeStack.pop()
            val node1 = nodeStack.pop()
            nodeStack.push(BinaryOperatorNode(operator, node1, node2))
        }

        else if(operatorStack.peek() is UnaryOperatorToken){
            val operator = operatorStack.pop()
            val node1 = nodeStack.pop()
            nodeStack.push(UnaryOperatorNode(operator, node1))
        }

        else throw InvalidSyntaxException("Couldn't create operator node from operator stack")
    }

    // parses a single token, and decides what to do with it
    private fun parseToken(){
        if(currentToken is OperandToken){
            nodeStack.push(OperandNode(currentToken))
        }

        else if(currentToken is VariableToken){
            nodeStack.push(VariableNode(currentToken))
        }

        else if(currentToken is UnaryOperatorToken || currentToken is LeftParenthesisToken){
            operatorStack.push(currentToken)
        }

        else if(currentToken is RightParenthesisToken){
            while(operatorStack.peek() !is LeftParenthesisToken) {
                createOperatorNode()
            }
            operatorStack.pop()
        }

        else if(currentToken is BinaryOperatorToken){
            while(operatorStack.isNotEmpty() && operatorStackHasPrecedence()){
                createOperatorNode()
            }
            operatorStack.push(currentToken)
        }

        else {
            throw InvalidSyntaxException("Couldn't parse token")
        }
    }

    // returns true if nodeStack operator has precedence over currenttoken
    private fun operatorStackHasPrecedence(): Boolean {
        val leftAssociative = currentToken.precedence == operatorStack.peek().precedence
        val comparePrecedence = currentToken.precedence!! < operatorStack.peek().precedence!!
        val notParenthesis = operatorStack.peek() !is LeftParenthesisToken

        return comparePrecedence || leftAssociative && notParenthesis
    }
}
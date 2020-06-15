package calculator.parser

import calculator.exception.InvalidSyntaxException
import calculator.lexer.*
import calculator.lexer.Token.*
import java.util.Stack

/**
 * A parser which creates a abstract syntax tree from the tokens of a lexer
 *
 * Uses the shunting yard algorithm (https://en.wikipedia.org/wiki/Shunting-yard_algorithm)
 *
 * @property lexer where we retrieve our tokens from, lexer.getNextToken to get the next token
 * @property currentToken The token which we are currently parsing
 * @property nodeStack stack which stores the nodes of the abstract syntax tree. Will eventually only be one root node left, which we return
 * @property operatorStack Where we intermediately store our operators, to check if it has precedence or not.
 */
class Parser(private val lexer: Lexer){
    private var currentToken : Token = lexer.getNextToken()
    private val nodeStack: Stack<AbstractSyntaxTree> = Stack<AbstractSyntaxTree>()
    private val operatorStack: Stack<Token> = Stack<Token>()

    /**
     *  the public method of the class
     *  retrieves tokens from the lexer until we reach the EOF (end of file) token.
     *  parses each tokens
     *
     *  @return extractAST() which is responsible for finishing the algorithm ,and will return the root node
     */
    fun parse(): AbstractSyntaxTree{
        while(currentToken !is EOFToken){
            parseToken()
            currentToken = lexer.getNextToken()
        }

        return extractAST()
    }

    /**
     *  Last part of the shunting yard algorithm
     *  empties the operator stack
     *
     *  @return the root node of the abstract syntax tree
     */
    private fun extractAST(): AbstractSyntaxTree {
        while(!operatorStack.empty()) createOperatorNode()

        if(nodeStack.size != 1) throw InvalidSyntaxException("Couldn't parse to a single tree, syntax is wrong")
        return nodeStack.pop()
    }

    /**
     *  creates a node which uses current nodes as child
     *
     *  example, operatorStack being sin token, and node stack plus node of two operands
     *    +     (root node)
     *  2   3   (child nodes)
     *  becomes
     *   sin    (root node)
     *    +     (child node)
     *  2   3   (child of child node)
     */
    private fun createOperatorNode(){
        if(nodeStack.isEmpty()) throw InvalidSyntaxException("Mismatch of operator, couldn't parse")

        else if(operatorStack.peek() is BinaryOperatorToken){
            val operator = operatorStack.pop()
            val node2 = nodeStack.pop()
            if(nodeStack.isEmpty()) throw InvalidSyntaxException("Mismatch of operator, couldn't parse")
            val node1 = nodeStack.pop()
            nodeStack.push(BinaryOperatorNode(operator, node1, node2))
        }

        else if(operatorStack.peek() is UnaryOperatorToken){
            val operator = operatorStack.pop()
            val node1 = nodeStack.pop()
            nodeStack.push(UnaryOperatorNode(operator, node1))
        }

        else if(operatorStack.peek() is LeftParenthesisToken) throw InvalidSyntaxException("Mismatch of parenthesis, couldn't parse")

        else throw InvalidSyntaxException("Couldn't create operator node from operator stack")
    }

    /**
     *  Parses a single token
     *
     *  Main part of the shunting yard algorithm
     */
    private fun parseToken(){
        if(currentToken is OperandToken) nodeStack.push(OperandNode(currentToken))

        else if(currentToken is VariableToken) nodeStack.push(VariableNode(currentToken))

        else if(currentToken is UnaryOperatorToken || currentToken is LeftParenthesisToken) operatorStack.push(currentToken)

        else if(currentToken is RightParenthesisToken){
            while(operatorStack.peek() !is LeftParenthesisToken) {
                createOperatorNode()
            }
            if(operatorStack.isEmpty()) throw InvalidSyntaxException("Mismatch of parenthesis, couldn't parse")
            operatorStack.pop()
        }

        else if(currentToken is BinaryOperatorToken){
            while(operatorStack.isNotEmpty() && operatorStackHasPrecedence()){
                createOperatorNode()
            }
            operatorStack.push(currentToken)
        }

        else throw InvalidSyntaxException("Couldn't parse token")
    }

    /**
     *  Determines if the token on the operator stack has precedence over currentToken
     *
     *  @return true if it has precedence, else false
     */
    private fun operatorStackHasPrecedence(): Boolean {
        val leftAssociative = (currentToken as OperatorToken).precedence == (operatorStack.peek() as OperatorToken).precedence
        val comparePrecedence = (currentToken as OperatorToken).precedence < (operatorStack.peek() as OperatorToken).precedence
        val notParenthesis = operatorStack.peek() !is LeftParenthesisToken

        return comparePrecedence || leftAssociative && notParenthesis
    }
}
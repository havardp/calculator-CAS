package com.havardp.calculator.lexer

import com.havardp.exception.InvalidSyntaxException
import com.havardp.calculator.lexer.token.*

/**
 * A lexer which should be used by the parser to get tokens from a string
 *
 * Lexer class should be created with the string to be analysed as an argument, for example Lexer("2x+3")
 * The only public function is getNextToken, which should be called in a while loop
 *
 * var gnt = getNextToken()
 * while(gnt !is EOFToken){
 *      // logic on token
 *      gnt = getNextToken()
 * }
 *
 * @property str the string which we do lexical analysis on
 * @property pointer points to where in the str we are currently analysing
 * @property previous contains the previous token which was returned, to check if we need to add multiplication token (for example 2(2) -> 2*(2)
 */
class Lexer(private var str: String) {
    private var pointer: Int = 0
    private var previous: Token? = null

    init {
        str = str.replace(" ", "")
    }

    /**
     *  increments the pointer by the number of characters in the extracted token
     *  used whenever a token is returned, so that the pointer points past the token we extracted from the string
     *
     *  @param length the amount which we want to increase the pointer by.
     */
    private fun advance(length: Int){
        pointer += length + 1
    }

    /**
     *  the public method of the class, which is responsible for getting the token from extractToken(), and sets it to previous
     *
     *  @return the token we extracted
     */
    fun getNextToken(): Token {
        val token = extractToken()
        previous = token
        return token
    }

    /**
     *  The method which is responsible for the logic of the lexical analysis, extracting a token from the string
     *  uses the pointer and a for loop to analyse a specific substring.
     *
     *  also has some logic for adding multiplication tokens where it should be present, for example 2(2) -> 2*(2)
     *
     *  @return the token we extracted
     */
    private fun extractToken() : Token {
        for(i in 0 until str.length - pointer){
            // The index of $str that we are currently analysing, actually the index of the string after the current
            val index = pointer+i+1

            // The substring that we do lexical analysis on, ss for substring
            val ss = str.substring(pointer, index)

            // sometimes we need to check the next element, use this to be safe that there is one
            val nextElement = str.length > index

            // Check whether the substring is of type operand
            if(OperandToken.assert(ss)){
                // (2)3 -> (2)*3
                // x2   -> x*2
                if(previous is VariableToken || previous is OperandToken || previous is ImaginaryToken || previous is RightParenthesisToken)
                    return Multiplication()

                if(ss == "e") {
                    advance(i)
                    return OperandToken("2.7182818284")
                }
                if(ss == "pi") {
                    advance(i)
                    return OperandToken("3.1415926535")
                }

                // If next element is part of the operand, continue
                if (nextElement && OperandToken.assert(str[index].toString())) continue
                if (nextElement && str[index] == '.') continue

                advance(i)
                return OperandToken(ss)
            }

            // Check if it is a binary operator (mostly arithmetic, or sometimes unary in the case of + and -)
            else if(BinaryOperatorToken.assert(ss)){

                // Checks if the arithmetic operator should be a unary operator
                // Regarding the double bang, it should never happen because whenever pointer != 0, previous will exist
                advance(i)
                if(UnaryOperatorToken.assert(ss)
                        && (previous == null
                            || previous is LeftParenthesisToken
                            || BinaryOperatorToken.assert(previous!!.value)))
                    return UnaryOperatorToken.acquire(ss)


                return BinaryOperatorToken.acquire(ss)
            }

            // check if it is a unary operator (mostly functions)
            else if(UnaryOperatorToken.assert(ss)){
                if(previous is VariableToken || previous is OperandToken || previous is ImaginaryToken || previous is RightParenthesisToken)
                    return Multiplication()

                advance(i)
                return UnaryOperatorToken.acquire(ss)
            }

            // Check if it is a left parenthesis
            else if(ParenthesisToken.assert(ss)){
                // 2(2) -> 2*(2)
                // x(x) -> x*(x)
                if(ss == "(" && previous != null && previous !is BinaryOperatorToken && previous !is UnaryOperatorToken && previous !is LeftParenthesisToken)
                    return Multiplication()

                advance(i)
                return ParenthesisToken.acquire(ss)
            }

            // Check if it is a variable, currently only use "x" and "y"
            else if (VariableToken.assert(ss)){
                // 2x   -> 2*x
                // (2)x -> (2)*x
                if(previous is VariableToken || previous is OperandToken || previous is ImaginaryToken || previous is RightParenthesisToken)
                    return Multiplication()

                advance(i)
                return VariableToken(ss)
            }

            else if (ImaginaryToken.assert(ss)){
                if(previous is VariableToken || previous is OperandToken || previous is ImaginaryToken || previous is RightParenthesisToken)
                    return Multiplication()

                advance(i)
                return ImaginaryToken(ss)
            }
        }

        // pointer == str.length means that the string was lexed correctly, so we return EOF (end of file) token.
        if(pointer == str.length) return EOFToken("EOF")

        // If we get here without returning the EOFToken,
        // that means that we weren't able to extract all the tokens from the string,
        // So we throw an invalid syntax error
        throw InvalidSyntaxException("Couldn't do lexical analysis on the string, syntax is wrong")
    }
}
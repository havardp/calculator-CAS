package calculator.lexer

import calculator.exception.InvalidSyntaxException

/**
 * A lexer which should be used by the parser to get tokens from a string
 *
 * Lexer class should be created with the string to be analysed as an argument, for example Lexer("2x+3")
 * The only public function is getNextToken, which should be called in a while loop
 *
 * var gnt = getNextToken()
 * while(gnt !is EOFToken){
 *      // logic on current token
 *      gnt = getNextToken()
 * }
 *
 * @property str the string which we do lexical analysis on
 * @property pointer points to where in the str we are currently analysing
 * @property previous contains the previous token which was returned, to check if we need to add multiplication token (for example 2(2) -> 2*(2)
 */
class Lexer(private var str: String) {
    private var pointer: Int
    private var previous: Token?

    init {
        str = str.replace(" ", "")
        pointer = 0
        previous = null
    }

    /**
     *  increments the pointer by the number of characters in the extracted token
     *  used whenever a token is returned, so that the pointer points past the token we extraced from the string
     *
     *  @param length the amount which we want to increase the pointer by.
     */
    private fun advance(length: Int){
        pointer += length + 1
    }

    /**
     *  the public method of the class, which is responsible for getting the token from extractToken(), and sets it to previous
     *
     *  @return returns the token we extraced
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
     *  @return returns the token we extracted
     */
    private fun extractToken() : Token{
        for(i in 0 until str.length - pointer){
            // The index of $str that we are currently analysing
            val index = pointer+i+1

            // The substring that we do lexical analysis on, ss for substring
            val ss = str.substring(pointer, index)

            // sometimes we need to check the next element, use this to be safe that there is one
            val nextElement = str.length > index

            // Check whether the substring is of type operand
            if(OperandToken.assert(ss)){
                // (2)3 -> (2)*3
                // x2   -> x*2
                if(previous?.value == ")" || previous is VariableToken) return BinaryOperatorToken("*", 1)

                // If next element is part of the operand, continue
                if (nextElement && OperandToken.assert(str[index].toString())) continue
                if (nextElement && str[index] == '.') continue

                advance(i)
                return OperandToken(ss)
            }

            // Check if it is a binary operator (mostly arithmetic, or sometimes unary in the case of + and -)
            else if(BinaryOperatorToken.assert(ss)){
                advance(i)

                // Checks if the arithmetic operator should be a unary operator
                // Regarding the double bang, it should never happen because whenever pointer != 0, previous will exist
                if(pointer == 0
                        || (UnaryOperatorToken.assert(ss)
                                && (previous?.value == "("
                                || BinaryOperatorToken.assert(previous!!.value)))) {
                    return UnaryOperatorToken(ss, Int.MAX_VALUE)
                }
                return BinaryOperatorToken(ss, BinaryOperatorToken.precedence(ss))
            }

            // check if it is a unary operator (mostly functions)
            else if(UnaryOperatorToken.assert(ss)){
                advance(i)
                return UnaryOperatorToken(ss, Int.MAX_VALUE)
            }

            // Check if it is a parenthesis
            else if(ParenthesisToken.assert(ss)){
                // 2(2) -> 2*(2)
                // x(x) -> x*(x)
                if(ss == "(" && previous !is BinaryOperatorToken) return BinaryOperatorToken("*", 1)

                advance(i)
                return ParenthesisToken(ss, -1)
            }

            // Check if it is a variable, currently only use "x" and "y"
            else if (VariableToken.assert(ss)){
                if(previous is OperandToken || previous?.value == ")") return BinaryOperatorToken("*", 1)

                advance(i)
                return VariableToken(ss)
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
package calculator.parser

import old.*
import java.util.*

class BinaryOperatorNode

class UnaryOperatorNode

class OperandNode

class Parser





fun infixToPostfix(expression: Expression): Expression {
    val output = Expression()
    val operatorStack = Stack<ExpressionEntity>() // Operator stack

    for(e in expression){
        /* If element is a number */
        if(e is Operand){
            output.add(e)
        }

        else if(e is FunctionOperator || e is LeftParenthesis || e is UnaryOperator){
            operatorStack.push(e)
        }

        else if(e is RightParenthesis){
            while(operatorStack.peek() !is LeftParenthesis) output.add(operatorStack.pop())
            operatorStack.pop()
        }

        else if(e is ArithmeticOperator){
            /* If operator stack is empty, we put element in operator stack */
            if(operatorStack.empty()) operatorStack.push(e)

            else {
                while(!operatorStack.empty()){
                    // plus minus is left associative, times and divides is left associative
                    val leftAssociative = (e.getPrecedence() == 0 && (operatorStack.peek() as Operator).getPrecedence() == 0)
                            || (e.getPrecedence() == 1 && (operatorStack.peek() as Operator).getPrecedence() == 1)

                    if(e.getPrecedence() < (operatorStack.peek() as Operator).getPrecedence()
                            || leftAssociative
                            && operatorStack.peek() !is LeftParenthesis
                    ){
                        output.add(operatorStack.pop())
                    }
                    else break
                }
                operatorStack.push(e)
            }
        }
    }
    while(!operatorStack.empty()) output.add(operatorStack.pop())
    return output
}

fun evaluatePostfix(expression: Expression): Operand {
    val operandStack = Stack<Operand>()

    for(e in expression){
        if(e is Operand) operandStack.push(e)

        else if(e is UnaryOperator){
            val operand1 = operandStack.pop()
            val result = e.operate(operand1)
            operandStack.push(result)
        }

        else if (e is BinaryOperator){
            val operand1 = operandStack.pop()
            val operand2 = operandStack.pop()
            val result = e.operate(operand2, operand1)
            operandStack.push(result)
        }
    }

    return operandStack.pop()
}
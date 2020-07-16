package com.havardp.calculator.interpreter.nodeVisitor

import com.havardp.calculator.parser.*

/**
 * Parent of all other visitors, implementation of visitor pattern and double dispatch
 *      https://en.wikipedia.org/wiki/Visitor_pattern
 *      https://en.wikipedia.org/wiki/Double_dispatch#Visitor_pattern
 */
abstract class NodeVisitor {
    abstract fun visit(node: BinaryOperatorNode): Any
    abstract fun visit(node: UnaryOperatorNode): Any
    abstract fun visit(node: OperandNode): Any
    abstract fun visit(node: VariableNode): Any
    abstract fun visit(node: ImaginaryNode): Any
}



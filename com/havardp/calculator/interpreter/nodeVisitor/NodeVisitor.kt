package com.havardp.calculator.interpreter.nodeVisitor

import com.havardp.calculator.parser.*

// https://en.wikipedia.org/wiki/Visitor_pattern implementation
// https://en.wikipedia.org/wiki/Double_dispatch#Visitor_pattern
// TODO: Add type checker visitor? like check which visitor to use, arithmetic, variable etc..
// TODO: Add rewrite visitor, node transformation

abstract class NodeVisitor {
    abstract fun visit(node: BinaryOperatorNode): Any
    abstract fun visit(node: UnaryOperatorNode): Any
    abstract fun visit(node: OperandNode): Any
    abstract fun visit(node: VariableNode): Any
}



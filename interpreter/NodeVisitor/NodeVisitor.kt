package calculator.interpreter.NodeVisitor

import calculator.parser.*

// https://en.wikipedia.org/wiki/Visitor_pattern implementation
// https://en.wikipedia.org/wiki/Double_dispatch#Visitor_pattern
// TODO: Add type checker visitor? like check which visitor to use, arithmetic, variable etc..
// TODO: Add rewrite visitor, node transformation

abstract class NodeVisitor {
    abstract fun visit(node: BinaryOperatorNode)
    abstract fun visit(node: UnaryOperatorNode)
    abstract fun visit(node: OperandNode)
    abstract fun visit(node: VariableNode)
}



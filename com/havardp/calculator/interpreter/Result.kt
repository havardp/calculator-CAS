package com.havardp.calculator.interpreter

import com.havardp.calculator.parser.AbstractSyntaxTree

abstract class Result

data class OrdinaryResult(val input: String, val result: String, val solveSteps: ArrayList<String>, val explanationSteps: ArrayList<String>): Result()

data class QuadraticResult(val input: String, val quadraticFormula: String, val root1: OrdinaryResult, val root2: OrdinaryResult): Result()
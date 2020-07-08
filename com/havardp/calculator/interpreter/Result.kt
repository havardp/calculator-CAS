package com.havardp.calculator.interpreter

class Result {
    var input: String = ""
    var result: String = ""
    var solveSteps = ArrayList<String>()
    var explanationSteps = ArrayList<String>()

    var isQuadratic = false
    var quadraticFormula = ""
    var root1: Result? = null
    var root2: Result? = null
}
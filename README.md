# Calculator-CAS
Step by step solutions for solving arithmetic, algebraic and quadratic equations!

All math that is returned is in LaTeX format.

## How it works
There are four main components that handles the data; lexer, parser, interpreter and node visitor. 
The data structures that stores the "equations" are tokens and abstract syntax tree.

#### Lexer and Tokens
The lexer is responsible for turning the elements of the equation string into tokens.
It does so by analysing the string letter for letter. It would for example turn "2+2" into 
` Operand(2) BinaryOperator(+) Operand(2) `. It has smart features like adding multiplication
where necessary, for example between 2 and x. The supported tokens are Operand (all numbers including decimal),
UnaryOperator (functions mostly, and unary plus and unary minus), Variable (handles "x"), Imaginary (handles "i") and
BinaryOperator (like plus, multiplication etc). The lexer basically splits up the parts of the equation and stores them in 
tokens which is an easy data structure to handle.

#### Parser and Abstract Syntax Tree
The parser then takes the tokens, and creates an [Abstract Syntax Tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree).
It does so using the [Shunting Yard Algorithm](https://en.wikipedia.org/wiki/Shunting-yard_algorithm). 
The parser finds errors like "2*/3", and mismatches of parentheses, like "2(3+2". 
An example of an Abstract Syntax tree of the tokens Operand(2) BinaryOperator(+) Operand(3) would be 
`Plus(2, 3)`, which would look like this graph:
```
        +
      /   \
    2       3
```

#### Interpreter and Node Visitor
The job of the interpreter is then to manipulate Abstract Syntax Tree. It does so using a [Visitor Pattern](https://en.wikipedia.org/wiki/Visitor_pattern) 
called [Double Dispatch](https://en.wikipedia.org/wiki/Double_dispatch). The RewriteVisitor is responsible for
manipulating the tree and doing all the calculations or changes that can be done on an equation. The RewriteVisitor
Does a [Post Order Traversal](https://www.techiedelight.com/postorder-tree-traversal-iterative-recursive/), Which means we
try to do changes from the bottom up. A change or transformation of the tree can be evaluating two operands in a binary operator, 
or distributing out a variable, for example `2x+xsin(x) -> x(2+sin(x)`. For every change we do, we store the rewritten
equation and the explanation, both in latex format (handled by a pretty print latex node visitor). In the interpreter there 
are also checks for quadratic equations, and depending on if it is one or not, the interpreter returns either a 
quadratic equation result object, or an ordinary result object (See under how they are structured).

Do note that this is nowhere near as complex as an actual Computer Algebra System, but it can handle a surprising
amount of equations, see [below](#Examples), or view the [test file](com/havardp/test/Test.kt) for some examples.


## Usage
Here is the code snippet to get a Result object (if there is no syntax errors)
```kotlin
try {
        val lexer = Lexer(yourMathString)
        val parser = Parser(lexer)
        val interpreter = Interpreter(parser)
        val result = interpreter.interpret()
} catch (e: InvalidSyntaxException){
        // handle error 
        // examples can be symbols not able to be lexed to tokens, like "2+a"
        // or not being parsed correctly, like 2*/2
}
```

The Result object is either OrdinaryResult
```
{
    input: String,
    result: String,
    solveSteps: Stack<String>,
    explanationSteps: Stack<String>, 
    error: String? = null
}
```

or QuadraticResult
```
{
    input: String,
    quadraticFormula: String,
    root1: OrdinaryResult,
    root2: OrdinaryResult
}
```

## Examples
Below are some example of user input, and the corresponding latex result

|Input   	|Result|
|---	|---	|
|sin(pi/2)  	|<img src="https://render.githubusercontent.com/render/math?math=0">|
|2^4 |<img src="https://render.githubusercontent.com/render/math?math=16">|
|2x-2=6|<img src="https://render.githubusercontent.com/render/math?math=x=4">|
|2+(2+x)+2x+2sin(x)+-3x*3*x/x+3x-(3x+2)+sin(x)|<img src="https://render.githubusercontent.com/render/math?math=2 - 6 \cdot x %2B 3 \cdot sin(x)">|
|x^2+2x+1=0|<img src="https://render.githubusercontent.com/render/math?math=x=-1">|
|x^2-2x+1=0|<img src="https://render.githubusercontent.com/render/math?math=\frac{2 \pm 2.8284271247461903 \cdot i}{2}">|


## License
Calculator-CAS is available under the MIT license. See the [LICENSE](LICENSE) file for more info.
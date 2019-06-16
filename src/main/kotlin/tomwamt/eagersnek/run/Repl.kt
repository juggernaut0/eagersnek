package tomwamt.eagersnek.run

import tomwamt.eagersnek.code.CodeGen
import tomwamt.eagersnek.code.CompiledCode
import tomwamt.eagersnek.parse.ASTParser
import tomwamt.eagersnek.parse.Scanner

fun repl() {
    val int = Interpreter("repl", true)
    val topFrame = CallFrame(CompiledFunction(CompiledCode(), Scope(null), int.rootNamespace, 0, "repl", 0))
    int.callStack.push(topFrame)
    while (true) {
        print(">>> ")
        val line = readLine().takeUnless { it.isNullOrBlank() } ?: break
        val code = CodeGen.compile(ASTParser.parse(Scanner.scan(line)))
        //println(code.map { it.opCode })
        try {
            int.run(code)
        } catch (e: InterpreterException) {
            System.err.println(e.message)
            int.callStack.asList()
                    .asReversed()
                    .mapIndexed { i, frame -> "  ${if (i == 0) "  in" else "from"} ${frame.fn} at line ${frame.srcLine}" }
                    .forEach { System.err.println(it) }
            // reset repl
            int.callStack.clear()
            int.callStack.push(topFrame)
            int.execStack.clear()

        }
        if (int.execStack.asList().isNotEmpty()) {
            val r = int.execStack.pop()
            if (r.type != Builtin.Unit) {
                println(Builtin.stringify(r))
            }
        }
        println(int.execStack)
    }
}

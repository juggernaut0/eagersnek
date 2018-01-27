package tomwamt.eagersnek

import tomwamt.eagersnek.parse.ASTParser
import tomwamt.eagersnek.parse.Scanner
import tomwamt.eagersnek.code.CodeGen
import tomwamt.eagersnek.run.Interpreter
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val path = "examples/predef.ess"
    val code = String(Files.readAllBytes(Paths.get(path)))
    println("file loaded")
    val ast = ASTParser.parse(Scanner.scan(code))
    Interpreter.run(CodeGen.compile(ast))
}

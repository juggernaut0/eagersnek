package tomwamt.eagersnek

import tomwamt.eagersnek.parse.ASTParser
import tomwamt.eagersnek.parse.Scanner
import tomwamt.eagersnek.code.CodeGen
import tomwamt.eagersnek.run.Interpreter
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val path = "examples/match.ess"
    val code = String(Files.readAllBytes(Paths.get(path)))
    println("file loaded")
    val ast = ASTParser.parse(Scanner.scan(code))
    val compiled = CodeGen.compile(ast)
    println(compiled)
}

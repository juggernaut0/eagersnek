package tomwamt.eagersnek

import tomwamt.eagersnek.parse.ASTParser
import tomwamt.eagersnek.parse.Scanner
import tomwamt.eagersnek.code.CodeGen
import tomwamt.eagersnek.run.Interpreter
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val path = args[0]
    val src = String(Files.readAllBytes(Paths.get(path)))
    println("loaded")
    val ast = ASTParser.parse(Scanner.scan(src))
    println("parsed")
    val code = CodeGen.compile(ast)
    println("compiled")
    Interpreter().exec(code)
}

package tomwamt.eagersnek

import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val path = "examples/big.txt"
    val code = String(Files.readAllBytes(Paths.get(path)))
    println("file loaded")
    val ast = ASTParser.parse(Scanner.scan(code))
    println(ast)
}

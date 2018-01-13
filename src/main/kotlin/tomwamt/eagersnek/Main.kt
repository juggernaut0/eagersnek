package tomwamt.eagersnek

import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val path = "examples.txt"
    val code = String(Files.readAllBytes(Paths.get(path)))
    ASTParser.parse(Scanner.scan(code))
}

package tomwamt.eagersnek.run

import tomwamt.eagersnek.code.CodeGen
import tomwamt.eagersnek.parse.ASTParser
import tomwamt.eagersnek.parse.Scanner
import java.nio.file.Files
import java.nio.file.Paths

class Module(private val names: Namespace) {
    companion object {
        var appDir: String = "."

        fun fromFile(filename: String): Module {
            val src = String(Files.readAllBytes(Paths.get(appDir, filename)))
            val ast = ASTParser.parse(Scanner.scan(src))
            val code = CodeGen.compile(ast)
            val interpreter = Interpreter()
            interpreter.exec(code)
            return Module(interpreter.rootNamespace)
        }
    }

    fun importInto(target: Namespace) {
        import(names, target)
    }

    private fun import(src: Namespace, target: Namespace) {
        src.bindings.forEach { (name, obj) -> target.bindings[name] = obj }
        src.types.forEach { (name, type) -> target.types[name] = type }

        src.subnames
                .filter { it.value.public }
                .forEach { (name, ns) ->
                    val t = target.subnames.computeIfAbsent(name) { Namespace(true) }
                    import(ns, t)
                }
    }
}
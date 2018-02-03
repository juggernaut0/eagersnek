package tomwamt.eagersnek.run

import tomwamt.eagersnek.code.CodeGen
import tomwamt.eagersnek.parse.ASTParser
import tomwamt.eagersnek.parse.Scanner
import java.nio.file.Files
import java.nio.file.Paths

class Module(val name: String, private val namespace: Namespace) {
    companion object {
        var appDir: String = "."

        private val cache: MutableMap<String, Module> = mutableMapOf()

        fun fromCode(src: String, name: String, predef: Boolean = true): Module {
            val ast = ASTParser.parse(Scanner.scan(src))
            val code = CodeGen.compile(ast)
            val interpreter = Interpreter(name, predef)
            interpreter.exec(code)
            return Module(name, interpreter.rootNamespace)
        }

        fun fromFile(filename: String): Module {
            return cache.computeIfAbsent(filename) {
                val src = String(Files.readAllBytes(Paths.get(appDir, filename)))
                fromCode(src, filename)
            }
        }

        val predef: Module = cache.computeIfAbsent("predef") {
            val reader = Module::class.java.getResourceAsStream("/predef.ess")?.reader() ?: throw InterpreterException("resource not found")
            fromCode(reader.readText(), "predef", false)
        }
    }

    fun importInto(target: Namespace) {
        import(namespace, target)
    }

    fun importNameInto(qname: List<String>, target: Namespace) {
        var src = namespace
        var tgt = target
        for (name in qname.subList(0, qname.lastIndex)) {
            src = src.subnames[name] ?: throw InterpreterException("No namespace $name")
            if (!src.public) throw InterpreterException("${qname.joinToString(".")} is not public")
            tgt = tgt.subnames.computeIfAbsent(name) { Namespace(true) }
        }
        val last = qname.last()

        src.subnames[last]?.let {
            if (!it.public) throw InterpreterException("${qname.joinToString(".")} is not public")
            import(it, tgt.subnames.computeIfAbsent(last) { Namespace(true) })
        }
        src.types[last]?.let { tgt.types[last] = it }
        src.bindings[last]?.let { tgt.bindings[last] = it }
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
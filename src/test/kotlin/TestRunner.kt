import tomwamt.eagersnek.code.CodeGen
import tomwamt.eagersnek.parse.ASTParser
import tomwamt.eagersnek.parse.Scanner
import tomwamt.eagersnek.run.Interpreter
import tomwamt.eagersnek.run.StringIOProvider
import kotlin.test.*

class TestRunner {
    private fun runTest(src: String, input: String = "", asserts: (String, String) -> Unit) {
        val out = StringBuffer()
        val err = StringBuffer()
        val ast = ASTParser.parse(Scanner.scan(src))
        val code = CodeGen.compile(ast)
        val interpreter = Interpreter("test", io = StringIOProvider(input, out, err))
        interpreter.exec(code)
        asserts(out.toString(), err.toString())
    }

    @Test
    fun hello() {
        runTest("""
            (println "Hello World!")
        """.trimIndent()) { out, err ->
            assertEquals("", err)
            assertEquals("Hello World!\n", out)
        }
    }

    @Test
    fun input() {
        runTest("""
            let n = (to_num (input "Enter a number: "))

            (println (* 2 n))
        """.trimIndent(), "42") { out, err ->
            assertEquals("", err)
            assertEquals("Enter a number: \n84\n", out)
        }
    }

    @Test
    fun list() {
        runTest("""
            (println [1 2 3 4 5])
        """.trimIndent()) { out, err ->
            assertEquals("", err)
            assertEquals("(:: 1 (:: 2 (:: 3 (:: 4 (:: 5 Empty)))))\n", out)
        }
    }

    @Test
    fun match() {
        runTest("""
            let foo = { n ->
                (match n [
                    { 0 -> (println "Foo") }
                    { _ ->
                        let _ = (println n)
                        (. (- n 1))
                    }
                ])
            }

            (foo 5)
        """.trimIndent()) { out, err ->
            assertEquals("", err)
            assertEquals("5\n4\n3\n2\n1\nFoo\n", out)
        }
    }

    @Test
    fun partial() {
        runTest("""
            let f = { a b c ->
                let _ = (println a)
                let _ = (println b)
                let _ = (println c)
                ()
            }

            let p = (f 1 2)

            (p 3)
        """.trimIndent()) { out, err ->
            assertEquals("", err)
            assertEquals("1\n2\n3\n", out)
        }
    }

    @Test
    fun scope() {
        runTest("""
            let a = "global"
            let f = { ->
                let g = { -> (println a) }
                let a = "local"
                (g)
            }

            (f)
        """.trimIndent()) { out, err ->
            assertEquals("", err)
            assertEquals("local\n", out)
        }
    }
    @Test
    fun scopeError() {
        runTest("""
            let a = "global"
            let f = { ->
                let g = { -> (println a) }
                let _ = (g)
                let a = "local"
                ()
            }

            (f)
        """.trimIndent()) { out, err ->
            assertTrue(err.isNotBlank())
            assertEquals("", out)
        }
    }
}
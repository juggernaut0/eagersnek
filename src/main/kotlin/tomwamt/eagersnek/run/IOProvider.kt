package tomwamt.eagersnek.run

import java.util.*

interface IOProvider {
    fun out(s: String)
    fun err(s: String)
    fun inp(): String?
}

object SystemIOProvider : IOProvider {
    override fun out(s: String) {
        println(s)
    }

    override fun err(s: String) {
        System.err.println(s)
    }

    override fun inp(): String? {
        return readLine()
    }
}

class StringIOProvider(input: String, val out: StringBuffer, val err: StringBuffer) : IOProvider {
    private val scan = Scanner(input)

    override fun out(s: String) {
        out.appendln(s)
    }

    override fun err(s: String) {
        err.appendln(s)
    }

    override fun inp(): String? {
        return try {
            scan.nextLine()
        } catch (e: NoSuchElementException) {
            null
        }
    }
}

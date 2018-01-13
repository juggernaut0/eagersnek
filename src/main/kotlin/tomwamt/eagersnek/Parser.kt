package tomwamt.eagersnek

abstract class Parser<out T> {
    abstract fun parse(tokens: Seq<Token>): T

    protected inline fun <T> Seq<Token>.require(expected: String, block: (Seq<Token>) -> SeqResult<T>?): SeqResult<T> {
        return block(this) ?: parseError(expected, head())
    }

    protected fun Seq<Token>.expect(type: TokenType): SeqResult<String>? {
        if (empty) return null
        val t = head()
        if (t.type != type) return null
        return SeqResult(t.value, tail())
    }

    protected fun Seq<Token>.expectOrThrow(type: TokenType): SeqResult<String> {
        return expect(type) ?: parseError(type.toString(), head())
    }

    protected fun Seq<Token>.match(type: TokenType, value: String): Seq<Token>? {
        if (empty) return null
        val t = head()
        if (t.matches(type, value)) return tail()
        return null
    }

    protected fun Seq<Token>.matchOrThrow(type: TokenType, value: String): Seq<Token> {
        return match(type, value) ?: parseError(value, head())
    }

    protected inline fun <T> Seq<Token>.star(block: (Seq<Token>) -> SeqResult<T>?) =
            starInto(mutableListOf(), block)

    protected inline fun <T> Seq<Token>.starInto(list: MutableList<T>, block: (Seq<Token>) -> SeqResult<T>?): SeqResult<List<T>> {
        var t = this
        while(!t.empty) {
            val res = block(t) ?: break
            list.add(res.result)
            t = res.seq
        }

        return SeqResult(list, t)
    }

    protected fun <T> parseError(expected: String, actual: Token): T {
        throw ParseException("Expected $expected, got ${actual.type}(${actual.value}) at line ${actual.line}")
    }
}
package tomwamt.eagersnek

abstract class Parser<out T> {
    abstract fun parse(tokens: Sequence<Token>): T

    protected fun expect(type: TokenType, tokens: Sequence<Token>): SeqResult<String>? {
        val t = tokens.first()
        if (t.type != type) return null
        return SeqResult(t.value, tokens.drop(1))
    }

    protected fun expectOrThrow(type: TokenType, tokens: Sequence<Token>): SeqResult<String> {
        return expect(type, tokens) ?: throw ParseException("Expected a $type; got ${tokens.first().value}")
    }

    protected fun match(type: TokenType, value: String, tokens: Sequence<Token>): Sequence<Token>? {
        val t = tokens.first()
        if (t.matches(type, value)) return tokens.drop(1)
        return null
    }

    protected fun matchOrThrow(type: TokenType, value: String, tokens: Sequence<Token>): Sequence<Token> {
        return match(type, value, tokens) ?: throw ParseException("Expected $value; got ${tokens.first().value}")
    }

    protected inline fun <T> parseStar(tokens: Sequence<Token>, block: (Sequence<Token>) -> SeqResult<T>?) =
            parseStarInto(mutableListOf(), tokens, block)

    protected inline fun <T> parseStarInto(list: MutableList<T>, tokens: Sequence<Token>, block: (Sequence<Token>) -> SeqResult<T>?): SeqResult<List<T>> {
        var t = tokens
        while(true) {
            val res = block(t) ?: break
            list.add(res.result)
            t = res.seq
        }

        return SeqResult(list, t)
    }
}
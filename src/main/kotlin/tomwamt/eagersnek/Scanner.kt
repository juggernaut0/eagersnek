package tomwamt.eagersnek

object Scanner {
    private val patterns: List<Pair<TokenType, Regex>>

    init {
        val symbols = listOf(
                ".",
                "{",
                "}",
                "()",
                "(",
                ")",
                "[]",
                "[",
                "]",
                "=",
                "->"
        )

        patterns = listOf(
                TokenType.IGNORE to Regex("#.*?(?=\\n|\$)"),
                TokenType.NUMBER to Regex("\\d+(\\.\\d*)?"),
                TokenType.STRING to Regex("""'[^\n\r]*?'|"[^\n\r]*?""""),
                TokenType.KEYWORD to Keywords.values().joinToString("|") { it.kw }.toRegex(),
                TokenType.SYMBOL to symbols.joinToString("|") { Regex.escape(it) }.toRegex(),
                TokenType.IDENT to Regex("[^()\\[\\].\\n\\r\\s]+"),
                TokenType.IGNORE to Regex("[ \t\r]+"),
                TokenType.NEWLINE to Regex("\\n")
        )
    }

    fun scan(code: String): Sequence<Token> =
            generateSequence(token(code,0, 1)) { token(code, it.start + it.value.length, it.line) }
                    .filterNot { it.type == TokenType.NEWLINE || it.type == TokenType.IGNORE }

    private fun token(code: String, start: Int, currentLine: Int): Token? {
        for ((name, re) in patterns) {
            val match = re.find(code, start)
            if (match != null && match.range.first == start) {
                val line = currentLine + if (name == TokenType.NEWLINE) 1 else 0
                return Token(name, match.value, line, start)
            }
        }
        return null
    }
}

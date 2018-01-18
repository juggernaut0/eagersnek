package tomwamt.eagersnek.parse

object Scanner {
    private val patterns: List<Pair<TokenType, Regex>>

    init {
        val symbols = listOf(
                "|",
                ".",
                "{",
                "}",
                "()",
                "(",
                ")",
                "[]",
                "[",
                "]",
                "->"
        )

        patterns = listOf(
                TokenType.IGNORE to Regex("#.*?(?=\\n|\$)"),
                TokenType.NUMBER to Regex("\\d+(\\.\\d*)?"),
                TokenType.STRING to Regex("""'[^\n\r]*?'|"[^\n\r]*?""""),
                TokenType.KEYWORD to Keyword.values().joinToString("|") { it.kw }.toRegex(),
                TokenType.SYMBOL to Regex("=(?!=)"),
                TokenType.SYMBOL to symbols.joinToString("|") { Regex.escape(it) }.toRegex(),
                TokenType.IDENT to Regex("[^()\\[\\].\\n\\r\\s]+"),
                TokenType.IGNORE to Regex("[ \t\r]+"),
                TokenType.NEWLINE to Regex("\\n")
        )
    }

    fun scan(code: String): Seq<Token> {
        val firstToken = token(code, 0, 1)
                ?: return Seq.empty()
        val state = Seq.SeqState(firstToken, { it }, { token(code, it.start + it.value.length, it.line) })
        return state.toSeq().filter { it.type != TokenType.NEWLINE && it.type != TokenType.IGNORE }
    }

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

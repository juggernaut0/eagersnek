package tomwamt.eagersnek

data class Token(val type: TokenType, val value: String, val line: Int, val start: Int) {
    fun matches(type: TokenType, value: String) = type == this.type && value == this.value
}

enum class TokenType {
    IGNORE,
    NEWLINE,
    IDENT,
    NUMBER,
    STRING,
    KEYWORD,
    SYMBOL
}

enum class Keywords(val kw: String = toString().toLowerCase()) {
    IMPORT,
    NAMESPACE,
    TYPE,
    LET
}

package tomwamt.eagersnek

import tomwamt.eagersnek.parse.*

object ASTParser : Parser<AST>() {
    override fun parse(tokens: Sequence<Token>): AST {
        val imports = imports(tokens)
        val rootNamespace = decls(imports.seq, QualifiedName(emptyList()))

        return AST(imports.result, rootNamespace.result)
    }

    private fun imports(tokens: Sequence<Token>): SeqResult<List<ImportStmt>> {
        return parseStar(tokens) {
            match(TokenType.KEYWORD, Keyword.IMPORT.kw, it)
                    ?.let { qualName(it) }
                    ?.map { ImportStmt(it) }
        }
    }

    private fun qualName(tokens: Sequence<Token>): SeqResult<QualifiedName> {
        return expectOrThrow(TokenType.IDENT, tokens)
                .flatMap {
                    parseStar(it) {
                        match(TokenType.SYMBOL, ".", it)?.let { expectOrThrow(TokenType.IDENT, it) }
                    }
                }
                .map { (first, rest) -> listOf(first, *rest.toTypedArray()) }
                .map { QualifiedName(it) }
    }

    private fun decls(tokens: Sequence<Token>, name: QualifiedName): SeqResult<NamespaceDecl> {
        val namespaces = mutableListOf<NamespaceDecl>()
        val types = mutableListOf<TypeDecl>()
        val bindings = mutableListOf<BindingDecl>()

        var t = tokens
        while (true) {
            val first = t.first()
            t = if (first.matches(Keyword.NAMESPACE)) {
                val res = namespace(t)
                namespaces.add(res.result)
                res.seq
            } else if (first.matches(Keyword.TYPE)) {
                val res = type(t)
                types.add(res.result)
                res.seq
            } else if (first.matches(Keyword.LET)) {
                val res = binding(t)
                bindings.add(res.result)
                res.seq
            } else {
                break
            }
        }

        return SeqResult(NamespaceDecl(name, namespaces, types, bindings), t)
    }

    private fun declBlock(tokens: Sequence<Token>, name: QualifiedName): SeqResult<NamespaceDecl> {
        return matchOrThrow(TokenType.SYMBOL, "{", tokens)
                .let { decls(it, name) }
                .thenConsume { matchOrThrow(TokenType.SYMBOL, "}", it) }
    }

    private fun namespace(tokens: Sequence<Token>): SeqResult<NamespaceDecl> {
        return matchOrThrow(TokenType.KEYWORD, Keyword.NAMESPACE.kw, tokens)
                .let { qualName(it) }
                .let { declBlock(it.seq, it.result) }
    }

    private fun type(tokens: Sequence<Token>): SeqResult<TypeDecl> {
        val name = matchOrThrow(TokenType.KEYWORD, Keyword.TYPE.kw, tokens).let { qualName(it) }
        val params = parseStar(name.seq) { expect(TokenType.IDENT, it) }
        val cases = match(TokenType.SYMBOL, "=", params.seq)
                    ?.let { typeCases(it) }
                    ?: SeqResult(emptyList(), params.seq)

        val namespace = cases.seq.let {
            if (it.first().matches(TokenType.SYMBOL, "{")) {
                declBlock(cases.seq, name.result)
            } else {
                SeqResult(null, cases.seq)
            }
        }

        return SeqResult(
                TypeDecl(name.result, params.result, cases.result, namespace.result),
                namespace.seq)
    }

    private fun typeCases(tokens: Sequence<Token>): SeqResult<List<TypeCase>> {
        return typeCase(tokens)
                .flatMap {
                    parseStar(it) {
                        match(TokenType.SYMBOL, "|", it)?.let { typeCase(it) }
                    }
                }
                .map { (first, rest) -> listOf(first, *rest.toTypedArray()) }
    }

    private fun typeCase(tokens: Sequence<Token>): SeqResult<TypeCase> {
        return expectOrThrow(TokenType.IDENT, tokens)
                .flatMap {
                    parseStar(it) { expect(TokenType.IDENT, it) }
                }
                .map { (name, params) -> TypeCase(name, params) }
    }

    private fun binding(tokens: Sequence<Token>): SeqResult<BindingDecl> {
        return matchOrThrow(TokenType.KEYWORD, Keyword.LET.kw, tokens)
                .let { pattern(it) ?: throw ParseException("Expected a pattern") }
                .thenConsume { matchOrThrow(TokenType.SYMBOL, "=", it) }
                .flatMap { block(it) }
                .map { (pattern, block) -> BindingDecl(pattern, block) }
    }

    private fun pattern(tokens: Sequence<Token>): SeqResult<Pattern>? {
        return match(TokenType.IDENT, "_", tokens)?.let { SeqResult(WildcardPattern(), it) }
                ?: expect(TokenType.IDENT, tokens)?.map { NamePattern(it) }
                ?: match(TokenType.SYMBOL, "[", tokens)
                        ?.let { parseStar(it) { pattern(it) } }
                        ?.map { ListPattern(it) }

                ?: TODO()
    }

    private fun block(tokens: Sequence<Token>): SeqResult<Block> {
        TODO()
    }
}
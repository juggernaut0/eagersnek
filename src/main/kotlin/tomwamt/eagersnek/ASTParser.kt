package tomwamt.eagersnek

import tomwamt.eagersnek.parse.*

object ASTParser : Parser<AST>() {
    override fun parse(tokens: Seq<Token>): AST {
        val imports = imports(tokens)
        val rootNamespace = decls(imports.seq, QualifiedName(emptyList()))
        val call = callExpr(rootNamespace.seq)

        return AST(imports.result, rootNamespace.result, call?.result)
    }

    private fun imports(tokens: Seq<Token>): SeqResult<List<ImportStmt>> {
        return tokens.star {
            it.match(TokenType.KEYWORD, Keyword.IMPORT.kw)
                    ?.require("qualified name") { qualName(it) }
                    ?.map { ImportStmt(it) }
        }
    }

    private fun qualName(tokens: Seq<Token>): SeqResult<QualifiedName>? {
        return (tokens.expect(TokenType.IDENT) ?: return null)
                .flatMap {
                    it.star {
                        it.match(TokenType.SYMBOL, ".")?.expectOrThrow(TokenType.IDENT)
                    }
                }
                .map { (first, rest) -> listOf(first, *rest.toTypedArray()) }
                .map { QualifiedName(it) }
    }

    private fun decls(tokens: Seq<Token>, name: QualifiedName): SeqResult<Namespace> {
        return tokens.star { decl(it) }.map { Namespace(name, it) }
    }

    private fun decl(tokens: Seq<Token>): SeqResult<Decl>? {
        return namespace(tokens) ?: type(tokens) ?: binding(tokens)
    }

    private fun declBlock(tokens: Seq<Token>, name: QualifiedName): SeqResult<Namespace> {
        return tokens.matchOrThrow(TokenType.SYMBOL, "{")
                .let { decls(it, name) }
                .thenConsume { it.matchOrThrow(TokenType.SYMBOL, "}") }
    }

    private fun namespace(tokens: Seq<Token>): SeqResult<Namespace>? {
        return tokens.match(TokenType.KEYWORD, Keyword.NAMESPACE.kw)
                ?.require("qualified name") { qualName(it) }
                ?.let { declBlock(it.seq, it.result) }
    }

    private fun type(tokens: Seq<Token>): SeqResult<TypeDecl>? {
        val name = tokens.match(TokenType.KEYWORD, Keyword.TYPE.kw)
                ?.require("qualified name") { qualName(it) }
                ?: return null
        val params = name.seq.star { it.expect(TokenType.IDENT) }
        val cases = params.seq.match(TokenType.SYMBOL, "=")
                    ?.let { typeCases(it) }
                    ?: SeqResult(emptyList(), params.seq)

        val namespace = cases.seq.let {
            if (it.head().matches(TokenType.SYMBOL, "{")) {
                declBlock(cases.seq, name.result)
            } else {
                SeqResult(null, cases.seq)
            }
        }

        return SeqResult(
                TypeDecl(name.result, params.result, cases.result, namespace.result),
                namespace.seq)
    }

    private fun typeCases(tokens: Seq<Token>): SeqResult<List<TypeCase>> {
        return typeCase(tokens)
                .flatMap {
                    it.star {
                        it.match(TokenType.SYMBOL, "|")?.let { typeCase(it) }
                    }
                }
                .map { (first, rest) -> listOf(first, *rest.toTypedArray()) }
    }

    private fun typeCase(tokens: Seq<Token>): SeqResult<TypeCase> {
        return tokens.expectOrThrow(TokenType.IDENT)
                .flatMap {
                    it.star { it.expect(TokenType.IDENT) }
                }
                .map { (name, params) -> TypeCase(name, params) }
    }

    private fun binding(tokens: Seq<Token>): SeqResult<Binding>? {
        return tokens.match(TokenType.KEYWORD, Keyword.LET.kw)
                ?.require("pattern") { pattern(it) }
                ?.thenConsume { it.matchOrThrow(TokenType.SYMBOL, "=") }
                ?.flatMap { it.require("block") { block(it) } }
                ?.map { (pattern, block) -> Binding(pattern, block) }
    }

    private fun pattern(tokens: Seq<Token>): SeqResult<Pattern>? {
        return tokens.match(TokenType.IDENT, "_")?.let { SeqResult(WildcardPattern(), it) }
                ?: tokens.expect(TokenType.IDENT)?.map { NamePattern(it) }
                ?: listPattern(tokens)
                ?: funcPattern(tokens)
                ?: constPattern(tokens)
    }

    private fun listPattern(tokens: Seq<Token>): SeqResult<ListPattern>? {
        return tokens.match(TokenType.SYMBOL, "[")
                ?.star { pattern(it) }
                ?.map { ListPattern(it) }
                ?.thenConsume { it.matchOrThrow(TokenType.SYMBOL, "]") }
    }

    private fun funcPattern(tokens: Seq<Token>): SeqResult<FuncPattern>? {
        return tokens.match(TokenType.SYMBOL, "(")
                ?.require("qualified name") { qualName(it) }
                ?.flatMap { it.star { pattern(it) } }
                ?.map { (name, params) -> FuncPattern(name, params) }
                ?.thenConsume { it.matchOrThrow(TokenType.SYMBOL, ")") }
    }

    private fun constPattern(tokens: Seq<Token>): SeqResult<ConstPattern>? {
        return constLiteral(tokens)?.map { ConstPattern(it) }
    }

    private fun block(tokens: Seq<Token>): SeqResult<Block>? {
        val bindings = tokens.star { binding(it) }
        return if (bindings.result.isNotEmpty()) {
            bindings.flatMap { it.require("expr") { expr(it) } }
                    .map { (bindings, expr) -> Block(bindings, expr) }
        } else {
            expr(bindings.seq)?.map { Block(emptyList(), it) }
        }
    }

    private fun expr(tokens: Seq<Token>): SeqResult<Expr>? {
        return callExpr(tokens)
                ?: lambdaExpr(tokens)
                ?: listExpr(tokens)
                ?: constLiteral(tokens)
                ?: qualName(tokens)
    }

    private fun callExpr(tokens: Seq<Token>): SeqResult<CallExpr>? {
        return tokens.match(TokenType.SYMBOL, "(")
                ?.require("expr") { expr(it) }
                ?.flatMap { it.star { expr(it) } }
                ?.map { (callable, args) -> CallExpr(callable, args) }
                ?.thenConsume { it.matchOrThrow(TokenType.SYMBOL, ")") }
    }

    private fun lambdaExpr(tokens: Seq<Token>): SeqResult<LambdaExpr>? {
        return tokens.match(TokenType.SYMBOL, "{")
                ?.star { pattern(it) }
                ?.thenConsume { it.matchOrThrow(TokenType.SYMBOL, "->") }
                ?.flatMap { it.require("block") { block(it) } }
                ?.thenConsume { it.matchOrThrow(TokenType.SYMBOL, "}") }
                ?.map { (patterns, block) -> LambdaExpr(patterns, block) }
    }

    private fun listExpr(tokens: Seq<Token>): SeqResult<ListExpr>? {
        return tokens.match(TokenType.SYMBOL, "[")
                ?.star { block(it) }
                ?.thenConsume { it.matchOrThrow(TokenType.SYMBOL, "]") }
                ?.map { ListExpr(it) }
    }

    private fun constLiteral(tokens: Seq<Token>): SeqResult<ConstLiteral>? {
        return tokens.expect(TokenType.NUMBER)?.map { ConstLiteral("number", it) }
                ?: tokens.expect(TokenType.STRING)?.map { ConstLiteral("string", it) }
                ?: tokens.match(TokenType.SYMBOL, "()")?.let { SeqResult(ConstLiteral("unit", "()"), it) }
                ?: tokens.match(TokenType.SYMBOL, "[]")?.let { SeqResult(ConstLiteral("emptylist", "[]"), it) }
    }
}
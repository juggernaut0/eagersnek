package tomwamt.eagersnek.parse

object ASTParser : Parser<AST>() {
    override fun parse(tokens: Seq<Token>): AST {
        val imports = tokens.imports()
        val rootNamespace = imports.seq.decls(QualifiedName(emptyList()))
        val call = rootNamespace.seq.callExpr()

        return AST(imports.result, rootNamespace.result, call?.result)
    }

    private fun Seq<Token>.imports(): SeqResult<List<ImportStmt>> {
        return star {
            match(TokenType.KEYWORD, Keyword.IMPORT.kw)
                    ?.require("qualified name") { qualName() }
                    ?.map { ImportStmt(it) }
        }
    }

    private fun Seq<Token>.qualName(): SeqResult<QualifiedName>? {
        return (expect(TokenType.IDENT) ?: return null)
                .then {
                    star {
                        match(TokenType.SYMBOL, ".")?.expectOrThrow(TokenType.IDENT)
                    }
                }
                .map { (first, rest) -> QualifiedName(listOf(first, *rest.toTypedArray())) }
    }

    private fun Seq<Token>.decls(name: QualifiedName): SeqResult<Namespace> {
        return star { decl() }.map { Namespace(name, it) }
    }

    private fun Seq<Token>.decl(): SeqResult<Decl>? {
        return namespace() ?: type() ?: binding()
    }

    private fun Seq<Token>.declBlock(name: QualifiedName): SeqResult<Namespace>? {
        return match(TokenType.SYMBOL, "{")
                ?.decls(name)
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, "}") }
    }

    private fun Seq<Token>.namespace(): SeqResult<Namespace>? {
        return match(TokenType.KEYWORD, Keyword.NAMESPACE.kw)
                ?.require("qualified name") { qualName() }
                ?.let { it.seq.declBlock(it.result) }
    }

    private fun Seq<Token>.type(): SeqResult<TypeDecl>? {
        val name = (match(TokenType.KEYWORD, Keyword.TYPE.kw) ?: return null)
                .require("qualified name") { qualName() }

        val cases = name.seq
                .matchOrThrow(TokenType.SYMBOL, "=")
                .typeCases()

        val namespace = cases.seq.maybe { declBlock(name.result) }

        return SeqResult(
                TypeDecl(name.result, cases.result, namespace.result),
                namespace.seq)
    }

    private fun Seq<Token>.typeCases(): SeqResult<List<TypeCase>> {
        return typeCase()
                .then {
                    star {
                        match(TokenType.SYMBOL, "|")?.typeCase()
                    }
                }
                .map { (first, rest) -> listOf(first, *rest.toTypedArray()) }
    }

    private fun Seq<Token>.typeCase(): SeqResult<TypeCase> {
        return expectOrThrow(TokenType.IDENT)
                .then {
                    star { expect(TokenType.IDENT) }
                }
                .map { (name, params) -> TypeCase(name, params) }
    }

    private fun Seq<Token>.binding(): SeqResult<Binding>? {
        return match(TokenType.KEYWORD, Keyword.LET.kw)
                ?.require("pattern") { pattern() }
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, "=") }
                ?.then { require("block") { block() } }
                ?.map { (pattern, block) -> Binding(pattern, block) }
    }

    private fun Seq<Token>.pattern(): SeqResult<Pattern>? {
        return match(TokenType.IDENT, "_")?.let { SeqResult(WildcardPattern(), it) }
                ?: expect(TokenType.IDENT)?.map { NamePattern(it) }
                ?: listPattern()
                ?: typePattern()
                ?: constPattern()
    }

    private fun Seq<Token>.listPattern(): SeqResult<ListPattern>? {
        return match(TokenType.SYMBOL, "[")
                ?.star { pattern() }
                ?.map { ListPattern(it) }
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, "]") }
    }

    private fun Seq<Token>.typePattern(): SeqResult<FuncPattern>? {
        return match(TokenType.SYMBOL, "(")
                ?.require("qualified name") { qualName() }
                ?.then { star { pattern() } }
                ?.map { (name, params) -> FuncPattern(name, params) }
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, ")") }
    }

    private fun Seq<Token>.constPattern(): SeqResult<ConstPattern>? {
        return constLiteral()?.map { ConstPattern(it) }
    }

    private fun Seq<Token>.block(): SeqResult<Block>? {
        val bindings = star { binding() }
        return if (bindings.result.isNotEmpty()) {
            bindings.then { require("expr") { expr() } }
                    .map { (bindings, expr) -> Block(bindings, expr) }
        } else {
            bindings.seq.expr()?.map { Block(emptyList(), it) }
        }
    }

    private fun Seq<Token>.expr(): SeqResult<Expr>? {
        return callExpr()
                ?: lambdaExpr()
                ?: listExpr()
                ?: constLiteral()
                ?: qualName()
    }

    private fun Seq<Token>.callExpr(): SeqResult<CallExpr>? {
        return match(TokenType.SYMBOL, "(")
                ?.require("expr") { expr() }
                ?.then { star { expr() } }
                ?.map { (callable, args) -> CallExpr(callable, args) }
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, ")") }
    }

    private fun Seq<Token>.lambdaExpr(): SeqResult<LambdaExpr>? {
        return match(TokenType.SYMBOL, "{")
                ?.star { pattern() }
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, "->") }
                ?.then { require("block") { block() } }
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, "}") }
                ?.map { (patterns, block) -> LambdaExpr(patterns, block) }
    }

    private fun Seq<Token>.listExpr(): SeqResult<ListExpr>? {
        return match(TokenType.SYMBOL, "[")
                ?.star { block() }
                ?.thenConsume { matchOrThrow(TokenType.SYMBOL, "]") }
                ?.map { ListExpr(it) }
    }

    private fun Seq<Token>.constLiteral(): SeqResult<ConstLiteral>? {
        return expect(TokenType.NUMBER)?.map { ConstLiteral(ConstType.NUMBER, it) }
                ?: expect(TokenType.STRING)?.map { ConstLiteral(ConstType.STRING, it) }
                ?: match(TokenType.SYMBOL, "()")?.let { SeqResult(ConstLiteral(ConstType.UNIT, "()"), it) }
                ?: match(TokenType.SYMBOL, "[]")?.let { SeqResult(ConstLiteral(ConstType.EMPTY_LIST, "[]"), it) }
    }
}
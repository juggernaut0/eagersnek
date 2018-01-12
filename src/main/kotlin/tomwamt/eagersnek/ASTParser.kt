package tomwamt.eagersnek

import tomwamt.eagersnek.parse.AST
import tomwamt.eagersnek.parse.ImportStmt
import tomwamt.eagersnek.parse.NamespaceDecl
import tomwamt.eagersnek.parse.QualifiedName

object ASTParser : Parser<AST>() {
    override fun parse(tokens: Sequence<Token>): AST {
        val imports = imports(tokens)
        val rootNamespace = decls(imports.seq)

        return AST(imports.result, rootNamespace.result)
    }

    private fun imports(tokens: Sequence<Token>): SeqResult<List<ImportStmt>> {
        return parseStar(tokens) {
            match(TokenType.KEYWORD, Keywords.IMPORT.kw, it)
                    ?.let { qualName(it) }
                    ?.map { ImportStmt(it) }
        }
    }

    private fun qualName(tokens: Sequence<Token>): SeqResult<QualifiedName> {
        return expectOrThrow(TokenType.IDENT, tokens)
                .map { mutableListOf(it) }
                .flatMap({ l1, l2: List<String> -> l1 + l2 }) {
                    parseStar(it) {
                        match(TokenType.SYMBOL, ".", it)?.let { expectOrThrow(TokenType.IDENT, it) }
                    }
                }
                .map { QualifiedName(it.subList(0, it.lastIndex), it.last()) }
    }

    private fun decls(tokens: Sequence<Token>): SeqResult<NamespaceDecl> {
        TODO()
    }
}
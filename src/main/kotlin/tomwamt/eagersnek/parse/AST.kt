package tomwamt.eagersnek.parse

class AST(val imports: List<ImportStmt>, val rootNamespace: NamespaceDecl, val expr: CallExpr?)
class ImportStmt(val filename: String, val names: List<QualifiedName>) {
    override fun toString() = "import ... from '$filename'"
}
interface Decl
class NamespaceDecl(val name: QualifiedName, val public: Boolean, val decls: List<Decl>) : Decl
class TypeDecl(val name: QualifiedName, val public: Boolean, val cases: List<TypeCase>, val namespace: NamespaceDecl?) : Decl {
    override fun toString(): String = "type $name ..."
}
class TypeCase(val name: String, val params: List<String>) {
    override fun toString(): String = (listOf(name) + params).joinToString(" ")
}
class Binding(val pattern: Pattern, val block: Block, val public: Boolean) : Decl {
    override fun toString() = "let $pattern = ..."
}
interface Pattern
class ConstPattern(val const: ConstLiteral) : Pattern {
    override fun toString(): String = const.toString()
}
class WildcardPattern : Pattern {
    override fun toString(): String = "_"
}
class NamePattern(val value: String) : Pattern {
    override fun toString(): String = value
}
class ListPattern(val inners: List<Pattern>) : Pattern
class TypePattern(val name: QualifiedName, val params: List<Pattern>) : Pattern
class Block(val bindings: List<Binding>, val expr: Expr)
interface Expr { val line: Int }
class CallExpr(override val line: Int, val callable: Expr, val args: List<Expr>) : Expr
class LambdaExpr(override val line: Int, val params: List<Pattern>, val block: Block) : Expr
class ListExpr(override val line: Int, val elements: List<Block>) : Expr
class ConstLiteral(override val line: Int, val type: ConstType, val value: String) : Expr {
    override fun toString(): String = value
}
enum class ConstType { NUMBER, STRING, EMPTY_LIST, UNIT }
class QualifiedName(override val line: Int, val parts: List<String>) : Expr {
    override fun toString(): String = parts.joinToString(".")
}
class DotExpr(override val line: Int) : Expr

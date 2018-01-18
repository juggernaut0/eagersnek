package tomwamt.eagersnek.parse

class AST(val imports: List<ImportStmt>, val rootNamespace: Namespace, val expr: CallExpr?)
class ImportStmt(val name: QualifiedName) {
    override fun toString() = "import $name"
}
interface Decl
class Namespace(val name: QualifiedName, val decls: List<Decl>) : Decl
class TypeDecl(val name: QualifiedName, val cases: List<TypeCase>, val namespace: Namespace?) : Decl {
    override fun toString(): String = "type $name ..."
}
class TypeCase(val name: String, val params: List<String>) {
    override fun toString(): String = (listOf(name) + params).joinToString(" ")
}
class Binding(val pattern: Pattern, val block: Block) : Decl {
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
class FuncPattern(val name: QualifiedName, val params: List<Pattern>) : Pattern
class Block(val bindings: List<Binding>, val expr: Expr)
interface Expr
class CallExpr(val callable: Expr, val args: List<Expr>) : Expr
class LambdaExpr(val params: List<Pattern>, val block: Block) : Expr
class ListExpr(val elements: List<Block>) : Expr
class ConstLiteral(val type: String, val value: String) : Expr {
    override fun toString(): String = value
}
class QualifiedName(val parts: List<String>) : Expr {
    override fun toString(): String = parts.joinToString(".")
}

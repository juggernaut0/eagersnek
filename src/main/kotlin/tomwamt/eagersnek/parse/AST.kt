package tomwamt.eagersnek.parse

class AST(val imports: List<ImportStmt>, val rootNamespace: NamespaceDecl)
class ImportStmt(val name: QualifiedName)
class NamespaceDecl(val name: QualifiedName, val namespaces: List<NamespaceDecl>, val types: List<TypeDecl>, val bindings: List<BindingDecl>)
class TypeDecl(val name: QualifiedName, val params: List<String>, val cases: List<TypeCase>, val namespace: NamespaceDecl?)
class TypeCase(val name: String, val params: List<String>)
class QualifiedName(val parts: List<String>)
class BindingDecl(val pattern: Pattern, val block: Block)
interface Pattern
class ConstPattern(val type: String, val value: String) : Pattern
class WildcardPattern : Pattern
class NamePattern(val value: String) : Pattern
class ListPattern(val inners: List<Pattern>) : Pattern
class FuncPattern(val name: QualifiedName, params: List<Pattern>) : Pattern
class Block

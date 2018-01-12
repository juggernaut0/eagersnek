package tomwamt.eagersnek.parse

class AST(val imports: List<ImportStmt>, val rootNamespace: NamespaceDecl)
class ImportStmt(val name: QualifiedName)
class NamespaceDecl(val name: QualifiedName, val namespaces: List<NamespaceDecl>, val types: List<TypeDecl>, val bindings: List<BindingDecl>)
class TypeDecl(val name: QualifiedName, val params: List<String>)
class QualifiedName(val parts: List<String>, val name: String)
class BindingDecl(val pattern: Pattern, val block: Block)
interface Pattern
class ConstPattern(val type: String, val value: String) : Pattern
class WildcardPattern : Pattern
class Block

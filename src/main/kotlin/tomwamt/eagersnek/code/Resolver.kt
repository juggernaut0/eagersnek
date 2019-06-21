package tomwamt.eagersnek.code

import tomwamt.eagersnek.parse.*


object Resolver {
    fun resolve(ast: AST): ResolvedBindings {
        val declarations = ast.rootNamespace.decls.flatMap { findDeclarations(it) }
        val rootScope = Scope(declarations, null)
        val usages = ResolvedBindings()
        usages.addDeclarations(declarations)
        for (decl in ast.rootNamespace.decls) {
            usages.resolveUsages(decl, rootScope)
        }
        if (ast.expr != null) {
            usages.resolveUsages(ast.expr, rootScope)
        }
        return usages
    }

    private fun findDeclarations(decl: Decl): List<Declaration> {
        return when (decl) {
            is Binding -> extractNames(decl.pattern)
            else -> emptyList()
        }
    }

    private fun extractNames(pattern: Pattern): List<Declaration> {
        return when (pattern) {
            is NamePattern -> listOf(Declaration(pattern, pattern.line))
            is ListPattern -> pattern.inners.flatMap { extractNames(it) }
            is TypePattern -> pattern.params.flatMap { extractNames(it) } // ignore typename because it is not a declaration
            else -> emptyList()
        }
    }

    private fun ResolvedBindings.resolveUsages(decl: Decl, scope: Scope) {
        when (decl) {
            is Binding -> resolveUsages(decl.block, scope)
            is NamespaceDecl -> resolveUsagesNs(decl, scope)
            is TypeDecl -> {
                if (decl.namespace != null) {
                    resolveUsagesNs(decl.namespace, scope)
                }
            }
        }
    }
    
    private fun ResolvedBindings.resolveUsagesNs(ns: NamespaceDecl, parent: Scope) {
        val declarations = ns.decls.flatMap { findDeclarations(it) }
        addDeclarations(declarations)
        val scope = Scope(declarations, parent)
        for (d in ns.decls) {
            resolveUsages(d, scope)
        }
    }

    private fun ResolvedBindings.resolveUsages(block: Block, parent: Scope) {
        val declarations = block.bindings.flatMap { extractNames(it.pattern) }
        addDeclarations(declarations)
        val scope = Scope(declarations, parent)
        for (binding in block.bindings) {
            resolveUsages(binding.block, scope)
        }
        resolveUsages(block.expr, scope)
    }

    private fun ResolvedBindings.resolveUsages(expr: Expr, scope: Scope) {
        when (expr) {
            is QualifiedName -> expr.takeIf { it.parts.size == 1 }?.let { scope[it.parts[0]] }?.let { addUsage(expr, it) }
            is CallExpr -> {
                resolveUsages(expr.callable, scope)
                for (a in expr.args) {
                    resolveUsages(a, scope)
                }
            }
            is ListExpr -> expr.elements.forEach { resolveUsages(it, scope) }
            is LambdaExpr -> {
                val params = expr.params.flatMap { extractNames(it) }
                addDeclarations(params)
                val paramScope = Scope(params, scope)
                resolveUsages(expr.block, paramScope)
            }
        }
    }

    private data class Scope(val declarations: List<Declaration>, val parent: Scope?) {
        operator fun get(name: String): Declaration? {
            return declarations.find { it.name.value == name } ?: parent?.get(name)
        }
    }
}


class ResolvedBindings {
    private val _declarations: MutableMap<NamePattern, Declaration> = mutableMapOf()
    private val _usages: MutableMap<QualifiedName, Declaration> = mutableMapOf()
    val declarations: Map<NamePattern, Declaration> get() = _declarations
    val usages: Map<QualifiedName, Declaration> get() = _usages

    fun addDeclarations(declarations: List<Declaration>) {
        for (declaration in declarations) {
            _declarations[declaration.name] = declaration
        }
    }

    fun addUsage(qname: QualifiedName, declaration: Declaration) {
        _usages[qname] = declaration
    }
}
class Declaration(val name: NamePattern, val line: Int)

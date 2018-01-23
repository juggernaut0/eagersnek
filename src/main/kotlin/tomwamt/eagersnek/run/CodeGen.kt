package tomwamt.eagersnek.run

import tomwamt.eagersnek.parse.*

object CodeGen {
    private inline fun withList(block: MutableList<OpCode>.() -> Unit): List<OpCode> {
        val list = mutableListOf<OpCode>()
        list.block()
        return list
    }

    fun compile(ast: AST) = withList {
        // TODO imports
        addAll(genNamespace(ast.rootNamespace))
        ast.expr?.let { addAll(genCall(it)) }
    }

    private fun genNamespace(namespace: NamespaceDecl, parent: List<String> = emptyList()) = withList {
        val name = parent + namespace.name.parts
        if (name.isNotEmpty()) {
            add(MkNamespace(name, namespace.name.parts[0]))
        }
        add(PushScope)
        addAll(namespace.decls.flatMap { genDecl(it, name) })
        add(PopScope)
    }

    private fun genDecl(decl: Decl, parent: List<String>): List<OpCode> {
        return when(decl) {
            is NamespaceDecl -> genNamespace(decl, parent)
            is TypeDecl -> genType(decl, parent)
            is Binding -> withList {
                addAll(gen(decl.block))
                addAll(savePattern(decl.pattern, parent))
            }
            else -> throw NotImplementedError("${decl.javaClass.name} is not supported")
        }
    }

    private fun genType(typeDecl: TypeDecl, parent: List<String>) = withList {
        val name = typeDecl.name.parts.last()
        val ns = parent + typeDecl.name.parts.run { subList(0, lastIndex) }
        add(MkType(name, ns, typeDecl.cases))
        typeDecl.namespace?.let { addAll(genNamespace(it, parent)) }
    }

    private fun gen(block: Block): List<OpCode> = withList {
        if (block.bindings.isNotEmpty()) {
            add(PushScope)
            for (b in block.bindings) {
                addAll(gen(b.block))
                addAll(savePattern(b.pattern, null))
            }
        }
        addAll(gen(block.expr))
        if (block.bindings.isNotEmpty()) {
            add(PopScope)
        }
    }

    // a value is on the stack -- save it or decompose it
    private fun savePattern(pattern: Pattern, namespace: List<String>?): List<OpCode> {
        return when (pattern) {
            is WildcardPattern -> listOf(Pop)
            is NamePattern -> saveName(pattern, namespace)
            is ListPattern -> saveList(pattern, namespace)
            is TypePattern -> saveType(pattern, namespace)
            else -> throw NotImplementedError("${pattern.javaClass.name} is not supported")
        }
    }

    private fun saveName(pattern: NamePattern, namespace: List<String>?) = withList {
        if (namespace != null) {
            add(Duplicate)
            add(SaveNamespace(pattern.value, namespace))
        }
        add(SaveLocal(pattern.value))
    }

    private fun saveList(pattern: ListPattern, namespace: List<String>?) = withList {
        for (p in pattern.inners) {
            add(Decompose(listOf("::"), 2))
            addAll(savePattern(p, namespace))
        }
        add(Decompose(listOf("Empty")))
    }

    private fun saveType(pattern: TypePattern, namespace: List<String>?) = withList {
        add(Decompose(pattern.name.parts, pattern.params.size))
        for (p in pattern.params) {
            addAll(savePattern(p, namespace))
        }
    }

    private fun gen(expr: Expr): List<OpCode> {
        return when (expr) {
            is ConstLiteral -> listOf(genConst(expr))
            is QualifiedName -> listOf(LoadName(expr.parts))
            is ListExpr -> genList(expr)
            is CallExpr -> genCall(expr)
            is LambdaExpr -> genLambda(expr)
            else -> throw NotImplementedError("${expr.javaClass.name} is not supported")
        }
    }

    private fun genConst(constLiteral: ConstLiteral): OpCode {
        return when (constLiteral.type) {
            ConstType.NUMBER -> LoadNumber(constLiteral.value.toDouble())
            ConstType.STRING -> LoadString(constLiteral.value)
            ConstType.UNIT -> LoadName(listOf("Unit"))
            ConstType.EMPTY_LIST -> genEmptyList()
        }
    }

    private fun genEmptyList() = LoadName(listOf("Empty"))

    private fun genList(listExpr: ListExpr) = withList {
        add(genEmptyList())
        for (block in listExpr.elements.reversed()) {
            addAll(gen(block))
            add(LoadName(listOf("::")))
            add(Call(2))
        }
    }

    private fun genCall(callExpr: CallExpr) = withList {
        addAll(callExpr.args.reversed().flatMap { gen(it) })
        addAll(gen(callExpr.callable))
        add(Call(callExpr.args.size))
    }

    private fun genLambda(lambdaExpr: LambdaExpr) = withList {
        val body = gen(lambdaExpr.block)
        add(LoadCallable(body))
    }
}
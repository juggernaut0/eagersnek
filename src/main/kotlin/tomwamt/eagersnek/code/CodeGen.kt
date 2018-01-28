package tomwamt.eagersnek.code

import tomwamt.eagersnek.parse.*

object CodeGen {
    private val emptyListName = listOf("Empty")
    private val listConsName = listOf("::")
    private val unitName = listOf("Unit")

    fun compile(ast: AST): CompiledCode {
        val code = CompiledCode()

        // TODO imports
        code.genNamespace(ast.rootNamespace)
        ast.expr?.let { code.genCall(it, false) }

        return code
    }

    private fun CompiledCode.genNamespace(namespace: NamespaceDecl, parent: List<String> = emptyList()) {
        val name = parent + namespace.name.parts
        if (name.isNotEmpty()) {
            add(MkNamespace(name, namespace.name.parts[0]))
            add(PushScope)
        }

        namespace.decls.forEach { genDecl(it, name) }

        if (name.isNotEmpty()) {
            add(PopScope)
        }
    }

    private fun CompiledCode.genDecl(decl: Decl, parent: List<String>) {
        return when(decl) {
            is NamespaceDecl -> genNamespace(decl, parent)
            is TypeDecl -> genType(decl, parent)
            is Binding -> {
                gen(decl.block, false)
                savePattern(decl.pattern, parent)
            }
            else -> throw NotImplementedError("${decl.javaClass.name} is not supported")
        }
    }

    private fun CompiledCode.genType(typeDecl: TypeDecl, parent: List<String>) {
        val name = typeDecl.name.parts.last()
        val ns = parent + typeDecl.name.parts.run { subList(0, lastIndex) }
        add(MkType(name, ns, typeDecl.cases))
        typeDecl.namespace?.let { genNamespace(it, parent) }
    }

    private fun CompiledCode.gen(block: Block, allowTail: Boolean) {
        if (block.bindings.isNotEmpty()) {
            add(PushScope)
            for (b in block.bindings) {
                gen(b.block, false)
                savePattern(b.pattern, null)
            }
        }
        gen(block.expr, allowTail)
        if (block.bindings.isNotEmpty()) {
            add(PopScope)
        }
    }

    // a value is on the stack -- save it or decompose it
    private fun CompiledCode.savePattern(pattern: Pattern, namespace: List<String>?) {
        return when (pattern) {
            is WildcardPattern -> add(Pop)
            is ConstPattern -> add(Match(convertPattern(pattern)))
            is NamePattern -> saveName(pattern, namespace)
            is ListPattern -> saveList(pattern, namespace)
            is TypePattern -> saveType(pattern, namespace)
            else -> throw NotImplementedError("${pattern.javaClass.name} is not supported")
        }
    }

    private fun convertPattern(pattern: Pattern): MatchPattern {
        return when (pattern) {
            is ConstPattern -> when (pattern.const.type) {
                ConstType.NUMBER -> NumberMatch(pattern.const.value.toDouble())
                ConstType.STRING -> StringMatch(pattern.const.value.trimQuotes())
                ConstType.EMPTY_LIST -> EmptyListMatch
                ConstType.UNIT -> UnitMatch
            }
            is NamePattern, is WildcardPattern -> AlwaysMatch
            is ListPattern -> {
                var res: MatchPattern = EmptyListMatch
                pattern.inners
                        .asReversed()
                        .forEach { res = TypeMatch(listConsName, listOf(convertPattern(it), res)) }
                res
            }
            is TypePattern -> TypeMatch(pattern.name.parts, pattern.params.map { convertPattern(it) })
            else -> throw NotImplementedError("${pattern.javaClass.name} is not supported")
        }
    }

    private fun CompiledCode.saveName(pattern: NamePattern, namespace: List<String>?) {
        if (namespace != null) {
            add(Duplicate)
            add(SaveNamespace(pattern.value, namespace))
        }
        add(SaveLocal(pattern.value))
    }

    private fun CompiledCode.saveList(pattern: ListPattern, namespace: List<String>?) {
        for (p in pattern.inners) {
            add(Decompose(listConsName, 2))
            savePattern(p, namespace)
        }
        add(Decompose(emptyListName))
    }

    private fun CompiledCode.saveType(pattern: TypePattern, namespace: List<String>?) {
        add(Decompose(pattern.name.parts, pattern.params.size))
        for (p in pattern.params) {
            savePattern(p, namespace)
        }
    }

    private fun CompiledCode.gen(expr: Expr, allowTail: Boolean) {
        when (expr) {
            is ConstLiteral -> genConst(expr)
            is QualifiedName -> add(LoadName(expr.parts))
            is ListExpr -> genList(expr)
            is CallExpr -> genCall(expr, allowTail)
            is LambdaExpr -> genLambda(expr)
            else -> throw NotImplementedError("${expr.javaClass.name} is not supported")
        }
    }

    private fun CompiledCode.genConst(constLiteral: ConstLiteral) {
        when (constLiteral.type) {
            ConstType.NUMBER -> add(LoadNumber(constLiteral.value.toDouble()))
            ConstType.STRING -> add(LoadString(constLiteral.value.trimQuotes()))
            ConstType.UNIT -> add(LoadName(unitName))
            ConstType.EMPTY_LIST -> add(LoadName(emptyListName))
        }
    }

    private fun String.trimQuotes(): String {
        return substring(1, lastIndex)
    }

    private fun CompiledCode.genList(listExpr: ListExpr) {
        add(LoadName(emptyListName))
        for (block in listExpr.elements.reversed()) {
            gen(block, false)
            add(LoadName(listOf("::")))
            add(Call(2))
        }
    }

    private fun CompiledCode.genCall(callExpr: CallExpr, allowTail: Boolean) {
        if (with(callExpr.callable) { this is QualifiedName && parts.size == 1 && parts[0] == "match" }) {
            genMatch(callExpr, allowTail)
        } else {
            callExpr.args.reversed().forEach { gen(it, false) }
            if (callExpr.callable == DotExpr) {
                if (!allowTail) throw CodeGenException("Tail call not allowed here")
                add(TailCall(callExpr.args.size))
            } else {
                gen(callExpr.callable, false)
                add(Call(callExpr.args.size))
            }
        }
    }

    private fun CompiledCode.genLambda(lambdaExpr: LambdaExpr) {
        val code = CompiledCode()
        lambdaExpr.params.forEach { code.savePattern(it, null) }
        code.gen(lambdaExpr.block, true)

        add(LoadFunction(code, lambdaExpr.params.size))
    }

    private fun CompiledCode.genMatch(callExpr: CallExpr, allowTail: Boolean) {
        if (callExpr.args.size != 2) throw CodeGenException("Incorrect number of args in match: ${callExpr.args.size}")

        gen(callExpr.args[0], false)

        val cases = (callExpr.args[1] as? ListExpr ?: throw CodeGenException("match requires a list as second argument"))
                .elements
                .map { it.expr as? LambdaExpr ?: throw CodeGenException("every case in a match must be a lambda") }

        cases.find { it.params.size != 1 }?.let { throw CodeGenException("every case in a match must have exactly one parameter") }

        val labels = cases.map { Label(it.params[0].toString()) }

        cases.zip(labels).forEach { (case, label) ->
            add(Duplicate)
            add(JumpIfMatch(convertPattern(case.params[0]), label))
        }
        add(Fail("no match"))

        val endLabel = Label("end")
        cases.zip(labels).forEach { (case, label) -> genMatchCase(case, label, endLabel, allowTail) }
        addLabel(endLabel)
        add(NoOp)
    }

    private fun CompiledCode.genMatchCase(caseExpr: LambdaExpr, caseLabel: Label, endLabel: Label, allowTail: Boolean) {
        addLabel(caseLabel)
        savePattern(caseExpr.params[0], null)
        gen(caseExpr.block, allowTail)
        add(Jump(endLabel))
    }
}

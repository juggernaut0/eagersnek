package tomwamt.eagersnek.run

import tomwamt.eagersnek.parse.*

class Module(val namespace: Namespace) {
    companion object {
        fun executeAst(ast: AST): Module {
            // TODO imports

            val rootScope = Scope(null)

            val ns = Namespace()
            for (decl in ast.rootNamespace.decls) {
                when (decl) {
                    is Binding -> evalAndBind(decl, rootScope, ns)
                }
            }

            TODO()
        }

        private fun evalAndBind(binding: Binding, scope: Scope, namespace: Namespace? = null) {
            val obj = bindAndEval(binding.block, scope)

            when (binding.pattern) {
                is WildcardPattern -> Unit // ignore
                else -> TODO()
            }
        }

        private fun bindAndEval(block: Block, parent: Scope): RuntimeObject {
            val scope = Scope(parent)
            block.bindings.forEach { evalAndBind(it, scope) }
            return eval(block.expr, scope)
        }

        private fun eval(expr: Expr, scope: Scope): RuntimeObject {
            return when (expr) {
                is ConstLiteral -> {
                    when(expr.type) {
                        ConstType.NUMBER -> NumberObject(expr.value.toDouble())
                        ConstType.STRING -> StringObject(expr.value)
                        ConstType.UNIT -> UnitObject
                        else -> TODO()
                    }
                }
                is CallExpr -> {
                    val fn = eval(expr.callable, scope)
                    val args = expr.args.map { eval(it, scope) }
                    if (fn is CallableObject) {
                        fn.call(args)
                    } else {
                        // TODO let code handle this?
                        throw RuntimeException("Attempt to call uncallable")
                    }
                }
                else -> TODO()
            }
        }
    }
}
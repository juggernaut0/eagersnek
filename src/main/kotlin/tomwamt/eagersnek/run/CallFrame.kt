package tomwamt.eagersnek.run

class CallFrame(baseScope: Scope) {
    var scope: Scope = baseScope
        private set

    fun pushScope() {
        scope = Scope(scope)
    }

    fun popScope() {
        scope = scope.parent ?: throw InterpreterException("no pop")
    }
}
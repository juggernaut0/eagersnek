package tomwamt.eagersnek.run

class CallFrame(val fn: CompiledFunction) {
    var scope: Scope = Scope(fn.baseScope)
        private set

    var srcLine = 0

    fun pushScope() {
        scope = Scope(scope)
    }

    fun popScope() {
        scope = scope.parent ?: throw InterpreterException("no pop")
    }

    fun tailCall() {
        scope = Scope(fn.baseScope)
    }
}
package tomwamt.eagersnek.run

class CallFrame(val fn: CompiledFunction) {
    var scope: Scope = Scope(fn.baseScope)
        private set

    var srcLine = 0

    fun tailCall() {
        scope = Scope(fn.baseScope)
    }
}
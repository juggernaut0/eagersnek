package tomwamt.eagersnek.run

import tomwamt.eagersnek.code.OpCode

abstract class RuntimeObject(val type: Type)

class NumberObject(val value: Double) : RuntimeObject(NumberType)

class StringObject(val value: String) : RuntimeObject(StringType)

abstract class FunctionObject(val numArgs: Int) : RuntimeObject(FunctionType) {
    abstract fun call(int: Interpreter)
}
class CompiledFunction(private val code: List<OpCode>, val baseScope: Scope, numArgs: Int) : FunctionObject(numArgs) {
    override fun call(int: Interpreter) {
        int.callStack.push(CallFrame(Scope(baseScope)))
        int.run(code)
        int.callStack.pop()
    }
}
class PartialFunction(val fn: FunctionObject, val args: List<RuntimeObject>) : FunctionObject(fn.numArgs - args.size) {
    override fun call(int: Interpreter) {
        args.forEach { int.execStack.push(it) }
        fn.call(int)
    }
}

class CaseObject(type: Type, val data: List<RuntimeObject>) : RuntimeObject(type)

package tomwamt.eagersnek.run

import tomwamt.eagersnek.code.OpCode

abstract class RuntimeObject(val type: Type)

class NumberObject(val value: Double) : RuntimeObject(NumberType)

class StringObject(val value: String) : RuntimeObject(StringType)

abstract class FunctionObject(val numArgs: Int) : RuntimeObject(FunctionType) {
    abstract fun call(int: Interpreter)
}
class CompiledFunction(private val code: List<OpCode>, val baseScope: Scope, val baseNs: Namespace, numArgs: Int) : FunctionObject(numArgs) {
    override fun call(int: Interpreter) {
        int.callStack.push(CallFrame(this))
        int.run(code)
        int.callStack.pop()
    }
}
class PartialFunction(val fn: FunctionObject, val args: List<RuntimeObject>) : FunctionObject(fn.numArgs - args.size) {
    override fun call(int: Interpreter) {
        args.asReversed().forEach { int.execStack.push(it) }
        fn.call(int)
    }
}

class ConstructorFunction(val resultType: TypeCase) : FunctionObject(resultType.fieldCount) {
    override fun call(int: Interpreter) {
        val data = List(numArgs) { int.execStack.pop() }
        val obj = CaseObject(resultType, data)
        int.execStack.push(obj)
    }
}

class CaseObject(type: TypeCase, val data: List<RuntimeObject>) : RuntimeObject(type)
class SingletonObject(type: TypeCase) : RuntimeObject(type)

package tomwamt.eagersnek.run

abstract class RuntimeObject(val type: Type)

class NumberObject(val value: Double) : RuntimeObject(Type.NUMBER)
class StringObject(val value: String) : RuntimeObject(Type.STRING)
class CallableObject(val code: List<CodeExpr>) : RuntimeObject(Type.CALLABLE)
class CustomObject(type: Type, val data: Array<RuntimeObject>) : RuntimeObject(type)
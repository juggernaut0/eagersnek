package tomwamt.eagersnek.run

abstract class RuntimeObject(val type: Type)

class NumberObject(val value: Double) : RuntimeObject(NumberType)
class StringObject(val value: String) : RuntimeObject(StringType)
class FunctionObject(val code: List<OpCode>, val baseScope: Scope) : RuntimeObject(FunctionType)
class CaseObject(type: Type, val data: Array<RuntimeObject>) : RuntimeObject(type)
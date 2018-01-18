package tomwamt.eagersnek.run

abstract class RuntimeObject(val type: Type)

class NumberObject(val value: Double) : RuntimeObject(Type.NUMBER)
class StringObject(val value: String) : RuntimeObject(Type.STRING)
object UnitObject : RuntimeObject(Type.UNIT)
class CallableObject : RuntimeObject(Type.CALLABLE) {
    fun call(args: List<RuntimeObject>): RuntimeObject {
        TODO()
    }
}
class CustomObject(type: Type, val data: Array<RuntimeObject>) : RuntimeObject(type)
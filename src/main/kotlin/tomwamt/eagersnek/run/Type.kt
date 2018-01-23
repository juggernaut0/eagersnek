package tomwamt.eagersnek.run

abstract class Type(val name: String)
object NumberType : Type("Number")
object StringType : Type("String")
object FunctionType : Type("Function")
class ParentType(name: String, val cases: List<TypeCase>) : Type(name)
class TypeCase(name: String, val fieldCount: Int) : Type(name)

package tomwamt.eagersnek.run

class Namespace(val public: Boolean) {
    val bindings: MutableMap<String, RuntimeObject> = mutableMapOf()
    val types: MutableMap<String, Type> = mutableMapOf()
    val subnames: MutableMap<String, Namespace> = mutableMapOf()

    fun addType(type: ParentType) {
        types[type.name] = type
        for (case in type.cases) {
            types[case.name] = case
            bindings[case.name] = if (case.fieldCount > 0) ConstructorFunction(case) else SingletonObject(case)
        }
    }
}

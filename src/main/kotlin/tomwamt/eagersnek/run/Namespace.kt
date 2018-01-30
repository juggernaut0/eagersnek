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

    fun findNamespace(path: List<String>): Namespace {
        var ns = this
        for (part in path) {
            ns = ns.subnames[part] ?: throw InterpreterException("No namespace $part")
        }
        return ns
    }

    fun findType(qname: List<String>): Type {
        val ns = findNamespace(qname.subList(0, qname.lastIndex))
        return ns.types[qname.last()] ?: throw InterpreterException("No type ${qname.last()}")
    }

    fun findName(qname: List<String>): RuntimeObject {
        val ns = findNamespace(qname.subList(0, qname.lastIndex))
        return ns.bindings[qname.last()] ?: throw InterpreterException("No name ${qname.last()}")
    }
}

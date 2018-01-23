package tomwamt.eagersnek.run

class Scope(val parent: Scope?) {
    private val bindings: MutableMap<String, RuntimeObject> = mutableMapOf()

    fun find(name: String): RuntimeObject? = bindings[name] ?: parent?.find(name)
    fun save(name: String, obj: RuntimeObject) {
        bindings[name] = obj
    }
}
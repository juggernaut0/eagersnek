package tomwamt.eagersnek.run

class Scope(private val parent: Scope?) {
    private val bindings: MutableMap<String, RuntimeObject> = mutableMapOf()

    fun find(name: String): RuntimeObject? = bindings[name] ?: parent?.find(name)
}
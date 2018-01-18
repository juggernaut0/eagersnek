package tomwamt.eagersnek.run

class Namespace {
    val bindings: MutableMap<String, RuntimeObject> = mutableMapOf()
    val types: MutableMap<String, Type> = mutableMapOf()
    val subnames: MutableMap<String, Namespace> = mutableMapOf()
}

package tomwamt.eagersnek.run

import tomwamt.eagersnek.code.Declaration

class Scope(val parent: Scope?) {
    private val bindings: MutableMap<Declaration, RuntimeObject> = mutableMapOf()

    fun find(name: Declaration): RuntimeObject? = bindings[name] ?: parent?.find(name)
    fun save(name: Declaration, obj: RuntimeObject) {
        bindings[name] = obj
    }
}
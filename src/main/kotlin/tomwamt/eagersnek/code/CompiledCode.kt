package tomwamt.eagersnek.code

class CompiledCode : AbstractList<OpCode>() {
    private val code: MutableList<OpCode> = ArrayList()

    override val size: Int
        get() = code.size

    override fun get(index: Int) = code[index]

    fun add(opCode: OpCode) {
        code.add(opCode)
    }

    fun addLabel(label: Label) {
        label.attach(code.size)
    }
}

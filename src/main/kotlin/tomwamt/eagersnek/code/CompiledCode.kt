package tomwamt.eagersnek.code

class CompiledCode : AbstractList<OpCode>() {
    private val code: MutableList<OpCode> = ArrayList()
    private val labels: MutableList<Label> = ArrayList()

    override val size: Int
        get() = code.size

    override fun get(index: Int) = code[index]

    fun add(opCode: OpCode) {
        code.add(opCode)
    }

    fun addAll(opCodes: Iterable<OpCode>) {
        code.addAll(opCodes)
    }

    fun addLabel(label: Label) {
        label.attach(code.size)
        labels.add(label)
    }
}

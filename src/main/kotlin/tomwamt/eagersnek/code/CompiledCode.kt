package tomwamt.eagersnek.code

class CompiledCode : AbstractList<SrcLineOpCode>() {
    var currentLine: Int = 0
    private val code: MutableList<SrcLineOpCode> = ArrayList()

    override val size: Int
        get() = code.size

    override fun get(index: Int) = code[index]

    fun add(opCode: OpCode) {
        code.add(SrcLineOpCode(currentLine, opCode))
    }

    fun addLabel(label: Label) {
        label.attach(code.size)
    }
}

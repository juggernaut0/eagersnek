package tomwamt.eagersnek.run

object Interpreter {
    private val callStack: Stack<CallFrame> = Stack()
    private val execStack: Stack<RuntimeObject> = Stack()
    private val rootNamespace = Namespace()

    fun run(code: List<OpCode>) {
        for (opcode in code) {
            when (opcode) {
                Pop -> execStack.pop()
                Duplicate -> execStack.push(execStack.peek())
                is Decompose -> {
                    val type = findType(opcode.type)
                    if (type.fieldCount != opcode.numParts) {
                        throw InterpreterError("Cannot decompose ${type.name} into ${opcode.numParts} parts")
                    }

                    val obj = execStack.pop()
                    if (obj.type != type) {
                        throw InterpreterError("Expected a ${type.name}, got ${obj.type.name}")
                    }

                    for (field in obj.fields.reversed())
                }
            }
        }
    }

    private fun findType(qname: List<String>): Type {
        var ns = rootNamespace
        for (part in qname.subList(0, qname.lastIndex)) {
            ns = ns.subnames[part] ?: throw InterpreterError("No namespace $part")
        }
        return ns.types[qname.last()] ?: throw InterpreterError("No type ${qname.last()}")
    }

    class Stack<T> {
        private val store: MutableList<T> = mutableListOf()

        fun push(obj: T) = store.add(obj)
        fun pop(): T = store.removeAt(store.lastIndex)
        fun peek(): T = store[store.lastIndex]
    }
}
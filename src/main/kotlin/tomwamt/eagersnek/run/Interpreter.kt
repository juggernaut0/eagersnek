package tomwamt.eagersnek.run

object Interpreter {
    private val callStack: Stack<CallFrame> = Stack()
    private val execStack: Stack<RuntimeObject> = Stack()
    private val rootNamespace = Namespace()

    init {
        callStack.push(CallFrame(Scope(null)))
    }

    fun run(code: List<OpCode>): RuntimeObject {
        for (opcode in code) {
            when (opcode) {
                Pop -> execStack.pop()
                Duplicate -> execStack.push(execStack.peek())
                is Decompose -> decompose(opcode)
                is LoadName -> execStack.push(findName(opcode.name))
                is LoadNumber -> execStack.push(NumberObject(opcode.value))
                is LoadString -> execStack.push(StringObject(opcode.value))
                is LoadFunction -> execStack.push(FunctionObject(opcode.code, callStack.peek().scope))
                is SaveLocal -> callStack.peek().scope.save(opcode.name, execStack.pop())
                is SaveNamespace -> findNamespace(opcode.namespace).bindings[opcode.name] = execStack.pop()
                is Call -> {
                    val fn = execStack.pop() as? FunctionObject ?: throw InterpreterError("not a func")
                    callStack.push(CallFrame(Scope(fn.baseScope)))
                    // TODO currying?
                    val res = run(fn.code)
                    callStack.pop()
                    execStack.push(res)
                }
                PushScope -> callStack.peek().pushScope()
                PopScope -> callStack.peek().popScope()
                is MkNamespace -> makeNamespace(opcode.name)
                is MkType -> makeType(opcode)
                else -> throw InterpreterError("unsupported opcode ${opcode.javaClass.name}")
            }
        }
        return execStack.pop()
    }

    private fun decompose(opcode: Decompose) {
        val type = findType(opcode.type)
        when (type) {
            NumberType, StringType, FunctionType -> {
                if (opcode.numParts != 1) {
                    throw InterpreterError("Cannot decompose ${type.name} into ${opcode.numParts} parts")
                }

                val obj = execStack.peek()
                // just check
                if (obj.type != type) {
                    throw InterpreterError("Expected a ${type.name}, got ${obj.type.name}")
                }
            }
            is ParentType -> {
                if (opcode.numParts != 1) {
                    throw InterpreterError("Cannot decompose ${type.name} into ${opcode.numParts} parts")
                }

                val obj = execStack.peek()
                // just check
                if (!type.cases.contains(obj.type)) {
                    throw InterpreterError("Expected a ${type.name}, got ${obj.type.name}")
                }
            }
            is TypeCase -> {
                if (opcode.numParts != type.fieldCount) {
                    throw InterpreterError("Cannot decompose ${type.name} into ${opcode.numParts} parts")
                }

                val obj = execStack.pop()
                if (obj !is CaseObject || obj.type != type) {
                    throw InterpreterError("Expected a ${type.name}, got ${obj.type.name}")
                }

                obj.data.reversed().forEach { execStack.push(it) }
            }
        }
    }

    private fun makeNamespace(path: List<String>) {
        var ns = rootNamespace
        for (part in path) {
            if (part !in ns.subnames) {
                ns.subnames[part] = Namespace()
            }
            ns = ns.subnames[part]!!
        }
    }

    private fun makeType(opcode: MkType) {

    }

    private fun findNamespace(path: List<String>): Namespace {
        var ns = rootNamespace
        for (part in path) {
            ns = ns.subnames[part] ?: throw InterpreterError("No namespace $part")
        }
        return ns
    }

    private fun findType(qname: List<String>): Type {
        val ns = findNamespace(qname.subList(0, qname.lastIndex))
        return ns.types[qname.last()] ?: throw InterpreterError("No type ${qname.last()}")
    }

    private fun findName(qname: List<String>): RuntimeObject {
        return if (qname.size == 1) {
            callStack.peek().scope.find(qname[0]) ?: throw InterpreterError("No name ${qname[0]}")
        } else {
            val ns = findNamespace(qname.subList(0, qname.lastIndex))
            ns.bindings[qname.last()] ?: throw InterpreterError("No name ${qname.last()}")
        }
    }

    class Stack<T> {
        private val store: MutableList<T> = mutableListOf()

        fun push(obj: T) = store.add(obj)
        fun pop(): T = store.removeAt(store.lastIndex)
        fun peek(): T = store[store.lastIndex]
    }
}
package tomwamt.eagersnek.run

import tomwamt.eagersnek.code.*

class Interpreter(private val modName: String, loadPredef: Boolean = true) {
    val callStack: Stack<CallFrame> = Stack()
    val execStack: Stack<RuntimeObject> = Stack()
    val rootNamespace = Builtin.makeRootNamespace()

    init {
        if (loadPredef) {
            Module.predef.importInto(rootNamespace)
        }
    }

    fun exec(code: CompiledCode) {
        val main = CompiledFunction(code, Scope(null), rootNamespace, 0, modName, 0)
        try {
            main.call(this)
        } catch (e: InterpreterException) {
            System.err.println(e.message)
            callStack.asList()
                    .asReversed()
                    .mapIndexed { i, frame -> "  ${if (i == 0) "  in" else "from"} ${frame.fn} at line ${frame.srcLine}" }
                    .forEach { System.err.println(it) }
        }
    }

    internal fun run(code: CompiledCode) {
        var ip = 0
        while (ip < code.size) {
            val (srcLine, opcode) = code[ip]
            callStack.peek().srcLine = srcLine
            ip += 1
            when (opcode) {
                NoOp -> Unit
                is Fail -> throw InterpreterException("Fail: ${opcode.msg}")
                Pop -> execStack.pop()
                Duplicate -> execStack.push(execStack.peek())
                is Decompose -> decompose(opcode)
                is Jump -> ip = opcode.label.target
                is Match -> if (!match(opcode.pattern)) throw InterpreterException("Failed match")
                is JumpIfMatch -> if (match(opcode.pattern)) ip = opcode.label.target
                is LoadLocal -> execStack.push(findDeclaration(opcode.declaration))
                is LoadName -> execStack.push(findName(opcode.name))
                is LoadNumber -> execStack.push(NumberObject(opcode.value))
                is LoadString -> execStack.push(StringObject(opcode.value))
                is LoadFunction -> execStack.push(CompiledFunction(opcode.code, callStack.peek().scope, rootNamespace, opcode.paramCount, modName, srcLine))
                is SaveLocal -> callStack.peek().scope.save(opcode.declaration, execStack.pop())
                is SaveNamespace -> rootNamespace.findNamespace(opcode.namespace).bindings[opcode.name] = execStack.pop()
                is Call -> call(opcode)
                is TailCall -> {
                    tailCall(opcode)
                    ip = 0
                }
                is MkNamespace -> makeNamespace(opcode.name, opcode.public)
                is MkType -> makeType(opcode)
                is ImportAll -> Module.fromFile(opcode.filename).importInto(rootNamespace)
                is ImportNames -> Module.fromFile(opcode.filename).let {
                    opcode.names.forEach { name -> it.importNameInto(name, rootNamespace) }
                }
                else -> throw InterpreterException("unsupported opcode ${opcode.javaClass.name}")
            }
        }
    }

    private fun decompose(opcode: Decompose) {
        val type = findType(opcode.type)
        when (type) {
            NumberType, StringType, FunctionType -> {
                if (opcode.numParts != 1) {
                    throw InterpreterException("Cannot decompose ${type.name} into ${opcode.numParts} parts")
                }

                val obj = execStack.peek()
                // just check
                if (obj.type != type) {
                    throw InterpreterException("Expected a ${type.name}, got ${obj.type.name}")
                }
            }
            is ParentType -> {
                if (opcode.numParts != 1) {
                    throw InterpreterException("Cannot decompose ${type.name} into ${opcode.numParts} parts")
                }

                val obj = execStack.peek()
                // just check
                if (obj.type !in type.cases) {
                    throw InterpreterException("Expected a ${type.name}, got ${obj.type.name}")
                }
            }
            is TypeCase -> {
                if (opcode.numParts != type.fieldCount) {
                    throw InterpreterException("Cannot decompose ${type.name} into ${opcode.numParts} parts")
                }

                val obj = execStack.pop()
                if (obj.type != type) {
                    throw InterpreterException("Expected a ${type.name}, got ${obj.type.name}")
                }

                if (obj is CaseObject) {
                    obj.data.reversed().forEach { execStack.push(it) }
                }
            }
        }
    }

    private fun match(pattern: MatchPattern, obj: RuntimeObject? = null): Boolean {
        val o = obj ?: execStack.pop()
        return when (pattern) {
            AlwaysMatch -> true
            UnitMatch -> o.type == Builtin.Unit
            EmptyListMatch -> o.type == Builtin.ListEmpty
            is NumberMatch -> o is NumberObject && o.value == pattern.value
            is StringMatch -> o is StringObject && o.value == pattern.value
            is TypeMatch -> {
                val type = findType(pattern.typename)
                when (type) {
                    NumberType, StringType, FunctionType ->
                        o.type == type
                                && pattern.inners.size == 1
                                && match(pattern.inners[0], o)
                    is ParentType ->
                        o.type in type.cases
                                && pattern.inners.size == 1
                                && match(pattern.inners[0], o)
                    is TypeCase -> {
                        when (o) {
                            is SingletonObject -> o.type == type
                            is CaseObject -> o.type == type
                                    && pattern.inners.size == type.fieldCount
                                    && pattern.inners.zip(o.data).all { (p, d) -> match(p, d) }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun call(opcode: Call) {
        val fn = execStack.pop() as? FunctionObject ?: throw InterpreterException("not a function")

        when {
            opcode.nargs == fn.numArgs -> fn.call(this)
            opcode.nargs < fn.numArgs -> {
                val args = List(opcode.nargs) { execStack.pop() }
                execStack.push(PartialFunction(fn, args))
            }
            else -> throw InterpreterException("Too many arguments given for function $fn")
        }
    }

    private fun tailCall(opcode: TailCall) {
        val topFrame = callStack.peek()
        if (topFrame.fn.numArgs != opcode.nargs) throw InterpreterException("Cannot create partial tail-call")

        topFrame.tailCall()
    }

    private fun makeNamespace(path: List<String>, public: Boolean) {
        var ns = rootNamespace
        for (part in path) {
            if (public && !ns.public) throw InterpreterException("Cannot add public namespace as child of private namespace")
            if (part !in ns.subnames) {
                ns.subnames[part] = Namespace(public)
            }
            ns = ns.subnames[part]!!
        }
    }

    private fun makeType(opcode: MkType) {
        val ns = rootNamespace.findNamespace(opcode.namespace)

        val cases = opcode.cases.map { TypeCase(it.name, it.params.size) }
        val parent = ParentType(opcode.name, cases)

        ns.addType(parent)
    }

    private fun findName(qname: List<String>): RuntimeObject {
        return callStack.peek().fn.baseNs.findName(qname)
    }

    private fun findDeclaration(declaration: Declaration): RuntimeObject {
        return callStack.peek().let {
            it.scope.find(declaration)
                    ?: throw InterpreterException("Access of name '${declaration.name}' before assignment")
        }
    }

    private fun findType(qname: List<String>): Type {
        return callStack.peek().fn.baseNs.findType(qname)
    }

    class Stack<T> {
        private val store: MutableList<T> = mutableListOf()

        fun push(obj: T) = store.add(obj)
        fun pop(): T = store.removeAt(store.lastIndex)
        fun peek(): T = store[store.lastIndex]

        override fun toString(): String {
            return store.toString()
        }

        fun asList(): List<T> = store

        fun clear() = store.clear()
    }
}
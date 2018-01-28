package tomwamt.eagersnek.run

object Builtin {
    val Unit = TypeCase("Unit", 0)
    val UnitT = ParentType("UnitT", listOf(Unit))

    val ListEmpty = TypeCase("Empty", 0)
    val ListCons = TypeCase("::", 2)
    val List = ParentType("List", listOf(ListCons, ListEmpty))

    val True = TypeCase("True", 0)
    val False = TypeCase("False", 0)
    val Bool = ParentType("Bool", listOf(True, False))

    fun makeRootNamespace(): Namespace {
        val ns = Namespace()

        ns.types["Number"] = NumberType
        ns.types["String"] = StringType
        ns.types["Function"] = FunctionType

        ns.addType(UnitT)
        ns.addType(List)
        ns.addType(Bool)

        ns.bindings["println"] = object : FunctionObject(1) {
            override fun call(int: Interpreter) {
                val obj = int.execStack.pop()
                println(stringify(obj))
                int.execStack.push(CaseObject(Unit, emptyList()))
            }


        }

        ns.bindings["input"] = object : FunctionObject(1) {
            override fun call(int: Interpreter) {
                val prompt = int.execStack.pop()
                print(stringify(prompt))
                val inp = readLine()!!
                int.execStack.push(StringObject(inp))
            }
        }

        ns.bindings["+"] = BinaryOp { n1, n2 -> n1 + n2 }
        ns.bindings["-"] = BinaryOp { n1, n2 -> n1 - n2 }
        ns.bindings["*"] = BinaryOp { n1, n2 -> n1 * n2 }
        ns.bindings["/"] = BinaryOp { n1, n2 -> n1 / n2 }

        return ns
    }

    private class BinaryOp(private val op: (Double, Double) -> Double) : FunctionObject(2) {
        override fun call(int: Interpreter) {
            val n1 = int.execStack.pop() as? NumberObject ?: throw InterpreterException("Expected a number")
            val n2 = int.execStack.pop() as? NumberObject ?: throw InterpreterException("Expected a number")

            int.execStack.push(NumberObject(op(n1.value, n2.value)))
        }
    }

    private fun stringify(obj: RuntimeObject): String {
        return when (obj) {
            is NumberObject -> if (obj.value - obj.value.toInt() == 0.0) obj.value.toInt().toString() else obj.value.toString()
            is StringObject -> obj.value
            is CaseObject -> "(${obj.type.name}${obj.data.joinToString("") { " ${stringify(it)}" }})"
            is SingletonObject -> obj.type.name
            is FunctionObject -> "<${obj.javaClass.name}>"
            else -> obj.toString()
        }
    }
}
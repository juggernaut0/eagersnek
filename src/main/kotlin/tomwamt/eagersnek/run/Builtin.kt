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
        val ns = Namespace(true)

        ns.types["Number"] = NumberType
        ns.types["String"] = StringType
        ns.types["Function"] = FunctionType

        ns.addType(UnitT)
        ns.addType(List)
        ns.addType(Bool)
        val trueObj = ns.bindings[True.name]!!
        val falseObj = ns.bindings[False.name]!!

        ns.subnames["String"] = makeStringNamespace()

        ns.bindings["println"] = object : FunctionObject(1) {
            override fun call(int: Interpreter) {
                val obj = int.execStack.pop()
                int.io.out(stringify(obj))
                int.execStack.push(CaseObject(Unit, emptyList()))
            }
        }

        ns.bindings["input"] = object : FunctionObject(1) {
            override fun call(int: Interpreter) {
                val prompt = int.execStack.pop()
                int.io.out(stringify(prompt))
                val inp = int.io.inp()!!
                int.execStack.push(StringObject(inp))
            }
        }

        ns.bindings["+"] = BinaryOp { n1, n2 -> n1 + n2 }
        ns.bindings["-"] = BinaryOp { n1, n2 -> n1 - n2 }
        ns.bindings["*"] = BinaryOp { n1, n2 -> n1 * n2 }
        ns.bindings["/"] = BinaryOp { n1, n2 -> n1 / n2 }

        ns.bindings["&"] = BoolOp(trueObj, falseObj) { b1, b2 -> b1 && b2 }
        ns.bindings["|"] = BoolOp(trueObj, falseObj) { b1, b2 -> b1 || b2 }

        ns.bindings["eq"] = object : FunctionObject(2) {
            override fun call(int: Interpreter) {
                val o1 = int.execStack.pop()
                val o2 = int.execStack.pop()

                int.execStack.push(if (eq(o1, o2)) trueObj else falseObj)
            }

            fun eq(o1: RuntimeObject, o2: RuntimeObject): Boolean {
                return when {
                    o1 == o2 -> true
                    o1.type != o2.type -> false
                    o1 is NumberObject && o2 is NumberObject -> o1.value == o2.value
                    o1 is StringObject && o2 is StringObject -> o1.value == o2.value
                    o1 is CaseObject && o2 is CaseObject -> o1.data.zip(o2.data).all { (d1, d2) -> eq(d1, d2) }
                    else -> false
                }
            }
        }

        ns.bindings["to_num"] = object : FunctionObject(1) {
            override fun call(int: Interpreter) {
                val str = int.execStack.pop().cast<StringObject>()

                try {
                    int.execStack.push(NumberObject(str.value.toDouble()))
                } catch (e: NumberFormatException) {
                    throw InterpreterException("Can't parse number ${e.message ?: ""}")
                }
            }
        }

        return ns
    }

    private fun makeStringNamespace(): Namespace {
        val ns = Namespace(true)

        ns.bindings["concat"] = object : FunctionObject(2) {
            override fun call(int: Interpreter) {
                val s1 = int.popString()
                val s2 = int.popString()

                int.execStack.push(StringObject(s1 + s2))
            }
        }

        ns.bindings["char_at"] = object : FunctionObject(2) {
            override fun call(int: Interpreter) {
                val i = int.popInt()
                val str = int.popString()

                val ch = str[i].toString()
                int.execStack.push(StringObject(ch))
            }
        }

        ns.bindings["length"] = object : FunctionObject(1) {
            override fun call(int: Interpreter) {
                val str = int.execStack.pop().cast<StringObject>()
                int.execStack.push(NumberObject(str.value.length.toDouble()))
            }
        }

        ns.bindings["substr"] = object : FunctionObject(3) {
            override fun call(int: Interpreter) {
                val start = int.popInt()
                val end = int.popInt()
                val str = int.popString()

                int.execStack.push(StringObject(str.substring(start, end)))
            }
        }

        return ns
    }

    private class BinaryOp(private val op: (Double, Double) -> Double) : FunctionObject(2) {
        override fun call(int: Interpreter) {
            val n1 = int.popNumber()
            val n2 = int.popNumber()

            int.execStack.push(NumberObject(op(n1, n2)))
        }
    }

    private class BoolOp(private val trueObj: RuntimeObject, private val falseObj: RuntimeObject, private val op: (Boolean, Boolean) -> Boolean) : FunctionObject(2) {
        override fun call(int: Interpreter) {
            val b1 = int.popBool()
            val b2 = int.popBool()

            int.pushBool(op(b1, b2))
        }

        private fun Interpreter.popBool(): Boolean {
            return when (val o = execStack.pop()) {
                trueObj -> true
                falseObj -> false
                else -> throw InterpreterException("Expected a boolean, got ${o.type}")
            }
        }

        private fun Interpreter.pushBool(b: Boolean) {
            execStack.push(if (b) trueObj else falseObj)
        }
    }

    fun stringify(obj: RuntimeObject): String {
        return when (obj) {
            is NumberObject -> if (obj.value.isInt()) obj.value.toInt().toString() else obj.value.toString()
            is StringObject -> obj.value
            is CaseObject -> "(${obj.type.name}${obj.data.joinToString("") { " ${stringify(it)}" }})"
            is SingletonObject -> obj.type.name
            is FunctionObject -> "<${obj.javaClass.name}>"
            else -> obj.toString()
        }
    }

    private fun Double.isInt() = this - toInt() == 0.0

    private inline fun <reified T : RuntimeObject> RuntimeObject.cast(): T {
        return this as? T ?: throw InterpreterException("Expected a ${T::class.java.simpleName}, got ${javaClass.simpleName}")
    }

    private fun Interpreter.popNumber() = execStack.pop().cast<NumberObject>().value
    private fun Interpreter.popString() = execStack.pop().cast<StringObject>().value
    private fun Interpreter.popInt(): Int {
        return execStack.pop()
                .cast<NumberObject>().value
                .takeIf { it.isInt() }
                ?.toInt()
                ?: throw InterpreterException("Expected an integer index")
    }
}
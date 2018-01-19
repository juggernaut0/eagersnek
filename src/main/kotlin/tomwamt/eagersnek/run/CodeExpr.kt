package tomwamt.eagersnek.run

import tomwamt.eagersnek.parse.TypeCase

interface CodeExpr
object Pop : CodeExpr
object Duplicate : CodeExpr
// pops value, if it is correct type decomposes it, pushing values in reverse order, otherwise error
class Decompose(val type: List<String>, val numParts: Int = 0) : CodeExpr
class LoadName(val name: List<String>) : CodeExpr
class LoadNumber(val value: Double) : CodeExpr
class LoadString(val value: String) : CodeExpr
class LoadCallable(val code: List<CodeExpr>) : CodeExpr
class SaveLocal(val name: String) : CodeExpr
class SaveNamespace(val name: String, val namespace: List<String>) : CodeExpr
// 1 value popped is callable, nargs popped are arguments (called with args in order of pop)
class Call(val nargs: Int) : CodeExpr
object PushScope : CodeExpr
object PopScope : CodeExpr
class MkNamespace(val name: List<String>) : CodeExpr
class MkType(val name: String, val namespace: List<String>, val cases: List<TypeCase>) : CodeExpr

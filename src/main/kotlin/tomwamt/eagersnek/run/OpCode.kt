package tomwamt.eagersnek.run

import tomwamt.eagersnek.parse.TypeCase

interface OpCode
object Pop : OpCode
object Duplicate : OpCode
// pops value, if it is correct type decomposes it, pushing values in reverse order, otherwise error
class Decompose(val type: List<String>, val numParts: Int = 0) : OpCode
class LoadName(val name: List<String>) : OpCode
class LoadNumber(val value: Double) : OpCode
class LoadString(val value: String) : OpCode
class LoadCallable(val code: List<OpCode>) : OpCode
class SaveLocal(val name: String) : OpCode
class SaveNamespace(val name: String, val namespace: List<String>) : OpCode
// 1 value popped is callable, nargs popped are arguments (called with args in order of pop)
class Call(val nargs: Int) : OpCode
object PushScope : OpCode
object PopScope : OpCode
class MkNamespace(val name: List<String>, val localName: String) : OpCode
class MkType(val name: String, val namespace: List<String>, val cases: List<TypeCase>) : OpCode

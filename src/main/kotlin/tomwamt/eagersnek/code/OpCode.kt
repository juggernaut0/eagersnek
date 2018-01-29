package tomwamt.eagersnek.code

import tomwamt.eagersnek.parse.QualifiedName
import tomwamt.eagersnek.parse.TypeCase

interface OpCode
object NoOp : OpCode
class Fail(val msg: String) : OpCode
object Pop : OpCode
object Duplicate : OpCode
// pops value, if it is correct type decomposes it, pushing values in reverse order, otherwise error
class Decompose(val type: List<String>, val numParts: Int = 0) : OpCode
class Jump(val label: Label) : OpCode
class Match(val pattern: MatchPattern) : OpCode
class JumpIfMatch(val pattern: MatchPattern, val label: Label) : OpCode
class LoadName(val name: List<String>) : OpCode
class LoadNumber(val value: Double) : OpCode
class LoadString(val value: String) : OpCode
class LoadFunction(val code: CompiledCode, val paramCount: Int) : OpCode
class SaveLocal(val name: String) : OpCode
class SaveNamespace(val name: String, val namespace: List<String>) : OpCode
// 1 value popped is callable, nargs popped are arguments (called with args in order of pop)
class Call(val nargs: Int) : OpCode
// nargs popped to be rebound to params, block restarts
class TailCall(val nargs: Int) : OpCode
object PushScope : OpCode
object PopScope : OpCode
class MkNamespace(val name: List<String>, val localName: String, val public: Boolean) : OpCode
class MkType(val name: String, val namespace: List<String>, val cases: List<TypeCase>) : OpCode
class ImportAll(val filename: String) : OpCode

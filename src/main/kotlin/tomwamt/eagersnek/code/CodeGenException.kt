package tomwamt.eagersnek.code

class CodeGenException(msg: String, line: Int) : Exception("Line $line: $msg")
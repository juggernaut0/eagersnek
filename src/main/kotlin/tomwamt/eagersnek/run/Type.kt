package tomwamt.eagersnek.run

class Type(val name: String) {
    companion object {
        val NUMBER = Type("Number")
        val STRING = Type("String")
        val CALLABLE = Type("Callable")
    }
}

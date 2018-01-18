package tomwamt.eagersnek.run

class Type(val name: String) {
    companion object {
        val NUMBER = Type("Number")
        val STRING = Type("String")
        val UNIT = Type("Unit")
        val CALLABLE = Type("Callable")
    }
}

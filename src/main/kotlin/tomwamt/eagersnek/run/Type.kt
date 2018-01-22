package tomwamt.eagersnek.run

class Type(val name: String, val fieldCount: Int) {
    companion object {
        val NUMBER = Type("Number", 1)
        val STRING = Type("String", 1)
        val CALLABLE = Type("Callable", 1)
    }
}

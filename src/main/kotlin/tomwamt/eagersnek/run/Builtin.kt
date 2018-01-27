package tomwamt.eagersnek.run

object Builtin {
    val Unit = TypeCase("Unit", 0)
    val UnitT = ParentType("UnitT", listOf(Unit))

    val ListEmpty = TypeCase("Empty", 0)
    val ListCons = TypeCase("::", 2)
    val List = ParentType("List", listOf(ListCons, ListEmpty))

    fun makeRootNamespace(): Namespace {
        val ns = Namespace()
        ns.types["UnitT"] = UnitT
        ns.types["Unit"] = Unit

        return ns
    }
}
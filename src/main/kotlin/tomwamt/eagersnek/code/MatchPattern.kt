package tomwamt.eagersnek.code

interface MatchPattern
class NumberMatch(val value: Double) : MatchPattern
class StringMatch(val value: String) : MatchPattern
object EmptyListMatch : MatchPattern
object UnitMatch : MatchPattern
object AlwaysMatch : MatchPattern
class TypeMatch(val typename: List<String>, val inners: List<MatchPattern>) : MatchPattern

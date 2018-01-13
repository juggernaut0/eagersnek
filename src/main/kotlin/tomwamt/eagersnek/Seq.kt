package tomwamt.eagersnek

interface Seq<T> {
    class SeqState<TOut, TState>(private val state: TState, private val hf: (TState) -> TOut, private val tf: (TState) -> TState?) {
        fun toSeq(): Seq<TOut> = object : Seq<TOut> {
            override fun head(): TOut = this@SeqState.head()
            override fun tail(): Seq<TOut> = this@SeqState.tail()?.toSeq() ?: empty()
            override val empty: Boolean = false
        }
        fun head(): TOut = hf(state)
        fun tail(): SeqState<TOut, TState>? {
            val newState = tf(state) ?: return null
            return SeqState(newState, hf, tf)
        }
    }

    companion object {
        fun <T> empty(): Seq<T> = object : Seq<T> {
            override fun head(): T = throw NoSuchElementException("Empty Seq")
            override fun tail(): Seq<T> = throw NoSuchElementException("Empty Seq")
            override val empty: Boolean = true
        }
    }

    fun head(): T
    fun tail(): Seq<T>
    val empty: Boolean
}

fun <T> Seq<T>.filter(pred: (T) -> Boolean): Seq<T> {
    var s = this
    while(!s.empty && !pred(s.head())) {
        s = s.tail()
    }
    return FilteredSeq(s, pred)
}

class FilteredSeq<T>(private val backing: Seq<T>, private val pred: (T) -> Boolean) : Seq<T> {
    override fun head(): T = backing.head()

    override fun tail(): Seq<T> {
        var s = backing.tail()
        while (!s.empty && !pred(s.head())) {
            s = s.tail()
        }
        return FilteredSeq(s, pred)
    }

    override val empty: Boolean = backing.empty
}

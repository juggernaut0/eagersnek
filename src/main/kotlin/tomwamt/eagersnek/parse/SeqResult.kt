package tomwamt.eagersnek.parse

data class SeqResult<out T>(val result: T, val seq: Seq<Token>) {
    inline fun <U> then(seqMap: Seq<Token>.() -> SeqResult<U>): SeqResult<Pair<T, U>> {
        val (res2, seq2) = seqMap(seq)
        return SeqResult(result to res2, seq2)
    }

    inline fun thenConsume(block: Seq<Token>.() -> Seq<Token>): SeqResult<T> {
        val seq2 = block(seq)
        return SeqResult(result, seq2)
    }

    inline fun <U> map(mapper: (T) -> U): SeqResult<U> = SeqResult(mapper(result), seq)
}

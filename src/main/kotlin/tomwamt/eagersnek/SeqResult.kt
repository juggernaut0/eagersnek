package tomwamt.eagersnek

data class SeqResult<out T>(val result: T, val seq: Seq<Token>) {
    inline fun <U> flatMap(seqMap: (Seq<Token>) -> SeqResult<U>) = flatMap({ a, b -> a to b }, seqMap)

    inline fun <U, V> flatMap(resultMap: (T, U) -> V, seqMap: (Seq<Token>) -> SeqResult<U>): SeqResult<V> {
        val (res2, seq2) = seqMap(seq)
        return SeqResult(resultMap(result, res2), seq2)
    }

    inline fun thenConsume(block: (Seq<Token>) -> Seq<Token>): SeqResult<T> {
        val seq2 = block(seq)
        return SeqResult(result, seq2)
    }

    inline fun <U> map(mapper: (T) -> U): SeqResult<U> = SeqResult(mapper(result), seq)
}

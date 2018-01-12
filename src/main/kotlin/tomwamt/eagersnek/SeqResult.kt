package tomwamt.eagersnek

data class SeqResult<out T>(val result: T, val seq: Sequence<Token>) {
    inline fun <U> flatMap(seqMap: (Sequence<Token>) -> SeqResult<U>) = flatMap({ a, b -> a to b }, seqMap)

    inline fun <U, V> flatMap(resultMap: (T, U) -> V, seqMap: (Sequence<Token>) -> SeqResult<U>): SeqResult<V> {
        val (res2, seq2) = seqMap(seq)
        return SeqResult(resultMap(result, res2), seq2)
    }

    inline fun thenConsume(block: (Sequence<Token>) -> Sequence<Token>): SeqResult<T> {
        val seq2 = block(seq)
        return SeqResult(result, seq2)
    }

    inline fun <U> map(mapper: (T) -> U): SeqResult<U> = SeqResult(mapper(result), seq)
}

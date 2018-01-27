package tomwamt.eagersnek.code

class Label(val display: String) {
    private var _target: Int? =  null
    val target: Int get() = _target ?: throw RuntimeException("Unattached label")

    fun attach(target: Int) {
        if (_target != null) throw CodeGenException("label $display already attached")
        _target = target
    }

}

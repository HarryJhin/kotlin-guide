package io.github.harryjhin.valueclass

@JvmInline
value class RGB(private val value: Int) {

    init {
        require(value in 0x000000..0xFFFFFF) { "Invalid RGB value: $value. It should be in the range 0x000000..0xFFFFFF." }
    }

    constructor(
        red: Int,
        green: Int,
        blue: Int
    ) : this((red and 0xFF shl 16) or (green and 0xFF shl 8) or (blue and 0xFF)) {
        require(red in 0..255) { "Invalid red value: $red. It should be in the range 0..255." }
        require(green in 0..255) { "Invalid green value: $green. It should be in the range 0..255." }
        require(blue in 0..255) { "Invalid blue value: $blue. It should be in the range 0..255." }
    }

    val red: Int get() = (value shr 16) and 0xFF
    val green: Int get() = (value shr 8) and 0xFF
    val blue: Int get() = value and 0xFF

    override fun toString(): String = StringBuilder("0x").append("%06X".format(value).uppercase()).toString()

    companion object {
        val BLACK = RGB(0x000000)
        val WHITE = RGB(0xFFFFFF)
        val RED = RGB(0xFF0000)
        val GREEN = RGB(0x00FF00)
        val BLUE = RGB(0x0000FF)
    }
}

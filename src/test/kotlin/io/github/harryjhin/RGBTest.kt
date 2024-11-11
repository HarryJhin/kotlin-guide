package io.github.harryjhin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RGBTest {

    @Test
    fun `RGB 인스턴스 생성`() {
        val sky = RGB(75, 137, 220)
        assertEquals(
            "0x4B89DC",
            sky.toString()
        )
    }

    @Test
    fun `RGB 인스턴스 비교`() {
        val red = RGB(255, 0, 0)
        assertEquals(
            RGB.RED,
            red
        )
        assertEquals(
            RGB.RED.red,
            red.red
        )
        assertEquals(
            RGB.RED.green,
            red.green
        )
        assertEquals(
            RGB.RED.blue,
            red.blue
        )
    }

    @Test
    fun `RGB 값 생성 실패`() {
        assertFailsWith(IllegalArgumentException::class) {
            RGB(256, 0, 0)
        }
        assertFailsWith(IllegalArgumentException::class) {
            RGB(0xFFFFFF + 1)
        }
    }
}
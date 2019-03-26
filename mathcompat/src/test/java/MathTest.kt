package androidx.mathcompat

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MathTest {
    @Test
    fun testPlus_1_1() {
        val result = Math.plus(1, 1)
        assertEquals(2, result)
    }

    @Test
    fun testMinus_1_1() {
        val result = Math.minus(1, 1)
        assertEquals(0, result)
    }
}
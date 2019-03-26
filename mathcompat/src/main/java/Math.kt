package androidx.mathcompat

import androidx.annotation.AnyThread

/**
 * Main class to keep Android devices up to date with the newest Math.
 * <p/>
 * At present all methods are static on Math so no constructor is needed
 * <p/>
 * <pre><code>val sum = Math.plus(1, 1)</pre>
 */
@AnyThread
class Math() {
    companion object {
        /**
         * Adds the given numbers together
         *
         * @param int Int the first number to add
         * @param int2 Int the second number to add
         *
         * @return int Int sum of the numbers passed in
         */
        fun plus(int: Int, int2: Int): Int {
            return int + int2
        }

        /**
         * Subtracts the second number from the first
         *
         * @param minuend Int number from which the other is to be subtracted
         * @param subtrahend Int number to be subtracted from the other
         *
         * @return int Int result of the subtraction
         */
        fun minus(minuend: Int, subtrahend: Int): Int {
            return minuend - subtrahend
        }
    }
}

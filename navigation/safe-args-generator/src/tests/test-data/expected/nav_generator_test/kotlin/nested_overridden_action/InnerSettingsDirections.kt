<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
package foo

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

class InnerSettingsDirections private constructor() {
    private data class Exit(val exitReason: Int) : NavDirections {
        override fun getActionId(): Int = foo.R.id.exit

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putInt("exitReason", this.exitReason)
            return result
        }
    }

    companion object {
        fun exit(exitReason: Int): NavDirections = Exit(exitReason)

        fun main(enterReason: String = "DEFAULT"): NavDirections =
                SettingsDirections.main(enterReason)
    }
}
=======
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)

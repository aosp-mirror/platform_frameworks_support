<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
package a.b

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

class MainFragmentDirections private constructor() {
    private data class Next(val main: String, val optional: String = "bla") : NavDirections {
        override fun getActionId(): Int = a.b.R.id.next

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putString("main", this.main)
            result.putString("optional", this.optional)
            return result
        }
    }

    companion object {
        fun previous(): NavDirections = ActionOnlyNavDirections(a.b.R.id.previous)

        fun next(main: String, optional: String = "bla"): NavDirections = Next(main, optional)
    }
}
=======
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)

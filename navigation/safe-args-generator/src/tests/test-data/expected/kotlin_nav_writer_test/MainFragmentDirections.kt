package a.b

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

class MainFragmentDirections {
    object Previous : NavDirections {
        override fun getActionId(): Int = a.b.R.id.previous

        override fun getArguments(): Bundle {
            val __result = Bundle()
            return __result
        }
    }

    data class Next internal constructor(val main: String, val optional: String) : NavDirections {
        override fun getActionId(): Int = a.b.R.id.next

        override fun getArguments(): Bundle {
            val __result = Bundle()
            __result.putString("main", main)
            __result.putString("optional", optional)
            return __result
        }
    }

    companion object {
        fun previous(): Previous = Previous

        fun next(main: String, optional: String = "bla"): Next = Next(main, optional)
    }
}

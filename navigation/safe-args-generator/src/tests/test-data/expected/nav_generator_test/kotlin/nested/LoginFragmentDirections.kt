package foo.flavor

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int

class LoginFragmentDirections {
    object Register : NavDirections {
        override fun getActionId(): Int = foo.R.id.register

        override fun getArguments(): Bundle {
            val result = Bundle()
            return result
        }
    }

    companion object {
        fun register(): Register = Register

        fun actionDone(): LoginDirections.ActionDone = LoginDirections.actionDone()
    }
}

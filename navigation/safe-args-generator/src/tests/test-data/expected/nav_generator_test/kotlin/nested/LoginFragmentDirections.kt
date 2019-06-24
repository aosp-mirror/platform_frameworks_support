<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
package foo.flavor.account

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.LoginDirections

class LoginFragmentDirections private constructor() {
    companion object {
        fun register(): NavDirections = ActionOnlyNavDirections(foo.R.id.register)

        fun actionDone(): NavDirections = LoginDirections.actionDone()
    }
}
=======
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)

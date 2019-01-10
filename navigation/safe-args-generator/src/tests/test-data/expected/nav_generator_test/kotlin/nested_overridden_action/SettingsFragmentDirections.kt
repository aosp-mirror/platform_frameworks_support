package foo.flavor

import kotlin.String

class SettingsFragmentDirections {
    companion object {
        fun exit(exitReason: String = "DEFAULT"): SettingsDirections.Exit =
                SettingsDirections.exit(exitReason)
    }
}

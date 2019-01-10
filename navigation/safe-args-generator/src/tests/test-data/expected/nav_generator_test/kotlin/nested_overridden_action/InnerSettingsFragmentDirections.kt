package foo.flavor

import kotlin.Int

class InnerSettingsFragmentDirections {
    companion object {
        fun exit(exitReason: Int): InnerSettingsDirections.Exit =
                InnerSettingsDirections.exit(exitReason)
    }
}

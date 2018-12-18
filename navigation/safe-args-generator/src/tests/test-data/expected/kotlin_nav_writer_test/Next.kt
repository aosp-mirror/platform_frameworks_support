package a.b

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavDirections
import java.io.Serializable
import java.lang.UnsupportedOperationException
import kotlin.Int
import kotlin.String
import kotlin.Suppress

data class Next internal constructor(
    val main: String,
    val mainInt: Int,
    val optional: String,
    val optionalInt: Int,
    val optionalParcelable: ActivityInfo?,
    val parcelable: ActivityInfo
) : NavDirections {
    override fun getActionId(): Int = a.b.R.id.next

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun getArguments(): Bundle {
        val __result = Bundle()
        __result.putString("main", main)
        __result.putInt("mainInt", mainInt)
        __result.putString("optional", optional)
        __result.putInt("optionalInt", optionalInt)
        if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            __result.putParcelable("optionalParcelable", optionalParcelable as Parcelable)
        } else if (Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            __result.putSerializable("optionalParcelable", optionalParcelable as Serializable)
        }
        if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            __result.putParcelable("parcelable", parcelable as Parcelable)
        } else if (Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            __result.putSerializable("parcelable", parcelable as Serializable)
        } else {
            throw UnsupportedOperationException(ActivityInfo::class.java.name +
                    " must implement Parcelable or Serializable or must be an Enum.")
        }
        return __result
    }
}

package a.b

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavArgs
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException
import java.nio.file.AccessMode
import kotlin.Array
import kotlin.Boolean
import kotlin.Float
import kotlin.FloatArray
import kotlin.Int
import kotlin.String
import kotlin.Suppress

data class MainFragmentArgs(
    val main: String,
    val optional: Int = -1,
    val reference: Int = a.b.R.drawable.background,
    val floatArg: Float = 1F,
    val floatArrayArg: FloatArray,
    val objectArrayArg: Array<ActivityInfo>,
    val boolArg: Boolean = true,
    val optionalParcelable: ActivityInfo? = null,
    val enumArg: AccessMode = AccessMode.READ
) : NavArgs {
    @Suppress("UNCHECKED_CAST")
    constructor(bundle: Bundle) : this(bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("main")) {
            getString("main")
                ?: throw IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.")
        } else {
            throw IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue")
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("optional")) {
            getInt("optional")
        } else {
            -1
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("reference")) {
            getInt("reference")
        } else {
            a.b.R.drawable.background
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("floatArg")) {
            getFloat("floatArg")
        } else {
            1F
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("floatArrayArg")) {
            getFloatArray("floatArrayArg")
                ?: throw IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.")
        } else {
            throw IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue")
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("objectArrayArg")) {
            getParcelableArray("objectArrayArg") as Array<ActivityInfo>?
                ?: throw IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.")
        } else {
            throw IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue")
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("boolArg")) {
            getBoolean("boolArg")
        } else {
            true
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("optionalParcelable")) {
            if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java) ||
                    Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
                get("optionalParcelable") as ActivityInfo?
            } else {
                throw UnsupportedOperationException(ActivityInfo::class.java.name +
                        " must implement Parcelable or Serializable or must be an Enum.")
            }
        } else {
            null
        }
    }
    , bundle.run {
        setClassLoader(MainFragmentArgs::class.java.classLoader)
        if (containsKey("enumArg")) {
            if (Parcelable::class.java.isAssignableFrom(AccessMode::class.java) ||
                    Serializable::class.java.isAssignableFrom(AccessMode::class.java)) {
                get("enumArg") as AccessMode?
            } else {
                throw UnsupportedOperationException(AccessMode::class.java.name +
                        " must implement Parcelable or Serializable or must be an Enum.")
            }
                ?: throw IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.")
        } else {
            AccessMode.READ
        }
    }
    )

    @Suppress("CAST_NEVER_SUCCEEDS")
    fun toBundle(): Bundle {
        val __result = Bundle()
        __result.putString("main", main)
        __result.putInt("optional", optional)
        __result.putInt("reference", reference)
        __result.putFloat("floatArg", floatArg)
        __result.putFloatArray("floatArrayArg", floatArrayArg)
        __result.putParcelableArray("objectArrayArg", objectArrayArg)
        __result.putBoolean("boolArg", boolArg)
        if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            __result.putParcelable("optionalParcelable", optionalParcelable as Parcelable?)
        } else if (Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            __result.putSerializable("optionalParcelable", optionalParcelable as Serializable?)
        }
        if (Parcelable::class.java.isAssignableFrom(AccessMode::class.java)) {
            __result.putParcelable("enumArg", enumArg as Parcelable)
        } else if (Serializable::class.java.isAssignableFrom(AccessMode::class.java)) {
            __result.putSerializable("enumArg", enumArg as Serializable)
        }
        return __result
    }
}

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
    @Suppress("UNCHECKED_CAST", "USELESS_CAST")
    constructor(bundle: Bundle) : this(bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __main : String?
        if (it.containsKey("main")) {
            __main = it.getString("main")
            if (__main == null) {
                throw IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.")
            }
        } else {
            throw IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue")
        }
        return@let __main as String
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __optional : Int
        if (it.containsKey("optional")) {
            __optional = it.getInt("optional")
        } else {
            __optional = -1
        }
        return@let __optional as Int
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __reference : Int
        if (it.containsKey("reference")) {
            __reference = it.getInt("reference")
        } else {
            __reference = a.b.R.drawable.background
        }
        return@let __reference as Int
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __floatArg : Float
        if (it.containsKey("floatArg")) {
            __floatArg = it.getFloat("floatArg")
        } else {
            __floatArg = 1F
        }
        return@let __floatArg as Float
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __floatArrayArg : FloatArray?
        if (it.containsKey("floatArrayArg")) {
            __floatArrayArg = it.getFloatArray("floatArrayArg")
            if (__floatArrayArg == null) {
                throw IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.")
            }
        } else {
            throw IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue")
        }
        return@let __floatArrayArg as FloatArray
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __objectArrayArg : Array<ActivityInfo>?
        if (it.containsKey("objectArrayArg")) {
            __objectArrayArg = it.getParcelableArray("objectArrayArg") as Array<ActivityInfo>?
            if (__objectArrayArg == null) {
                throw IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.")
            }
        } else {
            throw IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue")
        }
        return@let __objectArrayArg as Array<ActivityInfo>
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __boolArg : Boolean
        if (it.containsKey("boolArg")) {
            __boolArg = it.getBoolean("boolArg")
        } else {
            __boolArg = true
        }
        return@let __boolArg as Boolean
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __optionalParcelable : ActivityInfo?
        if (it.containsKey("optionalParcelable")) {
            if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java) ||
                    Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
                __optionalParcelable = it.get("optionalParcelable") as ActivityInfo?
            } else {
                throw UnsupportedOperationException(ActivityInfo::class.java.name +
                        " must implement Parcelable or Serializable or must be an Enum.")
            }
        } else {
            __optionalParcelable = null
        }
        return@let __optionalParcelable as ActivityInfo?
    }
    , bundle.let {
        it.setClassLoader(MainFragmentArgs::class.java.classLoader)
        val __enumArg : AccessMode?
        if (it.containsKey("enumArg")) {
            if (Parcelable::class.java.isAssignableFrom(AccessMode::class.java) ||
                    Serializable::class.java.isAssignableFrom(AccessMode::class.java)) {
                __enumArg = it.get("enumArg") as AccessMode?
            } else {
                throw UnsupportedOperationException(AccessMode::class.java.name +
                        " must implement Parcelable or Serializable or must be an Enum.")
            }
            if (__enumArg == null) {
                throw IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.")
            }
        } else {
            __enumArg = AccessMode.READ
        }
        return@let __enumArg as AccessMode
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

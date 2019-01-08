/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.util.ArrayMap
import androidx.navigation.NavArgs
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

/**
 * Lazily extract the specified [NavArgs] from [Fragment.getArguments].
 */
inline fun <reified Args : NavArgs> Fragment.navArgs() = NavArgsLazy(Args::class, this)

internal val constructorSignature = arrayOf(Bundle::class.java)
internal val constructorMap = ArrayMap<KClass<out NavArgs>, Constructor<out NavArgs>>()

/**
 * An implementation of [Lazy] used by [navArgs].
 *
 * [fragment] is the Fragment to extract the arguments from by calling
 * [Fragment.getArguments].
 */
class NavArgsLazy<Args : NavArgs>(
    private val navArgsClass: KClass<Args>,
    private val fragment: Fragment
) : Lazy<Args> {
    private var cached: Args? = null

    override val value: Args
        get() {
            var args = cached
            if (args == null) {
                val fragmentArguments = fragment.arguments
                    ?: throw IllegalStateException("Fragment $fragment has null arguments")
                val constructor: Constructor<out NavArgs> = constructorMap[navArgsClass]
                    ?: navArgsClass.java.getConstructor(*constructorSignature).apply {
                        isAccessible = true
                    }.also { constructor ->
                        // Save a reference to the constructor
                        constructorMap[navArgsClass] = constructor
                    }

                @Suppress("UNCHECKED_CAST")
                args = constructor.newInstance(fragmentArguments) as Args
                cached = args
            }
            return args
        }

    override fun isInitialized() = cached != null
}

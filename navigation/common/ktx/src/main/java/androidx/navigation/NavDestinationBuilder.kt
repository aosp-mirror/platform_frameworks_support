/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation

import android.support.annotation.IdRes

@DslMarker
annotation class NavDestinationDsl

/**
 * DSL for constructing a new [NavDestination]
 */
@NavDestinationDsl
open class NavDestinationBuilder<out D : NavDestination>(
    protected val navigator: Navigator<out D>,
    @IdRes val id: Int
) {
    /**
     * The descriptive label of the destination
     */
    var label: CharSequence? = null

    /**
     * The arguments supported by this destination
     */
    private var navArguments = mutableListOf<NavArgument<*>>()

    /**
     * Adds a new [NavAction] to the destination
     */
    fun <T> argument(name: String, navType: NavType<T>, block: NavArgumentBuilder<T>.() -> Unit) {
        navArguments.add(NavArgumentBuilder(name, navType).apply(block).build())
    }

    private var deepLinks = mutableListOf<String>()

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * *    Uris without a scheme are assumed as http and https. For example,
     *      `www.example.com` will match `http://www.example.com` and
     *      `https://www.example.com`.
     * *    Placeholders in the form of `{placeholder_name}` matches 1 or more
     *      characters. The String value of the placeholder will be available in the arguments
     *      [Bundle] with a key of the same name. For example,
     *      `http://www.example.com/users/{id}` will match
     *      `http://www.example.com/users/4`.
     * *    The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param uriPattern The uri pattern to add as a deep link
     */
    fun deepLink(uriPattern: String) {
        deepLinks.add(uriPattern)
    }

    private var actions = mutableMapOf<Int, NavAction>()

    /**
     * Adds a new [NavAction] to the destination
     */
    fun action(actionId: Int, block: NavActionBuilder.() -> Unit) {
        actions[actionId] = NavActionBuilder().apply(block).build()
    }

    /**
     * Build the NavDestination by calling [Navigator.createDestination].
     */
    open fun build(): D {
        return navigator.createDestination().also { destination ->
            destination.id = id
            destination.label = label
            navArguments.forEach {
                destination.putArgument(it.name, it)
            }
            deepLinks.forEach { deepLink ->
                destination.addDeepLink(deepLink)
            }
            actions.forEach { (actionId, action) ->
                destination.putAction(actionId, action)
            }
        }
    }
}

/**
 * DSL for building a [NavAction].
 */
@NavDestinationDsl
class NavActionBuilder {
    /**
     * The ID of the destination that should be navigated to when this action is used
     */
    var destinationId: Int = 0

    private var navOptions: NavOptions? = null

    /**
     * Sets the [NavOptions] for this action that should be used by default
     */
    fun navOptions(block: NavOptionsBuilder.() -> Unit) {
        navOptions = NavOptionsBuilder().apply(block).build()
    }

    internal fun build() = NavAction(destinationId, navOptions)
}

/**
 * DSL for building a [NavArgument].
 */
@NavDestinationDsl
class NavArgumentBuilder<T>(name: String, type: NavType<T>) {
    private val builder: NavArgument.Builder<T> = NavArgument.Builder()

    init {
        builder.setName(name)
        builder.setType(type)
    }

    var defaultValue: T? = null
        set(value) {
            builder.setDefaultValue(value)
        }

    var isNullable: Boolean = false
        set(value) {
            builder.setIsNullable(value)
        }

    fun build(): NavArgument<T> {
        return builder.build()
    }
}

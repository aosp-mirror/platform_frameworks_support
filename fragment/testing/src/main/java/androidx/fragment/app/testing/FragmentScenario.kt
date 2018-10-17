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

package androidx.fragment.app.testing

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.core.util.Preconditions
import androidx.core.util.Preconditions.checkNotNull
import androidx.core.util.Preconditions.checkState
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario

/**
 * FragmentScenario provides API to start and drive a Fragment's lifecycle state for testing. It
 * works with arbitrary fragments and works consistently across different versions of the Android
 * framework.
 *
 * FragmentScenario only supports [androidx.fragment.app.Fragment]. If you are using a
 * deprecated fragment class such as [android.support.v4.app.Fragment] or [android.app.Fragment],
 * please update your code to [androidx.fragment.app.Fragment].
 *
 * @param <F> The Fragment class being tested
 *
 * @see ActivityScenario a scenario API for Activity
*/
class FragmentScenario<F : Fragment> internal constructor(
    @Suppress("MemberVisibilityCanBePrivate") /* synthetic access */
    internal val fragmentClass: Class<F>,
    private val activityScenario: ActivityScenario<EmptyFragmentActivity>
) {

    /**
     * An empty activity inheriting FragmentActivity. This Activity is used to host Fragment in
     * FragmentScenario.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    class EmptyFragmentActivity : FragmentActivity()

    companion object {

        private const val FRAGMENT_TAG = "FragmentScenario_Fragment_Tag"

        /**
         * Launches a Fragment with given arguments hosted by an empty [FragmentActivity] using
         * given [FragmentFactory] and waits for it to reach a resumed state.
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentArgs a bundle to passed into fragment
         * @param factory a fragment factory to use or null to use default factory
         */
        inline fun <reified F : Fragment> launch(
            fragmentArgs: Bundle? = null,
            factory: FragmentFactory? = null
        ): FragmentScenario<F> {
            return launch(F::class.java, fragmentArgs, factory)
        }

        /**
         * Launches a Fragment with given arguments hosted by a [FragmentActivity] using
         * given [FragmentFactory] and waits for it to reach a resumed state.
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmStatic
        @JvmOverloads
        fun <F : Fragment> launch(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle? = null,
            factory: FragmentFactory? = null
        ): FragmentScenario<F> {
            return internalLaunch(fragmentClass, fragmentArgs, factory, /*containerViewId=*/ 0)
        }

        /**
         * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
         * given arguments hosted by an empty [FragmentActivity] and waits for it to reach a
         * resumed state.
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentArgs a bundle to passed into fragment
         * @param factory a fragment factory to use or null to use default factory
         */
        inline fun <reified F : Fragment> launchInContainer(
            fragmentArgs: Bundle? = null,
            factory: FragmentFactory? = null
        ): FragmentScenario<F> {
            return launchInContainer(
                F::class.java,
                fragmentArgs,
                factory)
        }

        /**
         * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
         * given arguments hosted by an empty [FragmentActivity] and waits for it to reach a
         * resumed state.
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmStatic
        @JvmOverloads
        fun <F : Fragment> launchInContainer(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle? = null,
            factory: FragmentFactory? = null
        ): FragmentScenario<F> {
            return internalLaunch(
                fragmentClass,
                fragmentArgs,
                factory,
                /*containerViewId=*/ android.R.id.content)
        }

        @Suppress("MemberVisibilityCanBePrivate") /* synthetic access */
        internal fun <F : Fragment> internalLaunch(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle?,
            factory: FragmentFactory?,
            containerViewId: Int
        ): FragmentScenario<F> {
            val scenario = FragmentScenario(
                fragmentClass, ActivityScenario.launch(EmptyFragmentActivity::class.java)
            )
            scenario.activityScenario.onActivity { activity ->
                if (factory != null) {
                    activity.supportFragmentManager.fragmentFactory = factory
                }
                val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                    Preconditions.checkNotNull(fragmentClass.classLoader),
                    fragmentClass.name,
                    fragmentArgs
                )
                fragment.arguments = fragmentArgs
                activity.supportFragmentManager.commitNow {
                    add(containerViewId, fragment, FRAGMENT_TAG)
                }
            }
            return scenario
        }
    }

    /**
     * Moves Fragment state to a new state.
     *
     * If a new state and current state are the same, this method does nothing. It accepts
     * [State.CREATED], [State.STARTED], [State.RESUMED], and [State.DESTROYED].
     *
     * [State.DESTROYED] is a terminal state. You cannot move to any other state
     * after the Fragment reaches that state.
     *
     * This method cannot be called from the main thread.
     */
    fun moveToState(newState: State): FragmentScenario<F> {
        if (newState == State.DESTROYED) {
            activityScenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
                // Null means the fragment has been destroyed already.
                if (fragment != null) {
                    activity.supportFragmentManager.commitNow(true) {
                        remove(fragment)
                    }
                }
            }
        } else {
            activityScenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
                checkNotNull(
                    fragment,
                    "The fragment has been removed from FragmentManager already."
                )
            }
            activityScenario.moveToState(newState)
        }
        return this
    }

    /**
     * Recreates the host Activity.
     *
     * After this method call, it is ensured that the Fragment state goes back to the same state
     * as its previous state.
     *
     * This method cannot be called from the main thread.
     */
    fun recreate(): FragmentScenario<F> {
        activityScenario.recreate()
        return this
    }

    /**
     * Runs a given `block` on the current Activity's main thread.
     *
     *
     * Note that you should never keep Fragment reference passed into your `block`
     * because it can be recreated at anytime during state transitions.
     *
     * Throwing an exception from `block` makes the host Activity crash. You can
     * inspect the exception in logcat outputs.
     *
     * This method cannot be called from the main thread.
     */
    fun onFragment(block: (fragment: F) -> Unit): FragmentScenario<F> {
        activityScenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag(
                FRAGMENT_TAG
            )
            checkNotNull(
                fragment,
                "The fragment has been removed from FragmentManager already."
            )
            checkState(fragmentClass.isInstance(fragment))
            block(Preconditions.checkNotNull(fragmentClass.cast(fragment)))
        }
        return this
    }
}

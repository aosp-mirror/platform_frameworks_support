<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
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

package androidx.navigation.ui

import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.Toolbar
import androidx.navigation.NavController

/**
 * Sets up a [CollapsingToolbarLayout] and [Toolbar] for use with a [NavController].
 *
 * By calling this method, the title in the Toolbar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The start destination of your navigation graph is considered the only top level
 * destination. On the start destination of your navigation graph, the Toolbar will show
 * the drawer icon if the given `drawerLayout` is non null. On all other destinations,
 * the Toolbar will show the Up button.
 *
 * This method will call [NavController.navigateUp] when the Navigation button is clicked.
 *
 * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the Toolbar.
 * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
 */
fun CollapsingToolbarLayout.setupWithNavController(
    toolbar: Toolbar,
    navController: NavController,
    drawerLayout: DrawerLayout?
) {
    NavigationUI.setupWithNavController(this, toolbar, navController,
        AppBarConfiguration(navController.graph, drawerLayout))
}

/**
 * Sets up a [CollapsingToolbarLayout] and [Toolbar] for use with a [NavController].
 *
 * By calling this method, the title in the Toolbar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The [AppBarConfiguration] you provide controls how the Navigation button is
 * displayed and what action is triggered when the Navigation button is tapped.
 *
 * This method will call [NavController.navigateUp] when the navigation icon is clicked.
 *
 * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the Toolbar.
 * @param configuration Additional configuration options for customizing the behavior of the
 *                      Toolbar
 */
fun CollapsingToolbarLayout.setupWithNavController(
    toolbar: Toolbar,
    navController: NavController,
    configuration: AppBarConfiguration = AppBarConfiguration(navController.graph)
) {
    NavigationUI.setupWithNavController(this, toolbar, navController, configuration)
}
=======
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)

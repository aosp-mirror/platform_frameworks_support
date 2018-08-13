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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Contains a route, its selection state and its capabilities.
 * This is used in DynamicRouteController#OnMemberRoutesChangedListener.
 *
 * @hide TODO unhide this class and updateApi
 */
@RestrictTo(LIBRARY_GROUP)
public final class DynamicMemberRouteDescriptor {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            UNSELECTED,
            UNSELECTING,
            SELECTING,
            SELECTED

    })
    public @interface SelectionState {}

    public static final int UNSELECTED = 0;
    /**
     * After a user unselects a route, it might take some time for a provider to complete
     * the operation. This state is used in this between time. MediaRouter can either
     * block the UI or show the route as unchecked.
     */
    public static final int UNSELECTING = 1;
    /**
     * After a user selects a route, it might take some time for a provider to complete
     * the operation. This state is used in this between time. MediaRouter can either
     * block the UI or show the route as checked.
     */
    public static final int SELECTING = 2;
    public static final int SELECTED = 3;

    MediaRouteDescriptor mMediaRouteDescriptor;
    @SelectionState int mSelectionState;
    boolean mIsUnselectable;
    boolean mIsGroupable;
    boolean mIsTransferable;

    /**
     * Returns this route's {@link MediaRouteDescriptor}. i.e. which route this info is for.
     */
    @NonNull
    public MediaRouteDescriptor getRouteDescriptor() {
        return mMediaRouteDescriptor;
    }

    public @SelectionState int getSelectionState() {
        return mSelectionState;
    }

    /**
     * Returns true if the route can be unselected.
     * <p>
     * For example, a static group has an old build which doesn't support dynamic session.
     * All its members can't be removed.
     * </p>
     * <p>
     * Only applicable to selected/selecting routes.
     * </p>
     */
    public boolean isUnselectable() {
        return mIsUnselectable;
    }

    /**
     * Returns true if the route can be grouped into the dynamic route.
     * <p>
     * Only applicable to unselected/unselecting routes.
     * Note that {@link #isGroupable()} and {@link #isTransferable()} are NOT mutually exclusive.
     * </p>
     */
    public boolean isGroupable() {
        return mIsGroupable;
    }

    /**
     * Returns true if the current dynamic route can be transferred to this route.
     * <p>
     * Only applicable to unselected/unselecting routes.
     * Note that {@link #isGroupable()} and {@link #isTransferable()} are NOT mutually exclusive.
     * </p>
     */
    public boolean isTransferable() {
        return mIsTransferable;
    }

    /**
     * Builder for {@link DynamicMemberRouteDescriptor}
     */
    public static final class  Builder {
        private MediaRouteDescriptor mRouteDescriptor;
        private @SelectionState int mSelectionState = UNSELECTED;
        private boolean mIsUnselectable = false;
        private boolean mIsGroupable = false;
        private boolean mIsTransferable = false;

        /**
         * Copies the properties from the given {@link DynamicMemberRouteDescriptor}
         */
        public Builder(DynamicMemberRouteDescriptor dynamicMemberRouteDescriptor) {
            mRouteDescriptor = dynamicMemberRouteDescriptor.getRouteDescriptor();
            mSelectionState = dynamicMemberRouteDescriptor.getSelectionState();
            mIsUnselectable = dynamicMemberRouteDescriptor.isUnselectable();
            mIsGroupable = dynamicMemberRouteDescriptor.isGroupable();
            mIsTransferable = dynamicMemberRouteDescriptor.isTransferable();
        }

        /**
         * Sets corresponding {@link MediaRouteDescriptor} to this route.
         */
        public Builder setRouteDescriptor(MediaRouteDescriptor routeDescriptor) {
            mRouteDescriptor = routeDescriptor;
            return this;
        }

        /**
         * Sets the selection state of this route within the associated dynamic route.
         */
        public Builder setSelectionState(@SelectionState int state) {
            mSelectionState = state;
            return this;
        }

        /**
         * Sets if this route can be unselected.
         */
        public Builder setIsUnselectable(boolean value) {
            mIsUnselectable = value;
            return this;
        }

        /**
         * Sets if this route can be a selected as a member of the associated dynamic route.
         */
        public Builder setIsGroupable(boolean value) {
            mIsGroupable = value;
            return this;
        }

        /**
         * Sets if the associated dynamic route can be transferred to this route.
         */
        public Builder setIsTransferable(boolean value) {
            mIsTransferable = value;
            return this;
        }

        /**
         * Builds the {@link DynamicMemberRouteDescriptor}.
         */
        public DynamicMemberRouteDescriptor build() {
            DynamicMemberRouteDescriptor descriptor = new DynamicMemberRouteDescriptor();
            descriptor.mMediaRouteDescriptor = this.mRouteDescriptor;
            descriptor.mSelectionState = this.mSelectionState;
            descriptor.mIsUnselectable = this.mIsUnselectable;
            descriptor.mIsGroupable = this.mIsGroupable;
            descriptor.mIsTransferable = this.mIsTransferable;
            return descriptor;
        }
    }
}

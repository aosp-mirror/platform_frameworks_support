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
package androidx.leanback.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

/**
 /**
 * A ClassPresenterBinderSelector selects a {@link PresenterBinder} based on the item's
 * Java class.
 *
 * @param <PresenterT> Type of Presenter.
 * @param <VH> Type of Presenter.ViewHolder.
 */
public final class ClassPresenterBinderSelector<PresenterT extends Presenter,
        VH extends Presenter.ViewHolder> extends PresenterBinderSelector<PresenterT, VH> {

    private final HashMap<Class<?>, Object> mClassMap = new HashMap<Class<?>, Object>();

    /**
     * Sets a presenter to be used for the given class.
     * @param cls The data model class to be rendered.
     * @param binder The PresenterBinder that binds the objects of the given class.
     * @return This ClassPresenterBinderSelector object.
     */
    public @NonNull <ItemT> ClassPresenterBinderSelector addClassPresenterBinder(
            @NonNull Class<ItemT> cls,
            @NonNull PresenterBinder<PresenterT, VH, ItemT> binder) {
        mClassMap.put(cls, binder);
        return this;
    }

    /**
     * Sets a presenter selector to be used for the given class.
     * @param cls The data model class to be rendered.
     * @param binderSelector The presenter selector that finds the right presenter for a given
     *                          class.
     * @return This ClassPresenterBinderSelector object.
     */
    public @NonNull ClassPresenterBinderSelector addClassPresenterBinderSelector(
            @NonNull Class<?> cls,
            @NonNull PresenterBinderSelector<PresenterT, VH> binderSelector) {
        mClassMap.put(cls, binderSelector);
        return this;
    }

    @Override
    public @Nullable PresenterBinder<PresenterT, VH, ?> getPresenterBinder(
            @NonNull Object item) {
        Class<?> cls = item.getClass();
        Object binder;

        do {
            binder = mClassMap.get(cls);
            if (binder instanceof PresenterBinderSelector) {
                PresenterBinder<PresenterT, VH, ?> innerBinder =
                        ((PresenterBinderSelector) binder).getPresenterBinder(item);
                if (innerBinder != null) {
                    return innerBinder;
                }
            }
            cls = cls.getSuperclass();
        } while (binder == null && cls != null);

        return (PresenterBinder<PresenterT, VH, ?>) binder;
    }
}

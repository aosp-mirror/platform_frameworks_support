/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method in a {@link Dao} annotated class as an update method.
 * <p>
 * The implementation of the method will update its parameters in the database if they already
 * exists (checked by primary keys). If they don't already exists, this option will not change the
 * database.
 * <p>
 * All of the parameters of the Update method must either be classes annotated with {@link Entity}
 * or collections/array of it. However if the target entity is specified via {@link #entity()} then
 * the method can contain a single parameter of a Pojo class or collection of a class that
 * will be interpreted as a partial entity.
 *
 * @see Insert
 * @see Delete
 */
@Retention(RetentionPolicy.CLASS)
public @interface Update {

    /**
     * The target entity of the update method.
     * <p>
     * When this is declared the update method must only contain a single parameter. The Pojo class
     * of the parameter must contain a subset of the fields of the target entity along with the its
     * primary keys.
     * <p>
     * By default the target entity is interpreted by the methods parameter.
     *
     * @return the target entity of the update method or none if the method should use the
     *         parameter type entities.
     */
    Class entity() default Object.class;

    /**
     * What to do if a conflict happens.
     * <p>
     * Use {@link OnConflictStrategy#ABORT} (default) to roll back the transaction on conflict.
     * Use {@link OnConflictStrategy#REPLACE} to replace the existing rows with the new rows.
     * Use {@link OnConflictStrategy#IGNORE} to keep the existing rows.
     *
     * @return How to handle conflicts. Defaults to {@link OnConflictStrategy#ABORT}.
     */
    @OnConflictStrategy
    int onConflict() default OnConflictStrategy.ABORT;
}

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

/**
 * Marks a method in a {@link Dao} annotated class as an update method.
 * <p>
 * The method implementation generated will update parameter entities in the database if they
 * already exist. In an entity has a primary key that isn't already present in the database,
 * the method won't insert it.
 * <p>
 * All of the parameters of the Update method must either be classes annotated with {@link Entity}
 * or a {@link java.util.Collection} or {@code array} containing them.
 * <p>
 * The method may be declared to return any of:
 * <ul
 *   <li>{@code void}
 *   <li>{@code long}, the primary key of the updated single entity parameter
 *   <li>{@code Long}, boxed version of above
 *   <li>{@code long[]}, the ordered array of the updated entities' primary keys
 *   <li>{@code List<Long>}, the ordered list of the updated entities' primary keys
 * </ul>
 * <p>
 * When using the Guava plugin, the method may also be declared to return a
 * {@link com.google.common.util.concurrent.ListenableFuture} containing {@code Long}, {@link Void},
 * or {@code List<Long>}.
 *
 * @see Delete
 * @see Insert
 * @see Query
 */
public @interface Update {
    /**
     * What to do if a conflict happens.
     * @see <a href="https://sqlite.org/lang_conflict.html">SQLite conflict documentation</a>
     *
     * @return How to handle conflicts. Defaults to {@link OnConflictStrategy#ABORT}.
     */
    @OnConflictStrategy
    int onConflict() default OnConflictStrategy.ABORT;
}

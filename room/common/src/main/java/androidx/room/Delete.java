/*
 * Copyright (C) 2016 The Android Open Source Project
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Dao} annotated class as a delete method.
 * <p>
 * The method implementation generated will delete its parameter entities from the database.
 * <p>
 * The declared method may have one or more parameters. Each parameter must be either a class
 * annotated with {@link Entity}, a {@link java.util.Collection} of that {@code Entity}, or
 * an array of that {@code Entity}. A varargs parameter of the {@code Entity} is also accepted.
 * <p>
 * The implementation of the method will delete all parameters from the database, by primary key.
 * <p>
 * The method may be declared to return any of:
 * <ul
 *   <li>{@code void}
 *   <li>{@code long}, the primary key of the deleted single entity parameter
 *   <li>{@code Long}, boxed version of above
 *   <li>{@code long[]}, the ordered array of deleted entities' primary keys
 *   <li>{@code List<Long>}, the ordered list of deleted entities' primary keys
 * </ul>
 * <p>
 * When using the Guava plugin, the method may also be declared to return a
 * {@link com.google.common.util.concurrent.ListenableFuture} containing {@code Long}, {@link Void},
 * or {@code List<Long>}.
 *
 * Example:
 * <pre>
 * {@literal @}Dao
 * public interface MyDao {
 *     {@literal @}Delete
 *     public void deleteUsers(User... users);
 *     {@literal @}Delete
 *     public List&lt;Long&gt; deleteAll(User user1, User user2);
 *     {@literal @}Delete
 *     public ListenableFuture&lt;Void&gt; deleteWithFriends(User user, List&lt;User&gt; friends);
 * }
 * </pre>
 *
 * @see Insert
 * @see Query
 * @see Update
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Delete {
}

/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated method should only be called on a worker thread.
 * <pre><code>
 *  &#64;WorkerThread
 *  FilterResults performFiltering(CharSequence constraint);
 * </code></pre>
 *
 * <p>If the annotated element is a class, then all methods in the class should be called
 * on a worker thread. </p>
 *
 * <pre><code>
 *  &#64;WorkerThread
 *  public class Foo { ... }
 * </code></pre>
 *
 * <p>When the class is annotated, but one of the methods has another threading annotation such as
 * {@link MainThread}, the method annotation takes precedence. In the following example,
 * <code>onResult()</code> should be called on the main thread.</p>
 *
 * <pre><code>
 *  &#64;WorkerThread
 *  public class Foo {
 *      &#64;MainThread
 *      void onResult(String result) { ... }
 *  }
 * </code></pre>
 *
 * <p>Multiple threading annotations can be combined. Following example illustrates that,
 * <code>isEmpty()</code> can be called on the worker thread or the main thread.
 * It's safe for <code>saveUser()</code> to invoke <code>isEmpty()</code>, whereas it's not safe
 * for <code>isEmpty()</code> to invoke <code>saveUser()</code>.
 * </p>
 *
 * <pre><code>
 *  public class Foo {
 *      &#64;WorkerThread
 *      void saveUser(User user) { ... }
 *
 *      &#64;WorkerThread
 *      &#64;UiThread
 *      boolean isEmpty(String value) { ... }
 *  }
 * </code></pre>
 */
@Documented
@Retention(CLASS)
@Target({METHOD,CONSTRUCTOR,TYPE,PARAMETER})
public @interface WorkerThread {
}
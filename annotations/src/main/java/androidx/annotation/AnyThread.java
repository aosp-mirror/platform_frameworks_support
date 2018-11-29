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
 * Denotes that the annotated method can be called from any thread (e.g. it is "thread safe".)
 * <p>
 * The main purpose of this annotation is to indicate that you believe a method can be called
 * from any thread; static tools can then check that nothing you call from within this method
 * or class have more strict threading requirements.
 * <pre><code>
 *  &#64;AnyThread
 *  public void deliverResult(D data) { ... }
 * </code></pre>
 *
 * <p>If the annotated element is a class, then all methods in the class can be called
 * from any thread. </p>
 *
 * <pre><code>
 *  &#64;AnyThread
 *  public class Foo { ... }
 * </code></pre>
 *
 * <p>When the class is annotated, but one of the methods has another threading annotation such as
 * {@link MainThread}, the method annotation takes precedence. In the following example,
 * <code>onResult()</code> should only be called on the main thread.</p>
 *
 * <pre><code>
 *  &#64;AnyThread
 *  public class Foo {
 *      &#64;MainThread
 *      void onResult(String result) { ... }
 *  }
 * </code></pre>
 *
 * <p>Multiple threading annotations can be combined. Following example illustrates that,
 * <code>saveUser()</code> can be called on a worker thread or the main thread.
 * It's safe for <code>saveUser()</code> to invoke <code>isEmpty()</code>, whereas it's not safe
 * for <code>isEmpty()</code> to invoke <code>saveUser()</code>.
 * </p>
 *
 * <pre><code>
 *  public class Foo {
 *      &#64;WorkerThread
 *      &#64;MainThread
 *      void saveUser(User user) { ... }
 *
 *      &#64;AnyThread
 *      boolean isEmpty(String value) { ... }
 *  }
 * </code></pre>
 */
@Documented
@Retention(CLASS)
@Target({METHOD,CONSTRUCTOR,TYPE,PARAMETER})
public @interface AnyThread {
}

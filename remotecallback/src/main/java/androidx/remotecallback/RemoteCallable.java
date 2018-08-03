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

package androidx.remotecallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to tag a method as callable using {@link CallbackReceiver#createRemoteCallback}.
 * <p>
 * This is only valid on methods on concrete classes that implement {@link CallbackReceiver}.
 * <p>
 * At compile time Methods tagged with {@link RemoteCallable} have hooks generated for
 * them. The vast majority of the calls are done through generated code directly,
 * so everything except for class names can be optimized/obfuscated. Given that
 * remote callbacks are only accessible on platform components such as receivers
 * and providers, they are already generally not able to be obfuscated.
 *
 * When {@link createRemoteCallback} is called, all of the parameters will be validated.
 * If there is an incorrect number or a type mismatch, then the callback will not
 * be created and a `RuntimeException` will be generated. It would be nice to do
 * these checks at compile time, but the java annotation processor API makes that
 * very difficult.
 *
 * @see CallbackReceiver#createRemoteCallback
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RemoteCallable {
}

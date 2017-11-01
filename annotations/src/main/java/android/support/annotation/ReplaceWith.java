/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a code fragment that can be used to replace a deprecated function, property or class.
 * Tools such as IDEs can automatically apply the replacements specified through this annotation.
 *
 * @property expression the replacement expression. The replacement expression is interpreted in
 * the context of the symbol being used, and can reference members of enclosing classes etc.
 * For function calls, the replacement expression may contain argument names of the deprecated
 * function, which will be substituted with actual parameters used in the call being updated.
 * The imports used in the file containing the deprecated function or property are NOT accessible;
 * if the replacement expression refers on any of those imports, they need to be specified
 * explicitly in the [imports] parameter.
 * @property imports the qualified names that need to be imported in order for the references in the
 *     replacement expression to be resolved correctly.
 */
@Target({})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface ReplaceWith {
    String expression() default "";
    String[] imports() default {};
}

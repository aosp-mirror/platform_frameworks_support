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

package androidx.remotecallback.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

public class CallbackReceiver {

    private static final String RUN = "run";

    private final ProcessingEnvironment mEnv;
    private final Element mElement;
    private final String mClsName;
    private final ArrayList<CallableMethod> mMethods = new ArrayList<>();
    private final Messager mMessager;

    public CallbackReceiver(Element c, ProcessingEnvironment env,
            Messager messager) {
        mEnv = env;
        mElement = c;
        mClsName = c.toString();
        mMessager = messager;
    }

    public void addMethod(Element element) {
        for (CallableMethod method: mMethods) {
            if (method.getName().equals(element.getSimpleName().toString())) {
                mMessager.printMessage(Diagnostic.Kind.ERROR,
                        "Multiple methods named " + element.getSimpleName());
                return;
            }
        }
        mMethods.add(new CallableMethod(mClsName, element, mEnv));
    }

    public void finish(ProcessingEnvironment env, Messager messager) {
        if (mMethods.size() == 0) {
            messager.printMessage(Diagnostic.Kind.ERROR, "No methods found for " + mClsName);
            return;
        }
        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(findInitClass(mElement))
                .addSuperinterface(ClassName.get("java.lang", "Runnable"))
                .addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder runBuilder = MethodSpec
                .methodBuilder(RUN)
                .addModifiers(Modifier.PUBLIC);
        for (CallableMethod method: mMethods) {
            method.addMethods(genClass, runBuilder, env, messager);
        }
        genClass.addMethod(runBuilder.build());
        try {
            TypeSpec typeSpec = genClass.build();
            String pkg = getPkg(mElement);
            JavaFile.builder(pkg, typeSpec)
                    .build()
                    .writeTo(mEnv.getFiler());
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Exception writing " + e);
        }
    }

    private String findInitClass(Element element) {
        return String.format("%sInitializer", element.getSimpleName());
    }

    private String getPkg(Element s) {
        String pkg = mEnv.getElementUtils().getPackageOf(s).toString();
        return pkg;
    }
}

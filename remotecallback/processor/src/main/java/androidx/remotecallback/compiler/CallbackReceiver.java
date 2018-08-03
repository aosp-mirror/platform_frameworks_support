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
import com.squareup.javapoet.FieldSpec;
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

/**
 * Holder class that is created for each class instance that is a
 * CallbackReceiver and has methods tagged with @RemoteCallable.
 */
public class CallbackReceiver {

    private static final String RESET = "reset";
    private static final String GET_METHOD = "getMethod";
    private static final String GET_ARGUMENTS = "getArguments";

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

    /**
     * Adds a method tagged with @RemoteCallable to this receiver.
     */
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

    /**
     * Generates the code to handle creating and executing callbacks. The code
     * is assembled in one class that implements runnable that when run,
     * registers all of the CallbackHandlers.
     */
    public void finish(ProcessingEnvironment env, Messager messager) {
        if (mMethods.size() == 0) {
            messager.printMessage(Diagnostic.Kind.ERROR, "No methods found for " + mClsName);
            return;
        }
        ClassName bundle = ClassName.get("android.os", "Bundle");
        ClassName string = ClassName.get("java.lang", "String");
        FieldSpec currentMethodField = FieldSpec.builder(string, "mMethodCalled", Modifier.PRIVATE)
                .build();
        FieldSpec bundleField = FieldSpec.builder(bundle, "mMethodArgs", Modifier.PRIVATE)
                .build();
        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(findInitClass(mElement))
                .superclass(TypeName.get(mElement.asType()))
                .addSuperinterface(ClassName.get("androidx.remotecallback.CallbackHandlerRegistry",
                        "RemoteCallStub"))
                .addField(currentMethodField)
                .addField(bundleField)
                .addModifiers(Modifier.PUBLIC);

        genClass.addMethod(MethodSpec.methodBuilder(RESET)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("mMethodCalled = null")
                .addStatement("mMethodArgs = new $L()", bundle)
                .build());
        genClass.addMethod(MethodSpec.methodBuilder(GET_METHOD)
                .addModifiers(Modifier.PUBLIC)
                .returns(string)
                .addStatement("return mMethodCalled")
                .build());
        genClass.addMethod(MethodSpec.methodBuilder(GET_ARGUMENTS)
                .addModifiers(Modifier.PUBLIC)
                .returns(bundle)
                .addStatement("return mMethodArgs")
                .build());

        MethodSpec.Builder runBuilder = MethodSpec
                .constructorBuilder()
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

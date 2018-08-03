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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

public class CallableMethod {

    private static final String BYTE = "byte";
    private static final String CHAR = "char";
    private static final String SHORT = "short";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BOOLEAN = "boolean";

    private static final String STRING = "java.lang.String";
    private static final String URI = "android.net.Uri";

    private static final String OBJ_BYTE = "java.lang.Byte";
    private static final String CHARACTER = "java.lang.Character";
    private static final String OBJ_SHORT = "java.lang.Short";
    private static final String INTEGER = "java.lang.Integer";
    private static final String OBJ_LONG = "java.lang.Long";
    private static final String OBJ_FLOAT = "java.lang.Float";
    private static final String OBJ_DOUBLE = "java.lang.Double";
    private static final String OBJ_BOOLEAN = "java.lang.Boolean";
    private static final String RELAY_PREFIX = "androidx.remotecallback:";

    private final Element mElement;
    private final ProcessingEnvironment mEnv;
    private final ArrayList<String> mTypes = new ArrayList<>();
    private final ArrayList<String> mNames = new ArrayList<>();
    private final String mClsName;

    public CallableMethod(String name, Element element,
            ProcessingEnvironment env) {
        mClsName = name;
        mElement = element;
        mEnv = env;
        init();
    }

    public String getName() {
        return mElement.getSimpleName().toString();
    }

    private void init() {
        ExecutableType type = (ExecutableType) mElement.asType();
        ExecutableElement element = (ExecutableElement) mElement;
        List<? extends TypeMirror> types = type.getParameterTypes();
        List<? extends VariableElement> vars = element.getParameters();
        for (int i = 0; i < types.size(); i++) {
            mTypes.add(types.get(i).toString());
            mNames.add(vars.get(i).getSimpleName().toString());
        }
    }

    public void addMethods(TypeSpec.Builder genClass, MethodSpec.Builder runBuilder,
            ProcessingEnvironment env, Messager messager) {
        // Validate types
        for (int i = 0; i < mTypes.size(); i++) {
            if (checkType(mTypes.get(i), messager)) {
                return;
            }
        }

        CodeBlock.Builder code = CodeBlock.builder();
        ClassName callbackHandlerRegistry = ClassName.get("androidx.remotecallback",
                "CallbackHandlerRegistry");
        ClassName callbackHandler = ClassName.get("androidx.remotecallback",
                "CallbackHandlerRegistry.CallbackHandler");
        ClassName bundle = ClassName.get("android.os", "Bundle");
        code.add("$L.registerCallbackHandler($L.class, $S, ", callbackHandlerRegistry, mClsName,
                mElement.getSimpleName().toString());
        code.beginControlFlow("new $L<$L>()", callbackHandler, mClsName);
        code.beginControlFlow("  public void executeCallback($L receiver, $L args)", mClsName,
                bundle);
        StringBuilder r = new StringBuilder();
        r.append("receiver.");
        r.append(mElement.getSimpleName());
        r.append("(");
        for (int i = 0; i < mNames.size(); i++) {
            code.addStatement("$L p" + i, mTypes.get(i));
            String key = getBundleKey(i);
            code.beginControlFlow("if (args.get($L) instanceof String "
                    + "&& args.getString($L).startsWith($S))", key, key, RELAY_PREFIX);
            code.addStatement("String key = args.getString($S).substring($L)",
                    RELAY_PREFIX, RELAY_PREFIX.length());
            code.addStatement("p$L = $L", i, getBundleParam(mTypes.get(i), "key"));
            code.nextControlFlow("else");
            code.addStatement("p$L = $L", i, getBundleParam(mTypes.get(i), i));
            code.endControlFlow();
            if (i != 0) {
                r.append(", ");
            }
            r.append("p" + i);
        }
        r.append(")");
        code.addStatement(r.toString());
        code.endControlFlow();
        code.beginControlFlow("public $L assembleArguments(Object... args)", bundle);

        code.beginControlFlow("if (args.length != $L)", mNames.size());
        code.addStatement("throw new IllegalArgumentException($S)", mElement.getSimpleName() + " takes " + mNames.size() + " arguments");
        code.endControlFlow();

        code.addStatement("$L b = new $L()", bundle, bundle);
        for (int i = 0; i < mNames.size(); i++) {
            code.beginControlFlow("if (args[$L] != null)", i);
            code.addStatement("b.put$L($L, ($L) args[$L])", getTypeMethod(mTypes.get(i)),
                    getBundleKey(i), mTypes.get(i), i);
            code.endControlFlow();
        }
        code.addStatement("return b");
        code.endControlFlow();

        code.endControlFlow();
        code.add(");\n");
        runBuilder.addCode(code.build());
    }

    private String getBundleParam(String type, int index) {
        String key = getBundleKey(index);
        return getBundleParam(type, key);
    }

    private String getBundleParam(String type, String key) {
        switch (type) {
            case BYTE:
                return "args.getByte(" + key + ", (byte) 0)";
            case CHAR:
                return "args.getChar(" + key + ", (char) 0)";
            case SHORT:
                return "args.getShort(" + key + ", (short) 0)";
            case INT:
                return "args.getInt(" + key + ", 0)";
            case LONG:
                return "args.getLong(" + key + ", 0)";
            case FLOAT:
                return "args.getFloat(" + key + ", 0f)";
            case DOUBLE:
                return "args.getDouble(" + key + ", 0.0)";
            case BOOLEAN:
                return "args.getBoolean(" + key + ", false)";
        }
        return "args.containsKey(" + key + ") ? (" + type + ") args.get"
                + getTypeMethod(type) + "(" + key + ") : null";
    }

    private String getTypeMethod(String type) {
        switch (type) {
            case BYTE:
                return "Byte";
            case CHAR:
                return "Char";
            case SHORT:
                return "Short";
            case INT:
                return "Int";
            case LONG:
                return "Long";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case BOOLEAN:
                return "Boolean";
            case STRING:
                return "String";
            case URI:
                return "Parcelable";
            case OBJ_BYTE:
                return "Byte";
            case CHARACTER:
                return "Char";
            case OBJ_SHORT:
                return "Short";
            case INTEGER:
                return "Int";
            case OBJ_LONG:
                return "Long";
            case OBJ_FLOAT:
                return "Float";
            case OBJ_DOUBLE:
                return "Double";
            case OBJ_BOOLEAN:
                return "Boolean";
        }
        throw new RuntimeException("Invalid type " + type);
    }

    public String getBundleKey(int index) {
        return "\"p" + index + "\"";
    }

    private boolean checkType(String type, Messager messager) {
        switch (type) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case URI:
            case OBJ_BYTE:
            case CHARACTER:
            case OBJ_SHORT:
            case INTEGER:
            case OBJ_LONG:
            case OBJ_FLOAT:
            case OBJ_DOUBLE:
            case OBJ_BOOLEAN:
                return false;
            default:
                return true;
        }
    }
}

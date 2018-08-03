package com.android.xsdc.descriptor;

class IntegerTypeDescriptor extends SimpleTypeDescriptor {
    IntegerTypeDescriptor(boolean isList) {
        super("int", "java.lang.Integer", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.lang.Integer.parseInt(%s)", varName);
    }
}

package com.android.xsdc.descriptor;

class BooleanTypeDescriptor extends SimpleTypeDescriptor {
    BooleanTypeDescriptor(boolean isList) {
        super("boolean", "java.lang.Boolean", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.lang.Boolean.parseBoolean(%s)", varName);
    }
}

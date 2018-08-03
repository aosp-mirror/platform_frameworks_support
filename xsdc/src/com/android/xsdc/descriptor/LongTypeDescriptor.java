package com.android.xsdc.descriptor;

class LongTypeDescriptor extends SimpleTypeDescriptor {
    LongTypeDescriptor(boolean isList) {
        super("long", "java.lang.Long", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.lang.Long.parseLong(%s)", varName);
    }
}

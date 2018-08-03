package com.android.xsdc.descriptor;

class ShortTypeDescriptor extends SimpleTypeDescriptor {
    ShortTypeDescriptor(boolean isList) {
        super("short", "java.lang.Short", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.lang.Short.parseShort(%s)", varName);
    }
}

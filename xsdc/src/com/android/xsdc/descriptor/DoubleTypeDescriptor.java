package com.android.xsdc.descriptor;

class DoubleTypeDescriptor extends SimpleTypeDescriptor {
    DoubleTypeDescriptor(boolean isList) {
        super("double", "java.lang.Double", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.lang.Double.parseDouble(%s)", varName);
    }
}

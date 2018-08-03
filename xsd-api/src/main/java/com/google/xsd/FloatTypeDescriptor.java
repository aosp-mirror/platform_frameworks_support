package com.google.xsd;

class FloatTypeDescriptor extends SimpleTypeDescriptor {
    FloatTypeDescriptor(boolean isList) {
        super("float", "java.lang.Float", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.lang.Float.parseFloat(%s)", varName);
    }
}

package com.android.xsdc.descriptor;

class HexTypeDescriptor extends SimpleTypeDescriptor {
    HexTypeDescriptor(boolean isList) {
        super("java.math.BigInteger", "java.math.BigInteger", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("new java.math.BigInteger(%s, 16)", varName);
    }
}

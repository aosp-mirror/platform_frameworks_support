package com.android.xsdc.descriptor;

class BigIntegerTypeDescriptor extends SimpleTypeDescriptor {
    BigIntegerTypeDescriptor(boolean isList) {
        super("java.math.BigInteger", "java.math.BigInteger", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("new java.math.BigInteger(%s)", varName);
    }
}

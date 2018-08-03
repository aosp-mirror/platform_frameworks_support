package com.android.xsdc.descriptor;

class BigDecimalTypeDescriptor extends SimpleTypeDescriptor {
    BigDecimalTypeDescriptor(boolean isList) {
        super("java.math.BigDecimal", "java.math.BigDecimal", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("new java.math.BigDecimal(%s)", varName);
    }
}

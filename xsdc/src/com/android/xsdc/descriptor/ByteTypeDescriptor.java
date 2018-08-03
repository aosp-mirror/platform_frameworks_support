package com.android.xsdc.descriptor;

class ByteTypeDescriptor extends SimpleTypeDescriptor {
    ByteTypeDescriptor(boolean isList) {
        super("byte", "java.lang.Byte", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.lang.Byte.parseByte(%s)", varName);
    }
}

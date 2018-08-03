package com.google.xsd;

class Base64TypeDescriptor extends SimpleTypeDescriptor {
    Base64TypeDescriptor(boolean isList) {
        super("byte[]", "byte[]", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("java.util.Base64.getDecoder().decode(%s)", varName);
    }
}

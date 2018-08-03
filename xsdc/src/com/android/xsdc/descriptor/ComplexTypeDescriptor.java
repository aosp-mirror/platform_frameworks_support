package com.android.xsdc.descriptor;

public class ComplexTypeDescriptor extends TypeDescriptor {
    public ComplexTypeDescriptor(String name) {
        super(name);
    }

    String getParsingExpression() {
        return String.format("%s value = %s.read(parser);", name, name);
    }

    boolean isSimple() {
        return false;
    }
}

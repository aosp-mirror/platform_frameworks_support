package com.google.xsd;

class ComplexTypeDescriptor extends TypeDescriptor {
    ComplexTypeDescriptor(String name) {
        super(name);
    }

    String getParsingExpression() {
        return String.format("%s value = %s.read(parser);", name, name);
    }

    boolean isSimple() {
        return false;
    }
}

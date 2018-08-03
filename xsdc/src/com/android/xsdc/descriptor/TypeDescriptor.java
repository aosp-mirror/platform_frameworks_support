package com.android.xsdc.descriptor;

public abstract class TypeDescriptor {
    protected String name;
    protected String nullableName;

    TypeDescriptor(String name) {
        this.name = this.nullableName = name;
    }

    TypeDescriptor(String name, String nullableName) {
        this.name = name;
        this.nullableName = nullableName;
    }

    String getFullName() {
        return name;
    }

    String getNullableFullName() {
        return nullableName;
    }

    abstract String getParsingExpression();

    abstract boolean isSimple();
}

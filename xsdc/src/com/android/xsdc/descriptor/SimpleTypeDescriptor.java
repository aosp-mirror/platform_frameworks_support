package com.android.xsdc.descriptor;

abstract class SimpleTypeDescriptor extends TypeDescriptor {
    private boolean list;

    SimpleTypeDescriptor(String name, String nullableName, boolean isList) {
        super(name, nullableName);
        this.list = isList;
    }

    boolean isList() {
        return list;
    }

    void convertToListType() {
        this.list = true;
    }

    @Override
    String getFullName() {
        return list ? String.format("java.util.List<%s>", nullableName) : name;
    }

    @Override
    String getNullableFullName() {
        return list ? String.format("java.util.List<%s>", nullableName) : nullableName;
    }

    String getParsingExpression() {
        StringBuilder expression = new StringBuilder();
        if (list) {
            expression.append(String.format("%s value = new java.util.ArrayList<>();\n", getFullName()));
            expression.append("for (String token : raw.split(\"\\\\s+\")) {\n");
            expression.append(String.format("\tvalue.add(%s);\n", getRawParsingExpression("token")));
            expression.append("}");
        } else {
            expression.append(String.format("%s value = %s;\n", this.name, getRawParsingExpression("raw")));
        }
        return expression.toString();
    }

    boolean isSimple() {
        return true;
    }

    abstract protected String getRawParsingExpression(String varName);
}

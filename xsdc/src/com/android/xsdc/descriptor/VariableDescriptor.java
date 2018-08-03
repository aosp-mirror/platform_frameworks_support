package com.android.xsdc.descriptor;

public class VariableDescriptor {
    private TypeDescriptor type;
    private String name;
    private String xmlName;
    private boolean nullable;
    private boolean multiple;

    public VariableDescriptor(TypeDescriptor type, String name, String xmlName, boolean nullable, boolean multiple) {
        this.type = type;
        this.name = name;
        this.xmlName = xmlName;
        this.nullable = nullable;
        this.multiple = multiple;
    }

    TypeDescriptor getType() {
        return type;
    }

    String getFullTypeName() {
        if (multiple) {
            return String.format("java.util.List<%s>", type.getNullableFullName());
        } else if (nullable) {
            return type.getNullableFullName();
        } else {
            return type.getFullName();
        }
    }

    String getName() {
        return name;
    }

    String getXmlName() {
        return xmlName;
    }

    boolean isMultiple() {
        return multiple;
    }
}

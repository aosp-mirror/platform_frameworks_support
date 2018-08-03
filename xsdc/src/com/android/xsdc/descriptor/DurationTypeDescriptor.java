package com.android.xsdc.descriptor;

class DurationTypeDescriptor extends SimpleTypeDescriptor {
    DurationTypeDescriptor(boolean isList) {
        super("javax.xml.datatype.Duration", "javax.xml.datatype.Duration", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("javax.xml.datatype.DatatypeFactory.newInstance().newDuration(%s)", varName);
    }
}

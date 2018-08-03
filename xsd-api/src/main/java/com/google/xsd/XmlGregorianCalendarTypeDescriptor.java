package com.google.xsd;

class XmlGregorianCalendarTypeDescriptor extends SimpleTypeDescriptor {
    XmlGregorianCalendarTypeDescriptor(boolean isList) {
        super("javax.xml.datatype.XMLGregorianCalendar", "javax.xml.datatype.XMLGregorianCalendar", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return String.format("javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(%s)", varName);
    }
}

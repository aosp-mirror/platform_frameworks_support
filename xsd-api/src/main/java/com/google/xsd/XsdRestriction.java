package com.google.xsd;

class XsdRestriction extends XsdSimpleType {
    private XsdTypeReferrer base;

    XsdRestriction(String name, XsdTypeReferrer base) {
        super(name);
        this.base = base;
    }

    XsdTypeReferrer getBase() {
        return base;
    }
}

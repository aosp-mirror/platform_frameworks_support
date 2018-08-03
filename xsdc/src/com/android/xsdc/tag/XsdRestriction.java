package com.android.xsdc.tag;

public class XsdRestriction extends XsdSimpleType {
    private XsdTypeReferrer base;

    public XsdRestriction(String name, XsdTypeReferrer base) {
        super(name);
        this.base = base;
    }

    public XsdTypeReferrer getBase() {
        return base;
    }
}

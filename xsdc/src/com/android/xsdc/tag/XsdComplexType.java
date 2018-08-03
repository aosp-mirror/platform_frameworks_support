package com.android.xsdc.tag;

import java.util.ArrayList;
import java.util.List;

public abstract class XsdComplexType extends XsdType {
    private XsdTypeReferrer base;
    private List<XsdAttribute> attributes = new ArrayList<>();

    XsdComplexType(String name, XsdTypeReferrer base) {
        super(name);
        this.base = base;
    }

    public XsdTypeReferrer getBase() {
        return base;
    }

    public List<XsdAttribute> getAttributes() {
        return attributes;
    }
}

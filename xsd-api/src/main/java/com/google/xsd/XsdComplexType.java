package com.google.xsd;

import java.util.ArrayList;
import java.util.List;

abstract class XsdComplexType extends XsdType {
    private XsdTypeReferrer base;
    private List<XsdAttribute> attributes = new ArrayList<>();

    XsdComplexType(String name, XsdTypeReferrer base) {
        super(name);
        this.base = base;
    }

    XsdTypeReferrer getBase() {
        return base;
    }

    List<XsdAttribute> getAttributes() {
        return attributes;
    }
}

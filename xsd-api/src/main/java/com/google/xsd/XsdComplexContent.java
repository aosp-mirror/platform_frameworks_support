package com.google.xsd;

import java.util.ArrayList;
import java.util.List;

class XsdComplexContent extends XsdComplexType {
    private List<XsdElement> elements = new ArrayList<>();

    XsdComplexContent(String name, XsdTypeReferrer base) {
        super(name, base);
    }

    List<XsdElement> getElements() {
        return elements;
    }
}

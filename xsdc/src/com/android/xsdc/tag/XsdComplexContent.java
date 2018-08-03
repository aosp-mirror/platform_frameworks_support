package com.android.xsdc.tag;

import java.util.ArrayList;
import java.util.List;

public class XsdComplexContent extends XsdComplexType {
    private List<XsdElement> elements = new ArrayList<>();

    public XsdComplexContent(String name, XsdTypeReferrer base) {
        super(name, base);
    }

    public List<XsdElement> getElements() {
        return elements;
    }
}

package com.google.xsd;

import java.util.ArrayList;
import java.util.List;

class XsdUnion extends XsdSimpleType {
    private List<XsdTypeReferrer> memberTypes;

    XsdUnion(String name, List<XsdTypeReferrer> memberTypes) {
        super(name);
        this.memberTypes = memberTypes;
    }

    List<XsdTypeReferrer> getMemberTypes() {
        if (memberTypes == null) {
            memberTypes = new ArrayList<>();
        }
        return memberTypes;
    }
}

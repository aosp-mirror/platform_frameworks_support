package com.android.xsdc.tag;

import java.util.ArrayList;
import java.util.List;

public class XsdUnion extends XsdSimpleType {
    private List<XsdTypeReferrer> memberTypes;

    public XsdUnion(String name, List<XsdTypeReferrer> memberTypes) {
        super(name);
        this.memberTypes = memberTypes;
    }

    public List<XsdTypeReferrer> getMemberTypes() {
        if (memberTypes == null) {
            memberTypes = new ArrayList<>();
        }
        return memberTypes;
    }
}

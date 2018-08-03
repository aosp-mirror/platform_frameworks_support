package com.android.xsdc.tag;

public class XsdList extends XsdSimpleType {
    private XsdTypeReferrer itemType;

    public XsdList(String name, XsdTypeReferrer itemType) {
        super(name);
        this.itemType = itemType;
    }

    public XsdTypeReferrer getItemType() {
        return itemType;
    }
}

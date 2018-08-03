package com.google.xsd;

class XsdList extends XsdSimpleType {
    private XsdTypeReferrer itemType;

    XsdList(String name, XsdTypeReferrer itemType) {
        super(name);
        this.itemType = itemType;
    }

    XsdTypeReferrer getItemType() {
        return itemType;
    }
}

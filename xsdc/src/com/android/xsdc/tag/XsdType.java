package com.android.xsdc.tag;

public abstract class XsdType extends XsdTag {
    private String name;

    XsdType() {
        super();
    }

    XsdType(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

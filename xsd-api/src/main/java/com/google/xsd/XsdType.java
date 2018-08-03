package com.google.xsd;

abstract class XsdType extends XsdTag {
    private String name;

    XsdType() {
        super();
    }

    XsdType(String name) {
        super();
        this.name = name;
    }

    String getName() {
        return name;
    }
}

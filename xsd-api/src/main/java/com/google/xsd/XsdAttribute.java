package com.google.xsd;

import javax.xml.namespace.QName;

class XsdAttribute extends XsdTag {
    private String name;
    private QName ref;
    private XsdTypeReferrer type;
    private boolean nullable;

    XsdAttribute(QName ref, boolean nullable) {
        super();
        this.ref = ref;
        this.nullable = nullable;
    }

    XsdAttribute(String name, XsdTypeReferrer type, boolean nullable) {
        super();
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    String getName() {
        return name;
    }

    QName getRef() {
        return ref;
    }

    XsdTypeReferrer getType() {
        return type;
    }

    boolean isNullable() {
        return nullable;
    }
}

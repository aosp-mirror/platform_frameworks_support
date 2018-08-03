package com.google.xsd;

import javax.xml.namespace.QName;

class XsdElement extends XsdTag {
    private String name;
    private QName ref;
    private XsdTypeReferrer type;
    private boolean nullable;
    private boolean multiple;

    XsdElement(String name, XsdTypeReferrer type, boolean nullable, boolean multiple) {
        super();
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.multiple = multiple;
    }

    XsdElement(QName ref, boolean nullable, boolean multiple) {
        super();
        this.ref = ref;
        this.nullable = nullable;
        this.multiple = multiple;
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

    boolean isMultiple() {
        return multiple;
    }
}

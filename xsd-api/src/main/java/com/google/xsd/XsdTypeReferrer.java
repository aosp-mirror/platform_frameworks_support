package com.google.xsd;

import javax.xml.namespace.QName;

class XsdTypeReferrer {
    private QName ref;
    private XsdType value;

    XsdTypeReferrer(QName ref) {
        this.ref = ref;
    }

    XsdTypeReferrer(XsdType value) {
        this.value = value;
    }

    QName getRef() {
        return ref;
    }

    XsdType getValue() {
        return value;
    }
}

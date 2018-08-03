package com.android.xsdc.tag;

import javax.xml.namespace.QName;

public class XsdTypeReferrer {
    private QName ref;
    private XsdType value;

    public XsdTypeReferrer(QName ref) {
        this.ref = ref;
    }

    public XsdTypeReferrer(XsdType value) {
        this.value = value;
    }

    public QName getRef() {
        return ref;
    }

    public XsdType getValue() {
        return value;
    }
}

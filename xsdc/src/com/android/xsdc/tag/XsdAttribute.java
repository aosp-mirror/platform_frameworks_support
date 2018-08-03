package com.android.xsdc.tag;

import javax.xml.namespace.QName;

public class XsdAttribute extends XsdTag {
    private String name;
    private QName ref;
    private XsdTypeReferrer type;
    private boolean nullable;

    public XsdAttribute(QName ref, boolean nullable) {
        super();
        this.ref = ref;
        this.nullable = nullable;
    }

    public XsdAttribute(String name, XsdTypeReferrer type, boolean nullable) {
        super();
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    public String getName() {
        return name;
    }

    public QName getRef() {
        return ref;
    }

    public XsdTypeReferrer getType() {
        return type;
    }

    public boolean isNullable() {
        return nullable;
    }
}

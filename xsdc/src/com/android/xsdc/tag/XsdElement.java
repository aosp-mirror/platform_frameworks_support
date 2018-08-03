package com.android.xsdc.tag;

import javax.xml.namespace.QName;

public class XsdElement extends XsdTag {
    private String name;
    private QName ref;
    private XsdTypeReferrer type;
    private boolean nullable;
    private boolean multiple;

    public XsdElement(String name, XsdTypeReferrer type, boolean nullable, boolean multiple) {
        super();
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.multiple = multiple;
    }

    public XsdElement(QName ref, boolean nullable, boolean multiple) {
        super();
        this.ref = ref;
        this.nullable = nullable;
        this.multiple = multiple;
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

    public boolean isMultiple() {
        return multiple;
    }
}

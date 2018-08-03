package com.android.xsdc.descriptor;

import com.android.xsdc.XmlSchema;
import com.android.xsdc.XsdConstants;
import com.android.xsdc.XsdParserException;
import com.android.xsdc.tag.*;

public class SimpleTypeInterpreter {
    public static SimpleTypeDescriptor predefinedInstance(String typename) throws XsdParserException {
        switch (typename) {
            case "string":
            case "token":
            case "normalizedString":
            case "language":
            case "ENTITY":
            case "ID":
            case "Name":
            case "NCName":
            case "NMTOKEN":
            case "anyURI":
            case "anyType":
            case "QName":
            case "NOTATION":
            case "IDREF":
                return new StringTypeDescriptor(false);
            case "ENTITIES":
            case "NMTOKENS":
            case "IDREFS":
                return new StringTypeDescriptor(true);
            case "date":
            case "dateTime":
            case "time":
            case "gDay":
            case "gMonth":
            case "gYear":
            case "gMonthDay":
            case "gYearMonth":
                return new XmlGregorianCalendarTypeDescriptor(false);
            case "duration":
                return new DurationTypeDescriptor(false);
            case "decimal":
                return new BigDecimalTypeDescriptor(false);
            case "integer":
            case "negativeInteger":
            case "nonNegativeInteger":
            case "positiveInteger":
            case "nonPositiveInteger":
            case "unsignedLong":
                return new BigIntegerTypeDescriptor(false);
            case "long":
            case "unsignedInt":
                return new LongTypeDescriptor(false);
            case "int":
            case "unsignedShort":
                return new IntegerTypeDescriptor(false);
            case "short":
            case "unsignedByte":
                return new ShortTypeDescriptor(false);
            case "byte":
                return new ByteTypeDescriptor(false);
            case "boolean":
                return new BooleanTypeDescriptor(false);
            case "double":
                return new DoubleTypeDescriptor(false);
            case "float":
                return new FloatTypeDescriptor(false);
            case "base64Binary":
                return new Base64TypeDescriptor(false);
            case "hexBinary":
                return new HexTypeDescriptor(false);
        }
        throw new XsdParserException("unknown xsd predefined type : " + typename);
    }

    public static SimpleTypeDescriptor parse(XmlSchema schema, XsdTypeReferrer typeRef) throws XsdParserException {
        XsdSimpleType simpleType;
        if (typeRef.getRef() != null) {
            if (typeRef.getRef().getNamespaceURI().equals(XsdConstants.XSD_NAMESPACE)) {
                return predefinedInstance(typeRef.getRef().getLocalPart());
            } else {
                XsdType type = schema.getType(typeRef.getRef().getLocalPart());
                if (type instanceof XsdSimpleType)
                    simpleType = (XsdSimpleType)type;
                else
                    throw new XsdParserException(String.format("not a simple type : %s", typeRef.getRef()));
            }
        } else {
            if (typeRef.getValue() instanceof XsdSimpleType)
                simpleType = (XsdSimpleType)typeRef.getValue();
            else
                throw new XsdParserException("not a simple type");
        }
        if (simpleType instanceof XsdList) {
            XsdList list = (XsdList)simpleType;
            SimpleTypeDescriptor inner = parse(schema, list.getItemType());
            if (inner.isList()) throw new XsdParserException("list of list is not supported");
            inner.convertToListType();
            return inner;
        } else if (simpleType instanceof XsdRestriction) {
            // we don't consider any restrictions.
            XsdRestriction restriction = (XsdRestriction)simpleType;
            return parse(schema, restriction.getBase());
        } else if (simpleType instanceof XsdUnion) {
            // unions are almost always interpreted as java.lang.String
            // Exceptionally, if any of member types of union are 'list', then we interpret it as List<String>
            XsdUnion union = (XsdUnion)simpleType;
            for (XsdTypeReferrer memberType : union.getMemberTypes()) {
                if (parse(schema, memberType).isList()) {
                    return new StringTypeDescriptor(true);
                }
            }
            return new StringTypeDescriptor(false);
        } else {
            throw new XsdParserException("unknown simple type");
        }
    }
}

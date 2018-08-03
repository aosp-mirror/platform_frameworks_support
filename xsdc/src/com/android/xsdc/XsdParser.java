package com.android.xsdc;

import com.android.xsdc.tag.*;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class XsdParser {
    static XmlSchema parse(InputStream in) throws XmlPullParserException, IOException, XsdParserException {
        XmlPullParser parser = new KXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(in, null);
        parser.nextTag();
        return readSchema(parser);
    }

    private static XmlSchema readSchema(XmlPullParser parser) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "schema");
        String targetNameSpace = parser.getAttributeValue(null, "targetNamespace");
        XmlSchema schema = new XmlSchema(targetNameSpace);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("element")) {
                schema.registerElement(readElement(parser));
            } else if (tagName.equals("attribute")) {
                schema.registerAttribute(readAttribute(parser));
            } else if (tagName.equals("complexType")) {
                schema.registerType(readComplexType(parser));
            } else if (tagName.equals("simpleType")) {
                schema.registerType(readSimpleType(parser));
            } else if (tagName.equals("annotation")){
                skip(parser);
            } else {
                throw new XsdParserException(String.format("unsupported tag : {%s}%s", parser.getNamespace(), tagName));
            }
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "schema");
        return schema;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException("skip function is called illegally");
        }
        int depth = 1;
        while (depth > 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private static QName parseQName(XmlPullParser parser, String str) throws XsdParserException {
        if (str == null) return null;
        String[] parsed = str.split(":");
        if (parsed.length == 2) {
            return new QName(parser.getNamespace(parsed[0]), parsed[1]);
        } else if (parsed.length == 1) {
            return new QName(null, str);
        }
        throw new XsdParserException(String.format("QName parse error(%s)", str));
    }

    private static List<QName> parseQNames(XmlPullParser parser, String str) throws XsdParserException {
        List<QName> qNames = new ArrayList<>();
        if (str == null) return qNames;
        String[] parsed = str.split("\\s+");
        for (String s : parsed) {
            qNames.add(parseQName(parser, s));
        }
        return qNames;
    }

    private static XsdElement readElement(XmlPullParser parser) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "element");
        String name = parser.getAttributeValue(null, "name");
        QName typename = parseQName(parser, parser.getAttributeValue(null, "type"));
        QName ref = parseQName(parser, parser.getAttributeValue(null, "ref"));

        String minOccurs = parser.getAttributeValue(null, "minOccurs");
        String maxOccurs = parser.getAttributeValue(null, "maxOccurs");
        boolean nullable = false, multiple = false;
        if (maxOccurs != null) {
            if (maxOccurs.equals("0")) return null;
            if (maxOccurs.equals("unbounded") || Integer.parseInt(maxOccurs) > 1) multiple = true;
        }
        if (minOccurs != null) {
            if (minOccurs.equals("0")) nullable = true;
        }

        XsdElement element;
        if (ref != null) {
            element = new XsdElement(ref, nullable, multiple);
            skip(parser);
        } else if (name == null) {
            throw new XsdParserException(String.format("element name and ref cannot be both null. line %d", parser.getLineNumber()));
        } else if (typename != null) {
            element = new XsdElement(name, new XsdTypeReferrer(typename), nullable, multiple);
            skip(parser);
        } else {
            XsdType type = null;
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tagName = parser.getName();
                if (tagName.equals("complexType")) {
                    if (type != null) throw new XsdParserException(String.format("element type definition duplicated : %s", name));
                    type = readComplexType(parser);
                } else if (tagName.equals("simpleType")) {
                    if (type != null) throw new XsdParserException(String.format("element type definition duplicated : %s", name));
                    type = readSimpleType(parser);
                } else if (tagName.equals("annotation")) {
                    skip(parser);
                } else {
                    throw new XsdParserException(String.format("unsupported tag contained in <element> : {%s}%s", parser.getNamespace(), tagName));
                }
            }
            if (type == null) throw new XsdParserException(String.format("element type definition not exist : %s", name));
            element = new XsdElement(name, new XsdTypeReferrer(type), nullable, multiple);
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "element");
        return element;
    }

    private static XsdAttribute readAttribute(XmlPullParser parser) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "attribute");
        String name = parser.getAttributeValue(null, "name");
        QName typename = parseQName(parser, parser.getAttributeValue(null, "type"));
        QName ref = parseQName(parser, parser.getAttributeValue(null, "ref"));

        String use = parser.getAttributeValue(null, "use");
        boolean nullable = true;
        if (use != null) {
            if (use.equals("prohibited")) return null;
            if (use.equals("required")) nullable = false;
        }

        XsdAttribute attribute;
        if (ref != null) {
            attribute = new XsdAttribute(ref, nullable);
            skip(parser);
        } else if (name == null) {
            throw new XsdParserException(String.format("attribute name and ref cannot be both null. line %d", parser.getLineNumber()));
        } else if (typename != null) {
            attribute = new XsdAttribute(name, new XsdTypeReferrer(typename), nullable);
            skip(parser);
        } else {
            XsdSimpleType type = null;
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tagName = parser.getName();
                if (tagName.equals("simpleType")) {
                    if (type != null) throw new XsdParserException(String.format("attribute type definition duplicated : %s", name));
                    type = readSimpleType(parser);
                } else if (tagName.equals("annotation")) {
                    skip(parser);
                } else {
                    throw new XsdParserException(String.format("unsupported tag contained in <attribute> : {%s}%s", parser.getNamespace(), tagName));
                }
            }
            if (type == null) throw new XsdParserException(String.format("attribute type definition not exist : %s", name));
            attribute = new XsdAttribute(name, new XsdTypeReferrer(type), nullable);
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "attribute");
        return attribute;
    }

    private static XsdComplexType readComplexType(XmlPullParser parser) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "complexType");
        String name = parser.getAttributeValue(null, "name");
        XsdComplexType type = null;
        XsdComplexContent content = new XsdComplexContent(name, null);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("annotation")) {
                skip(parser);
            } else if (type != null) {
                throw new XsdParserException(String.format("complex type definition duplicated. line %d", parser.getLineNumber()));
            } else if (tagName.equals("attribute")) {
                XsdAttribute attribute = readAttribute(parser);
                if (attribute != null) content.getAttributes().add(attribute);
            } else if (tagName.equals("sequence")) {
                content.getElements().addAll(readSequence(parser));
            } else if (tagName.equals("complexContent")) {
                type = readComplexContent(parser, name);
            } else if (tagName.equals("simpleContent")) {
                type = readSimpleContent(parser, name);
            } else {
                throw new XsdParserException(String.format("unsupported tag contained in <complexType> : {%s}%s", parser.getNamespace(), tagName));
            }
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "complexType");
        return (type != null) ? type : content;
    }

    private static XsdComplexContent readComplexContent(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "complexContent");
        XsdComplexContent content = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("restriction")) {
                content = readComplexContentRestriction(parser, name);
            } else if (tagName.equals("extension")) {
                content = readComplexContentExtension(parser, name);
            } else {
                skip(parser);
            }
        }
        if (content == null) throw new XsdParserException("complex content cannot be inferred");
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "complexContent");
        return content;
    }

    private static XsdComplexContent readComplexContentRestriction(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "restriction");
        QName base = parseQName(parser, parser.getAttributeValue(null, "base"));
        if (base == null) throw new XsdParserException(String.format("base could not be null. line %d", parser.getLineNumber()));
        XsdComplexContent content = new XsdComplexContent(name, new XsdTypeReferrer(base));

        // ignore restrictions
        skip(parser);

        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "restriction");
        return content;
    }

    private static XsdComplexContent readComplexContentExtension(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "extension");
        QName base = parseQName(parser, parser.getAttributeValue(null, "base"));
        if (base == null) throw new XsdParserException(String.format("base could not be null. line %d", parser.getLineNumber()));
        XsdComplexContent content = new XsdComplexContent(name, new XsdTypeReferrer(base));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("attribute")) {
                XsdAttribute attribute = readAttribute(parser);
                if (attribute != null) content.getAttributes().add(attribute);
            } else if (tagName.equals("sequence")) {
                content.getElements().addAll(readSequence(parser));
            } else {
                skip(parser);
            }
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "extension");
        return content;
    }

    private static XsdSimpleContent readSimpleContent(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "simpleContent");
        XsdSimpleContent content = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("restriction")) {
                content = readSimpleContentRestriction(parser, name);
            } else if (tagName.equals("extension")) {
                content = readSimpleContentExtension(parser, name);
            } else {
                skip(parser);
            }
        }
        if (content == null) throw new XsdParserException("simple content cannot be inferred");
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "simpleContent");
        return content;
    }

    private static XsdSimpleContent readSimpleContentRestriction(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "restriction");
        QName base = parseQName(parser, parser.getAttributeValue(null, "base"));
        if (base == null) throw new XsdParserException(String.format("base could not be null. line %d", parser.getLineNumber()));
        XsdSimpleContent content = new XsdSimpleContent(name, new XsdTypeReferrer(base));

        // ignore restrictions
        skip(parser);
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "restriction");
        return content;
    }

    private static XsdSimpleContent readSimpleContentExtension(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "extension");
        QName base = parseQName(parser, parser.getAttributeValue(null, "base"));
        if (base == null) throw new XsdParserException(String.format("base could not be null. line %d", parser.getLineNumber()));
        XsdSimpleContent content = new XsdSimpleContent(name, new XsdTypeReferrer(base));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("attribute")) {
                XsdAttribute attribute = readAttribute(parser);
                if (attribute != null) content.getAttributes().add(attribute);
            } else {
                skip(parser);
            }
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "extension");
        return content;
    }

    private static XsdSimpleType readSimpleType(XmlPullParser parser) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "simpleType");
        String name = parser.getAttributeValue(null, "name");
        XsdSimpleType type = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("annotation")) {
                skip(parser);
            } else if (type != null) {
                throw new XsdParserException(String.format("simple type definition duplicated. line %d", parser.getLineNumber()));
            } else if (tagName.equals("list")) {
                type = readSimpleTypeList(parser, name);
            } else if (tagName.equals("restriction")) {
                type = readSimpleTypeRestriction(parser, name);
            } else if (tagName.equals("union")) {
                type = readSimpleTypeUnion(parser, name);
            } else {
                throw new XsdParserException(String.format("unsupported tag contained in <simpleType> : {%s}%s", parser.getNamespace(), tagName));
            }
        }
        if (type == null) throw new XsdParserException("simple type cannot be inferred");
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "simpleType");
        return type;
    }

    private static XsdList readSimpleTypeList(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "list");
        QName itemTypeName = parseQName(parser, parser.getAttributeValue(null, "itemType"));

        XsdList list;
        if (itemTypeName != null) {
            list = new XsdList(name, new XsdTypeReferrer(itemTypeName));
            skip(parser);
        } else {
            XsdSimpleType type = null;
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tagName = parser.getName();
                if (tagName.equals("simpleType")) {
                    type = readSimpleType(parser);
                } else {
                    skip(parser);
                }
            }
            if (type == null) throw new XsdParserException("list item type cannot be inferred");
            list = new XsdList(name, new XsdTypeReferrer(type));
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "list");
        return list;
    }

    private static XsdRestriction readSimpleTypeRestriction(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "restriction");
        QName baseName = parseQName(parser, parser.getAttributeValue(null, "base"));

        XsdRestriction restriction;
        if (baseName != null) {
            restriction = new XsdRestriction(name, new XsdTypeReferrer(baseName));
            skip(parser);
        } else {
            XsdSimpleType type = null;
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tagName = parser.getName();
                if (tagName.equals("simpleType")) {
                    type = readSimpleType(parser);
                } else {
                    skip(parser);
                }
            }
            if (type == null) throw new XsdParserException("restriction base type cannot be inferred");
            restriction = new XsdRestriction(name, new XsdTypeReferrer(type));
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "restriction");
        return restriction;
    }

    private static XsdUnion readSimpleTypeUnion(XmlPullParser parser, String name) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "union");
        List<QName> memberTypeNames = parseQNames(parser, parser.getAttributeValue(null, "memberTypes"));
        List<XsdTypeReferrer> memberTypes = memberTypeNames.stream().map(XsdTypeReferrer::new).collect(Collectors.toList());

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("simpleType")) {
                memberTypes.add(new XsdTypeReferrer(readSimpleType(parser)));
            } else {
                skip(parser);
            }
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "union");
        return new XsdUnion(name, memberTypes);
    }

    private static List<XsdElement> readSequence(XmlPullParser parser) throws XmlPullParserException, IOException, XsdParserException {
        parser.require(XmlPullParser.START_TAG, XsdConstants.XSD_NAMESPACE, "sequence");
        List<XsdElement> elements = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("element")) {
                XsdElement element = readElement(parser);
                if (element != null) elements.add(element);
            } else if (tagName.equals("sequence")) {
                elements.addAll(readSequence(parser));
            } else if (tagName.equals("annotation")){
                skip(parser);
            } else {
                throw new XsdParserException(String.format("unsupported tag contained in <sequence> : {%s}%s", parser.getNamespace(), tagName));
            }
        }
        parser.require(XmlPullParser.END_TAG, XsdConstants.XSD_NAMESPACE, "sequence");
        return elements;
    }
}

package com.android.xsdc.descriptor;

import com.android.xsdc.XsdParserException;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class SchemaDescriptor {
    private Map<String, ClassDescriptor> classDescriptorMap;
    private Map<String, VariableDescriptor> rootElementMap;

    public SchemaDescriptor() {
        classDescriptorMap = new HashMap<>();
        rootElementMap = new HashMap<>();
    }

    public void registerClass(ClassDescriptor descriptor) throws XsdParserException {
        if (classDescriptorMap.containsKey(descriptor.getName()) || descriptor.getName().equals("XmlParser")) {
            throw new XsdParserException(String.format("duplicate class name : %s", descriptor.getName()));
        }
        classDescriptorMap.put(descriptor.getName(), descriptor);
    }

    public void registerRootElement(VariableDescriptor element) throws XsdParserException {
        if (rootElementMap.containsKey(element.getXmlName())) {
            throw new XsdParserException(String.format("duplicate root element name : %s", element.getXmlName()));
        }
        rootElementMap.put(element.getXmlName(), element);
    }

    public Map<String, ClassDescriptor> getClassDescriptorMap() {
        return classDescriptorMap;
    }

    public void printXmlParser(String packageName, PrintWriter out) {
        out.printf("package %s;\n\n", packageName);

        out.println("public class XmlParser {");

        out.print("\tpublic static java.lang.Object read(java.io.InputStream in)\n" +
                "\t\tthrows org.xmlpull.v1.XmlPullParserException, java.io.IOException, javax.xml.datatype.DatatypeConfigurationException {\n" +
                "\t\torg.xmlpull.v1.XmlPullParser parser = new org.kxml2.io.KXmlParser();\n" +
                "\t\tparser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);\n" +
                "\t\tparser.setInput(in, null);\n" +
                "\t\tparser.nextTag();\n" +
                "\t\tString tagName = parser.getName();\n" +
                "\t\t");
        for (VariableDescriptor element : rootElementMap.values()) {
            out.printf("if (tagName.equals(\"%s\")) {\n", element.getXmlName());
            if (element.getType().isSimple()) {
                out.print("\t\t\traw = XmlParser.readText(parser);\n");
            }
            String expression = element.getType().getParsingExpression();
            for (String code : expression.split("\n")) {
                out.printf("\t\t\t%s\n", code);
            }
            out.print("\t\t\treturn value;\n" +
                    "\t\t} else ");
        }
        out.print("{\n" +
                "\t\t\tthrow new RuntimeException(String.format(\"unknown element '%s'\", tagName));\n" +
                "\t\t}\n\t}\n");

        out.print("\n\tpublic static java.lang.String readText(org.xmlpull.v1.XmlPullParser parser) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {\n" +
                "\t\tString result = \"\";\n" +
                "\t\tif (parser.next() == org.xmlpull.v1.XmlPullParser.TEXT) {\n" +
                "\t\t\tresult = parser.getText();\n" +
                "\t\t\tparser.nextTag();\n" +
                "\t\t}\n" +
                "\t\treturn result;\n" +
                "\t}\n");

        out.print("\n\tpublic static void skip(org.xmlpull.v1.XmlPullParser parser) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {\n" +
                "\t\tif (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) {\n" +
                "\t\t\tthrow new IllegalStateException();\n" +
                "\t\t}\n" +
                "\t\tint depth = 1;\n" +
                "\t\twhile (depth != 0) {\n" +
                "\t\t\tswitch (parser.next()) {\n" +
                "\t\t\tcase org.xmlpull.v1.XmlPullParser.END_TAG:\n" +
                "\t\t\t\tdepth--;\n" +
                "\t\t\t\tbreak;\n" +
                "\t\t\tcase org.xmlpull.v1.XmlPullParser.START_TAG:\n" +
                "\t\t\t\tdepth++;\n" +
                "\t\t\t\tbreak;\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n");

        out.println("}");
    }
}

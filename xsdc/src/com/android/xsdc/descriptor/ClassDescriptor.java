package com.android.xsdc.descriptor;

import com.android.xsdc.Utils;
import com.android.xsdc.XsdParserException;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassDescriptor {

    private String name;
    private ClassDescriptor base;
    private TypeDescriptor valueType;
    private List<VariableDescriptor> elements;
    private List<VariableDescriptor> attributes;
    private List<ClassDescriptor> innerClasses;
    private Set<String> nameSet;

    public ClassDescriptor(String name) {
        this.name = name;
        elements = new ArrayList<>();
        attributes = new ArrayList<>();
        innerClasses = new ArrayList<>();
        nameSet = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void setBase(ClassDescriptor base) throws XsdParserException {
        for (String name : base.nameSet) {
            addNameSet(name);
        }
        this.base = base;
    }

    public void setValueType(TypeDescriptor valueType) throws XsdParserException {
        addNameSet("value");
        this.valueType = valueType;
    }

    public void registerElement(VariableDescriptor element) throws XsdParserException {
        addNameSet(element.getName());
        elements.add(element);
    }

    public void registerAttribute(VariableDescriptor attribute) throws XsdParserException {
        addNameSet(attribute.getName());
        attributes.add(attribute);
    }

    public void registerInnerClass(ClassDescriptor innerClass) throws XsdParserException {
        addNameSet(innerClass.getName());
        innerClasses.add(innerClass);
    }

    private void addNameSet(String name) throws XsdParserException {
        if (nameSet.contains(name)) {
            throw new XsdParserException(String.format("duplicate variable name : class %s, variable %s", this.name, name));
        }
        nameSet.add(name);
    }

    public void print(String packageName, PrintWriter out) {
        out.printf("package %s;\n\n", packageName);
        print(out, 0);
    }

    private void print(PrintWriter out, int indent) {
        if (indent > 0) {
            Utils.printIndentFormat(out, indent, "public static class %s ", getName());
        } else {
            Utils.printIndentFormat(out, indent, "public class %s ", getName());
        }

        if (base != null) {
            out.printf("extends %s {\n", base.getName());
        } else {
            out.print("{\n");
        }

        List<VariableDescriptor> values = new ArrayList<>();
        if (valueType != null) {
            values.add(new VariableDescriptor(valueType, "value", null, true, false));
        }

        printVariables(out, indent+1, elements);
        printVariables(out, indent+1, attributes);
        printVariables(out, indent+1, values);

        printGetterAndSetter(out, indent+1, elements);
        printGetterAndSetter(out, indent+1, attributes);
        printGetterAndSetter(out, indent+1, values);

        out.println();
        printParser(out, indent+1);

        for (ClassDescriptor descriptor : innerClasses) {
            out.println();
            descriptor.print(out, indent+1);
        }
        Utils.printIndentFormat(out, indent, "}\n");
    }

    private void printVariables(PrintWriter out, int indent, List<VariableDescriptor> variables) {
        for (VariableDescriptor variable : variables) {
            Utils.printIndentFormat(out, indent, "protected %s %s;\n", variable.getFullTypeName(), variable.getName());
        }
    }

    private void printGetterAndSetter(PrintWriter out, int indent, List<VariableDescriptor> variables) {
        for (VariableDescriptor variable : variables) {
            out.println();
            Utils.printIndentFormat(out, indent, "public %s get%s() {\n", variable.getFullTypeName(), Utils.capitalize(variable.getName()));
            if (variable.isMultiple()) {
                Utils.printIndentFormat(out, indent+1, "if (%s == null) {\n", variable.getName());
                Utils.printIndentFormat(out, indent+2, "%s = new java.util.ArrayList<>();\n", variable.getName());
                Utils.printIndentFormat(out, indent+1, "}\n");
            }
            Utils.printIndentFormat(out, indent+1, "return %s;\n", variable.getName());
            Utils.printIndentFormat(out, indent, "}\n");

            if (!variable.isMultiple()) {
                out.println();
                Utils.printIndentFormat(out, indent, "public void set%s(%s %s) {\n", Utils.capitalize(variable.getName()), variable.getFullTypeName(), variable.getName());
                Utils.printIndentFormat(out, indent+1, "this.%s = %s;\n", variable.getName(), variable.getName());
                Utils.printIndentFormat(out, indent, "}\n");
            }
        }
    }

    private List<VariableDescriptor> getAllAttributes() {
        List<VariableDescriptor> allAttributes;
        if (base != null) {
            allAttributes = base.getAllAttributes();
        } else {
            allAttributes = new ArrayList<>();
        }
        allAttributes.addAll(attributes);
        return allAttributes;
    }

    private List<VariableDescriptor> getAllElements() {
        List<VariableDescriptor> allElements;
        if (base != null) {
            allElements = base.getAllElements();
        } else {
            allElements = new ArrayList<>();
        }
        allElements.addAll(elements);
        return allElements;
    }

    private TypeDescriptor getBaseValueType() {
        if (base != null) {
            return base.getBaseValueType();
        } else {
            return valueType;
        }
    }

    private void printParser(PrintWriter out, int indent) {
        Utils.printIndentFormat(out, indent, "public static %s read(org.xmlpull.v1.XmlPullParser parser) " +
                "throws org.xmlpull.v1.XmlPullParserException, java.io.IOException, javax.xml.datatype.DatatypeConfigurationException {\n", name);

        Utils.printIndentFormat(out, indent+1, "%s instance = new %s();\n", name, name);
        Utils.printIndentFormat(out, indent+1, "String raw = null;\n");
        for (VariableDescriptor attribute : getAllAttributes()) {
            Utils.printIndentFormat(out, indent+1, "raw = parser.getAttributeValue(null, \"%s\");\n", attribute.getXmlName());
            Utils.printIndentFormat(out, indent+1, "if (raw != null) {\n");
            String expression = attribute.getType().getParsingExpression();
            for (String code : expression.split("\n")) {
                Utils.printIndentFormat(out, indent+2, code + "\n");
            }
            Utils.printIndentFormat(out, indent+2, "instance.set%s(value);\n", Utils.capitalize(attribute.getName()));
            Utils.printIndentFormat(out, indent+1, "}\n");
        }

        TypeDescriptor baseValueType = getBaseValueType();
        List<VariableDescriptor> allElements = getAllElements();
        if (baseValueType != null) {
            Utils.printIndentFormat(out, indent+1, "raw = XmlParser.readText(parser);\n");
            Utils.printIndentFormat(out, indent+1, "if (raw != null) {\n");
            String expression = baseValueType.getParsingExpression();
            for (String code : expression.split("\n")) {
                Utils.printIndentFormat(out, indent+2, code + "\n");
            }
            Utils.printIndentFormat(out, indent+2, "instance.setValue(value);\n");
            Utils.printIndentFormat(out, indent+1, "}\n");
        } else if (!allElements.isEmpty()){
            Utils.printIndentFormat(out, indent + 1, "while (parser.next() != org.xmlpull.v1.XmlPullParser.END_TAG) {\n");
            Utils.printIndentFormat(out, indent + 2, "if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) continue;\n");
            Utils.printIndentFormat(out, indent + 2, "String tagName = parser.getName();\n");
            Utils.printIndentFormat(out, indent + 2, "");

            for (VariableDescriptor element : allElements) {
                out.printf("if (tagName.equals(\"%s\")) {\n", element.getXmlName());
                if (element.getType().isSimple()) {
                    Utils.printIndentFormat(out, indent + 3, "raw = XmlParser.readText(parser);\n");
                }
                String expression = element.getType().getParsingExpression();
                for (String code : expression.split("\n")) {
                    Utils.printIndentFormat(out, indent + 3, code + "\n");
                }
                if (element.isMultiple()) {
                    Utils.printIndentFormat(out, indent + 3, "instance.get%s().add(value);\n", Utils.capitalize(element.getName()));
                } else {
                    Utils.printIndentFormat(out, indent + 3, "instance.set%s(value);\n", Utils.capitalize(element.getName()));
                }
                Utils.printIndentFormat(out, indent + 2, "} else ");
            }
            out.print("{\n");
            Utils.printIndentFormat(out, indent + 3, "XmlParser.skip(parser);\n");
            Utils.printIndentFormat(out, indent + 2, "}\n");
            Utils.printIndentFormat(out, indent + 1, "}\n");
        }

        Utils.printIndentFormat(out, indent+1, "return instance;\n");

        Utils.printIndentFormat(out, indent, "}\n");
    }
}

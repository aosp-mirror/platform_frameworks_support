package com.android.xsdc;

import com.android.xsdc.descriptor.*;
import com.android.xsdc.tag.*;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class XmlSchema {
    private Map<String, XsdElement> elements;
    private Map<String, XsdType> types;
    private Map<String, XsdAttribute> attributes;
    private String targetNameSpace;

    XmlSchema(String targetNameSpace) {
        elements = new HashMap<>();
        types = new HashMap<>();
        attributes = new HashMap<>();
        this.targetNameSpace = targetNameSpace;
    }

    void registerElement(XsdElement element) throws XsdParserException {
        String name = element.getName();
        if (name == null) throw new XsdParserException("root element should have name.");
        if (elements.containsKey(name)) {
            throw new XsdParserException(String.format("duplicate element name : %s", name));
        }
        elements.put(name, element);
    }

    void registerType(XsdType type) throws XsdParserException {
        String name = type.getName();
        if (name == null) throw new XsdParserException("root type should have name.");
        if (elements.containsKey(name)) {
            throw new XsdParserException(String.format("duplicate type name : %s", name));
        }
        types.put(name, type);
    }

    void registerAttribute(XsdAttribute attribute) throws XsdParserException {
        String name = attribute.getName();
        if (name == null) throw new XsdParserException("root attribute should have name.");
        if (attributes.containsKey(name)) {
            throw new XsdParserException(String.format("duplicate attribute name : %s", name));
        }
        attributes.put(name, attribute);
    }

    public XsdType getType(String name) throws XsdParserException {
        if (!types.containsKey(name)) {
            throw new XsdParserException(String.format("type does not exist : %s", name));
        }
        return types.get(name);
    }

    XsdElement getElement(String name) throws XsdParserException {
        if (!elements.containsKey(name)) {
            throw new XsdParserException(String.format("element does not exist : %s", name));
        }
        return elements.get(name);
    }

    String getTargetNameSpace() {
        return targetNameSpace;
    }

    SchemaDescriptor explain() throws XsdParserException {
        // validation
        Set<String> nameSet = new HashSet<>();
        for (Map.Entry<String, XsdType> entry : types.entrySet()) {
            if (entry.getValue() instanceof XsdSimpleType) continue;
            String name = Utils.toClassName(entry.getKey());
            if (nameSet.contains(name)) {
                throw new XsdParserException(String.format("duplicate class name : %s", name));
            }
            nameSet.add(name);
        }
        for (Map.Entry<String, XsdElement> entry : elements.entrySet()) {
            XsdTypeReferrer type = entry.getValue().getType();
            if (type.getRef() == null && type.getValue() instanceof XsdComplexType) {
                String name = Utils.toClassName(entry.getKey());
                if (nameSet.contains(name)) {
                    throw new XsdParserException(String.format("duplicate class name : %s", name));
                }
                nameSet.add(name);
            }
        }

        SchemaDescriptor schemaDescriptor = new SchemaDescriptor();
        Set<String> visited = new HashSet<>();
        for (Map.Entry<String, XsdType> entry : types.entrySet()) {
            String name = Utils.toClassName(entry.getKey());
            XsdType type = entry.getValue();
            if (type instanceof XsdComplexType && !visited.contains(name))
                schemaDescriptor.registerClass(convertToClassDescriptor(schemaDescriptor, visited, name, (XsdComplexType)type, ""));
        }
        for (Map.Entry<String, XsdElement> entry : elements.entrySet()) {
            String name = Utils.toClassName(entry.getKey());
            XsdElement element = entry.getValue();
            XsdTypeReferrer type = element.getType();
            TypeDescriptor typeDesc;
            if (type.getRef() != null) {
                QName typeRef = type.getRef();
                if (typeRef.getNamespaceURI().equals(XsdConstants.XSD_NAMESPACE)) {
                    typeDesc = SimpleTypeInterpreter.predefinedInstance(typeRef.getLocalPart());
                } else {
                    XsdType elementType = getType(typeRef.getLocalPart());
                    if (elementType instanceof XsdComplexType) {
                        typeDesc = new ComplexTypeDescriptor(Utils.toClassName(typeRef.getLocalPart()));
                    } else {
                        typeDesc = SimpleTypeInterpreter.parse(this, new XsdTypeReferrer(elementType));
                    }
                }
            } else {
                if (type.getValue() instanceof XsdComplexType) {
                    typeDesc = new ComplexTypeDescriptor(name);
                    schemaDescriptor.registerClass(convertToClassDescriptor(schemaDescriptor, visited, name, (XsdComplexType)type.getValue(), ""));
                } else {
                    typeDesc = SimpleTypeInterpreter.parse(this, element.getType());
                }
            }
            schemaDescriptor.registerRootElement(new VariableDescriptor(typeDesc, Utils.toVariableName(element.getName()), element.getName(), element.isNullable(), element.isMultiple()));
        }
        return schemaDescriptor;
    }

    private ClassDescriptor convertToClassDescriptor(SchemaDescriptor schemaDescriptor, Set<String> visited, String name, XsdComplexType complexType, String nameScope) throws XsdParserException {
        if (nameScope.isEmpty()) {
            visited.add(name);
        }
        ClassDescriptor classDescriptor = new ClassDescriptor(name);
        String baseName = null;
        if (complexType.getBase() != null) {
            QName baseRef = complexType.getBase().getRef();
            if (complexType instanceof XsdComplexContent) {
                if (baseRef.getNamespaceURI().equals(XsdConstants.XSD_NAMESPACE)) {
                    if (!baseRef.getLocalPart().equals("anyType")) {
                        throw new XsdParserException(String.format("complex content should be derived from complex content(name: %s)", name));
                    }
                } else {
                    baseName = baseRef.getLocalPart();
                }
            } else {
                if (baseRef.getNamespaceURI().equals(XsdConstants.XSD_NAMESPACE)) {
                    classDescriptor.setValueType(SimpleTypeInterpreter.predefinedInstance(baseRef.getLocalPart()));
                } else {
                    baseName = baseRef.getLocalPart();
                }
            }
        }
        if (baseName != null) {
            XsdType baseType = getType(baseName);
            if (baseType instanceof XsdComplexType) {
                String baseClassName = Utils.toClassName(baseName);
                if (!schemaDescriptor.getClassDescriptorMap().containsKey(baseClassName)) {
                    if (visited.contains(baseClassName)) {
                        throw new XsdParserException(String.format("cross reference detected : %s", baseClassName));
                    }
                    schemaDescriptor.registerClass(convertToClassDescriptor(schemaDescriptor, visited, baseClassName, (XsdComplexType) baseType, ""));
                }
                classDescriptor.setBase(schemaDescriptor.getClassDescriptorMap().get(baseClassName));
            } else {
                classDescriptor.setValueType(SimpleTypeInterpreter.parse(this, new XsdTypeReferrer(baseType)));
            }
        }
        if (complexType instanceof XsdComplexContent) {
            for (XsdElement element : ((XsdComplexContent)complexType).getElements()) {
                TypeDescriptor typeDesc;
                boolean isRef = false;
                if (element.getRef() != null) {
                    element = getElement(element.getRef().getLocalPart());
                    isRef = true;
                }
                if (element.getType().getRef() != null) {
                    QName typeRef = element.getType().getRef();
                    if (typeRef.getNamespaceURI().equals(XsdConstants.XSD_NAMESPACE)) {
                        typeDesc = SimpleTypeInterpreter.predefinedInstance(typeRef.getLocalPart());
                    } else {
                        XsdType elementType = getType(typeRef.getLocalPart());
                        if (elementType instanceof XsdComplexType) {
                            typeDesc = new ComplexTypeDescriptor(Utils.toClassName(typeRef.getLocalPart()));
                        } else {
                            typeDesc = SimpleTypeInterpreter.parse(this, new XsdTypeReferrer(elementType));
                        }
                    }
                } else {
                    if (element.getType().getValue() instanceof XsdComplexType) {
                        if (isRef) {
                            typeDesc = new ComplexTypeDescriptor(Utils.toClassName(element.getName()));
                        } else {
                            typeDesc = new ComplexTypeDescriptor(nameScope + name + "." + Utils.toClassName(element.getName()));
                            classDescriptor.registerInnerClass(convertToClassDescriptor(schemaDescriptor, visited, Utils.toClassName(element.getName()), (XsdComplexType) element.getType().getValue(), nameScope + name + "."));
                        }
                    } else {
                        typeDesc = SimpleTypeInterpreter.parse(this, element.getType());
                    }
                }
                classDescriptor.registerElement(new VariableDescriptor(typeDesc, Utils.toVariableName(element.getName()), element.getName(), element.isNullable(), element.isMultiple()));
            }
        }
        for (XsdAttribute attribute : complexType.getAttributes()) {
            TypeDescriptor typeDesc = SimpleTypeInterpreter.parse(this, attribute.getType());
            classDescriptor.registerAttribute(new VariableDescriptor(typeDesc, Utils.toVariableName(attribute.getName()), attribute.getName(), attribute.isNullable(), false));
        }
        return classDescriptor;
    }
}

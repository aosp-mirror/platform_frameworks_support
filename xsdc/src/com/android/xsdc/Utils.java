package com.android.xsdc;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;

public class Utils {
    private static final String[] keywords = {
            "abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default",
            "double", "do", "else", "enum", "extends", "false",
            "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "null", "package", "private", "protected",
            "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "void", "volatile", "while"
    };
    private static final HashSet<String> keywordSet = new HashSet<>(Arrays.asList(keywords));

    public static String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private static String lowerize(String input) {
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

    static String toVariableName(String name) throws XsdParserException {
        // remove non-alphanumeric and non-underscore characters
        String trimmed = name.replaceAll("[^A-Za-z0-9_]", "");
        if (trimmed.isEmpty()) {
            throw new XsdParserException(String.format("cannot convert to a variable name : %s", name));
        }
        String lowered = (trimmed.charAt(0) >= '0' && trimmed.charAt(0) <= '9') ? "_" + trimmed : lowerize(trimmed);
        // always starts with a lowercase or underscore character.
        return (keywordSet.contains(trimmed)) ? "_" + lowered : lowered;
    }

    static String toClassName(String name) throws XsdParserException {
        // remove non-alphanumeric characters
        String trimmed = name.replaceAll("[^A-Za-z0-9]", "");
        if (trimmed.isEmpty() || (trimmed.charAt(0) >= '0' && trimmed.charAt(0) <= '9')) {
            throw new XsdParserException(String.format("cannot convert to a class name : %s", name));
        }
        return capitalize(trimmed);
    }

    public static void printIndentFormat(PrintWriter out, int indent, String str, Object... arguments) {
        while (indent-- > 0) {
            out.print("\t");
        }
        out.printf(str, arguments);
    }
}

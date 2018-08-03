package com.google.xsd;

import org.xmlpull.v1.XmlPullParserException;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.fail;

// https://myshittycode.com/2014/02/13/java-programmatically-compile-and-unit-test-generated-java-source-code/
// https://www.hugopicado.com/2016/08/20/java-compiling-java-into-memory.html

class TestHelper {
    static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String contents;

        InMemoryJavaFileObject(String className, String contents) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.contents = contents;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return contents;
        }
    }

    static class InMemoryJavaClassObject extends SimpleJavaFileObject {
        private ByteArrayOutputStream baos;
        private String name;

        InMemoryJavaClassObject(String name, Kind kind) {
            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
            baos = new ByteArrayOutputStream();
            this.name = name;
        }

        byte[] getBytes() {
            return baos.toByteArray();
        }

        @Override
        public OutputStream openOutputStream() {
            return baos;
        }

        String getClassName() {
            return name;
        }
    }

    static class InMemoryClassManager extends ForwardingJavaFileManager<JavaFileManager> {
        private List<InMemoryJavaClassObject> classObjects;

        InMemoryClassManager(JavaFileManager fileManager) {
            super(fileManager);
            classObjects = new ArrayList<>();
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject sibling) {
            InMemoryJavaClassObject object = new InMemoryJavaClassObject(name, kind);
            classObjects.add(object);
            return object;
        }

        List<InMemoryJavaClassObject> getAllClasses() {
            return classObjects;
        }
    }

    final static String packageName = "test";

    static TestCompilationResult parseXsdAndCompile(InputStream in) throws IOException, XmlPullParserException, XsdParserException, ClassNotFoundException {
        XmlSchema schema = XsdParser.parse(in);
        List<JavaFileObject> javaFileObjects = new ArrayList<>();
        SchemaDescriptor schemaDescriptor = schema.explain();
        for (ClassDescriptor descriptor : schemaDescriptor.getClassDescriptorMap().values()) {
            StringWriter codeOutput = new StringWriter();
            descriptor.print(packageName, new PrintWriter(codeOutput));
            javaFileObjects.add(new InMemoryJavaFileObject(descriptor.getName(), codeOutput.toString()));
        }
        StringWriter codeOutput = new StringWriter();
        schemaDescriptor.printXmlParser(packageName, new PrintWriter(codeOutput));
        javaFileObjects.add(new InMemoryJavaFileObject("XmlParser", codeOutput.toString()));

        return new TestCompilationResult(compile(javaFileObjects));
    }

    private static List<InMemoryJavaClassObject> compile(List<JavaFileObject> javaFileObjects) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<InMemoryJavaClassObject> ret = null;

        try (InMemoryClassManager fileManager = new InMemoryClassManager(compiler.getStandardFileManager(diagnostics,null,null))) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, javaFileObjects);
            boolean success = task.call();

            if (!success) {
                StringBuilder log = new StringBuilder();
                log.append("Compilation failed!\n\n");
                for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                    log.append("Code: ").append(diagnostic.getCode()).append("\n");
                    log.append("Kind: " + diagnostic.getKind() + "\n");
                    log.append("Line: " + diagnostic.getLineNumber() + "\n");
                    log.append("Column: " + diagnostic.getColumnNumber() + "\n");
                    log.append("Source: " + diagnostic.getSource() + "\n");
                    log.append("Message: " + diagnostic.getMessage(Locale.getDefault()) + "\n");
                }
                fail(log.toString());
            }
            ret = fileManager.getAllClasses();
        }
        return ret;
    }
}
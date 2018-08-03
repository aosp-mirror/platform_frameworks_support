package com.google.xsd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.xsd.TestHelper.packageName;

class TestCompilationResult {
    private static class ByteArrayClassLoader extends ClassLoader {
        private Map<String, byte[]> codeMap;

        ByteArrayClassLoader(List<TestHelper.InMemoryJavaClassObject> objects) {
            super();
            codeMap = new HashMap<>();
            for (TestHelper.InMemoryJavaClassObject object : objects) {
                codeMap.put(object.getClassName(), object.getBytes());
            }
        }

        @Override
        protected Class findClass(String name) throws ClassNotFoundException {
            byte[] code = codeMap.get(name);
            return defineClass(name, code, 0, code.length);
        }
    }

    private Map<String, Class<?>> classes;

    TestCompilationResult(List<TestHelper.InMemoryJavaClassObject> objects) throws ClassNotFoundException {
        ByteArrayClassLoader loader = new ByteArrayClassLoader(objects);
        classes = new HashMap<>();

        for (TestHelper.InMemoryJavaClassObject object : objects) {
            Class<?> cls = loader.loadClass(object.getClassName());
            classes.put(object.getClassName(), cls);
        }
    }

    Class<?> loadClass(String name) {
        return classes.get(packageName + "." + name);
    }
}

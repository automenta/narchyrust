//package jcog.util;
//
//
//import proguard.ClassPathEntry;
//import proguard.Configuration;
//import proguard.ConfigurationParser;
//import proguard.ProGuard;
//
//import java.io.*;
//import java.util.jar.JarEntry;
//import java.util.jar.JarInputStream;
//import java.util.jar.JarOutputStream;
//
//public class DynamicOptimizer {
//
//    private static final String PROGUARD_CONFIG = "-keepattributes *Annotation*\n" +
//            "-keep public class * {\n" +
//            "    public protected *;\n" +
//            "}\n" +
//            "-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*\n" +
//            "-optimizationpasses 2\n";
//
//    public static Class<?> optimizeAndLoad(String className) throws Exception {
//        // Load the original class
//        var originalClass = Class.forName(className);
//        var classBytes = getClassBytes(originalClass);
//
//        // Optimize the class
//        var optimizedBytes = optimizeClass(classBytes, className);
//
//        // Load the optimized class
//        return loadOptimizedClass(className, optimizedBytes);
//    }
//
//    private static byte[] getClassBytes(Class<?> clazz) throws IOException {
//        var className = clazz.getName();
//        var classAsPath = className.replace('.', '/') + ".class";
//        var is = clazz.getClassLoader().getResourceAsStream(classAsPath);
//        var buffer = new ByteArrayOutputStream();
//        int nRead;
//        var data = new byte[1024];
//        while ((nRead = is.read(data, 0, data.length)) != -1) {
//            buffer.write(data, 0, nRead);
//        }
//        buffer.flush();
//        return buffer.toByteArray();
//    }
//
//    private static byte[] optimizeClass(byte[] classBytes, String className) throws Exception {
//        var inJar = File.createTempFile("in", ".jar");
//        var outJar = File.createTempFile("out", ".jar");
//
//        try (var jos = new JarOutputStream(new FileOutputStream(inJar))) {
//            jos.putNextEntry(new JarEntry(className.replace('.', '/') + ".class"));
//            jos.write(classBytes);
//        }
//
//        var configuration = new Configuration();
//        var parser = new ConfigurationParser(PROGUARD_CONFIG.split("\n"), null);
//        parser.parse(configuration);
//
//        configuration.programJars.add(new ClassPathEntry(inJar, false));
//        configuration.programJars.add(new ClassPathEntry(outJar, true));
//
//        new ProGuard(configuration).execute();
//
//        try {
//            try (var jis = new JarInputStream(new FileInputStream(outJar))) {
//                JarEntry entry;
//                while ((entry = jis.getNextJarEntry()) != null) {
//                    if (entry.getName().endsWith(".class")) {
//                        var baos = new ByteArrayOutputStream();
//                        var buffer = new byte[1024];
//                        int bytesRead;
//                        while ((bytesRead = jis.read(buffer)) != -1) {
//                            baos.write(buffer, 0, bytesRead);
//                        }
//                        return baos.toByteArray();
//                    }
//                }
//            }
//            throw new RuntimeException("Optimized class not found");
//        } finally {
//            inJar.delete();
//            outJar.delete();
//        }
//
//    }
//
//    private static Class<?> loadOptimizedClass(String klass, byte[] bytes) throws ClassNotFoundException {
//        return new ClassLoader() {
//            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
//                return name.equals(klass) ? defineClass(name, bytes, 0, bytes.length) : super.findClass(name);
//            }
//        }.loadClass(klass);
//    }
//}
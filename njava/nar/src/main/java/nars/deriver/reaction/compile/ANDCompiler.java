package nars.deriver.reaction.compile;

import nars.term.control.AND;
import nars.term.control.PREDICATE;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

public enum ANDCompiler {
    ;

    private static final AtomicLong CLASS_COUNTER = new AtomicLong(0);

    public static <X> PREDICATE<X> compile(AND<X> a) {
        try {
            PREDICATE<X>[] predicates = a.cond;

            String className = "nars.term.control.InlinedAND" + CLASS_COUNTER.getAndIncrement();

            // Define the dynamic class
            DynamicType.Unloaded<PREDICATE/*<X>*/> dynamicType = new ByteBuddy()
                    .subclass(PREDICATE.class)
                    .name(className)
                    .defineField("predicates", PREDICATE[].class, java.lang.reflect.Modifier.PRIVATE | java.lang.reflect.Modifier.FINAL)
                    .defineConstructor(java.lang.reflect.Modifier.PUBLIC)
                    .withParameters(nars.Term.class, PREDICATE[].class)
                    .intercept(MethodDelegation.to(ConstructorInterceptor.class))
                    .method(ElementMatchers.named("test"))
                    .intercept(MethodDelegation.to(TestMethodInterceptor.class))
                    .method(ElementMatchers.named("cost"))
                    .intercept(MethodDelegation.to(CostMethodInterceptor.class))
                    .make();

            // Load the class
            Class<? extends PREDICATE> loadedClass = dynamicType
                    .load(ANDCompiler.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();

            // Instantiate the class
            return (PREDICATE<X>) loadedClass
                    .getDeclaredConstructor(nars.Term.class, PREDICATE[].class)
                    .newInstance(a.term(), predicates);
        } catch (Exception e) {
            e.printStackTrace();
            return a; // Fallback to the original AND predicate
        }
    }

    // Interceptor for the constructor
    public static class ConstructorInterceptor {
        public void intercept(@net.bytebuddy.implementation.bind.annotation.This PREDICATE<?> obj,
                              @net.bytebuddy.implementation.bind.annotation.Argument(0) nars.Term term,
                              @net.bytebuddy.implementation.bind.annotation.Argument(1) PREDICATE<?>[] predicates) throws Exception {
            // Initialize the superclass with the Term
            Field termField = PREDICATE.class.getDeclaredField("ref"); // Assuming 'ref' holds the Term
            termField.setAccessible(true);
            termField.set(obj, term); // Assuming Compound wraps Term

            // Initialize the predicates array
            Field predicatesField = obj.getClass().getDeclaredField("predicates");
            predicatesField.setAccessible(true);
            predicatesField.set(obj, predicates);
        }
    }

    // Interceptor for the 'test' method
    public static class TestMethodInterceptor<X> {
        public boolean intercept(@net.bytebuddy.implementation.bind.annotation.This PREDICATE<?> obj,
                                 @net.bytebuddy.implementation.bind.annotation.Argument(0) Object x) throws Exception {
            PREDICATE<X>[] predicates = (PREDICATE<X>[]) getPredicatesField(obj);
            for (PREDICATE<X> p : predicates)
                if (!p.test((X) x))
                    return false;
            return true;
        }

        private Object getPredicatesField(PREDICATE<?> obj) throws Exception {
            Field field = obj.getClass().getDeclaredField("predicates");
            field.setAccessible(true);
            return field.get(obj);
        }
    }

    // Interceptor for the 'cost' method
    public static class CostMethodInterceptor<X> {
        public float intercept(@net.bytebuddy.implementation.bind.annotation.This PREDICATE<?> obj) throws Exception {
            PREDICATE<X>[] predicates = (PREDICATE<X>[]) getPredicatesField(obj);
            float sum = 0.0f;
            for (PREDICATE<X> p : predicates) {
                sum += p.cost();
            }
            return sum;
        }

        private Object getPredicatesField(PREDICATE<?> obj) throws Exception {
            Field field = obj.getClass().getDeclaredField("predicates");
            field.setAccessible(true);
            return field.get(obj);
        }
    }
}
//package nars.derive.reaction.compile;
//
//import nars.Term;
//import nars.term.control.AND;
//import nars.term.control.PREDICATE;
//import org.objectweb.asm.*;
//import org.objectweb.asm.tree.ClassNode;
//import org.objectweb.asm.tree.FieldInsnNode;
//import org.objectweb.asm.tree.MethodInsnNode;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.objectweb.asm.Opcodes.*;
//
//public enum ANDCompiler {
//    ;
//
//    public static <X> PREDICATE<X> compile(AND<X> a) {
//        try {
//            var predicates = a.cond;
//
//            var className = "InlinedAND" + System.nanoTime();
//            Label endLabel = new Label(), trueLabel = new Label();
//
//            var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//            cw.visit(V23, ACC_PUBLIC, className, null, "nars/term/control/PREDICATE", null);
//
//            Map<PREDICATE<X>, Map<String, Integer>> predicateFields = new HashMap<>();
//
//            addFields(predicates, cw, predicateFields);
//
//            var mv = constructor(cw);
//
//            initPredicateFields(predicates, mv, className);
//
//            mv = predicateMethod(mv, cw);
//
//
//            inlinePredicateBodies(predicates, mv, className, predicateFields, endLabel);
//
//            finish(mv, trueLabel, endLabel, cw);
//
//            var bytecode = cw.toByteArray();
//
//            var loader = new ClassLoader(ANDCompiler.class.getClassLoader()) {
//                @Override
//                protected Class<?> findClass(String name) throws ClassNotFoundException {
//                    return name.equals(className) ? defineClass(name, bytecode, 0, bytecode.length) : super.findClass(name);
//                }
//            };
//
//            var compiledClass = loader.loadClass(className);
//
//            return (PREDICATE<X>) compiledClass
//                    .getDeclaredConstructor(Term.class, PREDICATE[].class)
//                    .newInstance(a.term(), predicates);
//        } catch (Throwable e) {
//            //throw new RuntimeException("Failed to create inlined AND", e);
//            return a;
//        }
//    }
//
//    private static MethodVisitor predicateMethod(MethodVisitor mv, ClassWriter cw) {
//        mv.visitInsn(RETURN);
//        mv.visitMaxs(4, 3);
//        mv.visitEnd();
//
//        // test method
//        mv = cw.visitMethod(ACC_PUBLIC, "test", "(Ljava/lang/Object;)Z", null, null);
//        mv.visitCode();
//        return mv;
//    }
//
//    private static <X> void initPredicateFields(PREDICATE<X>[] predicates, MethodVisitor mv, String className) {
//        // Initialize predicate fields
//        for (var i = 0; i < predicates.length; i++) {
//            var predicate = predicates[i];
//            var predicateFieldName = "predicate" + i;
//
//            mv.visitVarInsn(ALOAD, 0);
//            mv.visitVarInsn(ALOAD, 2);
//            mv.visitLdcInsn(i);
//            mv.visitInsn(AALOAD);
//            mv.visitFieldInsn(PUTFIELD, className, predicateFieldName, "Lnars/term/control/PREDICATE;");
//
//            // Copy field values
//            for (var field : predicate.getClass().getDeclaredFields()) {
//                field.setAccessible(true);
//                mv.visitVarInsn(ALOAD, 0);
//                mv.visitVarInsn(ALOAD, 2);
//                mv.visitLdcInsn(i);
//                mv.visitInsn(AALOAD);
//                mv.visitTypeInsn(CHECKCAST, predicate.getClass().getName().replace('.', '/'));
//                mv.visitFieldInsn(GETFIELD, predicate.getClass().getName().replace('.', '/'),
//                        field.getName(), Type.getDescriptor(field.getType()));
//                mv.visitFieldInsn(PUTFIELD, className, predicateFieldName + "_" + field.getName(),
//                        Type.getDescriptor(field.getType()));
//            }
//        }
//    }
//
//    private static <X> void inlinePredicateBodies(PREDICATE<X>[] predicates, MethodVisitor mv, String className, Map<PREDICATE<X>, Map<String, Integer>> predicateFields, Label endLabel) {
//        for (var i = 0; i < predicates.length; i++) {
//            var predicate = predicates[i];
//            inlinePredicateBody(mv, predicate, className, "predicate" + i, predicateFields.get(predicate));
//            mv.visitJumpInsn(IFEQ, endLabel);
//        }
//    }
//
//    private static void finish(MethodVisitor mv, Label trueLabel, Label endLabel, ClassWriter cw) {
//        mv.visitJumpInsn(GOTO, trueLabel);
//
//        mv.visitLabel(endLabel);
//        mv.visitInsn(ICONST_0);
//        mv.visitInsn(IRETURN);
//
//        mv.visitLabel(trueLabel);
//        mv.visitInsn(ICONST_1);
//        mv.visitInsn(IRETURN);
//
//        mv.visitMaxs(0, 0);  // Let ASM compute the max stack and locals
//        mv.visitEnd();
//
//        cw.visitEnd();
//    }
//
//    private static MethodVisitor constructor(ClassWriter cw) {
//        // Constructor
//        var mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lnars/Term;[Lnars/term/control/PREDICATE;)V", null, null);
//        mv.visitCode();
//        mv.visitVarInsn(ALOAD, 0);
//        mv.visitVarInsn(ALOAD, 1);
//        mv.visitMethodInsn(INVOKESPECIAL, "nars/term/control/PREDICATE", "<init>", "(Lnars/Term;)V", false);
//        return mv;
//    }
//
//    private static <X> void addFields(PREDICATE<X>[] predicates, ClassWriter cw, Map<PREDICATE<X>, Map<String, Integer>> predicateFields) {
//        var fieldIndex = 0;
//        // Add fields for each predicate instance
//        for (var i = 0; i < predicates.length; i++) {
//            var predicate = predicates[i];
//            var predicateFieldName = "predicate" + i;
//            cw.visitField(ACC_PRIVATE | ACC_FINAL,
//                    predicateFieldName,
//                    "Lnars/term/control/PREDICATE;", null, null);
//
//            Map<String, Integer> fieldMap = new HashMap<>();
//            for (var field : predicate.getClass().getDeclaredFields()) {
//                var fn = field.getName();
//                var fieldName = predicateFieldName + "_" + fn;
//                cw.visitField(ACC_PRIVATE, fieldName, Type.getDescriptor(field.getType()), null, null);
//                fieldMap.put(fn, fieldIndex++);
//            }
//            predicateFields.put(predicate, fieldMap);
//        }
//    }
//
//    private static <X> void inlinePredicateBody(MethodVisitor mv, PREDICATE<X> predicate, String className,
//                                     String predicateFieldName, Map<String, Integer> fieldMap) {
//        var predicateClassName = predicate.getClass().getName();
//        try {
//            inlinePredicateBody(mv, className, predicateFieldName, fieldMap, new ClassReader(predicateClassName), predicateClassName);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//
//    private static void inlinePredicateBody(MethodVisitor mv, String className, String predicateFieldName, Map<String, Integer> fieldMap, ClassReader cr, String predicateClassName) {
//        var classNode = new ClassNode();
//        cr.accept(classNode, 0);
//
//        for (var methodNode : classNode.methods) {
//            if (methodNode.name.equals("test") && methodNode.desc.equals("(Ljava/lang/Object;)Z")) {
//                var insnList = methodNode.instructions;
//                var s = insnList.size();
//                for (var i = 0; i < s; i++) {
//                    var insn = insnList.get(i);
//                    if (insn instanceof FieldInsnNode fieldInsn && insn.getOpcode() == GETFIELD) {
//                        if (fieldMap.containsKey(fieldInsn.name)) {
//                            // Replace field access with access to our copied field
//                            mv.visitVarInsn(ALOAD, 0);
//                            mv.visitFieldInsn(GETFIELD, className,
//                                    predicateFieldName + "_" + fieldInsn.name, fieldInsn.desc);
//                        } else {
//                            insn.accept(mv);
//                        }
//                    } else if (insn instanceof MethodInsnNode methodInsn) {
//                        if (methodInsn.owner.equals(predicateClassName.replace('.', '/'))) {
//                            // If it's a method call on the predicate itself, we need to use our field
//                            mv.visitVarInsn(ALOAD, 0);
//                            mv.visitFieldInsn(GETFIELD, className, predicateFieldName,
//                                    "Lnars/term/control/PREDICATE;");
//                            mv.visitTypeInsn(CHECKCAST, predicateClassName.replace('.', '/'));
//                        }
//                        insn.accept(mv);
//                    } else {
//                        insn.accept(mv);
//                    }
//                }
//                break;
//            }
//        }
//    }
//}
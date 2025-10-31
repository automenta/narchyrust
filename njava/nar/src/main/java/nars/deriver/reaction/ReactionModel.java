package nars.deriver.reaction;

import jcog.TODO;
import nars.Deriver;
import nars.Premise;
import nars.action.Action;
import nars.term.control.*;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static jcog.Str.indent;
import static jcog.io.Serials.jsonNode;

/**
 * compiled derivation rules. can be shared by multiple Derivers
 * what -> can
 * TODO subclass to Weighted deriver runner; and make a non-weighted subclass
 */
public class ReactionModel {

    public final PREDICATE<Deriver> what;

    public ReactionModel(PREDICATE<Deriver> what, Action[] how) {
        this.what = what;
//        this.cause = Util.map(x -> x.why, new Cause[how.length], how);

        //print();
        //System.out.println(analyze());
    }

    public ConditionAnalysis analyze() {
        return new ConditionAnalysis(what);
    }


    public ReactionModel print() {
        return print(System.out);
    }

    public ReactionModel print(PrintStream p) {
        print(p, 0);
        p.println(analyze());
        return this;
    }

    public void print(PrintStream p, int indent) {
        print(what, p, indent);
    }

    protected static void print(Object x, PrintStream out, int indent) {

        indent(indent);

        switch (x) {
            case ReactionModel r -> r.print(out, indent);
            case Action a -> {

                //out.println(a.why.id + " ==> {");
                Object aa;
                // + ((PremisePatternAction.TruthifyDeriveAction) a).unify;
                //TODO
                //                out.println(((DirectPremiseUnify)x).taskPat + ", " + ((DirectPremiseUnify)x).beliefPat + " ==> {");
                //                print(((DirectPremiseUnify)x).taskify, out, indent + 2);
                //                Texts.indent(indent);
                //                out.println("}");
//            if (a instanceof NA) {
//                PatternReactionBuilder.PatternReaction.DeriveTaskAction td = (PatternReactionBuilder.PatternReaction.DeriveTaskAction) a;
//                td.source()
//                aa = td.taskPattern + ", " + td.beliefPattern + " |- " + td.taskify + " " + td.truthify; //Arrays.toString(td.constraints) + " ...";
//            } else
                aa = a.toString();


                print(aa, out, 0);

                //Texts.indent(indent);out.println("}");
            }
            case AND and -> {
                out.println("and {");
                for (var c : and.conditions())
                    print(c, out, indent + 2);
                indent(indent);
                out.println("}");
            }
            case FORK fork -> {

                out.println("fork {");
                for (var b : fork.branches())
                    print(b, out, indent + 2);
                indent(indent);
                out.println("}");
            }
            case IF iff -> {
                indent(indent); out.print("if (\n");
                print(iff.cond, out, indent+2);
                indent(indent); out.print(") {\n");
                print(iff.ifTrue, out, indent+4);
                indent(indent); out.print("} else {\n");
                print(iff.ifFalse, out, indent+4);
                indent(indent); out.println("}");
            }
            case SWITCH aSwitch -> throw new TODO();
            case null, default -> {
                out.print( /*Util.className(p) + ": " +*/ x);
                out.println();
            }
        }


    }

//    public void runAll(int n, Deriver d) {
//        short[] s = d.hows;
//        int offset = d.howOffset;
//        for (int i = 0; i < n; i++)
//            run(s[i] - offset, d);
//    }
//    private void run(int x, Deriver d) {
//        if (NAL.derive.ACTION_METHODHANDLE) {
//            runMH(x, d);
//        } else {
//            how[x].test(d);
//        }
//    }

//    private void runMH(int x, Deriver d) {
//        try {
//            howMethods[x].invokeExact(d);
//        } catch (Throwable throwable) {
//            throw new RuntimeException(throwable);
//        }
//    }


//    private IntConsumer runnerMH(Deriver d) {
//        MethodHandle[] howD = Util.map(z -> z.bindTo(d), new MethodHandle[howMethods.length], howMethods);
//        return n -> {
//            short[] s = d.hows;
//            int offset = d.howOffset;
//            try {
//                for (int i = 0; i < n; i++)
//                    howD[s[i] - offset].invokeExact();
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }

//    public Runnable runner(Deriver d) {
//        return _runner(d);
//    }
//
//    private Runnable _runner(Deriver d) {
//        return () -> what.test(d);
//        //return () -> PREDICATE.test(what, d);
//    }

    @Nullable
    public BiFunction<Premise, Deriver,Premise> premisePreProcessor = null;

    public Premise pre(Premise p, Deriver d) {
        return premisePreProcessor != null ? premisePreProcessor.apply(p, d) : p;
    }

//    /** may confuse the JIT if it doesn't see that each bound Deriver instance isn't the same
//     *  "When you create multiple method handles, each bound to a different Deriver instance, it can potentially impact the JIT (Just-In-Time) compiler's ability to optimize the code effectively. The JIT compiler relies on code sharing and inlining to optimize performance, and creating many different method handles bound to different instances may hinder its ability to do so."
//     * */
//    private  Runnable testerMHBound(Deriver d) {
//        var w = what.method().bindTo(d).asType(V);
//        return () -> {
//            try {
//                w.invokeExact();
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }
//
//    private  Runnable testerMH(Deriver d) {
//        var w = what.method().asType(T);
//        return () -> {
//            try {
//                w.invokeExact(d);
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }

    public static class ConditionAnalysis implements Consumer<PREDICATE<Deriver>> {
        public int ifs;
        public int forks;
        public long forkBranchSum;
        public int and;
        public long andConditionSum;

        public ConditionAnalysis(PREDICATE<Deriver> c) {
            accept(c);
        }

        @Override public String toString() {
            return jsonNode(this).toString();
        }

        @Override
        public void accept(PREDICATE<Deriver> x) {
            switch (x) {
                case AND a -> {
                    and++;
                    int c = 0;
                    for (var xx : a.conditions()) {
                        accept((PREDICATE<Deriver>) xx);
                        c++;
                    }
                    andConditionSum += c;
                }
                case FORK f -> {
                    forks++;
                    int b = 0;
                    for (var xx : f.branches()) {
                        accept((PREDICATE<Deriver>) xx);
                        b++;
                    }
                    forkBranchSum += b;
                }
                case IF i -> {
                    ifs++;
                    accept(i.cond);
                    accept(i.ifTrue);
                    accept(i.ifFalse);
                }
                case null, default -> {
                }
                //throw new TODO();
            }
        }

    }

//    private static final MethodType T = MethodType.methodType(void.class, Deriver.class);
//    private static final MethodType V = MethodType.methodType(void.class);
//    private static MethodHandle[] methodHandles(Action[] how) {
//        return Util.arrayOf(i -> how[i].method().asType(T), new MethodHandle[how.length]);
//    }
//    /** TODO wrap runnerMH/runnerInvoke in this */
//    public PremiseRunner run() {
////        return howMethods != null ?
////            new PremiseRunnerMethodHandles()
////            :
//          return new PremiseRunnerInvoke();
////        } else
////            return new CachedPremiseRunner(d, cache);
//    }


//    public abstract static class PremiseRunner {
//        public abstract boolean run(int i, Deriver d);
//    }
//
//    private final class PremiseRunnerInvoke extends PremiseRunner {
//        @Override
//        public boolean run(int x, Deriver d) {
//            return how[x].test(d);
//        }
//    }

//    private final class PremiseRunnerMethodHandles extends PremiseRunner {
//        @Override
//        public void run(int x, Deriver d) {
//            try {
//                howMethods[x].invokeExact(d);
//            } catch (Throwable throwable) {
//                throw new RuntimeException(throwable);
//            }
//        }
//    }

}


//    private Consumer<Deriver> runnerCompile() {
//        final String className = ReactionModel.class.getSimpleName() + hashCode();
//
//        StringBuilder source = new StringBuilder(1024);
//        source.append(
//            "import " + Consumer.class.getName() + ";" +
//            "import " + Deriver.class.getName() + ";" +
//            "public class " + className + " implements Consumer<Deriver> {" +
//                "@Override public void accept(Object d) {" +
//                    "System.out.println(\"x\");" +
//                "}" +
//            "}"
//        );
//
//
//        try {
//            final ISimpleCompiler cc = CompilerFactoryFactory.getDefaultCompilerFactory().newSimpleCompiler();
//            cc.cook(source.toString());
//
//            ClassLoader cll = cc.getClassLoader();
//
//            Class<?> cl = cll.loadClass(className);
//            return (Consumer<Deriver>) cl.getConstructor().newInstance();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private Consumer<Deriver> runnerCompile() {
//        // Start building the class.
//        // Add the main method.
//        // Compose the equivalent of this java code:
//        //     System.out.println("Hello, world!");
//        // We don't need to preverify simple code that doesn't have
//        // special control flow. It works fine without a stack map
//        // table attribute.
//        // Retrieve the final class.
//        final String className = ReactionModel.class.getSimpleName() + BinTxt.uuid128();
//
//        ProgramClass c = new ClassBuilder(
//                VersionConstants.CLASS_VERSION_14,
//                AccessConstants.PUBLIC,
//                className,
//                ClassConstants.NAME_JAVA_LANG_OBJECT)
//                .addInterface(Consumer.class.getName())
//                .addMethod(AccessConstants.PUBLIC,  "<init>", "()V")
//                .addMethod(
//                        AccessConstants.PUBLIC |
//                                AccessConstants.STATIC,
//                        "main",
//                        "([Ljava/lang/String;)V",
//                        50,
//
//                        // Compose the equivalent of this java code:
//                        //     System.out.println("Hello, world!");
//                        code -> code
//                                .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
//                                .ldc("x")
//                                .invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
//                                .return_())
//
//                // We don't need to preverify simple code that doesn't have
//                // special control flow. It works fine without a stack map
//                // table attribute.
//
//                // Retrieve the final class.
//                .getProgramClass();
//
//        DynBytes o = new DynBytes(16 * 1024);
//        new ProgramClassWriter(o).visitProgramClass(c);
//
//
//        Map<String, byte[]> m = new UnifriedMap();
//        m.put(className, o.arrayCompactDirect());
//
//        try {
//            final Class<?> cl = new ByteArrayClassLoader(getClass().getClassLoader(), m).loadClass(className);
//            return (Consumer<Deriver>) cl.getConstructor().newInstance();
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }
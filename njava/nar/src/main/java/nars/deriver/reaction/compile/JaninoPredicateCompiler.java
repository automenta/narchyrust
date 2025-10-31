package nars.deriver.reaction.compile;

import nars.term.control.*;
import org.codehaus.janino.SimpleCompiler;

import java.util.concurrent.atomic.AtomicInteger;

public class JaninoPredicateCompiler {
    private static final AtomicInteger classCounter = new AtomicInteger();

    public static <X> PREDICATE<X> compile(PREDICATE<X> composedPredicate) throws Exception {
        var className = "CompiledPredicate" + classCounter.getAndIncrement();

        var src = generateSourceCode(className, composedPredicate);

        var compiler = new SimpleCompiler();
        compiler.cook(src);

        var compiled = compiler.getClassLoader().loadClass("nars.term.control." + className);
        return (PREDICATE<X>) compiled.getDeclaredConstructor().newInstance();
    }

    private static <X> String generateSourceCode(String className, PREDICATE<X> predicate) {
        // Generate Java source code that overrides the test and cost methods
        // Embed the logic of composedPredicate.test(x) directly
        return "package nars.term.control;\n" +
               "public class " + className + " extends PREDICATE {\n" +
               "    public boolean test(Object x) {\n" +
               "        " + code(predicate) + ";  return true;\n" +
               "    }\n" +
               "    public float cost() {\n" +
               "        " + predicate.cost() + "f;\n" +
               "    }\n" +
               "}";
    }

    /** Recursively generate the test logic based on predicate composition */
    private static <X> String code(PREDICATE<X> p) {
        if (p instanceof IF ifPredicate) {
            return codeIf(ifPredicate);
        } else if (p instanceof AND andPredicate) {
            return codeAnd(andPredicate);
        } else if (p instanceof FORK forkPredicate) {
            return codeFork(forkPredicate);
        } else if (p instanceof NOT np) {
            return "(!" + code(np.cond) + ")";
        } else if (p == PREDICATE.TRUE) {
            return "true";
        } else if (p == PREDICATE.FALSE) {
            return "false";
        } else {
            return "predicate.test(x)"; // For other predicates, delegate to the original test method
        }
    }

    private static String codeIf(IF ifPredicate) {
        return "if (" + code(ifPredicate.cond) + ") {" + code(ifPredicate.ifTrue) + "; } else { " + code(ifPredicate.ifFalse) + "; }\n";
//        return "(" + code(ifPredicate.cond) + " ? " +
//                code(ifPredicate.ifTrue) + " : " +
//                code(ifPredicate.ifFalse) + ")";
    }

    private static String codeFork(FORK f) {
        var sb = new StringBuilder();
        for (var b : f.branch)
            sb.append(code(b)).append(".test(x);\n");
        return sb.toString();
    }

    private static String codeAnd(AND andPredicate) {
        var sb = new StringBuilder();
        for (var i = 0; i < andPredicate.cond.length; i++) {
            if (i > 0) sb.append(" && ");
            sb.append(code(andPredicate.cond[i]));
        }
        return "if (!(" + sb + ")) return false;\n";
    }
}

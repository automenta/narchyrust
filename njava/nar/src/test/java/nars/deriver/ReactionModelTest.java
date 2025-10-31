package nars.deriver;

import jcog.data.list.Lst;
import nars.NAR;
import nars.NARS;
import nars.deriver.reaction.ReactionModel;
import nars.deriver.reaction.Reactions;
import nars.test.TestNAR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;
import static nars.premise.NALPremise.NonSeedTaskPremise.temporal;
import static org.junit.jupiter.api.Assertions.*;

class ReactionModelTest {

    final NAR nar = NARS.shell();

    private final List<TestNAR> tests = new Lst<>();

//    private static void print(NAR n, PrintStream p) {
//        derivers(n).forEach(d -> {
//            p.println(d);
//            d.rules.printRecursive(p);
//            p.println();
//        });
//    }

    private ReactionModel testCompile(String... rules) {
        return testCompile(false, rules);
    }

    private ReactionModel testCompile(boolean debug, String... rules) {

        assertNotEquals(0, rules.length);


        Reactions src = new Reactions().rules(rules);
        assertNotEquals(0, src.size());
		return src.compile(nar);
	}

//    @Test
//    void printTrie() {
//
//        print(NARS.tmp(), System.out);
//    }

//    @Test
//    void testConclusionWithXTERNAL() {
//        NAR n = NARS.shell();
//        PatternIndex idx = new PatternIndex(n) {
//            @Override
//            public @Nullable Termed get(@NotNull Term x, boolean create) {
//                Termed u = super.get(x, create);
//                assertNotNull(u);
//                if (u != x) {
//                    System.out.println(x + " (" + x.getClass() + ")" + " -> " + u + " (" + u.getClass() + ")");
//                    if (u.equals(x) && u.getClass().equals(x)) {
//                        fail("\t ^ same class, wasteful duplicate");
//                    }
//                }
//                return u;
//            }
//        };
//
//
//        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(idx,
//                "Y, Y, task(\"?\") |- (?1 &| Y), (Punctuation:Question)",
//                "X, X, task(\"?\") |- (?1 &&+- X), (Punctuation:Question)"
//        ));
//
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        d.printRecursive(new PrintStream(baos));
//
//
//        String ds = new String(baos.toByteArray());
//        System.out.println(ds);
//        assertTrue(ds.contains("?2&|"));
//        assertTrue(ds.contains("?2 &&+-"));
//
//
//    }

    @Test
    void testAmbiguousPunctuation() {
        assertThrows(RuntimeException.class,
            () -> new Reactions()
                .rules("Y, Y |- (?1 &| Y), ()")
                .compile(nar)
        );
    }

    @Test
    void testCompile() {
        testCompile(
                "(A --> B), (B --> C), neqRCom(A,C) |- (A --> C), (Belief:Deduction, Goal:Deduction)"
        );

    }

    @Test
    void testCompilePatternOpSwitch() {
        ReactionModel c = testCompile(
            "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
            "(A ==> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        );
        c.print();

    }

//    @Test
//    void testConclusionFold() {
//
//        TestNAR t = test(
//        "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
//                "(A --> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
//        );
//
//        t.ask("(a-->b)")
//                .mustQuestion(256, "(a ==> b)")
//                .mustQuestion(256, "(a ==>+- a)")
//        ;
//
//    }

//    @Test
//    void testDeriveQuest() {
//
//        int cycles = 64;
//        test("(P --> S), (S --> P), task(\"?\") |- (P --> S),   (Punctuation:Quest)")
//                .ask("b:a")
//                .believe("a:b")
//                .mustOutput(cycles, "b:a", QUEST)
//                .run(cycles)
//                ;
//    }

//    private TestNAR test(String... rules) {
//        NAR n = new NARS().get();
//		//TODO instantiate DeriverExecutor
//		standard(new DeriverRules(n, rules)).compile();
//		n.synch();
//        TestNAR t = new TestNAR(n);
//        tests.add(t);
//        return t;
//    }

    @AfterEach
    void runTests() {
        for (TestNAR test : tests) {
            test.run();
        }
    }

    @Deprecated @Test void Temporal() {
        assertFalse(temporal($$("(x==>y)")));
        assertTrue(temporal($$("(x ==>+1 y)")));
    }

}
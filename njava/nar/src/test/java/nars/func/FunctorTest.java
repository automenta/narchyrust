package nars.func;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Term;
import nars.eval.Evaluation;
import nars.eval.Evaluator;
import nars.term.Compound;
import nars.term.Functor;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;


class FunctorTest {

    final TestNAR n = new TestNAR(NARS.tmp());

    @Deprecated
    static Set<Term> eval(Compound x, NAR n) {
        Evaluation.All all = new Evaluation.All();

        if (Functor.evalable(x)) {
            Evaluator evaluator = new Evaluator(n::axioms);
            Term[] queries = {x};

            assert (queries.length > 0);

            Evaluation e = new Evaluation(evaluator, all);

            for (Term x1 : queries) {
                if (x1 instanceof Compound) //HACK
                    e.eval(x1);
            }

        } else {
            all.test(x); //didnt need evaluating, just input
        }
        return all.the();
    }

    @Test
    void testImmediateTransformOfInput() throws Narsese.NarseseException {

        NAR n = this.n.nar;

        Term y = $$("[a,b]");

        boolean[] got = {false};
        //String s = t.toString();
        //assertFalse(s.contains("union"));
        n.main().onTask(t -> {
            if (t.BELIEF() && t.term().equals(y))
                got[0] = true;
            //String s = t.toString();
            //assertFalse(s.contains("union"));
        }, BELIEF);
        n.input("union([a],[b]).");

        n.run(1);

        assertTrue(got[0]);

        assertNotNull(n.beliefTruth(y, ETERNAL));
    }

    @Disabled @Test
    void testAdd1() {

        n.input("add(1,2,#x)!");
        n.run(16);
        n.input("add(4,5,#x)!");
        n.run(16);
    }

    @Disabled @Test
    void testAdd1Temporal() {


        n.input("add(1,2,#x)! |");
        n.run(16);
        n.input("add(4,5,#x)! |");
        n.run(16);
    }

    /** tests correct TRUE fall-through behavior, also backward question triggered execution */
    @Test
    void testFunctor1() {


        n.nar.freqRes.set(0.25f);


        n.believe("((complexity($1)<->3)==>c3($1))");
        n.question("c3(x:y)");



        n.mustBelieve(128, "c3(x:y)", 1f, 0.81f);


        n.run();

    }

    static final int cycles = 64;


    @Test
    void testFunctor2() {
        

        n.volMax(18);
        
        n.believe("(c({$1,$2}) ==> (complexity($1)=complexity($2)))");
        n.question("c({x, y})");
        n.question("c({x, (x)})");
        n.mustBelieve(cycles, "(complexity((x))=complexity(x))", 0f, 0.90f);
        n.mustBelieve(cycles, "c({x,y})", 1f, 0.81f);
        n.mustBelieve(cycles, "c({x,(x)})", 0f, 0.81f);
        n.run();
    }

    @Disabled
    @Test
    void testExecutionResultIsCondition() throws Narsese.NarseseException {
        NAR d = NARS.tmp();
        d.input("(add($x,1,$y) ==> ($y <-> inc($x))).");
        d.input("((inc(2) <-> $x) ==> its($x)).");
        d.run(cycles);
    }

    @Test
    @Disabled void testAnon1() {
        NAR d = NARS.shell();
        Set<Term> result = eval($$c("anon((a,b),#x)"), d);
        assertEquals("[anon((a,b),(_1,_2))]", result.toString());
    }

    @Test
    void testAnon2() throws Narsese.NarseseException {
        NAR d = NARS.shell();

        d.input("anon((a,b),#x)?");
        d.run(3);
    }






















}
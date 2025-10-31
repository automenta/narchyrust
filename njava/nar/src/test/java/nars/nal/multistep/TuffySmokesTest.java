package nars.nal.multistep;

import nars.Concept;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.QUESTION;
import static nars.Op.XTERNAL;

/**
 * Created by me on 1/28/16.
 * TODO add unit test conditions
 */
@Disabled
public class TuffySmokesTest {

    @Test
    void tuffy() throws Narsese.NarseseException {

        NAR n = NARS.tmp(6);

        n.complexMax.set(24);
        n.time.dur(200);

        n.log();
//        n.logPriMin(System.out, 0f);

        axioms(n);
        input(n);


        //TEMPORARY
        n.main().onTask(t -> {
            if (t.QUESTION() && t.term().IMPL() && t.term().dt()==XTERNAL && t.term().sub(0).equals(t.term().sub(1))) {
                System.err.println(t);
            }
        }, QUESTION);

        n.run(5000);


        print(n);
		/*
		0.81	Cancer(Edward)
		0.66	Cancer(Anna)
		0.50	Cancer(Bob)
		0.45	Cancer(Frank)
		 */
    }


    void print(NAR n) {
        for (String name : new String[]{"Edward", "Anna", "Bob", "Frank", "Gary", "Helen"}) {
            String t = "(" + name + " --> Cancer)";

            Concept c = n.concept(t);
            if (c == null)
                continue;
            //assertNotNull(c);

            c.beliefs().print(System.out);
            System.out.println();

//            System.err.print(System.identityHashCode(c) + " ");
//
//            if (c == null) {
//                System.err.println(t + " unknown" + " ");
//            } else {
//
//
//            }

        }
    }

    void input(NAR n) throws Narsese.NarseseException {
        n.input("<(Anna&Bob) --> Friends>. %1.00;0.90%");
        n.input("<(Anna&Edward) --> Friends>. %1.00;0.90%");
        n.input("<(Anna&Frank) --> Friends>. %1.00;0.90%");
        n.input("<(Edward&Frank) --> Friends>. %1.00;0.90%");
        n.input("<(Gary&Helen) --> Friends>. %1.00;0.90%");
        n.input("<(Gary&Frank) --> Friends>. %0.00;0.90%");

        n.input("<Anna --> Smokes>. %1.00;0.90%");
        n.input("<Edward --> Smokes>. %1.00;0.90%");
        n.input("<Gary --> Smokes>. %0.00;0.90%");
        n.input("<Frank --> Smokes>. %0.00;0.90%");
        n.input("<Helen --> Smokes>. %0.00;0.90%");
        n.input("<Bob --> Smokes>. %0.00;0.90%");

    }

    void axioms(NAR n) throws Narsese.NarseseException {


        // Smokes(x) => Cancer(x)
        n.input("$0.9 (($x --> Smokes) ==> ($x --> Cancer)). %1.00;0.90%");


        // Friends(x, y) => (Smokes(x) <=> Smokes(y))
        n.input("$0.9 ( (((#1 & $2) --> Friends) &&   (#1-->Smokes)) ==>   ($2-->Smokes)). %1.00;0.90%");
        n.input("$0.9 ( (((#1 & $2) --> Friends) && --(#1-->Smokes)) ==> --($2-->Smokes)). %1.00;0.90%");

        n.input("$1.0 (?1 --> Cancer)?");

    }


}
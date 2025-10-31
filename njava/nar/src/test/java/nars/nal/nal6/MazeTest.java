package nars.nal.nal6;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Term;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.util.OpExec;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.CONJ;
import static nars.Op.IMPL;

/** TODO junit */
@Disabled
class MazeTest {

    static Term f(int x, int y) {
        return $.p(x, y);
    }
    static Term f(String f, int x, int y) {
        return $.inh($.p(x, y), f);
    }

    @Test
    void test1() {
        NAR n = NARS.tmp();
//        n.log();
        n.beliefPriDefault.pri(0.1f);
        n.goalPriDefault.pri(1f);

        int size = 3;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                n.believe(IMPL.the(f(x, y), +1, f(x+1, y)));
                n.believe(IMPL.the(f(x, y), +1, f(x, y+1)));
            }
        }

        n.believe(f(1,1), Tense.Present);
        n.want(f(size-1,size-1), 1f, 0.9f, Tense.Present);

        n.run(1000);

    }

    /** with distinction between at and go, and feedback */
    @Test  void test2() {
        NAR n = NARS.tmp();

        n.complexMax.set(24);
        n.log();
        n.beliefPriDefault.pri(0.1f);
        n.goalPriDefault.pri(1f);

        n.add(Atomic.atom("go"), new OpExec((x, nar) -> {
            System.err.println("GO " + x);
            n.believe($.func("at", x.sub(0).subterms().arrayShared()));
        }, 0f));

        int size = 3;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                n.believe(IMPL.the(f("at",x, y), +1, f("go", x+1, y)));
                n.believe(IMPL.the(f("at", x, y), +1, f("go", x, y+1)));

                n.believe(IMPL.the(CONJ.the(f("at", x, y), f("go", x+1, y)), +1, f("at",x+1, y)));
                n.believe(IMPL.the(CONJ.the(f("at", x, y), f("go", x, y+1)), +1, f("at", x, y+1)));
            }
        }

        n.believe(f("at",1,1), Tense.Present);
        n.want(f("at",size-1,size-1), 1f, 0.9f, Tense.Eternal);

        n.run(1000);

    }

}
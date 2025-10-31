package nars.nal.nal5;

import jcog.data.list.Lst;
import nars.NALTask;
import nars.NAR;
import nars.NARS;
import nars.Term;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.fail;

class ConsistencyTest {

    @Test
    void test1() {
        Term xAndNotY = $$("(x && --y)");
        Term notXAndY = $$("(--x && y)");

        new ConsistencyModel()
                .input("(x && y)", "(--x && --y)")
                .validBelief((t) -> {
                    Term tt = t.term();
                    if (t.conf() > 0.01f) {
                        return !tt.CONJ() || (!tt.equals(xAndNotY) && !tt.equals(notXAndY));
                    }
                    return true;
                }).run(NARS.tmp(), 1000);
    }

    static class ConsistencyModel {

        /**
         * inputs to be believeed; may be negated
         */
        final Set<Term> inputBeliefs = new LinkedHashSet();
        final List<Predicate<NALTask>> validBelief = new Lst();

        ConsistencyModel() {

        }

        ConsistencyModel input(String... s) {
            for (String x : s) {
                inputBeliefs.add($$(x));
            }
            return this;
        }

        ConsistencyModel validBelief(Predicate<NALTask> p) {
            this.validBelief.add(p);
            return this;
        }

        boolean validBelief(NALTask t) {
            for (Predicate<NALTask> p : validBelief) {
                if (!p.test(t))
                    return false;
            }
            return true;
        }

        void run(NAR n, int cycles) {
//            n.log();
            for (Term x : inputBeliefs)
                n.believe(x);

            n.run(cycles);

            n.concepts().flatMap(c -> c.beliefs().taskStream()).forEach(t -> {
                if (!validBelief(t))
                    fail(() -> t.toString() + " invalid");
                if (t.POSITIVE() && inputBeliefs.contains(t.term().neg()))
                    fail(() -> t + " contradicts negative input");
                if (t.NEGATIVE() && inputBeliefs.contains(t.term()))
                    fail(() -> t + " contradicts positive input");
            });
        }
    }
}
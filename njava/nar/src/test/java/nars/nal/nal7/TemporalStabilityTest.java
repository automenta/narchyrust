package nars.nal.nal7;

import nars.NALTask;
import nars.NAR;
import nars.Task;
import nars.Term;
import nars.term.Compound;

import static nars.Op.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertFalse;


abstract class TemporalStabilityTest {

    private boolean unstable;


    public void test(int cycles, NAR n) {
        //n.log();
        n.complexMax.set(16);
        n.freqRes.set(0.1f);
        n.confRes.set(0.02f);

        n.main().eventTask.on(this::validate);

        input(n);

        if (cycles > 0)
            n.run(cycles);

        assertFalse(unstable);
    }

    private long minInput = ETERNAL, maxInput = ETERNAL;

    private void validate(Task _t) {
        NALTask t = (NALTask) _t;

        long ts = t.start();
        long te = Math.max(ts + t.term().seqDur(), t.end());

        if (t.isInput()) {
            System.out.println("in: " + t);
            if (!t.ETERNAL()) {
                if (minInput == ETERNAL || minInput > ts)
                    minInput = ts;
                if (maxInput == ETERNAL || maxInput < te)
                    maxInput = te;
            }
        } else {
            if (t.QUESTION_OR_QUEST())
                return; //ignore. it is natural for it to be curious!!!!

            if (ts < minInput || te > maxInput + (maxInput - minInput)) {
                System.err.println("  OOB: " + '\n' + t.proof() + '\n');
                unstable = true;
            } else if (!validOccurrence(ts) || (ts!=te && !validOccurrence(te)) || refersToOOBEvents(t)) {
                System.err.println("  instability: " + '\n' + t.proof() + '\n');
                unstable = true;
            }
        }
    }

    private boolean refersToOOBEvents(NALTask t) {
        long s = t.start();
        if (s == ETERNAL)
            return false;
        Term tt = t.term();
        if (tt instanceof Compound tc)
            return tc.condsAND((r, xt) -> !validOccurrence(s + r), 0, false, false, false);
        else
            return !validOccurrence(s);
    }


    protected abstract boolean validOccurrence(long o);

    /**
     * inputs the tasks for a test
     */
    protected abstract void input(NAR n);
}
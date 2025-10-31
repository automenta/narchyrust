package nars.control;

import jcog.Is;
import jcog.Research;
import jcog.pri.Prioritizable;
import nars.$;
import nars.Term;
import nars.deriver.reaction.PatternReaction;
import nars.term.Termed;
import nars.term.atom.Int;

/**
 * 'cause (because)
 * represents a registered causal influence for analyzing its
 * positive and negative influence in system activity via
 * 'causal traces' attached to Tasks.
 * <p>
 * multiple threads can safely affect the accumulators. it must be commited
 * periodically (by a single thread, ostensibly) to apply the accumulated values
 * and calculate the values
 * <p>
 * as reported by the value() function which represents the effective
 * positive/negative balance that has been accumulated. a decay function
 * applies forgetting, and this is applied at commit time by separate
 * positive and negative decay rates.  the value is clamped to a range
 * (ex: 0..+1) so it doesn't explode.
 * <p>
 * https://cogsci.indiana.edu/pub/parallel-terraced-scan.pdf
 */
@Research
@Is("Credit_assignment")
public class Cause<X> implements Comparable<Cause>, Prioritizable, Termed {

    /**
     * internally assigned id
     */
    public final short id;
    public final Term ID;
    public final X name;

    /**
     * current scalar utility estimate for this cause's support of the current MetaGoal's.
     * may be positive or negative, and is in relation to other cause's values
     */
    @Deprecated public volatile float value = 0;

    /** an effective priority value */
    public volatile float pri = 1;



    //private static final Logger logger = Log.log(Cause.class);

    public Cause(short id, X name) {
        //logger.info("{} {}", id, name);
        this.id = id;
        this.ID = Int.i(id);
        this.name = name;
    }

//    /**
//     * estimate the priority factor determined by the current value of priority-affecting causes
//     */
//    public static double pri(Lst<Cause> values, short[] effect) {
//
//        int n = effect.length;
//        if (n == 0) return 0;
//
//        double value = 0;
//        Object[] vv = values.array();
//        for (short c : effect)
//            value += ((Cause) vv[c]).pri();
//
//
//        return value / n;
//    }

    public final float pri() {
        return pri;
    }
    public float value() {
        return value;
    }

    /** set NaN if no value actually accumulated during the last cycle */
    public void value(float value) {
        this.value = value;
    }

    /**
     * value may be in any range (not normalized); 0 is neutral
     */
    public void pri(float p) {
        //assert(Float.isFinite(nextValue));
        this.pri = p;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public Term term() {
        return $.identity(name);
    }

    @Override
    public String toString() {
        if (name instanceof PatternReaction r) {
            return r.tag + ":\t" + r.source; //HACK
        } else {
            return name.toString();// + "[" + id + "]=" + super.toString();
        }
    }

    @Override
    public int compareTo(Cause o) {
        return Short.compare(id, o.id);
    }

}
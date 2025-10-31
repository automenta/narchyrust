package nars.table.eternal;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.dynamic.DynBeliefTable;
import nars.task.EternalTask;
import nars.truth.AbstractMutableTruth;
import nars.truth.MutableTruth;

import static nars.Op.BELIEF;

/** provides an overriding eternal default answer only if the Answer has found no other options in other tables.
 *  should be added only to the end of BeliefTables
 *  TODO separate the strong,weak functionality into a subclass. leave the superclass use the default functionality
 *  */
public class EternalDefaultTable extends DynBeliefTable {

    private final EternalTask task;
    public final AbstractMutableTruth truth;

    private EternalDefaultTable(Term c, Truth t, byte punc, NAR n) {
        super(c, punc == BELIEF);

        EternalTask tt = (EternalTask) NALTask.taskEternal(c, punc, truth = new MutableTruth(t), n);
        tt.pri(n.priDefault(punc));

        this.task = tt;
    }

    public static EternalDefaultTable add(TaskConcept c, float freq, NAR n) {
        return add(c, $.t(freq, n.beliefConfDefault.conf()), n);
    }

    public static EternalDefaultTable add(TaskConcept c, Truth t, NAR n) {
        return add(c, t, BELIEF, n);
    }

    public static EternalDefaultTable add(TaskConcept c, Truth t, byte punc, NAR n) {
        EternalDefaultTable tb = new EternalDefaultTable(c.term(), t, punc, n);

        BeliefTables tables = (BeliefTables) c.table(punc, true);

        tables.add(tb);

        return tb;
    }

    public static FloatSupplier ifNot(float interceptedValue, FloatSupplier r) {
        return ()->
            Util.equals(r.asFloat(), interceptedValue, NAL.truth.FREQ_EPSILON) ? Float.NaN : r.asFloat();
    }

    @Override
    public void match(Answer a) {
        a.test(task);
    }

}
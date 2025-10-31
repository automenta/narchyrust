package nars.link;

import jcog.Util;
import jcog.pri.bag.util.AtomicFixedPoint4x16bitVector;
import jcog.pri.op.PriMerge;
import jcog.util.PriReturn;
import nars.Term;
import nars.deriver.reaction.Reaction;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToFloatFunction;
import org.jetbrains.annotations.Nullable;


public class AtomicTaskLink extends MutableTaskLink {

    private AtomicTaskLink(Term source, Term target, int hash) {
        super(source, target, hash);
    }

    AtomicTaskLink(Term source, Term target) {
        super(source, target);
    }

    private final AtomicFixedPoint4x16bitVector punc = new AtomicFixedPoint4x16bitVector(); //OR: new MutableFloatArray(4);

    /** TODO */
    @Override public Reaction reaction() {
        return null;
    }

    @Override
    public final AtomicTaskLink clone() {
        AtomicTaskLink y = new AtomicTaskLink(from, to, hash);
        y.priSet(this);
        return y;
    }

//    private AtomicTaskLink clone(Term from, Term to) {
//        AtomicTaskLink y = new AtomicTaskLink(from, to);
//        y.priSet(this);
////        clone.why = why;
//        return y;
//    }

    /**
     * TODO atomic
     *
     * @return
     */
    @Override public MutableTaskLink priSet(TaskLink copied, float factor) {
        //priSet(copied);
        //priMul(factor);

        FloatFloatToFloatFunction r = PriMerge.replace::mergeUnitize;
        for (byte c = 0; c < 4; c++)
            mergeDirect(c, copied.priComponent(c) * factor, r, null);
        commit();
        return this;
    }

    @Override public void priSet(TaskLink copied) {
        punc.data(((AtomicTaskLink)copied).punc.data());
        commit();
    }

    @Override
    public double variance() {
        return Util.variance(punc.floatArray());
    }

    @Override
    public void epsilonize() {
        punc.commit(x-> x > TaskLinkEpsilonF ? TaskLinkEpsilonF : 0);
        commit();
    }

    @Override
    protected double priSum() {
        return punc.sumValues();
    }

    @Override
    protected float mergeDirect(int ith, float pri, FloatFloatToFloatFunction componentMerge, @Nullable PriReturn returning) {
        return punc.merge(ith, pri, componentMerge, returning);
    }

    @Override
    public final float priComponent(byte c) {
        return punc.getAt(c);
    }

    @Override
    public void pri(float pri) {
        punc.fill(pri);
        commit();
    }

    @Override
    public float[] priGet() {
        return punc.snapshot();
    }

    @Override
    public float[] priGet(float[] buf) {
        punc.writeTo(buf);
        return buf;
    }

    @Override
    public String toString() {
        Term f = from(), t = to();
        return toBudgetString() + ':' + punc + ' ' + (f.equals(t) ? f : (f + " " + t));
    }

    private static final FloatFloatToFloatFunction mult = PriMerge.and::mergeUnitize;
    private static final FloatFloatToFloatFunction replace = PriMerge.replace::mergeUnitize;

    @Override public final void priMul(float b, float q, float g, float Q) {
        merge(mult, b, q, g, Q);
    }

    private void merge(FloatFloatToFloatFunction f, float b, float q, float g, float Q) {
        punc.merge(b, q, g, Q, f);
        commit(); //assume changed
    }

}
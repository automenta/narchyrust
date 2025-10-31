package nars.link;

import jcog.Util;
import jcog.pri.op.PriMerge;
import jcog.util.PriReturn;
import nars.*;
import nars.control.DefaultBudget;
import nars.task.util.TaskException;
import nars.term.Neg;
import nars.term.atom.Bool;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static jcog.util.PriReturn.Changed;
import static jcog.util.PriReturn.Delta;

/** base class for tasklinks which contain live, changing data */
public abstract class MutableTaskLink extends TaskLink {

	public static MutableTaskLink link(Term self) {
		return link(self, null);
	}

	/**
	 * tasklink endpoints are usualy the concept() form of a term
	 * (which could differ from the input if temporal or not normalized)
	 *
	 * source is reduced to concept
	 * target is reduced to root (not concept);
	 * 	  this is for separate links to the different
	 * 	  variable-containing unnormalized compounds
	 * */
	public static MutableTaskLink link(Term _source, @Nullable Term target) {
//		if (target instanceof Variable)
//		    return null;
		//assert(!(target instanceof Neg));
		var source = component(_source);

		return new AtomicTaskLink(source,
			target==null || target.equals(_source) ?
				source :
				component(target) //<- will not work in DecomposedTermLinking for structural && subterms because they become &&+-
		);
	}

	/** TODO use premise belief's pri? */
	public static MutableTaskLink link(Term source, @Nullable Term target, Deriver d) {
		var p = d.premise;
		return link(source, target).priPunc(p.task().punc(), p, d);
	}

	private static Term component(Term f) {
		return f.root();
	}

	/**
	 * source,target as a 2-ary subterm
	 */
	final Term from, to;
	final int hash;

	MutableTaskLink(Term from, Term to) {
		super();
		/* assertValid(from, to); */
		this.hash = Term.hashShort(
			this.from = from,
			this.to = Util.maybeEqual(to, from)
		);
	}

	protected MutableTaskLink(Term from, Term to, int hash) {
		super();
		this.from = from;
		this.to = to;
		this.hash = hash;
	}

	@Override
	public Term term() {
		Term f = from();
		Term t = to();
		return f.equals(t) ? f : $.pFast(f, t);
	}

	@Override
	public @Nullable Term other(Term x) {
		int fh = fromHash(), th = toHash();
		if (fh == th && self()) return null;

		int xh = x.hashShort();
		if (xh == fh && x.equals(from))
			return to;
		if (xh == th && x.equals(to))
			return from;

		return null;
	}

	@Override public @Nullable Term other(int xh, Predicate<Term> xEq) {
		int fh = fromHash(), th = toHash();
        if (fh != th || !self()) {
            if (xh == fh && xEq.test(from))
                return to;
            if (xh == th && xEq.test(to))
                return from;
        }
		return null;
    }

	@Override
	public final boolean self() {
		return from==to;
	}

	private int toHash() {
		return hash >>> 16;
	}

	private int fromHash() {
		return hash & 0xffff;
	}

	@Override
	public final Premise id() {
		return this;
	}

	@Override
	public final boolean equals(Object obj) {
        return this == obj
			||
			(obj instanceof MutableTaskLink t &&
                hash == t.hash &&
                from.equals(t.from) &&
                to.equals(t.to));
    }

	@Override
	public final int hashCode() {
		return hash;
	}

	@Override
	public final Term from() {
		return from;
	}

	@Override
	public final Term to() {
		return to;
	}

	public abstract double variance();

	/** TODO atomic, maybe using old pri() method above */
	protected final void commit() {
        float x = (float) (priSum() / 4);
        super.pri(Math.max(x, TaskLinkEpsilonF));
	}

	@Override
	public void priAdd(float a) {
		if (Math.abs(a) < EPSILON)
			return; //HACK

		float p = pri();
		if (p < EPSILON) {
			//HACK
//			if (a >= Prioritized.EPSILON) pri(a); //'white' fill
			return;
		}

		float prop = (float)(((double)a+p)/p);
		if (prop >= EPSILON)
			priMul(prop);
		else if (prop < -EPSILON)
			epsilonize(); //pri(0);
	}

	abstract public void epsilonize();

//	public void priScale(float p) {
//		if (p == 0) {
//			set(0);
//			return;
//		}
//
//		float p0 = priElseZero();
//		if (p0 < Prioritized.EPSILON) {
//			set(p);
//		} else {
//			float factor = (float) (((double) p) / p0);
//			if (factor != 1)
//				priMul(factor, factor, factor, factor); //HACK
//
//			//TODO fill any remainder divided among components that have capacity
//		}
//	}

	public abstract void priMul(float b, float q, float g, float Q);

//	public final MutableTaskLink priMul(PuncBag punc, OpPri op) {
//		return priMul(punc, op, 1);
//	}
//
//	public final MutableTaskLink priMul(PuncBag punc, OpPri op, double x) {
//		double f = op.apply(from()), t = op.apply(to());
//
//		//mix function TODO refine
//		x *=
//			//Util.mean(f, t);
//			(2 * f + t)/3; //2:1 ratio
//
//		priMul(
//			(float)(x * punc.belief.doubleValue()),
//			(float)(x * punc.question.doubleValue()),
//			(float)(x * punc.goal.doubleValue()),
//			(float)(x * punc.quest.doubleValue())
//		);
//		return this;
//	}


	@Override public @Nullable Term equalReverse(Predicate<Term> fromEquals, int fromHash, Predicate<Term> toEquals, int toHash) {
		int f = fromHash();
		return f != fromHash && f != toHash &&
			toHash() == toHash &&
			toEquals.test(this.to) &&
			!fromEquals.test(this.from) ? this.from : null;
	}

	@Override
	public void delete(byte punc) {
		priPunc(punc, 0);
	}

	@Override
	public final boolean delete() {
		super.delete();
		pri(0);
		return true;
	}

	protected abstract double priSum();

	/**
	 * merge a component; used internally.  does not invalidate so use the high-level methods like merge()
	 * implementations are not resonsible for calling invalidate() themselves.
	 */
	protected abstract float mergeDirect(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning);

//	/** fills all components with the provided value */
//	@Override public abstract void set(float pri);

	@Override
	public abstract String toString();

	public void priSet(TaskLink copied) {
		priSet(copied, 1.0f);
	}

	public abstract MutableTaskLink priSet(TaskLink copied, float factor);

	public final MutableTaskLink priPunc(NALTask t) {
		return priPunc(t.punc(), t.priElseZero());
	}

	public final MutableTaskLink priPunc(byte punc, Premise p, Deriver d) {
		return priPunc(punc, (float)((DefaultBudget)d.focus.budget).priTaskPremise(p));
	}

	/** @return this instance */
	public final MutableTaskLink priPunc(byte punc, float puncPri) {
		mergePunc(punc, puncPri, PriMerge.replace);
		return this;
	}
	public final MutableTaskLink priComponent(byte component, float puncPri) {
		mergeComponent(component, puncPri, PriMerge.replace, null);
		return this;
	}

	public final MutableTaskLink priDirect(byte component, float puncPri) {
		mergeDirect(component, puncPri, PriMerge.replace, null);
		return this;
	}

	public final float merge(TaskLink x, PriMerge merge, PriReturn r) {
		float o = switch (r) {
			case Overflow, Delta -> mergeDelta(x, merge);
			case Void -> mergeVoid(x, merge);
			case Result -> mergeResult(x, merge);
			default -> throw new UnsupportedOperationException();
		};

		commit();

		return o;
	}

	/** use with caution */
	private float mergeVoid(TaskLink incoming, PriMerge merge) {
		for (byte i = 0; i < 4; i++)
			mergeDirect(i, incoming.priComponent(i), merge, null);
		return Float.NaN;
	}

	private float mergeResult(TaskLink incoming, PriMerge merge) {
		mergeVoid(incoming, merge);
		return priElseZero();
	}

	public final float mergeDelta(TaskLink incoming, PriMerge m) {
		double deltaSum = 0;
		for (byte i = 0; i < 4; i++)
			deltaSum += mergeDirect(i, incoming.priComponent(i), m, Delta);
		return deltaPost(deltaSum);
	}

	private float deltaPost(double deltaSum) {
		if (deltaSum != 0) commit();
		return (float) (deltaSum / 4);
	}

	public final float mergeDelta(float incoming, PriMerge M) {
		FloatFloatToFloatFunction m = M::mergeUnitize;
		double deltaSum = 0;
		for (byte i = 0; i < 4; i++)
			deltaSum += mergeDirect(i, incoming, m, Delta);
		return deltaPost(deltaSum);
	}
	public final float mergeDelta(float[] incoming, PriMerge M) {
		FloatFloatToFloatFunction m = M::mergeUnitize;
		double deltaSum = 0;
		for (byte i = 0; i < 4; i++)
			deltaSum += mergeDirect(i, incoming[i], m, Delta);
		return deltaPost(deltaSum);
	}

	public final void mergePunc(byte punc, float pri, PriMerge merge) {
		mergePunc(punc, pri, merge, null);
	}

	public final float mergePunc(byte punc, float pri, PriMerge merge, @Nullable PriReturn returning) {
		return mergeComponent(NALTask.i(punc), pri, merge, returning);
	}

	public final float mergeComponent(byte comp, float pri, PriMerge merge, @Nullable PriReturn returning) {

		float y = mergeDirect(comp, pri, merge, returning);

		if (y != 0 || returning != Delta) //delta==0 on individual component = unchanged
			commit();

		return y/4;
	}

	public final float mergeDirect(int ith, float pri, PriMerge merge, @Nullable PriReturn returning) {
		return mergeDirect(ith, pri, merge::mergeUnitize, returning);
	}


	/** not atomic */
	public final MutableTaskLink priNormalize(float mag) {
        priMul(mag/ Math.max(EPSILON, pri()));
		return this;
	}

	public void priSqrt() {
		float p = pri();
		float pSqrt = (float) Math.sqrt(p);
		priMul(pSqrt/p);
	}

	@Override
	public void priMul(float x) {
		//assert(x >= 0);
		if (x!=1) {

			boolean changed = false;
			//HACK not fully atomic but at least consistent
			for (int i = 0; i < 4; i++)
				changed |= this.mergeDirect(i, x, PriMerge.and, Changed) != 0;

			if (changed)
				commit();
		}
	}

	private static void assertValid(Term from, Term to) {
		if (!NAL.DEBUG) return;

		//		if (from instanceof Neg)
		//			throw new WTF(); //TEMPORARY

		NALTask.TASKS(from, (byte) 0, false); //asserts that it's a valid taskterm

		if (from instanceof Neg || !from.TASKABLE())
			throw new TaskException("tasklink 'from' invalid", from);
		if (to instanceof Neg || to instanceof Bool /* ... */)
			throw new TaskException("tasklink 'to' invalid", to);
	}

//	/** TODO atomic if possible */
//	public void addFill(float v, RandomBits b) {
//		float p = pri();
//		float overflow;
//		if (p > AtomicTaskLink.EPSILON) {
//			float pTarget = Math.min(1, p + v);
//			if (Util.equals(p, pTarget, AtomicTaskLink.EPSILON)) return; //limit reached
//			float div = pTarget / p;
//			float diff = mergeDelta(div, PriMerge.and);
//			overflow = v - diff;
//		} else
//			overflow = v;
//
//		if (overflow >= AtomicTaskLink.EPSILON) {
//			int r = b.nextBits(2);
//			for (byte i = 0; i < 4; i++) {
//				float diff = mergeDirect((byte) ((i + r) % 4), overflow, PriMerge.plus, Delta);
//				overflow -= diff;
//				if (overflow < EPSILON)
//					break;
//			}
//			commit();
//
////			float minHeadroom = Float.POSITIVE_INFINITY, maxHeadroom = Float.NEGATIVE_INFINITY;
////			int fillable = 0;
////			for (byte i = 0; i < 4; i++) {
////				float pi = priComponent(i);
////				if (pi < 1-EPSILON) {
////					minHeadroom = Math.min(minHeadroom, pi);
////					maxHeadroom = Math.max(maxHeadroom, pi);
////					fillable++;
////				}
////			}
////			if (fillable > 0) {
////				for (byte i = 0; i < 4; i++) {
////
////				}
////			}
////
//
//		}
//	}
}
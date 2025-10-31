package nars.term.util;

import jcog.Util;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.util.conj.ConjList;
import nars.term.util.transform.RecursiveTermTransform;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;

/**
 * temporal target intermpolation
 */
public class Intermpolate {
	final float abProp;
	final int dtDither;
	int curDepth;

    public Intermpolate(float abProp, int dither) {
        this.abProp = abProp;
        this.dtDither = dither;
    }

    public Term get(Compound a, Compound b) {
		return a.equals(b) ? a :
			(a instanceof Neg ?
				imtermpolateNeg(a, b) :
				intermpolatePos(a, b));
	}

	@Nullable
	private Term get(Term a, Term b) {
		if (a==b) return a;
		boolean aic = a instanceof Compound, bic = b instanceof Compound;
		if (aic!=bic)
			return null;
		else if (aic)
			return get((Compound) a, (Compound) b);
		else
			return a.equals(b) ? a : null;
	}

	/**
	 * heuristic representing the difference between the dt components
	 * of two temporal terms.
	 * XTERNAL matches anything
	 */
	static long dtDiff(Term a, Term b) {
		if (a == b)
			return 0;

		if (a instanceof Neg) {
			if (!(b instanceof Neg))
				return TIMELESS;
			a = a.unneg();
			b = b.unneg();
		}

        var compound = a instanceof Compound;
		if (compound != b instanceof Compound)
			return TIMELESS;

		if (a.equals(b))
			return 0;

		if (!compound)
			return TIMELESS;

        var ao = a.opID();
		if (ao != b.opID())
			return TIMELESS;

		long dSubterms;
		Subterms as = a.subtermsDirect(), bs = b.subtermsDirect();
		if (ao == CONJ.id && (a.SEQ() || b.SEQ())) {

			boolean ax = a.dt()==XTERNAL, bx = b.dt()==XTERNAL;
			if (ax||bx) {

				dSubterms = dtDiff(as, bs); //compare XTERNAL's as parallel
				if (dSubterms == TIMELESS)
					return TIMELESS;

			} else if (!as.equalTerms(bs))
				return d((Compound)a, (Compound)b);
			else
				dSubterms = 0;

		} else {
			dSubterms = dtDiff(as, bs);
			if (dSubterms == TIMELESS)
				return TIMELESS;
		}

		return dtDiff(a.dt(), b.dt()) + dSubterms;
	}

	private static long d(Compound A, Compound B) {
		try (var a = ConjList.conds(A, 0, true, false, true)) {
			try (var b = ConjList.conds(B, 0, true, false, true)) {
				a.symmetricDifference(b);
				return d(a, b);
			}
		}
	}

	private static long d(ConjList a, ConjList b) {
        var n = a.size();
		return n == b.size() ? diffSeq(a, b) : TIMELESS;
	}

	public static long dtDiff(int adt, int bdt) {
        if (adt == bdt || adt == XTERNAL || bdt == XTERNAL) return 0;
		else {
			if (adt == DTERNAL) adt = 0;
			if (bdt == DTERNAL) bdt = 0;
			return Math.abs(adt - bdt);
		}
    }

	private Term intermpolatePos(Compound a, Compound b) {
		int ao = a.opID;
		if (ao != b.opID)
			return Null;

//		if ((a.structure() & ~CONJ.bit) != (b.structure() & ~CONJ.bit))
//			return Null;

		Subterms aa = a.subtermsDirect(), bb = b.subtermsDirect();
		var subsEqual = aa.equals(bb);
		if (!subsEqual) {
			if (ao == CONJ.id)
				return intermpolateSeq(a, b);

			if (aa.subs() != bb.subs())
				return Null;
		}

		var dtNext = ((1<<ao) & Temporals) != 0 ? chooseDT(a, b) : DTERNAL;
		if (dtNext == XTERNAL)
			return Null;
		else {
			return subsEqual ?
				a.dt(dtNext) //just change dt
				:
				intermpolateSubs(a, b, aa, bb, dtNext);
		}
	}

	private Term intermpolateSubs(Compound a, Compound b, Subterms aa, Subterms bb, int dt) {
		int n = aa.subs();
        var ab = new Term[n];
        var diffFromA = false;
		for (var i = 0; i < n; i++) {
			Term ai = aa.sub(i), bi = bb.sub(i);
			var aic = ai instanceof Compound;
			if (aic != (bi instanceof Compound))
				return Null;
			if (!ai.equals(bi)) {
				if (!aic)
					return Null; //different atomics
				var y = new Intermpolate(abProp, dtDither).get((Compound) ai, (Compound) bi);
				if (y == Null)
					return Null;
				if (!ai.equals(y)) {
					diffFromA = true;
					ai = y;
				}
			}
			ab[i] = ai;
		}

		return diffFromA ? Util.maybeEqual(a.op().the(dt, ab), /*a,*/ b) : a;
	}

	/** equality will have been tested by this point */
	private Term imtermpolateNeg(Compound a, Compound b) {
        if (b instanceof Neg && a.unneg() instanceof Compound ac && b.unneg() instanceof Compound bc)
            return intermpolatePos(ac, bc).neg();
		return Null;
    }

	/** assumes a and b are of the same size
	 *  TODO move any negated conjunction sub-events to separate list for closer comparison
	 */
	private static long diffSeq(ConjList a, ConjList b) {
        var s = a.size();
        //arity differs
        return b.size() == s ? switch (s) {
            case 0 -> 0;
            case 1 -> diffSeq1(a, b);
            default -> diffSeqN(a, b, s);
        } : TIMELESS;

    }

	private static long diffSeq1(ConjList a, ConjList b) {
        var ab = dtDiff(a.getFirst(), b.getFirst());
		return ab == TIMELESS ? TIMELESS :
			ab + diffWhen(a.when, b.when, 0);
	}

	private static long diffSeqN(ConjList a, ConjList b, int s) {
		//HACK attempts to arrange the terms for maximum equality, but not full-proof
		a.sortThisByValue();
		b.sortThisByValue();

		long d = 0;
		long[] aw = a.when, bw = b.when;
		for (var i = 0; i < s; i++) {
			Term ai = a.get(i), bi = b.get(i);
			if (ai==bi)
				continue;

			var diffTerm = dtDiff(ai, bi);
			if (diffTerm == TIMELESS) return TIMELESS;

			d += diffTerm + diffWhen(aw, bw, i);
		}
		return d;
	}

	private static long diffWhen(long[] aw, long[] bw, int i) {
		return Math.abs(aw[i] - bw[i]);
	}

	/** TODO refine
	 * @param abProp - a to b balance; 0: a (full) ..  1: b (full)
	 * */
	private Term intermpolateSeq(Compound a, Compound b) {
		float abProp = this.abProp;
		if (abProp > 0.5f) {
			//swap so that a is dominant (<0.5)
            var c = a;
			a = b;
			b = c;
			abProp = 1 - abProp;
		}

        var ax = a.dt() == XTERNAL;
        //one xternal, the other isnt
        return ax == (b.dt() == XTERNAL) ? intermpolateSeq(a, b, abProp, ax) : Null;
    }

	/** abProp may be swapped  from the class's, so it must be passed as arg */
	private Term intermpolateSeq(Compound a, Compound b, float abProp, boolean ax) {
		ConjList ae, be;
		if (ax) {
			//HACK special handling for XTERNAL case
			ae = ConjList.eventsParallel(a);
			be = ConjList.eventsParallel(b);
		} else {
			ae = ConjList.conds(a, 0, true, false, true);
			be = ConjList.conds(b, 0, true, false, true);
		}

        var s = ae.size();
		if (be.size() != s)
			return Null; //?

		//canonical order
		ae.sortThisByValueEternalFirst();
		be.sortThisByValueEternalFirst();

		boolean changedA = false, changedB = false;
		long[] aw = ae.when,   bw = be.when;
		Term[] A = ae.array(), B = be.array();
		for (var i = 0; i < s; i++) {
			long ai = aw[i], bi = bw[i];
			if ((ai == ETERNAL) != (bi == ETERNAL))
				return Null;

			Term AI = A[i], BI = B[i];
			Term ABI;
			if (!AI.equals(BI)) {
				if (AI instanceof Atomic || BI instanceof Atomic)
					return Null;
				ABI = new Intermpolate(abProp, dtDither).get((Compound) AI, (Compound) BI);
				if (ABI == null || ABI.opID() != AI.opID())
					return Null;
				if (!changedA) changedA = !ABI.equals(AI);
				if (!changedB) changedB = !ABI.equals(BI);
			} else
				ABI = AI;

            var abi = Util.lerpLong(abProp, ai, bi);

			if (abi!=ai || abi!=bi || changedA || changedB) {
				long aiOrig = aw[i], biOrig = bw[i];

				if (aw[i]!=abi) { changedA = true; aw[i] = abi;}
				if (bw[i]!=abi) { changedB = true; bw[i] = abi;}

                var abiDur = ABI.seqDur();
                var dABa = abiDur - AI.seqDur();
                var dABb = abiDur - BI.seqDur();
				A[i] = ABI;
				if (dABa!=0 || dABb!=0) {
					//shift subsequent times
					//TODO defer shift until current (possibly parallel batch of) events at current time is done, take the maximum shift of them for everything after the parallel
					for (var j = i + 1; j < s; j++) {
						if (dABa != 0 && aw[j] > aiOrig) {
							aw[j] += dABa;
							changedA = true;
						}
						if (dABb != 0 && bw[j] > biOrig) {
							bw[j] += dABb;
							changedB = true;
						}
					}
				}
			}
		}
		return interpolateSeq(a, b, ae, aw, bw, ax, changedA, changedB);
	}

	private Term interpolateSeq(@Nullable Compound a, @Nullable Compound b, ConjList ab, long[] abw, long[] bw, boolean ax, boolean changedA, boolean changedB) {
        var s = ab.size();
		for (var i = 0; i < s; i++) {
			long ai = abw[i], bi = bw[i];

            if ((ai == ETERNAL) != (bi == ETERNAL))
				return Null; //one is eternal, the other isnt

            var abi = Tense.dither(ai, dtDither);
			changedA |= abi != ai;
			changedB |= abi != bi;
			abw[i] = /*bw[i] =*/ abi;
		}
		if (!changedA /*&& a!=null*/) return a;
		else if (!changedB /*&& b!=null*/) return b;
		else {
			//Util.maybeEqual(y, a, b) ???
			return ax ?
					CONJ.the(XTERNAL, ab) :
					ab.term();
		}
	}

	/**
	 * if returns XTERNAL, it is not possible
	 */
	private int chooseDT(Term a, Term b) {
		return chooseDT(a.dt(), b.dt());
	}

	int chooseDT(int adt, int bdt) {
        return adt == XTERNAL || bdt == XTERNAL ?
			XTERNAL :
			Tense.dither(merge(adt, bdt), dtDither);
	}

	/**
	 * merge delta
	 */
	private int merge(int adt, int bdt) {
		//HACK
		if (adt == DTERNAL) adt = 0;
		if (bdt == DTERNAL) bdt = 0;

		if (adt == bdt)
			return adt;

        var ab = Util.lerpSafe(abProp, bdt, adt);
		//float delta = Math.max(Math.abs(ab - adt), Math.abs(ab - bdt));
		return Math.round(ab);
//		float ratio = delta / range;
//		return ratio < 1 ? AB : XTERNAL;
	}


	private static long dtDiff(Subterms aa, Subterms bb) {

        var n = aa.subs();
		if (n != bb.subs())
			return TIMELESS;

		long dSubterms = 0;
		for (var i = 0; i < n; i++) {
            var dn = dtDiff(aa.sub(i), bb.sub(i));
			if (dn==TIMELESS)
				return TIMELESS;
			dSubterms += dn;
		}

		return dSubterms;
		//return (dSubterms / n);
	}

	private static class DTVectorTransform extends RecursiveTermTransform {
		private final int[] dt;
		int j;

		DTVectorTransform(int[] dt) {
			this.dt = dt;
			j = 0;
		}

		@Override
		protected Term applyCompound(Compound x) {
            var xdt = x.unneg().TEMPORAL() ? dt[j++] : DTERNAL;
			return super.applyCompound(x, xdt);
		}

		@Override
		protected int dtAlign(Op yOp, int yDt, Subterms xx, Subterms yy, boolean xEqY) {
			return yDt;
		}

	}
}
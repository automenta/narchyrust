package nars.unify;

import jcog.Is;
import jcog.WTF;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.util.conj.CondMatch;
import nars.unify.mutate.CommutivePermutations;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

public enum Unifier implements AbstractUnifier {
	Equal() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return true;
		}
		@Override
		public float cost() {
			return 0;
		}
		@Override
		public @Nullable CompiledUnification compile(Term xi, Term yi) {
			return null;
		}
	},
	
	Direct() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
            return u.uni(x, y);
        }
		@Override
		public float cost() {
			return 0.5f;
		}
	},

	DirectVarX() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return u.uniVarUnneg(x, y);
		}
		@Override
		public float cost() {return 0.75f;}
	},
	DirectVarXNeg() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return u.uniVarUnneg(x.unneg(), y.neg());
		}
		@Override
		public float cost() {return 0.9f;}
	},
	DirectVarY() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return u.uniVarUnneg(y, x);
		}
		@Override
		public float cost() {
			return 0.75f;
		}
	},
	DirectVarYNeg() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return u.uniVarUnneg(y.unneg(), x.neg());
		}
		@Override
		public float cost() {
			return 0.9f;
		}
	},

	@Deprecated First() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
            return u.uni(x.sub(0), y.sub(0));
        }
		@Override
		public float cost() {
			return 0.75f;
		}
	},
	@Deprecated Linear() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return unifyLinear((Compound)x, (Compound)y, u);
		}
		@Override
		public float cost() {
			return 1f;
		}
	},
	LinearReverse() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return unifyLinearN_Reverse((Compound)x, (Compound)y, u);
		}
		@Override
		public float cost() {
			return 1f;
		}
	},
	Commutive() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return unifyCommute(x.subterms(), y.subterms(),  true, u);
		}

		@Override
		public float cost() {
			return 2;
		}
	},
	Conj() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			return CondMatch.unifyConj((Compound) x, (Compound) y, u);
		}
		@Override
		public float cost() {
			return 2;
		}
	},
//	CommutivePad() {
//		@Override
//		public boolean apply(Term x, Term y, Unify u) {
//			Subterms xs = x.subterms();
//			Subterms ys = y.subterms();
//			int diff = xs.subs() - ys.subs();
//			TermList padding;
//			if (diff > 0) {
//				ys = padding = ys.toList();
//			} else {
//				diff = -diff;
//				xs = padding = xs.toList();
//			}
//			padding.ensureCapacityForAdditional(diff, false);
//			for (int i = 0; i < diff; i++)
//				padding.addFast(True);
//
//			return unifyCommute(xs, ys,  true, u);
//		}
//		@Override
//		public float cost() {
//			return 2;
//		}
//	},

	/** for conjunctions with different arity, the smaller of which has a direct variable subterm to glob the remainder */
	@Is("Glob_%28programming%29") ConjPartition() {
		@Override
		public boolean apply(Term x, Term y, Unify u) {
			if (x instanceof Neg) {
				if (!(y instanceof Neg)) throw new WTF();
				x = x.unneg();
				y = y.unneg();
			}
			return CondMatch.conjPartition(x, y, u, x.subterms(), y.subterms());
		}
		@Override
		public float cost() {
			return 2;
		}
	}
	;

	private static final Unifier EqualExceptDT = Equal;

//	EqualExceptDT() {
//		@Override
//		public boolean apply(Term x, Term y, Unify u) {
//			return true;
//		}
//		@Override
//		public float cost() {
//			return 0f;
//		}
////
////		@Override
////		public @Nullable Predicate<Unify> compile(Term xi, Term yi) {
////			return null;
////		}
//
//	},

	static @Nullable AbstractUnifier howSubterms(int op, Subterms x, Subterms y, int vars, int dur, boolean serious) {

		Subterms
			xx = x instanceof Compound X ? X.subtermsDirect() : x,
			yy = y instanceof Compound Y ? Y.subtermsDirect() : y;

        var xn = xx.subs();
		if (op != CONJ.id && xn != yy.subs())
			return null;

		int xss = xx.structSubs(), yss = yy.structSubs();
        var structSame = xss == yss;
		if (!structSame && !hasAny(vars, xss) && !hasAny(vars, yss)) {
			if (op == CONJ.id) {
                var mask = ~(CONJ.bit/* | NEG.bit*/);
				if ((xss & mask) != (yss & mask))
					return null;

				//TODO if (CONJ) { count # of events }

			} else
				return null;
		}

        var varsOrConj = vars | CONJ.bit;
		if (!hasAny(varsOrConj, xss) && !hasAny(varsOrConj, yss)) {
			if (!structSame)
				return null;
			if (!hasAny(xss, CONJ.bit | IMPL.bit))
				return xx.equalTerms(yy) ? EqualExceptDT : null; //subterms both constant (no unifiable variables) on the surface, so if structure differs, it is impossible
		}

        var temporal = op == CONJ.id || op == IMPL.id;
		if (temporal) {
			if (structSame && xx.equalTerms(yy))
				return EqualExceptDT; // compound equality would have been true if non-temporal
			if (op == CONJ.id)
				return CondMatch.unifyPossibleConjSubterms((Compound) x, (Compound) y, vars);
		}

		return howSubterms(op, vars, dur, serious, xx, yy, xn);
	}

	@Nullable
	private static AbstractUnifier howSubterms(int op, int vars, int dur, boolean serious, Subterms xx, Subterms yy, int xn) {
		if (xn > 1 && ((1 << op) & Commutatives) != 0) {
			if (serious)
				return AbstractUnifier.compileCommute(xx, yy, vars, dur);
			else
				return Commutive;
		} else {
			if (serious) {
				return AbstractUnifier.compileLinear(xx, yy, vars, dur);
			} else {
				for (var i = xn - 1; i >= 0; i--)
					if (!Unify.isPossible(xx.sub(i), yy.sub(i), vars, dur))
						return null;
				return Linear;
			}
		}
	}

	public static boolean unifyCommute(Subterms xx, Subterms yy, boolean eliminate, Unify u) {
        var n = xx.subs();
		if (n != yy.subs())
			return false;

		if (eliminate) {
            var exy = eliminateResolve(xx, yy, u);
			if (exy!=null) {
				if (exy.length == 0) //the signal for complete elimination
					return true;
				xx = exy[0];
				yy = exy[1];

				//some eliminated, test again
				n = xx.subs();
				if (n > 1 && null == howSubterms(SETe.id, xx, yy, u.vars, u.dur, false))
					return false;
			}
		}

		if (n == 1)
            return u.uni(xx.sub(0), yy.sub(0));
		else {
			u.termute(new CommutivePermutations(xx, yy));
			return true;
		}
	}

	public static boolean unifyLinear(Subterms x, Subterms y, Unify u) {
        var n = x.subs();
        return y.subs() == n && switch (n) {
            case 0 -> true;
            case 1 -> unifySub(0, x, y, u);
            default -> unifyLinearN(x, y, u, n);
        };
    }

	private static boolean unifyLinearN(Subterms x, Subterms y, Unify u, int n) {
		if (NAL.SUBTERM_UNIFY_ORDER_RANDOM_PROBABILITY>0 && u.random.nextFloat() < NAL.SUBTERM_UNIFY_ORDER_RANDOM_PROBABILITY)
			return unifyLinearN_ShuffledOrder(x, y, n, u);
		else
			return n == 2 ?
					unifyLinear2_complexityHeuristic(x, y, u)
					:
					unifyLinearN_Reverse(x, y, u);
		//unifyLinearN_TwoPhase(x, y, n, u);
	}

	private static boolean unifyLinear2_complexityHeuristic(Subterms x, Subterms y, Unify u) {
		Term x0 = x.sub(0), y0 = y.sub(0);
		if (x0 == y0)
			return unifySub(1, x, y, u);

        var v0 = u.vars(x0) + u.vars(y0);
		if (v0 == 0) {
            return u.uni(x0, y0) && unifySub(1, x, y, u);
		} else {
			Term x1 = x.sub(1), y1 = y.sub(1);
			if (x1 == y1)
                return u.uni(x0, y0);

            var v1 = u.vars(x1) + u.vars(y1);
			//forward or reverse
            return (v1 == v0 ? x0.complexity() + y0.complexity() <= x1.complexity() + y1.complexity() : v0 < v1) ?
				u.uni(x0, y0) && u.uni(x1, y1) :
				u.uni(x1, y1) && u.uni(x0, y0);
		}
	}

	private static boolean unifyLinearN_ShuffledOrder(Subterms x, Subterms y, int n, Unify u) {
		if (n == 2) {
            var s = u.random.nextBoolean() ? 0 : 1;
			return unifySub(s, x, y, u) && unifySub(1 - s, x, y, u);
		} else {
            var order = new byte[n];
			for (var i = 0; i < n; i++)
				order[i] = (byte) i;
			ArrayUtil.shuffle(order, u.random);
			for (var b : order) {
				if (!unifySub(b, x, y, u))
					return false;
			}
			return true;
		}
	}

	public static boolean unifyLinearN_Forward(Subterms x, Subterms y, Unify u) {
        var s = x.subs();
		for (var i = 0; i < s; i++) {
			if (!unifySub(i, x, y, u))
				return false;
		}
		return true;
	}

	/** potentially faster than forward since in commutive cases smaller terms are sorted right */
	private static boolean unifyLinearN_Reverse(Subterms x, Subterms y, Unify u) {
        var s = x.subs();
		for (var i = s-1; i >= 0; i--)
			if (!unifySub(i, x, y, u))
				return false;
		return true;
	}

//	public static boolean unifyLinearN_TwoPhase(Subterms x, Subterms y, int n, Unify u) {
//		//TODO elide subsequent repeats
//		MetalBitSet m = null;
//		for (int i = 0; i < n; i++) {
//			Term xi = x.sub(i), yi = y.sub(i);
//
//			if (xi.equals(yi))
//				continue;
//
//			boolean now = (i == n - 1 && m == null /* last one anyway so just do it */) || (!u.varIn(xi) && !u.varIn(yi));
//			if (now) {
//                if (!u.uni(xi, yi))
//					return false;
//			} else {
//				if (m == null) m = MetalBitSet.bits(n);
//				m.set(i);
//			}
//		}
//		if (m == null)
//			return true;
//
//		//process remaining non-constant subterms
//
//		int nonconst = m.cardinality();
//		switch (nonconst) {
//			case 1:
//				return unifySub(m.next(true, 0, n), x, y, u);
//			case 2: {
//				int a = m.next(true, 0, n);
//				int b = m.next(true, a+1, n);
//				if (x.sub(a).volume()+y.sub(a).volume() > x.sub(b).volume()+y.sub(b).volume()) {
//					int c = a;
//					a = b;
//					b = c;
//				}
//				return unifySub(a, x, y, u) && unifySub(b, x, y, u);
//			}
//			default:
//
//				int[] c = new int[nonconst];
//				int k = 0;
//				//sort based on heuristic of estimated simplicity
//				for (int i = 0; i < n && k < nonconst; i++) {
//					if (((IntBitSet) m).testFast(i)) //if (m.test(i)) //HACK
//						c[k++] = i;
//				}
//				QuickSort.sort(c, cc -> -(x.sub(cc).volume() + y.sub(cc).volume())); //sorts descending
//
//				for (int cc : c) {
//					if (!unifySub(cc, x, y, u))
//						return false;
//				}
//				return true;
//		}
//	}

	private static boolean unifySub(int n, Subterms x, Subterms y, Unify u) {
        //return u.unify(x.sub(n), y.sub(n));
		return u.uni(x.sub(n), y.sub(n));
    }


	@Nullable public static Subterms[] eliminateResolve(Subterms x, Subterms y, Unify u) {

		int n;
        var xl = u.resolveListIfChanged(x);
		if (xl != null) {
			n = xl.subs();
			x = xl;
		} else
			n = x.subs();

        var yl = u.resolveListIfChanged(y);
		if (yl != null) {
			if (yl.subs() != n && x.subs() != n)
				return null;
			y = yl;
		}
		return eliminate(x, y);
	}

	/** @return null if nothing eliminated, 0-length TermList[] array if all removed */
	@Nullable public static Subterms[] eliminate(Subterms x, Subterms y) {

		if ((x.structSurface() & y.structSurface()) == 0)
			return null; //nothing in common to eliminate

		TermList xx = x.toList(), yy = y.toList();
		int xn = x.subs(), yn = y.subs(), sn;
        var xBigger = xn >= yn;
		TermList bigger, smaller;
		if (xBigger) { bigger = xx; smaller = yy; sn = yy.subs(); }
		else { bigger = yy; smaller = xx; sn = xx.subs(); }

        var removed = 0;
		for (var i = 0; i < sn; ) {
            var xi = smaller.get(i);
			//TODO attempt unify, in case of conj?
			if (bigger.removeOnce(xi)) {
				smaller.removeFast(i);
				sn--;
				removed++;
			} else
				i++;
		}

		return removed > 0 ?
				(xx.isEmpty() && yy.isEmpty() ?
						TermList.EmptyTermListArray :
						new TermList[] { xx, yy })
				: null;
	}




}
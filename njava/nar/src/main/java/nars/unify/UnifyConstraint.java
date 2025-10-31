package nars.unify;

import jcog.Util;
import jcog.data.list.Lst;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.PREDICATE;
import nars.term.var.Variable;
import nars.unify.constraint.RelationConstraint;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;


/**
 * must be stateless
 */
public abstract class UnifyConstraint<U extends Unify> extends PREDICATE<U> {

	public static final UnifyConstraint[] EmptyUnifyConstraints = new UnifyConstraint[0];
	private static final Map<Term, UnifyConstraint> constra = new ConcurrentHashMap<>(256);
	public final Variable x;

	protected UnifyConstraint(Variable x, String func, @Nullable Term... args) {
		this(x, Atomic.atom(func), args);
	}

	protected UnifyConstraint(Variable x, Term func, @Nullable Term... args) {
		super($.p(x, args != null && args.length > 0 ? $.p(func, $.pOrOnly(args)) : func));
		this.x = x;
	}


	private static UnifyConstraint intern(UnifyConstraint x) {
        var y = constra.putIfAbsent(x.term(), x);
		return y != null ? y : x;
	}

	/**
	 * returns a stream of constraints bundled by any multiple respective targets, and sorted by cost increasing
	 */
	public static <U extends Unify> UnifyConstraint<U>[] the(Stream<UnifyConstraint<U>> c) {
		return c
			.collect(Collectors.groupingBy(x -> x.x, toCollection(Lst::new))).values().stream()
			.map(CompoundConstraint::the)
			.map(UnifyConstraint::intern)
			.toArray(x -> x == 0 ? EmptyUnifyConstraints : new UnifyConstraint[x]);
	}

	@Override
	public final boolean test(U p) {
		p.constrain(this);
		return true;
	}

	/**
	 * cost of testing this, for sorting. higher value will be tested later than lower
	 */
	@Override
	public abstract float cost();

	/**
	 * @param targetVariable current value of the target variable (null if none is setAt)
	 * @param potentialValue potential value to assign to the target variable
	 * @param f              match context
	 * @return true if match is INVALID, false if VALID (reversed)
	 */
	public abstract boolean invalid(Term x, U f);

	/** note: cc may contain nulls while reducing */
	public boolean remainAmong(UnifyConstraint... cc) {
		Term source = this.x;
		if (this instanceof RelationConstraint X) {
			var target = X.y;
			for (var c : cc) {
				if (this != c) {
					if (c instanceof RelationConstraint rc) {
                        if (source.equals(rc.x) && target.equals(rc.y) && !X.remainAmong(rc))
							return false;
					}
				}
			}
		}

//		if (this instanceof UnaryConstraint U) {
//			for (var c : cc) {
//				if (this != c) {
//					if (c instanceof UnaryConstraint uc) {
//						if (source.equals(uc.x) && !U.remainAmong(uc))
//							return false;
//					}
//				}
//			}
//		}

		return true;
	}


	/**
	 * TODO group multiple internal relationconstraints for the same target so only one xy(target) lookup invoked to use with all
	 */
	public static final class CompoundConstraint<U extends Unify> extends UnifyConstraint<U> {

		static final Atom AND = Atomic.atom("&&");
		private final UnifyConstraint<U>[] subConstraint;
		private final float cost;

		private CompoundConstraint(UnifyConstraint[] c) {
			super(c[0].x, AND, Op.SETe.the(Util.map(
				//extract the unique UnifyIf parameter
				cc -> $.pOrOnly(
					cc.term().sub(1)
					//cc.sub(0).subterms().subRangeArray(1, Integer.MAX_VALUE)
				), new Term[c.length], c)
			));
			this.subConstraint = c;
			this.cost = (float)Util.sum((FloatFunction<PREDICATE<U>>) PREDICATE::cost, subConstraint);
		}

		static <U extends Unify> UnifyConstraint<U> the(List<UnifyConstraint<U>> cc) {
			var ccn = cc.size();
            switch (ccn) {
                case 0:
                    throw new UnsupportedOperationException();
                case 1:
                    return cc.getFirst();
            }

//            nextX: for (int i = 0, ccSize = ccn; i < ccSize; i++) {
//                UnifyConstraint x = cc.get(i);
//                for (UnifyConstraint y : cc) {
//                    if (x != y) {
//                        if (x instanceof RelationConstraint && y instanceof RelationConstraint) {
//                            RelationConstraint X = (RelationConstraint) x;
//                            RelationConstraint Y = (RelationConstraint) y;
//                            if (X.x.equals(Y.x) && X.y.equals(Y.y)) {
//                                if (!X.remainInAndWith(Y)) {
//                                    cc.set(i, null);
//                                    continue nextX;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            if (((FasterList)cc).removeNulls()) {
//                ccn = cc.size();
//                if (ccn == 0)
//                    throw new UnsupportedOperationException();
//                else if (ccn == 1)
//                    return cc.get(0);
//            }

            var d = cc.toArray(new UnifyConstraint[ccn]);
			Arrays.sort(d, CostIncreasing);

			if (NAL.test.DEBUG_EXTRA) {
				Term target = d[0].x;
                var ii = d.length;
				for (var i = 1; i < ii; i++)
					assert (d[i].x.equals(target));
			}

			return new CompoundConstraint<>(d);
		}

		@Override
		public float cost() {
			return cost;
		}

		@Override
		public boolean invalid(Term x, U f) {
			for (var c : subConstraint) {
				if (c.invalid(x, f))
					return true;
			}
			return false;
		}


	}
}

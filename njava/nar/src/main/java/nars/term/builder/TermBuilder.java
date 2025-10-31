package nars.term.builder;

import nars.NAL;
import nars.Op;
import nars.Term;
import nars.io.IO;
import nars.subterm.Subterms;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termlike;
import nars.term.anon.Intrin;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.IntrinAtomic;
import nars.term.util.Statement;
import nars.term.util.TermException;
import nars.term.util.TermTransformException;
import nars.term.util.Terms;
import nars.term.util.conj.ConjPar;
import nars.term.util.conj.ConjSeq;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.VariableNormalization;
import nars.term.var.NormalizedVariable;
import nars.term.var.Variable;
import nars.time.Tense;
import org.eclipse.collections.impl.map.mutable.primitive.ShortByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.term.util.Image.imageNormalize;

/**
 * interface for target and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another target or True/False/Null
 */
public abstract class TermBuilder {

    /** TODO VAR_QUERY and VAR_INDEP, including non-0th variable id*/
    private static Term postNormalize(Compound y, Compound x) {
        var s = y.struct();
        var z = !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
                y : new CompoundPostNormalizer(y).apply(y);
        if (NAL.NORMALIZE_STRICT && z != y) assertSameOp(x, z);
        return z;
    }

    private static void assertSameOp(Compound x, Term y) {
        if (x == y)
            return;
        if (y.opID() != x.opID) {
            //if (!NAL.DEBUG && x.CONDS() && y.CONDS()) return; //HACK allow for now
            throw new TermTransformException("normalization op fault", x, y);
        }
    }

    public final Term compound(byte[] b) {
        return compound(b, 0, b.length);
    }

    private Term compound(byte[] b, int start, int end) {
        if (start != 0 && end != b.length)
            b = Arrays.copyOfRange(b, start, end);

        return IO.bytesToTerm(b, this);
    }

    public abstract Atomic atomNew(String id);

    protected abstract Term negNew(Term u);

    protected abstract Term compound1New(Op o, Term x);

    public Term compoundNew(Op o, int dt, Term... t) {
        return compoundNew(o, dt, subterms(t));
    }

    public abstract Term compoundNew(Op o, int dt, Subterms subs);

    protected abstract Subterms subtermsNew(Term[] t);

    public Term root(Compound x) {
        return NAL.conceptualization.applyCompound(x);
    }

    public Compound normalize(Compound x, byte varOffset) {
        var nVars = x.vars();
        if (nVars == 0) return x;

        var y = new VariableNormalization(nVars, varOffset).apply(x);
        if (NAL.NORMALIZE_STRICT) assertSameOp(x, y);

        if (NAL.POST_NORMALIZE && y instanceof Compound Y)
            y = postNormalize(Y, x);

        return (Compound) y;
    }

    public final Term compound(Op o, int dt, boolean sort, Term[] __t) {
        var _t = pre(__t);
        if (_t == null) return Null;

        var t = sort && o.commutative && _t.length > 1 && Tense.parallel(dt) ?
                Terms.commute(_t) : _t;

        var s = t.length;

        if (o.subsMin > s || o.subsMax < s)
            throw new TermException("subterm overflow/underflow", o, dt, t);

        return switch (s) {
            case 0 -> switch (o) {
                case PROD -> EmptyProduct;
                case CONJ -> True;
                default -> throw new TermException("unsupported 0-length subterms", o, dt, t);
            };
            case 1 -> {
                var t0 = t[0];
                yield o.id == NEG.id ? neg(t0) : compound1New(o, t0);
            }
            default -> compoundNew(o, dt, t);
        };
    }

    @Nullable
    private Term[] pre(Term[] _t) {
        var t = _t;
        var s = t.length;
        for (var i = 0; i < s; i++) {
            var x = t[i];
            if (x instanceof Compound X) {
                var y = imageNormalize(X, this);
                if (y != x) {
                    if (y == Null)
                        throw new TermTransformException("image normalize failure", x, y); //HACK
                    if (t == _t) t = _t.clone(); //TODO make unnecessary
                    t[i] = y;
                }
            } else {
                if (x == Null) return null; //try to detect earlier
            }
        }
        return t;
    }

    public final Term neg(Term u) {
        return switch (u) {
            case Neg n -> u.neg();
            case Bool b -> u.neg();
            case IntrinAtomic a -> Intrin.neg(a.intrin());
            case null, default -> negNew(u);
        };
    }


    /**
     * individual subterm resolution should be performed here
     *
     * @param t accepts the array and may modify it, as well as use it in the final output.
     *          so in other cases the array may need to be cloned (.arrayClone() not .arrayShared())
     */
    public final Subterms subterms(Term... t) {
        return t.length == 0 ? EmptySubterms : subtermsNew(t);
    }

    public final Term conj(Term... u) {
        return conj(DTERNAL, u);
    }

    public final Term conj(Subterms u) {
        return conj(DTERNAL, u);
    }

    public final Term conj(int dt, Subterms u) {
        return conj(dt, Subterms.array(u));
    }

    public final Term conj(int dt, Term... u) {
        if (u.length == 1) return u[0];

        var uu = conjSubs(dt, u);
        return uu == null ? Null : switch (uu.size()) {
            case 0 -> True;
            case 1 -> uu.getFirst();
            default -> conjN(dt, uu);
        };
    }

    /** HACK */
    private static final TmpTermList FalseTermList = new TmpTermList(False);

    /**
     * prepares subterms list for CONJ args, depending on DT that will connect them
     */
    @Nullable
    private TmpTermList conjSubs(int dt, Term[] x) {

        var trues = 0;
        for (var t : x) {
            if (t instanceof Bool) {
                if (t == Null)
                    return null;
                if (t == False)
                    return FalseTermList;
                if (t == True)
                    trues++;
            } else if (!NAL.term.IMPL_IN_CONJ && t.unneg().IMPL())
                return null;
        }

        if (trues == x.length) return new TmpTermList();

        var parallel =
            dt==DTERNAL || (dt==XTERNAL && x.length < 3) || x.length==1 /*?*/;
            //dt==DTERNAL;
            //Tense.parallel(dt);

        var l = new TmpTermList(x.length - (parallel ? trues : 0));
        for (var xx : x) {
            if (parallel) {
                if (xx == True) {
                    continue;
                } else if (dt == DTERNAL && xx.CONJ() && xx.dt() == DTERNAL) {
                    conjSubsInline(l, xx);
                    continue;
                }
            }

            l.addImageNormalized(xx, this);
        }

        if (dt == XTERNAL && l.subs()==2 && l.subEquals(0, 1))
            l.sortThis(); //sort but dont deduplicate, allowing repeat xternal
        else if (parallel)
            l.commuteThis();

        return l;
    }

    private void conjSubsInline(TmpTermList l, Term t) {
        var tt = t.subtermsDirect();
        var n = tt.subs();
        l.ensureCapacityForAdditional(n, true);
        for (var i = 0; i < n; i++)
            l.addImageNormalized(tt.sub(i), this);
    }

    /**
     * assume u is already sorted
     */
    protected Term conjN(int dt, TmpTermList u) {
        return switch (dt) {
            case DTERNAL, 0 -> ConjPar.parallel(dt, u, false, this);
            case XTERNAL -> conjXternal(u);
            default -> conjSeq(dt, u);
        };
    }

    private Term conjXternal(TmpTermList u) {
        if (u.size() < 2)
            throw new TermException("xConj n<2 subterms", CONJ, XTERNAL, u);

        return compound(CONJ, XTERNAL, false, u.arrayTake());
    }

    private Term conjSeq(int dt, TmpTermList u) {
        if (u.size() != 2)
            throw new TermException("sequence n!=2 subterms", CONJ, dt, u);

        return ConjSeq.conjAppend(u.get(0), dt, u.get(1), this);
    }

    public final Term inh(Term s, Term p) {
        return statement(INH, DTERNAL, s, p);
    }

    public final Term statement(Op o, int dt, Subterms u) {
        return statement(o, dt, Subterms.array(u));
    }

    public final Term statement(Op o, int dt, Term... sp) {
        if (sp.length != 2) throw new TermException("requires 2 arguments", o, dt, sp);
        return statement(o, dt, sp[0], sp[1]);
    }

    private Term statement(Op o, int dt, Term S, Term P) {
        return statementNew(o, dt, imageNormalize(S), imageNormalize(P));
    }

    Term statementNew(Op o, int dt, Term S, Term P) {
        return Statement.statement(this, o, dt, S, P);
    }

    /** TODO move this to Equal */
    public final Term equal(Term x, Term y) {
//		return Equal.compute(null, x, y);
        if (x == y) return True;
//		if (x == False)
//			return y.neg();
//		else if (y == False)
//			return x.neg();
//		else if (x == True)
//			return y;
//		else if (y == True)
//			return x;

//		boolean negOneSide = false;
        if (x instanceof Neg && y instanceof Neg) {
            x = x.unneg();
            y = y.unneg();
            if (x == y) return True;
        }
//		} else if (y instanceof Neg) {
//			negOneSide = true;
//			y = y.unneg();
//		} else if (x instanceof Neg) {
//			negOneSide = true;
//			x = x.unneg();
//		} else {
//
//		}


        x = imageNormalize(x);
        y = imageNormalize(y);

        if (x.equals(y)) {
            return True;
        } else {
            //Term xy = Equal.compute(null, x, y);
            if (!x.hasVars() && !y.hasVars()) {
                //TODO better test for inner functors
                if (!x.hasAll(FuncBits) && !y.hasAll(FuncBits)) //TODO in case it can reduce by evaluation
                    return False;
            }
        }


//		int xy = x.compareTo(y);
//		if (xy == 0) return True; //negOneSide ? False : True; //equal
//		if (xy > 0) {
//			Term c = x; x = y; y = c; //swap for commute
//		}
//            @Nullable Term p = Equal.pretest(x, y);
//            return p != null ? p : theCommutive(equal, x, y);

//		boolean negOuter = false;
//		if (negOneSide) {
//			if (x instanceof Int || y instanceof Int) //TODO other constant types?
//				negOuter = true;
//			else {
//				x = x.neg();
//				if (x.compareTo(y) > 0) {
//					Term c = x; x = y; y = c; //swap again
//				}
//			}
//		}

        return compound(EQ, true, x, y);
    }

    public final Term compound(Op o, Term... s) {
        return compound(o, true, s);
    }

    private Term compound(Op o, boolean preCommute, Term... s) {
        return compound(o, DTERNAL, preCommute, s);
    }


    static final class CompoundPostNormalizer implements Predicate<Term> {

        private final ShortByteHashMap counts;
        private boolean skipNext, negVars;
        private transient Subterms xx;

        CompoundPostNormalizer(Compound x) {
            counts = new ShortByteHashMap(x.vars());
            x.recurseTermsOrdered(Termlike::hasVars, this, null);
        }

        @Override
        public boolean test(Term x) {
            if (skipNext)
                skipNext = false; //this is the variable contained inside a Neg that was counted
            else
                next(x);
            return true;
        }

        private void next(Term x) {
            byte b;
            switch (x) {
                case Neg n -> {
                    var xu = x.unneg();
                    if (!(xu instanceof Variable)) return;

                    skipNext = true;
                    x = xu;
                    b = -1;
                    negVars = true;
                }
                case Variable v -> b = +1;
                case null, default -> {
                    return;
                }
            }

            counts.addToValue(((NormalizedVariable) x).intrin(), b);
        }

        @Nullable
        private RecursiveTermTransform invertVariables() {
            //keep only entries where more neg than positive. these will be flipped
            return counts.keySet().removeIf(cc -> {
                var c = counts.get(cc);
                if (c > 0)
                    return true; //positive dominant; dont change
                else if (c < 0)
                    return false; //negative, keep to be inverted
                else
                    return removeCount(cc);
            }) && counts.isEmpty() ? null :
                new VariableInverter(counts);
        }

         /** TODO better */
         private boolean removeCount(short cc) {
            //determine if the first appearance of the variable is pos or neg. if neg, then keep so it can be inverted
             var ccc = Intrin.term(cc);

            var firstIsNeg = new boolean[]{false};
            xx.ANDrecurse(t -> t.containsRecursively(ccc), (t, zs) -> {
                //first appearance is neg or non-neg?
                if (t == ccc) {
                    firstIsNeg[0] = zs instanceof Neg;
                    return false;
                } else
                    return true;
            }, null);

            //first is negative, so keep to invert to normal form where neg is first
            //pos is already first, do nothing
            return firstIsNeg[0];
        }

        Term apply(Compound x) {
             if (!negVars)
                 return x;

            var cs = counts.size();
            xx = x.subtermsDirect();
            if (cs > 0 && x.IMPL()) {
                //exclude the subject/pred of a statement as this can affect the semantics of the outer compound
                var implPred = xx.sub(1);
                if (implPred instanceof Variable) {
                    if (!(implPred instanceof NormalizedVariable nv))
                        throw new TermException("unnormalized variable in Task term", x);

                    counts.remove(nv.intrin());
                    cs = counts.size();
                }
            }

            if (cs >= 1) {
            //if (cs == 1) {
                var f = invertVariables();
                if (f != null) {
                    var u = f.apply(x);
                    if (u!=x) {
                        if (u.opID() == x.opID) {
                            //??only if op changed; may be a negation caused by an implication predicate changing polarity; dont post-normalize
                            //return u;

                            if (u.complexity() < x.complexity())
                                return u;
                            if (u.compareTo(x) < 0)
                                return u;
                        }
                    }
                }
            }

            return x;
        }

        private static final class VariableInverter extends RecursiveTermTransform {
            private final ShortByteHashMap counts;

            private VariableInverter(ShortByteHashMap counts) {
                this.counts = counts; //assert(!counts.isEmpty());
            }

            @Override
            public Term applyCompound(Compound c) {
                if (c instanceof Neg) {
                    var d = c.unneg();
                    if (invert(d))
                        return d;
                }

                return c.hasVars() ? super.applyCompound(c) : c;
            }

            @Override
            public Term applyAtomic(Atomic a) {
                return invert(a) ? a.neg() : a;
            }

            boolean invert(Term d) {
                return d instanceof NormalizedVariable n &&
                    counts.containsKey(n.intrin());
            }
        }
    }




}
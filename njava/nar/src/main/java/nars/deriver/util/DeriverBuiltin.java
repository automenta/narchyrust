package nars.deriver.util;

import jcog.Log;
import jcog.WTF;
import jcog.data.map.UnifriedMap;
import nars.*;
import nars.eval.Evaluation;
import nars.func.Replace;
import nars.func.SetFunc;
import nars.func.UniSubst;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Neg;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.CharAtom;
import nars.term.functor.AbstractInlineFunctor;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.util.conj.Cond;
import nars.term.util.conj.ConjList;
import nars.term.util.impl.ImplSyl;
import nars.term.util.transform.InlineFunctor;
import nars.term.util.transform.Subst;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

import static nars.NAL.derive.TTL_UNISUBST;
import static nars.Op.*;
import static nars.premise.NALPremise.nalPremise;
import static nars.term.atom.Bool.Null;

/** functors for use in Deriver's */
public enum DeriverBuiltin { ;
    public static final Atom BEFORE = Atomic.atom("before");
    public static final Atom BEFORE_OR_DURING = Atomic.atom("beforeOrDuring");
    public static final Atom DURING = Atomic.atom("during");
    public static final Atom AFTER = Atomic.atom("after");
    public static final Atom AFTER_OR_DURING = Atomic.atom("afterOrDuring");
    public static final Atom BEFORE_UNIFY = Atomic.atom("beforeUnify");
    public static final Atom AFTER_UNIFY = Atomic.atom("afterUnify");
    public static final Atom WITHOUT_UNIFY = Atomic.atom("conjWithoutUnify");
    /** TODO rename event -> cond */
    public static final Atom UNIFIABLE_SUBCOND =    Atomic.atom("unifiableSubEvent");
    /** TODO rename event -> cond */
    public static final Atom UNIFIABLE_SUBCOND_PN = Atomic.atom("unifiableSubEventPN");
    private static final Logger logger = Log.log(DeriverBuiltin.class);
    public static MutableMap<Atomic, Term> get(Deriver d) {
        var m = new UnifriedMap<Atomic, Term>(32, 1.0f);

        for (Term s : NARS.Functors.statik)
            if (s instanceof InlineFunctor)
                add(m, s);

        /* override any statik's */
        for (Term x : functors(d))
            add(m, x);

        m.trimToSize();
        return m;
    }

    private static Functor[] functors(Deriver d) {


        return new Functor[] {

            new UniSubst(d),

            new Replace(Subst.SUBSTITUTE) {
                @Override public @Nullable Term apply(Evaluation e, Subterms s) {
                    var z = Replace.replaceEval(s);
                    if (z != null && z != Null && z != s.sub(1)) {
                        var r = d.unify.retransform;
                        r.put(z, s.sub(0));
                        //r.put(input, z);
                        //r.put(x, y);
                    }

                    return z;
                }
            },
            new AbstractInlineFunctor1.AbstractInstantFunctor1("polarizeTask") {
                @Override
                protected Term apply1(Term arg) {
                    return d.polarize(arg, true);
                }
            },
            new AbstractInlineFunctor1.AbstractInstantFunctor1("polarizeBelief") {
                @Override
                protected Term apply1(Term arg) {
                    return d.premise.belief() == null ? arg.negIf(d.randomBoolean()) : d.polarize(arg, false);
                }
            },

//                /**
//                 * either:
//                 *    "((x||y)&&(--,(x&&y)))"
//                 *    "(((--,x)&&y)||((--,y)&&x))"
//                */
//                new AbstractInlineFunctor2("xor") {
//                    @Override
//                    protected Term apply(Term x, Term y) {
//                        return CONJ.the(
//                            Op.DISJ(x, y),
//                                CONJ.the(x, y).neg());
//                    }
//                },

            /**
             * cant be inline since the value will be cached and repeated?
            */
            new AbstractInlineFunctor1("polarizeRandom") {
                //	public final Functor polarizeRandom = Functor.f1("polarizeRandom", (arg)->random().nextBoolean() ? arg : arg.neg());
                @Override
                protected Term apply1(Term arg) {
                    return arg.negIf(d.randomBoolean());
                }
            },

            SetFunc.union,
            //SetFunc.interSect,
            //SetFunc.unionSect,
            SetFunc.differ,

            narFunc("without", d.nar),
            narFunc("withoutPN", d.nar),

//            /** wrapper that sets retransform to assist occurrence solver */
//            new AbstractInlineFunctor1("negateConds") {
//                @Override protected Term apply1(Term x) {
//                    Term y = Cond.negateConds(x);
//                    if (y!=Null && !x.equals(y))
//                        d.unify.retransform.put(y, x);
//                    return y;
//                }
//            },

            /** applies any substitutions already applied to some part of the derivation, to another or the entire derivation */
            new AbstractInlineFunctor1("retransform") {
                @Override
                protected Term apply1(Term x) {
                    return d.unify.retransform(x);
                }
            },

            new AbstractInlineFunctor2(UNIFIABLE_SUBCOND_PN) {
                @Override
                protected Term apply(Term conj, Term x) {
                    return unifiableSubEvent(conj, x, false,true, d);
                }
            },

            /** excludes equal subevents */
            new AbstractInlineFunctor2(UNIFIABLE_SUBCOND) {
                @Override
                protected Term apply(Term conj, Term x) {
                    return unifiableSubEvent(conj, x, false,false, d);
                }
            },

            /* similar to without() but for any (but not necessarily ALL) (possibly-recursive) CONJ sub-events. removes all instances of the positive event */
            new AbstractInlineFunctor2(WITHOUT_UNIFY) {
                @Override
                protected Term apply(Term conj, Term x) {
                    return ifNovel(d.cond(true, false, true, true, conj, x, true, false), conj);
                }
            },
            new AbstractInlineFunctor2(BEFORE) {
                @Override
                protected Term apply(Term conj, Term x) {
                    boolean during = false, unify = false;
                    return ifNovel(d.cond(true, false, false, during, conj, x, unify, false), conj);
                }
            },
            new AbstractInlineFunctor2(DURING) {
                @Override
                protected Term apply(Term conj, Term x) {
                    boolean during = true, unify = false;
                    return ifNovel(d.cond(false, false, false, during, conj, x, unify, false), conj);
                }
            },
            new AbstractInlineFunctor2(BEFORE_OR_DURING) {
                @Override
                protected Term apply(Term conj, Term x) {
                    boolean during = true, unify = false;
                    return ifNovel(d.cond(true, false, false, during, conj, x, unify, false), conj);
                }
            },

            new AbstractInlineFunctor2(AFTER) {
                @Override
                protected Term apply(Term conj, Term x) {
                    boolean unify = false, during = false;
                    return ifNovel(d.cond(false, false, true, during, conj, x, unify, false), conj);
                }
            },
            new AbstractInlineFunctor2(AFTER_OR_DURING) {
                @Override
                protected Term apply(Term conj, Term x) {
                    boolean unify = false, during = true;
                    return ifNovel(d.cond(false, false, true, during, conj, x, unify, false), conj);
                }
            },
            new AbstractInlineFunctor2(BEFORE_UNIFY) {
                @Override
                protected Term apply(Term conj, Term x) {
                    boolean unify = true, during = true;
                    return ifNovel(d.cond(true, false, false, during, conj, x, unify, false), conj);
                }
            },
            new AbstractInlineFunctor2(AFTER_UNIFY) {
                @Override
                protected Term apply(Term conj, Term x) {
                    boolean unify = true, during = true;
                    return ifNovel(d.cond(false, false, true, during, conj, x, unify, false), conj);
                }
            },

            /**
             * implication constructor for syllogistic rules (w/ specific temporal DT calculation)
             * ex: implSyl(B,A,1,-1,p)
             *             subj, pred, taskTerm dt mult, beliefTerm dt mult, mode={i,o,s,p}
             *
            */
            new AbstractInlineFunctor(Atomic.atom("implSyl")) {
                @Override
                public Term apply(Evaluation e, Subterms args) {
                    var P = nalPremise(d.premise);
                    var taskTerm = (Compound) P.task().term();
                    var beliefTerm = (Compound) P.beliefTerm();

                    return ImplSyl.implSyl(taskTerm, beliefTerm, args, d.timeRes());
                }
            },

            new AbstractInlineFunctor1(Atomic.atom("implWithoutCommonEvents")) {

                private static final boolean filterBools = true;

                @Override
                protected Term apply1(Term i) {
                    assert (i.IMPL());
                    Term x = i.sub(0), y = i.sub(1);
                    if (!(x.CONDS() && y.CONDS() && !x.SEQ() && !y.SEQ())) {
                        if (NAL.DEBUG)
                            throw new WTF(); //TODO
                        return Null;
                    }
                    var X = Cond.conds((Compound) x, true, false);
                    var Y = Cond.conds((Compound) y, true, false);
                    if (NAL.term.INH_BUNDLE && X.hasAny(INH) && Y.hasAny(INH)) {
                        X._inhExplode(false, terms);
                        Y._inhExplode(false, terms);
                    }
                    java.util.Set<Term> XY = Sets.intersect(java.util.Set.copyOf(X), java.util.Set.copyOf(Y));
                    var xr = X.removeIf(XY::contains);
                    if (filterBools && xr && X.isEmpty())
                        return Null;

                    var yr = Y.removeIf(XY::contains);
                    if (filterBools && yr && Y.isEmpty())
                        return Null;

                    assert (xr && yr);
                    var XX = CONJ.the(x.dt(), (Collection) X);
                    if (filterBools && XX instanceof Bool)
                        return Null; //safety filter TODO

                    var YY = CONJ.the(y.dt(), (Collection) Y);
                    if (filterBools && XX instanceof Bool)
                        return Null; //safety filter TODO

                    return IMPL.the(
                        XX,
                        i.dt(),
                        YY
                    );
                }
            },

            /**
             *
             * implConj(X,Y,Z,c,s) 		|-		  ( (X && Y) ==> Z)
             * implConj(X,Y,Z,d,s) 		|-		  ( (X || Y) ==> Z)
             * implConj(X,Y,Z,c,p) 		|-		  ( Z ==> (X && Y))
             * implConj(X,Y,Z,d,p) 		|-		  ( Z ==> (X || Y))
             * */
            new AbstractInlineFunctor("implConj") {
                @Override
                public Term apply(Evaluation evaluation, Subterms xyz) {
                    if (xyz.subs() != 5) throw new WTF();
                    return implConj(
                            xyz.sub(0), xyz.sub(1), xyz.sub(2),
                            CharAtom.the(xyz.sub(3)),
                            CharAtom.the(xyz.sub(4)) == 's');
                }

                private Term implConj(Term x, Term y, Term z, char mode, boolean SP) {
                    var p = nalPremise(d.premise);
                    return Cond.implConj(x, y, z, mode, SP, p.task.term(), p.beliefTerm());
                }
            },


//			new AbstractInlineFunctor1(ConjMatch.EARLIEST) {
//				@Override
//				protected Term apply1(Term conj) {
//					if (!conj.CONJ()) return Null;
//					final Term[] first = { Null };
//					((Compound)conj).eventsAND((when,what)->{ first[0] = what; return false; }, 0, false, false);
//					return first[0];
//				}
//			},

            new AbstractInlineFunctor1("debug") {
                @Override
                protected Term apply1(Term x) {
                    logger.info("{} {} {}", x, d.premise, d.unify);
                    return x;
                }
            },

//			new AbstractInlineFunctor1("negateRandomSubterm") {
//
//				@Override
//				protected Term apply1(Term _arg) {
//
//					boolean neg = _arg instanceof Neg;
//					Term arg = neg ? _arg.unneg() : _arg;
//
//					if (!(arg instanceof Compound))
//						return Null;
//
//					Subterms x = arg.subterms();
//					int n = x.subs();
//					if (n == 0)
//						return Null;
//
//					int which = d.randomBits.nextInt(n);
//					Subterms y = x.transformSub(which, Term::neg);
//					if (x != y)
//						return arg.op().the(y).negIf(neg);
//
//					return Null;
//				}
//			},

            /** TODO needs more work */
            new AbstractInlineFunctor2("mutex") {
                @Override
                protected Term apply(Term x, Term y) {
                    var xx = ConjList.conds(x);
                    var yy = ConjList.conds(y);
                    var common = new TermList();
                    var diff = new TermList();
                    if (xx.removeIf((w, xxx) -> {
                        {
                            var yi = yy.indexOf((Predicate<Term>) xxx::equals);
                            if (yi != -1) {
                                common.add(xxx);
                                yy.removeThe(yi);
                                return true;
                            }
                        }
                        {
                            var yj = yy.indexOf(xxx::equalsNeg);
                            if (yj != -1) {
                                diff.add(xxx);
                                yy.removeThe(yj);
                                return true;
                            }
                        }
                        return false;
                    })) {
                        common.add(DISJ(xx.term(), yy.term()));
                        var dd = diff.subs();
                        if (dd == 1) {
                            //drop it
                        } else {
                            var dpos = diff.count(t -> t instanceof Neg);
                            if (dpos == dd) {
                                //TODO polarities match; x <=> y
                            } else {
                                if (dd == 2) {
                                    if (dpos == 1) {
                                        //2-ary xor
                                        diff.set(0, diff.get(0).unneg());
                                        diff.set(1, diff.get(1).unneg());
                                        common.add(CONJ.the((Subterms) diff).neg());
                                    } else {
                                        return Null; //TODO
                                    }
                                }
                            }
                        }
                        return CONJ.the((Subterms) common);
                    } else
                        return Null; //shouldnt happen
                }
            }
        };
    }

    private static Term ifNovel(Term y, Term x) {
        return y.equals(x) ? Null : y;
    }

    private static Term unifiableSubEvent(Term conds, Term event, boolean novel, boolean pn, Deriver d) {
        //assert(conj.EVENTS());
        if (!conds.CONDS())
            return Null;

        try (var u = d.unifyTransform(TTL_UNISUBST)) {
            var t = Cond.unifiableCond(conds, event, pn, novel, u);
            return t == null ? Null : t;
        }
    }

    private static Functor narFunc(String term, NAR nar) {
        var c = nar.concept(term);
        if (c == null)
            throw new WTF("missing Deriver functor: " + term);
        return (Functor) c.term();
    }

    private static void add(Map<Atomic, Term> m, Term x) {
        m.put((Atomic) x.term(), x);
    }


}
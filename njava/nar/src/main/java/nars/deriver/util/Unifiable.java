package nars.deriver.util;

import nars.$;
import nars.Deriver;
import nars.Term;
import nars.deriver.reaction.MutableReaction;
import nars.deriver.reaction.Reaction;
import nars.func.UniSubst;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.util.transform.Subst;
import nars.term.var.Variable;
import nars.unify.Unify;
import nars.unify.constraint.RelationConstraint;

import java.util.Set;
import java.util.function.Predicate;

import static nars.Op.VAR_DEP;
import static nars.Op.VAR_INDEP;
import static nars.deriver.util.DeriverBuiltin.*;
import static nars.unify.Unifier.Equal;

public enum Unifiable { ;

    /** invariant conditions for which the arguments of functors present in a rule imply a winnowing constraint */
    public static Predicate<Term> eventFuncConstraints(MutableReaction p) {
        return x -> {
            var f = Functor.func(x);
            if (f != null) {
                var xx = Functor._args((Compound) x);
                if (f.equals(UniSubst.UNISUBST)) {
                    constrainUnifiable(xx, p);
                } else if (f.equals(Subst.SUBSTITUTE)) {
                    constrainSubstitute(xx, p);
//                } else if (f.equals(ConjMatch.EARLIEST)) {
//                    h.is(a.sub(0), CONJ); //TODO maybe conjSequence as this is the only case this functor makes sense to apply to
//                } else if (f.equals(Builtin.CONJ_WITHOUT)) {// || f.equals(ConjMatch.CONJ_WITHOUT_ALL)) {
//                    constrainCond(xx, true, false, p);
//                } else if (f.equals(Builtin.CONJ_WITHOUT_PN)) { // || f.equals(ConjMatch.CONJ_WITHOUT_ALL_PN)) {
//                    constrainCond(xx, true, true, p);
                } else if (condFunctors.contains(f)) {
                    constrainCond(xx, false, false, p);
                } else if (f.equals(UNIFIABLE_SUBCOND_PN)) {
                    constrainCond(xx, false, true, p);
                }
				/*
				TODO  || f.equals(ConjMatch.CONJ_WITHOUT_ALL)  || f.equals(ConjMatch.CONJ_WITHOUT_ALL_PN)
				which need special handling beause some, not all need to match
				*/

            }
            return true;
        };
    }

    private static final Set<Term> condFunctors = Set.of(
        BEFORE, DURING, BEFORE_OR_DURING, AFTER, AFTER_OR_DURING, UNIFIABLE_SUBCOND,WITHOUT_UNIFY
    );

    private static void constrainSubstitute(Subterms a, Reaction p) {
        //TODO for strict case
    }

    private static void constrainUnifiable(Subterms a, MutableReaction p) {

        var x = /*unwrapPolarize...*/(a.sub(1));
        if (x instanceof Variable xv) {

            var y = a.sub(2);
            if (y instanceof Variable yv) {

                //both x and y are constant
                int varBits;
                if (a.indexOf(UniSubst.DEP_VAR) > 2)
                    varBits = VAR_DEP.bit;
                else if (a.indexOf(UniSubst.INDEP_VAR) > 2)
                    varBits = VAR_INDEP.bit;
                else
                    varBits = VAR_DEP.bit | VAR_INDEP.bit;

                p.constrain(new Unifiability(xv, yv, a.contains(UniSubst.NOVEL), varBits));
            }
        }
    }

    private static void constrainCond(Subterms a, boolean exact, boolean depolarized, MutableReaction p) {
        p.condConstraint(a.sub(0), a.sub(1), exact, depolarized);
    }


//    public static class EventUnifiability extends RelationConstraint<Deriver.PremiseUnify> {
//        private static final Atom U = Atomic.atom(EventUnifiability.class.getSimpleName());
//        private final boolean forward;
//        private final boolean xNeg;
//
//        public EventUnifiability(Variable conj, Variable x, boolean xNeg) {
//            this(conj, x, xNeg, true);
//        }
//
//        private EventUnifiability(Variable conj, Variable x, boolean xNeg, boolean forward) {
//            super(U, conj, x, $.the(xNeg), $.the(forward));
//            this.xNeg = xNeg;
//            this.forward = forward;
//        }
//
//        @Override
//        protected EventUnifiability newMirror(Variable newX, Variable newY) {
//            return new EventUnifiability(newX, newY, xNeg, false);
//        }
//
//        /** TODO refactor */
//        @Override public boolean invalid(Term a, Term b, Deriver.PremiseUnify u) {
//
//            Term _x;
//            Compound conj;
//            if (forward) {
//                if (!(a instanceof Compound)) return true; //HACK why is this necessary (image normalization?)
//                conj = (Compound) a; _x = b;
//            } else {
//                if (!(b instanceof Compound)) return true; //HACK why is this necessary  (image normalization?)
//                conj = (Compound) b; _x = a;
//            }
//            if (!conj.CONJ()) return true; //HACK why is this necessary  (image normalization?)
//
//            Term x = _x.negIf(xNeg);
//
//            int cs = conj.structureSubs();
//            boolean cv =
//                    //conj.hasVars() || conj.SEQUENCE();
//                    Op.hasAny(cs, Op.Variables | CONJ.bit);
//            int xs = _x.structure();
//            boolean xv =
//                    Op.hasAny(xs, Op.Variables | CONJ.bit);
//
//            if (!cv && !xv)
//                return !Conj.eventOf(conj, x); //constant
//            else {
//
//                if (!Op.hasAny(cs, Op.Variables) && !Op.hasAny(xs, Op.Variables)) {
//                    if (!Conj.eventOfStructurally(conj, x, false))
//                        return true;
//                }
//
//
//                return x.CONJ() ?
//                        !((Compound)x).eventsAND((ww, hh) -> conj.eventsOR((when, what) ->
//                                        Unify.is(what, hh, u, TTL_CONJ_MATCH), 0,
//                                true, true), 0,
//                                    true, true)
//                        :
//                        !conj.eventsOR((when, what) ->
//                                        Unify.is(what, x, u, TTL_CONJ_MATCH), 0,
//                                true, true);
//            }
//        }
//
//        @Override
//        public float cost() {
//            return 2f;
//        }
//    }

    private static final class Unifiability extends RelationConstraint<Deriver.PremiseUnify> {
        final boolean isStrict; final int varBits;


        Unifiability(Variable x, Variable y, boolean isStrict, int varBits) {
            super(Unifiability.class, x, y, $.the(isStrict), $.the(varBits));
            this.isStrict = isStrict; this.varBits = varBits;
        }

        @Override
        protected Unifiability newMirror(Variable newX, Variable newY) {
            return new Unifiability(newX, newY, isStrict, varBits);
        }

        @Override
        public boolean invalid(Term x, Term y, Deriver.PremiseUnify context) {
            var u = Unify.how(x, y, varBits, context.dur, false);
            return !(u != null && (!isStrict || u != Equal));
        }

        @Override
        public float cost() {
            return 0.25f;
        }
    }
}
package nars.deriver.op;

import nars.$;
import nars.Deriver;
import nars.Premise;
import nars.Term;
import nars.deriver.reaction.compile.TrieReactionCompiler;
import nars.term.atom.Atom;
import nars.term.control.NOT;
import nars.term.control.PREDICATE;
import nars.term.util.TermPaths;
import nars.term.var.Variable;
import nars.unify.UnifyConstraint;
import nars.unify.constraint.PredicateConstraint;
import nars.unify.constraint.RelationConstraint;
import nars.unify.constraint.UnaryConstraint;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static nars.$.shortSubs;

public enum PredicateConstraints { ;

    private static final BiFunction<Term, Term, Term> TASK = (t, b) -> t, BELIEF = (t, b) -> b;

    /**
     * TODO generify a version of this allowing: U extends Unify
     */
    public static PredicateConstraint the(UnifyConstraint m, byte[] xInTask, byte[] xInBelief, byte[] yInTask, byte[] yInBelief) {

        int pathLen;

        BiFunction<Term, Term, Term> extractX;
        Term extractXterm;
        if (xInTask != null) { // && (xInBelief == null || xInTask.length < xInBelief.length)) {
            assert(xInBelief==null);
            int xtl = xInTask.length;
            extractX = xtl == 0 ? TASK : (t, b) -> t.subSafe(xInTask);
            extractXterm = xtl == 0 ? Premise.Task : $.func(Premise.Task, shortSubs(xInTask));
            pathLen = 1 + xtl;
        } else {
            int xbl = xInBelief.length;
            extractX = xbl == 0 ? BELIEF : (t, b) -> b.subSafe(xInBelief);
            extractXterm = xbl == 0 ? Premise.Belief : $.func(Premise.Belief, shortSubs(xInBelief));
            pathLen = 1 + xbl;
        }


        float cost = m.cost();
        Term M = m.term().replace(m.x, extractXterm);
        if (m instanceof final RelationConstraint r) {
            BiFunction<Term, Term, Term> extractY;
            Term extractYterm;
            int ytl = yInTask != null ? yInTask.length : 0;
            int ybl = yInBelief != null ? yInBelief.length : 0;
            if (yInTask != null && (yInBelief == null || ytl < ybl)) {
                extractY = ytl == 0 ? TASK : (t, b) ->
                        t.subSafe(yInTask);
                        //t.sub(yInTask);
                extractYterm = ytl == 0 ? Premise.Task : $.func(Premise.Task, shortSubs(yInTask));
                pathLen += 1 + ytl;
            } else {
                extractY = ybl == 0 ? BELIEF : (t, b) ->
                        b.subSafe(yInBelief);
                        //b.sub(yInBelief);
                extractYterm = ybl == 0 ? Premise.Belief : $.func(Premise.Belief, shortSubs(yInBelief));
                pathLen += 1 + ybl;
            }
            Term MM = M.replace(r.y, extractYterm);
            return new RelationPremisePredicateConstraint(MM,
                r, extractX, extractY, cost + pathLen * TrieReactionCompiler.pathCost);
        } else {
            return new UnaryPremisePredicateConstraint(M,
                (UnaryConstraint) m, extractX, cost + pathLen * TrieReactionCompiler.pathCost);
        }
    }

    private static Term xTerm(int r, Atom task, byte[] xInTask) {
        return r == 0 ? task : $.func(task, shortSubs(xInTask));
    }

    public static @Nullable PREDICATE<Deriver> predicateFromUnaryConstraint(UnifyConstraint cc, Term taskPattern, Term beliefPattern, Variable x) {
        byte[] xInT = TermPaths.pathExact(taskPattern, x),
                xInB = TermPaths.pathExact(beliefPattern, x);
        if (xInT != null || xInB != null) {
            if (xInT != null && xInB != null) {
                if (xInB.length < xInT.length) xInT = null;
                else xInB = null; //erase longer path
            }
            return the(cc, xInT, xInB, null, null);
        }
        return null;
    }

    @Nullable
    public static PREDICATE<Deriver> constraintPredicate(UnifyConstraint u, Term taskPattern, Term beliefPattern) {
        return switch (u) {
            case RelationConstraint.NegRelationConstraint negRelationConstraint ->
                predicateFromNegConstraint(negRelationConstraint, taskPattern, beliefPattern);
            case RelationConstraint relationConstraint ->
                predicateFromRelationConstraint(u, taskPattern, beliefPattern, relationConstraint, u.x);
            case UnaryConstraint unaryConstraint -> predicateFromUnaryConstraint(u, taskPattern, beliefPattern, u.x);
            default -> null;
        };
    }

    private static PREDICATE<Deriver> predicateFromNegConstraint(RelationConstraint.NegRelationConstraint r, Term taskPattern, Term beliefPattern) {
        var y = constraintPredicate(r.r, taskPattern, beliefPattern);
        return y != null ? y.neg() : null;
    }

    private static @Nullable PREDICATE<Deriver> predicateFromRelationConstraint(UnifyConstraint cc, Term taskPattern, Term beliefPattern, RelationConstraint relationConstraint, Variable x) {
        var y = relationConstraint.y;
        var xInT = TermPaths.pathExact(taskPattern, x);
        var xInB = TermPaths.pathExact(beliefPattern, x);
        if (xInT != null || xInB != null) {
            var yInT = TermPaths.pathExact(taskPattern, y);
            var yInB = TermPaths.pathExact(beliefPattern, y);
            if (yInT != null || yInB != null) {
                if (xInT != null && xInB != null) {
                    if (xInB.length < xInT.length) xInT = null;
                    else xInB = null; //erase longer path
                }
                if (yInT != null && yInB != null) {
                    if (yInB.length < yInT.length) yInT = null;
                    else yInB = null; //erase longer path
                }
                return the(cc, xInT, xInB, yInT, yInB);
            }
        }
        return null;
    }

    public static boolean isMirror(PREDICATE<Deriver> x, PREDICATE<Deriver> y) {
        if (x instanceof NOT ^ y instanceof NOT)
            return false;
        if (!(x.unneg() instanceof RelationPremisePredicateConstraint))
            return false;
        if (!(y.unneg() instanceof RelationPremisePredicateConstraint))
            return false;
        return ((RelationPremisePredicateConstraint) x.unneg()).isMirror((RelationPremisePredicateConstraint) y.unneg());
    }

    public static final class UnaryPremisePredicateConstraint extends PredicateConstraint<Deriver, UnaryConstraint<Deriver.PremiseUnify>> {

        UnaryPremisePredicateConstraint(Term id, UnaryConstraint<Deriver.PremiseUnify> m, BiFunction<Term, Term, Term> extractX, float cost) {
            super(id, m, extractX, null, cost);
        }

        @Override
        public boolean test(Deriver d) {
            Premise p = d.premise;
            Term x = extractX.apply(p.from(), p.to());
            return
                //x == null ||
                constraint.match(x);
        }

    }

    public static final class RelationPremisePredicateConstraint extends PredicateConstraint<Deriver, RelationConstraint<Deriver.PremiseUnify>> {

        RelationPremisePredicateConstraint(Term id, RelationConstraint<Deriver.PremiseUnify> m, BiFunction<Term, Term, Term> extractX, BiFunction<Term, Term, Term> extractY, float cost) {
            super(id, m, extractX, extractY, cost);
        }

        public boolean symmetric() {
            return this.constraint.symmetric();
        }

        @Override
        public boolean test(Deriver d) {
            Premise p = d.premise;
            Term T = p.from(), B = p.to();
            Term x = extractX.apply(T, B);
            if (x == null) {
                //throw new NullPointerException();
                return false;
            }
            Term y = extractY.apply(T, B);
            if (y == null) {
                //throw new NullPointerException();
                return false;
            }
            return !constraint.invalid(x, y, d.unify);
        }

        public boolean isMirror(RelationPremisePredicateConstraint r) {
            return
                    term().complexity() == r.term().complexity() && !constraint.x.equals(r.constraint.x) && r.constraint.equals(constraint.mirror()) //safe exhaustive
                    //constraint.x.equals(r.constraint.y) && constraint.y.equals(r.constraint.x))
            ;
        }
    }


}
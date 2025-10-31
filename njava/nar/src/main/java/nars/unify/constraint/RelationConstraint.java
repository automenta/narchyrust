package nars.unify.constraint;

import jcog.WTF;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.util.Terms;
import nars.term.var.Variable;
import nars.unify.Unify;
import nars.unify.UnifyConstraint;
import org.jetbrains.annotations.Nullable;

/**
 * tests a relation between two terms which may be involved (and prevened) from unifying
 */
public abstract class RelationConstraint<U extends Unify> extends UnifyConstraint<U> {

    public final Variable y;

    protected RelationConstraint(Class func, Variable x, Variable y, Term... args) {
        this(func.getSimpleName(), x, y, args);
    }

    protected RelationConstraint(String func, Variable x, Variable y, Term... args) {
        this(Atomic.atom(func), x, y, args);
    }

    private RelationConstraint(Term func, Variable x, Variable y) {
        this(func, x, y, Op.EmptyTermArray);
    }

    private RelationConstraint(Term func, Variable x, Variable y, Term... args) {
        super(x, func, ArrayUtil.prepend(y, args));
        this.y = y;
        if (x.equals(y))
            throw new WTF("x and y must be different");
    }

    @Override
    public final boolean invalid(Term x, U f) {
        Term yy = f.resolveVar(y);
        return yy != y && invalid(x, yy, f);
    }

    public abstract boolean invalid(Term x, Term y, U context);

    public final boolean valid(Term x, Term y, U context) {
        return !invalid(x, y, context);
    }

    public final RelationConstraint<U> mirror() {
        return newMirror(y, x);
    }

    /**
     * provide the reversed (mirror) constraint
     */
    protected abstract <R extends RelationConstraint<U>> R newMirror(Variable newX, Variable newY);

    public RelationConstraint<U> neg() {
        return new NegRelationConstraint<>(this);
    }


    /**
     * override to implement subsumption elimination
     */
    public boolean remainAmong(RelationConstraint<U> c) {
        return true;
    }

    /** override for functions which are not necessarily true: f(x,y) != f(y,x) */
    public boolean symmetric() {
        return true;
    }

    public static final class NegRelationConstraint<U extends Unify> extends RelationConstraint<U> {

        public final RelationConstraint<U> r;

        private NegRelationConstraint(RelationConstraint<U> r) {
            super(r.term().neg(), r.x, r.y);
            this.r = r;
            assert (!(r instanceof NegRelationConstraint) && (!(r.term() instanceof Neg)));
        }

        @Override
        public boolean remainAmong(RelationConstraint<U> c) {
            if (r.equals(c))
                throw new WTF(this + " conflicts with opposite " + c);
            return true;
        }

        @Override
        protected RelationConstraint<U> newMirror(Variable newX, Variable newY) {
            return r.mirror().neg();
            //return new NegRelationConstraint<>(this);
            //return new NegRelationConstraint(r.mirror());
        }

        @Override
        public RelationConstraint<U> neg() {
            return r;
        }

        @Override
        public boolean invalid(Term x, Term y, U context) {
            return r.valid(x, y, context);
        }

        @Override
        public float cost() {
            return r.cost() + 0.001f;
        }


    }

    public static final class NotEqual extends RelationConstraint {


        public NotEqual(Variable target, Variable other) {
            super(NotEqual.class, target, other);
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NotEqual(newX, newY);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            if (x == y) return true;
            boolean c = x instanceof Compound;
            if (c != y instanceof Compound) return false;
            //return c ? imageNormalize(x).equals(imageNormalize(y)) : x.equals(y);
            return x.equals(y);
        }

        @Override
        public boolean remainAmong(RelationConstraint c) {
            return
                    //subsumed by more specific types of inequality:
                    !(c instanceof EqualNeg) &&
                    !(c instanceof EqualPosOrNeg) &&
                    !(c instanceof NotEqualRoot) &&
                    //!(c instanceof SubConstraint) &&
                    !(c instanceof ComplexCmp cc && cc.cmp != 0);
        }

        //    static final PREDICATE<PreDerivation> TaskOrBeliefHasNeg = new AbstractPred<>($$("TaskOrBeliefHasNeg")) {
        //
        //        @Override
        //        public boolean test(PreDerivation d) {
        //            return d.taskTerm.hasAny(Op.NEG) || d.beliefTerm.hasAny(Op.NEG);
        //        }
        //
        //        @Override
        //        public float cost() {
        //            return 0.12f;
        //        }
        //    };

        //    /** compares target equality, unnegated */
        //    public static final class NotEqualUnnegConstraint extends RelationConstraint {
        //
        //
        //        public NotEqualUnnegConstraint(Term target, Term y) {
        //            super(target, y, "neqUnneg");
        //        }
        //
        //        @Override
        //        public float cost() {
        //            return 0.2f;
        //        }
        //
        //        @Override
        //        public boolean invalid(Term x, Term y) {
        //
        //            return
        //
        //                    y.equals(x);
        //
        //        }
        //
        //
        //    }

    }

//    public static final class NotEqualPosNegConstraint extends RelationConstraint {
//
//        public static final Atom neq = Atomic.atom("neqPN");
//
//        public NotEqualPosNegConstraint(Variable target, Variable other) {
//            super(neq, target, other);
//        }
//
//        @Override
//        protected NotEqualPosNegConstraint newMirror(Variable newX, Variable newY) {
//            return new NotEqualPosNegConstraint(newX, newY);
//        }
//
//        @Override
//        public float cost() {
//            return 0.19f;
//        }
//
//        @Override
//        public boolean invalid(Term x, Term y, Unify context) {
//            return y.equalsPosOrNeg(x);
//        }
//    }

    public static class OpEqual extends RelationConstraint {
        public OpEqual(Variable x, Variable y) {
            super(OpEqual.class, x, y);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new OpEqual(newX, newY);
        }

        @Override
        public float cost() {
            return 0.05f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return x.opID() != y.opID();
        }
    }

    public static final class NotEqualRoot extends RelationConstraint {


        public NotEqualRoot(Variable target, Variable other) {
            super(NotEqualRoot.class, target, other);
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NotEqualRoot(newX, newY);
        }

        @Override
        public float cost() {
            return 0.2f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            //return x==y || imageNormalize(x).equalsRoot(imageNormalize(y));
            return x == y || x.equalsRoot(y);
        }

        @Override
        public boolean remainAmong(RelationConstraint c) {
            return !(c instanceof EqualNeg);
        }

        //        @Override
//        public boolean remainInAndWith(RelationConstraint c) {
//            if (c instanceof NeqRootAndNotRecursiveSubtermOf)
//                return false;
//            return true;
//        }
    }

    public static class StructureIntersect extends RelationConstraint {
        public StructureIntersect(Variable x, Variable y) {
            super(StructureIntersect.class, x, y);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new StructureIntersect(newX, newY);
        }

        @Override
        public float cost() {
            return 0.05f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return x != y && (x.struct() & y.struct()) == 0;
        }
    }
//    public static class StructureContained extends RelationConstraint {
//
//        private final int exceptStruct;
//
//        public StructureContained(Variable x, Variable y, int exceptStruct) {
//            super(StructureContained.class.getSimpleName(), x, y, $.intRadix(exceptStruct, 2));
//            this.exceptStruct = exceptStruct;
//        }
//        @Override
//        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
//            return this; //HACK TODO
//        }
//
//        @Override
//        public float cost() {
//            return 0.03f;
//        }
//        @Override
//        public boolean invalid(Term x, Term y, Unify context) {
//            if (x==y) return false;
//            int xs = (x.structure() & ~exceptStruct), ys = (y.structure() & ~exceptStruct);
//            return !Op.hasAll(xs, ys);
//        }
//    }

    /**
     * compares according to volume, if different
     */
    public static class StructureCoContained extends RelationConstraint {

        private final int structExcept;

        public StructureCoContained(Variable x, Variable y, int structExcept) {
            super(StructureCoContained.class, x, y, $.intRadix(structExcept, 2));
            this.structExcept = structExcept;
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new StructureCoContained(newX, newY, structExcept);
        }

        @Override
        public float cost() {
            return 0.03f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            if (x==y) return false;
            if (x.equals(y)) return false;
            int xs = x.struct() & ~structExcept, ys = y.struct() & ~structExcept;
            if (xs == ys)
                return false; //identical structure

            int xv = x.complexity(), yv = y.complexity();
            if (xv == yv)
                return xv == 1
                       ||
                       (!Op.hasAll(xs, ys) && !Op.hasAll(ys, xs));
            else if (xv > yv)
                return !Op.hasAll(xs, ys);
            else
                return !Op.hasAll(ys, xs);
        }
    }

    public static class SetsIntersect extends RelationConstraint {
        public SetsIntersect(Variable x, Variable y) {
            super(SetsIntersect.class, x, y);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new SetsIntersect(newX, newY);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {

            if (x instanceof Compound && y instanceof Compound) {
                Op xo = x.op();
                if (xo.set && y.opID() == xo.id) {
                    Subterms xx = x.subterms(), yy = y.subterms();

                    //TODO check heuristic direction
                    return xx.complexity() <= yy.complexity() ? !xx.containsAny(yy) : !yy.containsAny(xx);
                }
            }
            return false;
        }
    }

    public static class NotSetsOrDifferentSets extends RelationConstraint {
        public NotSetsOrDifferentSets(Variable target, Variable other) {
            super(NotSetsOrDifferentSets.class, target, other);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NotSetsOrDifferentSets(newX, newY);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            if (!(x instanceof Compound) || !(y instanceof Compound))
                return false;

            Op xo = x.op();
            return xo.set && (xo.id == y.opID());
        }
    }

//    public static final class SubCountEqual extends RelationConstraint {
//
//        public SubCountEqual(Variable target, Variable other) {
//            super("SubsEqual", target, other);
//        }
//
//        @Override
//        protected RelationConstraint newMirror(Variable newX, Variable newY) {
//            return new SubCountEqual(newX, newY);
//        }
//
//        @Override
//        public float cost() {
//            return 0.04f;
//        }
//
//        @Override
//        public boolean invalid(Term x, Term y, Unify context) {
//            return !(y instanceof Compound) || (x != y && x.subs() != y.subs());
//        }
//
//    }

//    /**
//     * if both are inheritance, prohibit if the subjects or predicates match.  this is to exclude
//     * certain derivations which occurr otherwise in NAL1..NAL3
//     */
//    public static final class NoCommonInh extends RelationConstraint {
//
//        public NoCommonInh(Variable target, Variable other) {
//            super("noCommonInh", target, other);
//        }
//
//        @Override
//        protected RelationConstraint newMirror(Variable newX, Variable newY) {
//            return new NoCommonInh(newX, newY);
//        }
//
//        @Override
//        public float cost() {
//            return 0.2f;
//        }
//
//        @Override
//        public boolean invalid(Term x, Term y, Unify context) {
//            return (x.op() == INH && y.op() == INH && (x.sub(0).equals(y.sub(0)) || x.sub(1).equals(y.sub(1))));
//        }
//
//    }

    /**
     * containment test of x to y's subterms and y to x's subterms
     */
    public static final class NotRecursiveSubtermOf extends RelationConstraint {

        public NotRecursiveSubtermOf(Variable x, Variable y) {
            super(NotRecursiveSubtermOf.class, x, y);
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NotRecursiveSubtermOf(newX, newY);
        }

        @Override
        public float cost() {
            return 0.4f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return Terms.rCom(x, y);
        }

    }


}
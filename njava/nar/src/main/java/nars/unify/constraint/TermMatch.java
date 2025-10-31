package nars.unify.constraint;

import jcog.Util;
import nars.$;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.var.Variable;
import nars.unify.UnifyConstraint;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.SETe;
import static nars.Op.XTERNAL;

/**
 * Is term a...
 */
public abstract class TermMatch implements Predicate<Term> {

    public static final Comparator<? super TermMatch> paramComparator = Comparator.comparing(TermMatch::param);
    private static final TermMatch[] EmptyArray = new TermMatch[0];

    public static Term resolve(Function<?, Term> resolve) {
        return resolve instanceof Termed rt ? rt.term() : $.quote(resolve.toString());
    }

    public static TermMatch AND(SortedSet<TermMatch> m) {
        return switch (m.size()) {
            case 0 -> throw new UnsupportedOperationException();
            case 1 -> m.first();
            default -> new TermMatcherAND(m);
        };
    }

    public Term name(@Nullable Function<?, Term> r) {
        var c = this.getClass();
        var cc = c.isAnonymousClass() ?
                this.toString() :
                c.getSimpleName();
        return name(cc, r, this.param());
    }

    public static Term name(String klass, @Nullable Function<?, Term> r, Term... p) {
        var a = Atomic.atom(klass);
        var P = p != null && p.length > 0;
        if (P)
            Util.replaceDirect(p, z -> z==null ? Bool.True : z); //replace nulls
        if (r != null) {
            var s = resolve(r);
            return P ? $.func(a, s, $.p(p)) : $.func(a, s);
        } else
            return P ? $.func(a, $.p(p)) : a;
    }

    public final Term name() {
        return name(null);
    }

    /**
     * target representing any unique parameters beyond the the class name which is automatically incorporated into the predicate it forms
     */
    @Nullable
    public Term param() {
        return null;
    }

    public final UnifyConstraint constraint(Variable x, boolean trueOrFalse) {
        return new UnaryConstraint<>(this, x, trueOrFalse);
    }

    public abstract float cost();

    /**
     * is the op one of the true bits of the provide vector ("is any")
     */
    public static class Is extends TermMatch {

        public static TermMatch is(Op op) {
            return is(op.bit);
        }

        public static TermMatch is(int struct) {
            return struct == Op.Variables ? Variable : new Is(struct);
        }

        public final int anyStruct;

        protected Is(int anyStruct) {
            this.anyStruct = anyStruct;
        }

        @Override
        public Term param() {
            return Op.strucTerm(anyStruct);
        }

        @Override
        public float cost() {
            return 0.04f;
        }

        @Override
        public boolean test(Term x) {
            return x.isAny(anyStruct);
        }

//        @Override
//        public boolean testSuper(Term sx) {
//            //return sx.subterms().hasAny(struct);
//            return true;
//        }

    }

    public static class HasAllStruct extends TermMatch {

        public final int allStruct;

        public HasAllStruct(int allStruct) {
            assert (allStruct != 0);
            this.allStruct = allStruct;
        }

        @Override
        public Term param() {
            return Op.strucTerm(allStruct);
        }

        @Override
        public float cost() {
            return 0.04f;
        }

        @Override
        public boolean test(Term x) {
            return x.hasAll(allStruct);
        }

    }

    public static class IsUnneg extends Is {

        public IsUnneg(int any) {
            super(any);
        }

        @Override
        public float cost() {
            return 0.03f + super.cost();
        }

        @Override
        public boolean test(Term x) {
            return super.test(x.unneg());
        }

//        @Override
//        public boolean testSuper(Term sx) {
//            return sx.hasAny(struct | (requireNegation ? NEG.bit : 0));
//        }
    }

    public static final class ComplexMin extends TermMatch {
        private final int min;
        private final Atomic param;

        public ComplexMin(int min) {
            if (min < 2)
                throw new UnsupportedOperationException();
            this.param = $.the(this.min = min);
        }

        @Override
        public Term param() {
            return param;
        }

        @Override
        public float cost() {
            return 0.025f;
        }

        @Override
        public boolean test(Term x) {
            return x instanceof Compound c && c.complexity() >= min;
        }
    }

    public static final class ComplexMax extends TermMatch {
        private final int volMax;
        private final Atomic param;

        public ComplexMax(int volMax) {
            if (volMax < 2) throw new IllegalArgumentException();
            this.param = $.the(this.volMax = volMax);
        }

        @Override
        public Term param() {
            return param;
        }

        @Override
        public float cost() {
            return 0.03f;
        }

        @Override
        public boolean test(Term x) {
            return x.complexity() <= volMax;
        }

    }

//    /**
//     * compound is of a specific type, and has the target in its structure
//     * TODO combine Is + Has in postprocessing step
//     */
//    public static final class IsHas extends TermMatcher {
//
//        final int struct;
//        final int structSubs;
//        private final byte is;
//
//        private final Term param;
//        //private final float cost;
//
//        public IsHas(Op is, int structSubs) {
//            //assert (!is.atomic && depth > 0 || is != NEG) : "taskTerm or beliefTerm will never be --";
//
//            this.is = is.id;
//            this.struct = structSubs | is.bit;
//            this.structSubs = structSubs;
//            assert (Integer.bitCount(struct) >= Integer.bitCount(structSubs));
//
//            //this.cost = structureCost(0.02f, struct);
//
//            Atom isParam = Op.the(this.is).atom;
//            this.param = structSubs != 0 ? $.p(isParam, Op.strucTerm(structSubs)) : isParam;
//        }
//
//
//        @Override
//        public boolean test(Term term) {
//            return term instanceof Compound &&
//                    term.opID() == is &&
//                    (structSubs == 0 || Op.hasAll(term.structureSubs(), structSubs));
//        }
////
////        @Override
////        public boolean testSuper(Term term) {
//////            return term instanceof Compound &&
//////                    testVol(term) && term.subterms().hasAll(struct);
////            return true;
////        }
//
//        @Override
//        public Term param() {
//            return param;
//        }
//
//        @Override
//        public float cost() {
//            return 0.025f;
//        }
//    }

    /**
     * non-recursive containment
     */
    public static final class Contains extends TermMatch {

        public final Term x;
//        private final int xStruct;

        public Contains(Term x) {
            assert (!x.VAR_PATTERN()); //HACK
            this.x = x;
//            this.xStruct = x.structure();
        }

        @Override
        public Term param() {
            return x;
        }

        @Override
        public float cost() {
            return 0.15f;
        }

        @Override
        public boolean test(Term term) {
            return term instanceof Compound c && c.contains(x);
        }

//        @Override
//        public boolean testSuper(Term term) {
//            //return term.hasAll(xStruct);
//                    //containsRecursively(this.x);
//            return true;
//        }
    }

//    /**
//     * non-recursive containment
//     */
//    public final static class EqualsRoot extends TermMatcher {
//
//        public final Term x;
//
//        public EqualsRoot(Term x) {
//            this.x = x;
//            assert(!(x instanceof VarPattern)); //HACK
//        }
//
//        @Override
//        public Term param() {
//            return x;
//        }
//
//        @Override
//        public float cost() {
//            return 0.15f;
//        }
//
//        @Override
//        public boolean test(Term term) {
//            return term.equalsRoot(x);
//        }
//
//        @Override
//        public boolean testSuper(Term x) {
//            return !x.impossibleSubTerm(this.x);
//        }
//    }

    /**
     * non-recursive containment
     */
    public static final class Equals extends TermMatch {

        public final Term x;
        final Predicate<Term> xEq;

        public Equals(Term x) {
            this.x = x;
            this.xEq = x.equals();
            assert (!x.VAR_PATTERN()); //HACK
        }

        @Override
        public Term param() {
            return x;
        }

        @Override
        public float cost() {
            return 0.03f;
        }

        @Override
        public boolean test(Term term) {
            return xEq.test(term);
        }

    }

    public static final class SubsMin extends TermMatch {

        final short subsMin;

        public SubsMin(short subsMin) {
            this.subsMin = subsMin;
            assert (subsMin > 0);
        }

        @Override
        public Term param() {
            return $.the(subsMin);
        }

        @Override
        public boolean test(Term term) {
            return term instanceof Compound && (subsMin == 1 || term.subs() >= subsMin);
        }

        @Override
        public float cost() {
            return 0.02f;
        }

    }

    private static final class TermMatcherAND extends TermMatch {

        final TermMatch[] m;
        final Term id;
        final float cost;

        TermMatcherAND(SortedSet<TermMatch> matchers) {
            assert (matchers.size() > 1);
            m = matchers.toArray(EmptyArray);
            id = SETe.map(TermMatch::name, m);
            cost = (float) Util.sum(TermMatch::cost, m);
        }

        @Override
        public Term name(@Nullable Function<?, Term> r) {
            return r != null ? $.p(id, resolve(r)) : id;
        }

        //TODO Arrays.sort(m, PREDICATE.CostIncreasing);

        @Override
        public boolean test(Term term) {
            for (var x : m)
                if (!x.test(term)) return false;
            return true;
        }

        @Nullable
        @Override
        public Term param() {
            return id;
        }

        @Override
        public float cost() {
            return cost;
        }
    }


    /**
     * applies to all subterms
     */
    public static final class TermMatcherSubterms extends TermMatch {

        private static final Atomic _I = Atomic.atomic(TermMatcherSubterms.class.getSimpleName());

        private final TermMatch m;
        private final Term param;

        public TermMatcherSubterms(TermMatch m) {
            super();
            this.m = m;
            this.param = $.func(_I, m.param());
        }

        @Override
        public @Nullable Term param() {
            return param;
        }

        @Override
        public float cost() {
            return m.cost() * 2 /* TODO ? */;
        }

        @Override
        public boolean test(Term term) {
            return term instanceof Compound C && C.AND(m);
        }
    }


    public static final TermMatch SEQ = new TermMatch() {

        @Override
        public boolean test(Term t) {
            return t.SEQ();
        }

        @Override
        public float cost() {
            return 0.06f;
        }
    };

    public static final TermMatch Conds = new TermConds();

    public static final TermMatch Variable = new Is(Op.Variables);

    public static final TermMatch Timeless = new Timeless();

    private static class TermConds extends TermMatch {

        @Override
        public boolean test(Term t) {
            return t.CONDS();
        }

        @Override
        public float cost() {
            return 0.04f;
        }
    }

    private static class Timeless extends TermMatch {

        @Override
        public boolean test(Term x) {
            return switch (x.op()) {
                case CONJ -> x.dt() != XTERNAL && x.seqDur() == 0;
                case IMPL -> x.dt() != XTERNAL && !Op.dtSpecial(x.dt()) && testSubterms((Compound) x);
                default -> !(x instanceof Compound c) ||
                        !x.hasAny(Op.Temporals) ||
                        testSubterms(c);
            };
        }

        private boolean testSubterms(Compound x) {
            return x.AND(this);
        }

        @Override
        public float cost() {
            return 0.25f;
        }
    }

    public static final Comparator<TermMatch> CostIncreasing = (a, b) -> {
        if (a==b) return 0;
        float ac = a.cost(), bc = b.cost();
        if (ac > bc) return +1;
        else if (ac < bc) return -1;
        else return a.name().compareTo(b.name());
    };
}
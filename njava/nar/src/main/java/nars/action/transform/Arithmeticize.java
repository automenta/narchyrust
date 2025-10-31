package nars.action.transform;

import jcog.Is;
import jcog.Research;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.decide.Roulette;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Deriver;
import nars.Op;
import nars.Term;
import nars.func.ArithmeticCommutiveFunctor;
import nars.func.Cmp;
import nars.func.MathFunc;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.Terms;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.var.Variable;
import nars.unify.constraint.TermMatch;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import static nars.Op.*;
import static nars.func.MathFunc.mul;

/**
 * introduces arithmetic relationships between differing numeric subterms
 * responsible for showing the reasoner mathematical relations between
 * numbers appearing in compound terms.
 * <p>
 * TODO
 * greater/less than comparisons
 * ranges
 * https://github.com/konsoletyper/teavm/blob/master/core/src/main/java/org/teavm/common/RangeTree.java
 */
@Research
@Is("Arithmetic_coding") public enum Arithmeticize {
    ;

    static final boolean monotonicOnly = false;

    private static final Variable A = $.varDep("A_");
    private static final Variable B = $.varDep("B_");
    private static final Op Aop = A.op();

    private static final Variable VAR_DEP_1 = $.varDep(1);
    private static final Variable VAR_DEP_2 = $.varDep(2);

    /** if false, if there are vector ops, individual scalar ops are skipped */
    private static final boolean SCALAR_AND_VECTOR_BOTH = false;

    public static Term apply(Term x, RandomGenerator rng) {
        return apply(x, x, rng);
    }

    /** @param y the subterm in x that is searched in numerically */
    public static Term apply(Term x, Term y, RandomGenerator rng) {
        IntHashSet ints = new IntHashSet(2);
        IntObjectHashMap<Set<Compound>> vectors =
            y.hasAny(PROD) ? new IntObjectHashMap<>(0) : null;

        y.ANDrecurse(t -> t.hasAny(INT), t -> {
            if (t instanceof Int I)
                ints.add(I.getAsInt());
            else if (t instanceof Compound T && vectors!=null && t.PROD()) {
                int arity;
                if ((arity = t.subs()) > 1 && t.structSurface() == INT.bit)
                    vectors.getIfAbsentPut(arity, ()->new UnifiedSet<>(2)).add(T);
            }
        });

        int vu = countVectors(vectors);

        return ints.size() < 2 && vu < 2 ? null :
            apply(x, rng, transforms(rng, ints, vectors, vu));
    }

    private static int countVectors(IntObjectHashMap<java.util.Set<Compound>> uniqueVectors) {
        if (uniqueVectors==null) return 0;
        int vu = 0;
        Iterator<Set<Compound>> vuu = uniqueVectors.iterator();
        while (vuu.hasNext()) {
            int s = vuu.next().size();
            if (s < 2)
                vuu.remove();
            else
                vu += s;
        }
        return vu;
    }

    private static Lst<PrioritizedTransform> transforms(RandomGenerator rng, IntHashSet uniqueInts, IntObjectHashMap<java.util.Set<Compound>> uniqueVectors, int vu) {
        Lst<PrioritizedTransform> l = new Lst<>(2);
        
        if (vu > 0)
            vectorMods(uniqueVectors, l, rng);

        if (SCALAR_AND_VECTOR_BOTH || l.isEmpty())
            scalarMods(uniqueInts, l);

        return l;
    }

    @Nullable private static Term apply(Term x, RandomGenerator rng, Lst<PrioritizedTransform> m) {
        int n = m.size();
        return n == 0 ? null :
            m.get(n > 1 ? Roulette.selectRoulette(n, c -> m.get(c).pri, rng) : 0).apply(x);
    }

    private static void vectorMods(IntObjectHashMap<Set<Compound>> uniqueVectors, Lst<PrioritizedTransform> l, RandomGenerator rng) {
        Set<Compound> s;
        if (uniqueVectors.size() > 1) {
            //choose one
            Object[] a = uniqueVectors.toArray();
            int which =
                    rng.nextInt(a.length); //random
            //TODO , max unique, max arity, etc

            s = (Set<Compound>) a[which];
        } else {
            s = uniqueVectors.getFirst();
        }

        //choose 2 at random
        //TODO more advanced heuristics: incl. clustering etc
        Compound[] ss = s.toArray(EmptyCompoundArray);
        ArrayUtil.shuffle(ss, rng);
        Compound A = ss[0], B = ss[1];

        l.add(new Translate(A, B));
        l.addIfNotNull(VectorMul.the(A,B));
    }

    private static void scalarMods(IntHashSet iii, Lst<PrioritizedTransform> l) {

        int[] ii = iii.toSortedArray();
        int iin = ii.length;

        for (int bIth = 0; bIth < iin - 1; bIth++) {
            int smaller = ii[bIth];
            for (int aIth = bIth + 1; aIth < iin; aIth++) {
                int bigger = ii[aIth];

                //compareMods(smaller, bigger, o);

                scalarMods(smaller, bigger, l);

                if (!monotonicOnly)
                    scalarMods(bigger, smaller, l);
            }
        }
    }
    private static Term _arith(ArithmeticCommutiveFunctor f, Term x, Term y) {
        return $.func(f, Terms.sort(x, y));
    }

    private static Term erase(Term a, boolean recursive) {
        return recursive ? EraseTransform.apply(a) : _erase(a);

//        if (a.PROD()) {
//            return EraseTransform.apply(a);
//        } else
//            return $.varDep(a instanceof Int ?
//                "_" + Int.the(a) :
//                "s" + System.identityHashCode(a)
//            );
    }

    private static final RecursiveTermTransform EraseTransform = new RecursiveTermTransform() {
        @Override
        protected Term applyAtomic(Atomic a) {
            return a instanceof Int ? _erase(a) : a;
        }
    };

    private static Variable _erase(Term a) {
        return $.varDep(a instanceof Int i ?
            "_" + Int.i(i) :
            "s" + System.identityHashCode(a)
        );
    }

    private static void scalarMods(int x, int y, Lst<PrioritizedTransform> o) {
        if (x != 0 && y != 0) {
            //noinspection IntegerDivisionInFloatingPointContext
            if ((-x == y) || (Math.abs(x) != 1 && Math.abs(y) != 1 && Util.equals(y / x, (float) y / x)))
                o.add(new ScalarMul(x, y));
        }

        o.add(new Translate(x, y));

//        if (x < y)
//            o.add(new CompareOp(x, y));

//        if (x!=y) {
//            o.add(new MaxMin(x, y, true));
//            o.add(new MaxMin(x, y, false));
//        }

//        if (y>1)
//            o.add(new Modulo(x, y));

        //o.add(new Power(x, y));

    }


    /** old impl */
    public static class ArithmeticIntroduction0 extends CondIntroduction {

        static final int VOLUME_MARGIN = 3;

        {
            hasAny(PremiseTask, INT);
            volMin(PremiseTask, 3);
            volMaxMargin(PremiseTask, VOLUME_MARGIN);
            //store = false;

//            codec = ()->new TermCodec.AnonTermCodec(true /* int's */,false)/* {
//                @Override
//                protected boolean abbreviate(Compound x) {
//                    return !x.hasAny(INT);
//                }
//            }*/;
        }

        @Override
        protected Term apply(Term x, int volMax, Deriver d) {
            return Arithmeticize.apply(x, d.rng);
        }

    }

    public static class ArithmeticIntroduction extends CondIntroduction {

        /** TODO refine */
        private static final int VOLUME_MARGIN = 3;

        private static final TermMatch Atleast2UniqueIntSubterms = new TermMatch() {

            @Override public boolean test(Term x) {
                //return ((Compound)term).countRecursive(Term::INT) >= 2;
                var ints = new IntHashSet(2);
                x.recurseTermsOrdered(z -> z.hasAny(INT), y -> {
                    if (y.INT())
                        ints.add(Int.i(y));
                });
                return ints.size()>=2;
//                return x.countRecursive(y -> {
//                    if (y.INT()) {
//                        ints.add((Int.the(y)));
//                        return true;
//                    }
//                    return false;
//                }) >= 2 && ints.size() >= 2;
            }

            @Override
            public float cost() {
                return 0.5f;
            }
        };

        {
            volMin(PremiseTask, 3);
            volMaxMargin(PremiseTask, VOLUME_MARGIN);
            hasAny(PremiseBelief, INT);
            iff(PremiseTask, Atleast2UniqueIntSubterms);

            //store = false;
//            codec = ()->new TermCodec.AnonTermCodec(true /* int's */,false)/* {
//                @Override
//                protected boolean abbreviate(Compound x) {
//                    return !x.hasAny(INT);
//                }
//            }*/;
        }

        @Override
        protected boolean self() {
            return false;
        }

        @Override
        protected Term apply(Term x, int volMax, Deriver d) {
            return Arithmeticize.apply(x, d.premise.to(), d.rng);
        }
    }

    private abstract static class PrioritizedTransform  {

        static final PrioritizedTransform[] EmptyArray = new PrioritizedTransform[0];
        private final float pri;

        PrioritizedTransform(float pri) {
            this.pri = pri;
        }

        public abstract Term apply(Term x);
    }

    @Deprecated private static class CompareOp extends PrioritizedTransform {
        static final Term cmpABNormalized = eqCmp(VAR_DEP_1, VAR_DEP_2, -1);
        static final Term cmpABUnnormalized = eqCmp(A, B, -1);
        private final int a;
        private final int b;


        CompareOp(int smaller, int bigger) {
            super(0.5f);
            //assert(smaller < bigger);
            //if (smaller >= bigger) throw new WTF();
            this.a = smaller;
            this.b = bigger;
        }

        @Deprecated
        private static Term eqCmp(Term a, Term b, int c) {
            return EQ.the(Int.i(c), Cmp.cmp(a, b));
        }

        @Override
        public Term apply(Term x) {
            //TODO anon
            Term cmp;
            Variable A, B;
            if (!x.hasAny(Aop)) {
                A = VAR_DEP_1;
                B = VAR_DEP_2; //optimistic prenormalization
                cmp = cmpABNormalized;
            } else {
                A = Arithmeticize.A;
                B = Arithmeticize.B;
                cmp = cmpABUnnormalized;
            }

            Term xx = x.replace(Map.of(Int.i(a), A, Int.i(b), B));

            return (xx instanceof Bool) ? null : CONJ.the(xx, cmp);
        }

    }

    /**
     * b > a
     */
    private static class ScalarMul extends PrioritizedTransform {
        final int a, b;

        private ScalarMul(int a, int b) {
            super(a == -b ? 1 : 0.25f);
            this.a = a;
            this.b = b;
        }


        @Override
        public Term apply(Term x) {
            Int A = Int.i(a);
            Term aV = erase(A, false);
            return x.replace(A, aV)
                    .replace(Int.i(b), _arith(mul, aV, Int.i(b / a)));
        }
    }

    private static class VectorMul extends PrioritizedTransform {
        static final boolean integersOnly = true;

        final Term a, b;
        final float y;

        @Nullable static VectorMul the(Term A, Term B) {
            int n = A.subs();
            float y = Float.NaN;
            for (int i = 0; i < n; i++) {
                int b = Int.i(B.sub(i));
                if (b == 0) return null; //div by zero

                int a = Int.i(A.sub(i));
                float aDivB = ((float) a) / b;
                if (i == 0) {
                    y = aDivB;
                    if (integersOnly && y!=Math.round(y))
                        return null;
                } else {
                    if (y!=aDivB)
                        return null;
                }
            }
            return new VectorMul(A, B, y);
        }

        private VectorMul(Term a, Term b, float y) {
            super(1);
            this.a = a;
            this.b = b;
            this.y = y;
        }

        @Override
        public Term apply(Term x) {
            //return x.replace(a, _arith(mul, $.the(y), b));

            Term bV = erase(b, true);
            return x.replace(b, bV)
                    .replace(a, _arith(mul, $.the(y), bV));
        }
    }

    private static class Translate extends PrioritizedTransform {
        final Term a, b;

        Translate(int a, int b) {
            this(Int.i(a), Int.i(b));
        }

        Translate(Term a, Term b) {
            super(1 /*0.5f * (a.subs() + 1)*/);
            this.a = a;
            this.b = b;
        }

        @Override
        public Term apply(Term x) {
            Term e = null;

            Term aV = erase(a, false);

            Term bMinA;
            if (a instanceof Int) {
                int A = Int.i(a);
                int bMinusA = Int.i(b) - A;
//                if (A != 1 && A == bMinusA) {
//                    e = _arith(mul, Int.TWO, aV);
//                } else if (A == -bMinusA) {
//                    //HACK
//                    return null; //e = func(mul, Terms.sort(Int.the(-2), a));
//                }
                bMinA = Int.i(bMinusA);

            } else {
                int n = a.subs();
                int[] delta = new int[n];
                for (int i = 0; i < n; i++)
                    delta[i] = Int.i(b.sub(i)) - Int.i(a.sub(i));

                bMinA = PROD.the($.intSubs(delta));
            }


            if (e == null)
                e = _arith(MathFunc.add, aV, bMinA);

            //2 steps to avoid erasing constants in 'e':
            return x.replace(a, aV)
                    .replace(b, e);
        }

    }
    private static class Modulo extends PrioritizedTransform {
        final int a, b;

        Modulo(int a, int b) {
            super(0.25f);
            this.a = a;
            this.b = b;
        }

        @Override
        public Term apply(Term x) {
            if (b == 0) return null; // Avoid division by zero
            var A = Int.i(a);
            var B = Int.i(b);
            Term aV = erase(A, false);
            Term bV = erase(B, false);
            return x.replace(A, aV)
                    .replace(B, $.func(MathFunc.mod, aV, bV));
        }
    }

    /** untested */
    private static class Power extends PrioritizedTransform {
        final int a, b;

        Power(int a, int b) {
            super(0.25f);
            this.a = a;
            this.b = b;
        }

        @Override
        public Term apply(Term x) {
            Term aV = erase(Int.i(a), false);
            Term bV = erase(Int.i(b), false);
            Term pow = $.func(MathFunc.pow, aV, bV);
            return x.replace(Int.i(a), pow);
        }
    }
    private static class MaxMin extends PrioritizedTransform {
        final int a, b;
        private final boolean maxOrMin;

        MaxMin(int a, int b, boolean maxOrMin) {
            super(0.1f);
            this.a = a;
            this.b = b;
            this.maxOrMin = maxOrMin;
        }

        @Override
        public Term apply(Term x) {
            Term aV = erase(Int.i(a), false);
            Term bV = erase(Int.i(b), false);
            Term function;
            int value;

            Term[] abvSorted = { aV, bV }; Arrays.sort(abvSorted);
            if (maxOrMin) {
                function = $.func(MathFunc.max, abvSorted);
                value = Math.max(a, b);
            } else {
                function = $.func(MathFunc.min, abvSorted);
                value = Math.min(a, b);
            }
            var X = x.replace(Int.i(a), aV).replace(Int.i(b), bV);
            return CONJ.the(X, EQ.the(Int.i(value), function));
        }
    }
}
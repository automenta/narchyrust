//package nars.derive.util.memoize;
//
//import jcog.Util;
//import jcog.data.bit.MetalBitSet;
//import jcog.data.bit.MetalDibitSet;
//import jcog.data.list.Lst;
//import nars.$;
//import nars.Term;
//import nars.Deriver;
//import nars.term.Neg;
//import nars.term.atom.Atomic;
//import nars.term.control.PREDICATE;
//
//import java.util.List;
//
//import static jcog.data.bit.MetalDibitSet.is;
//import static jcog.data.bit.MetalDibitSet.needTest;
//
//final class MemoizedPredicateN extends PREDICATE<Deriver> {
//
//    private final int[] slots;
//    private final float costMin, costMax;
//    private final PREDICATE<Deriver>[] cond;
//
//    private static final Atomic MEMOIZED_N = Atomic.the(MemoizedPredicateN.class.getSimpleName());
//    final MetalBitSet polarity;
//
//    private MemoizedPredicateN(List<PREDICATE<Deriver> /*MemoizedPredicate1 | negations of MemoizedPredicate1*/> m) {
//        super($.p(MEMOIZED_N, $.p(m.stream().map(x -> x.ref).toArray(Term[]::new))));
//
//        int s = 0, n = m.size();
//        polarity = MetalBitSet.bits(n);
//
//        assert (n <= 32); //bitvector
//        slots = new int[n];
//        cond = new PREDICATE[n];
//        float cMin = Float.POSITIVE_INFINITY, cMax = Float.NEGATIVE_INFINITY;
//        for (PREDICATE _ms : m) {
//            boolean neg = _ms instanceof Neg;
//            MemoizedPredicate1 ms = (MemoizedPredicate1)(neg ? _ms.unneg() : _ms);
//            slots[s] = ms.slot;
//            cond[s] = ms.test;
//            polarity.set(s, !neg);
//            s++;
//
//            float cs = ms.cost();
//            cMin = Util.min(cMin, cs);
//            cMax = Util.max(cMax, cs);
//        }
//        this.costMax = cMax;
//        this.costMin = cMin;
//    }
//
//    public static MemoizedPredicateN the(Lst<PREDICATE<Deriver>> m) {
//        //m.sort(increasingCost);
//        m.sort(CostIncreasing);
//        return new MemoizedPredicateN(m);
//    }
//
//    @Override
//    public float cost() {
//        return costMin;
//    }
//
//    @Override
//    public boolean test(Deriver d) {
//        int n = slots.length;
//
//        MetalDibitSet s = d.predMemo;
//
//        int pending = s.pending(slots, polarity, n);
//        if (pending == -1) return false;
//        if (pending == 0)  return true;
//
//        for (int i = 0; i < n; i++) {
//            /* TODO replace needTest with a faster bit finder */
//            if (needTest(pending, i) && !test(i, s, d))
//                return false;
//        }
//        return true;
//    }
//
//    private boolean test(int i, MetalDibitSet s, Deriver d) {
//        return is(i, s.set(slots[i], true, cond[i].test(d)), polarity);
//    }
//
//
////    private static final Comparator<PREDICATE<Deriver>> increasingCost =
////        (a,b) -> PREDICATE.CostIncreasing.compare(unmemoized(a), unmemoized(b));
////
////    private static PREDICATE<Deriver> unmemoized(PREDICATE<Deriver> x) {
////        Term xu = x.unneg();
////        return xu instanceof MemoizedPredicate1 xu1 ? xu1.test : x;
////    }
//
//}
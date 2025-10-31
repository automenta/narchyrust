//package nars.derive.util.memoize;
//
//import jcog.data.bit.MetalDibitSet;
//import nars.Deriver;
//import nars.term.control.PREDICATE;
//
//import static jcog.data.bit.MetalDibitSet.itIs;
//import static jcog.data.bit.MetalDibitSet.known;
//
//final class MemoizedPredicate1 extends PREDICATE<Deriver> {
//
//    public final int slot;
//    public final PREDICATE<Deriver> test;
//
//    MemoizedPredicate1(PREDICATE<Deriver> test, int slot) {
//        super(test.ref);
//        this.test = test;
//        this.slot = slot;
//    }
//
//    @Override
//    public boolean test(Deriver d) {
//        MetalDibitSet memo = d.predMemo;
//        int k = memo.isKnown(slot);
//        return known(k) ? itIs(k) : test(d, memo);
//    }
//
//    private boolean test(Deriver d, MetalDibitSet memo) {
//        return memo.set(slot, true, test.test(d));
//    }
//
//    @Override public float cost() {
//        return test.cost();
//    }
//
//}
//package nars.derive.util.memoize;
//
//import jcog.data.bit.MetalBitSet;
//import jcog.data.list.Lst;
//import jcog.data.map.UnifriedMap;
//import nars.Term;
//import nars.Deriver;
//import nars.term.ProxyCompound;
//import nars.term.control.AND;
//import nars.term.control.FORK;
//import nars.term.control.NegPREDICATE;
//import nars.term.control.PREDICATE;
//import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Map;
//
//import static nars.term.control.PREDICATE.CostIncreasing;
//
///** TODO merge memoization reads within an AND for potential fast fail before doing any tests */
//public enum MemoizedPredicate { ;
//
//
//    public static PREDICATE<Deriver> compileMemoize(PREDICATE<Deriver> t, boolean merge) {
//        Map<Term, PREDICATE<Deriver>> c = cachedConds(t);
//        PREDICATE<Deriver> c1 = memoize(t, c);
//        return c.isEmpty() || !merge ? t : merge(c1);
//    }
//
//    private static PREDICATE<Deriver> memoize(PREDICATE<Deriver> t, Map<Term, PREDICATE<Deriver>> cached) {
//        return t.transformPredicate(_x -> {
//            boolean neg = _x instanceof NegPREDICATE n;
//            Term x = neg ? ((NegPREDICATE) _x).p : _x;
//            PREDICATE<Deriver> y = cached.get(((ProxyCompound) x).ref);
//            return y != null ?
//                ((PREDICATE<Deriver>) y.negIf(neg)) :
//                _x;
//        });
//    }
//
//    private static Map<Term, PREDICATE<Deriver>> cachedConds(PREDICATE<Deriver> t) {
//        ObjectIntHashMap<PREDICATE<Deriver>> condCount = new ObjectIntHashMap<>(256);
//        t.recurseTermsOrdered(x -> true, _x -> {
//            if (_x instanceof PREDICATE && !(_x instanceof FORK) && !(_x instanceof AND)) {
//                PREDICATE<Deriver> x = (PREDICATE<Deriver>) (_x.unneg());
//
//                //NegPredicate's  contents will be unwrapped in the recurse. dont double count them
//                //if (!(x instanceof NegPredicate)) {
//                    if (Float.isFinite(x.cost()))
//                        condCount.addToValue(x, 1);
//                //}
//            }
//            return true;
//        }, null);
//        condCount.values().removeIf(x -> x < 2);
//
//        int n = condCount.size();
//        if (n == 0) return Map.of();
//
//        ObjectIntHashMap<PREDICATE<Deriver>> condid = new ObjectIntHashMap<>(n);
//        int ci = 0;
//        for (PREDICATE<Deriver> k : condCount.keySet())
//            condid.put(k, ci++);
//
//        //TODO co-occurrence graph to find combineable conditions
//
//        Map<Term, PREDICATE<Deriver>> cached = new UnifriedMap<>(condCount.size());
//        condid.forEachKeyValue((c,id) -> cached.put(c, new MemoizedPredicate1(c, id)));
//        return cached;
//    }
//
//    private static PREDICATE<Deriver> merge(PREDICATE<Deriver> t) {
//        return t.transformPredicate(x -> {
//            if (!(x instanceof AND<Deriver> a)) return x;
//
//            int as = a.subs();
//
//            MetalBitSet b = bits(a, as);
//            if (b == null || b.cardinality() <= 1) return x;
//
//            Lst<PREDICATE<Deriver>>
//                y = new Lst<>(as - b.cardinality()),
//                m = new Lst<>(b.cardinality());
//
//            int c = 0;
//            for (PREDICATE<Deriver> aa : a.conditions()) {
//                if (b.test(c++))
//                    m.add(aa);
//                else
//                    y.add(aa);
//            }
//            MemoizedPredicateN mp = MemoizedPredicateN.the(m);
//
//            //y.addFirst(mp); //add at beginning so that order is maintained
//
//            y.add(mp);
//            y.sort(CostIncreasing);
//
//            return AND.the(y);
//        });
//    }
//
//    private static @Nullable MetalBitSet bits(AND<Deriver> a, int as) {
//        MetalBitSet b = null;
//        int c = 0;
//        for (PREDICATE<Deriver> aa : a.conditions()) {
//            if (aa.unneg() instanceof MemoizedPredicate1) {
//                if (b == null) b = MetalBitSet.bits(as);
//                b.set(c);
//            }
//            c++;
//        }
//        return b;
//    }
//
//
//}
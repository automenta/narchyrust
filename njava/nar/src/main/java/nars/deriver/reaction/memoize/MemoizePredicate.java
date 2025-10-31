package nars.deriver.reaction.memoize;

import jcog.data.bit.MetalBitSet;
import jcog.data.map.ObjIntHashMap;
import nars.$;
import nars.Deriver;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AND;
import nars.term.control.NOT;
import nars.term.control.PREDICATE;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a memoized predicate for Deriver instances.
 */
public final class MemoizePredicate extends PREDICATE<Deriver> {

    private final PREDICATE<Deriver> predicate;
    private final int memoId; // Unique ID for indexing BitSet


    private static final Atom MEMOIZE = Atomic.atom("Memoize");

    /**
     * Constructs a Memoize instance.
     *
     * @param predicate The predicate to memoize.
     * @param memoId    Unique ID for indexing in the BitSet.
     */
    public MemoizePredicate(PREDICATE<Deriver> predicate, int memoId) {
        super($.func(MEMOIZE, predicate.term()));
        this.predicate = predicate;
        this.memoId = memoId;
    }

    public static PREDICATE<Deriver> memoize(PREDICATE<Deriver> p) {
        ObjIntHashMap<PREDICATE<Deriver>> o = new ObjIntHashMap<>();
        p.conditionsRecursive(c -> o.addToValue(c.unneg(), 1));
        var _m = o.keyValuesView().reject(z -> z.getTwo() <= 1 || !z.getOne().deterministic()).toList();
        if (_m.isEmpty())
            return p;

        Map<PREDICATE<Deriver>, MemoizePredicate> mm = new HashMap<>();
        int id = 0;
        for (var z : _m)
            mm.put(z.getOne(), new MemoizePredicate(z.getOne(), id++));

        return AND.the(new MemoizeClear(id), p.transform(z -> {
            if (z instanceof NOT) {
                var y = mm.get(z.unneg());
                if (y != null)
                    return y.neg();
            } else {
                var y = mm.get(z);
                if (y != null)
                    return y;
            }
            return z;
        }));
    }


    /**
     * Tests the predicate with memoization.
     *
     * @param d The Deriver instance to test.
     * @return The result of the predicate.
     */
    @Override
    public boolean test(Deriver d) {
        var b = d.bits;
        int bitIndex = memoId * 2;
        return b.test(bitIndex + 1) ?
                b.test(bitIndex) // HIT: Return cached result
                :
                miss(bitIndex, b, d); // MISS: Evaluate and cache
    }

    private boolean miss(int bitIndex, MetalBitSet b, Deriver d) {
        boolean y = predicate.test(d);
        if (y)
            b.set(bitIndex);  // Store the memoized result
        b.set(bitIndex + 1);
        return y;
    }


    /**
     * Returns the cost of evaluating the predicate.
     *
     * @return The cost as a float.
     */
    @Override
    public float cost() {
        return predicate.cost();
    }

//        /**
//         * Implements the memoization logic using MethodHandles.
//         *
//         * @return A MethodHandle representing the memoized predicate.
//         */
//        @Override
//        protected MethodHandle _mh() {
//            // Calculate the bit index based on memoId
//            final int bitIndex = memoId * 2;
//
//            // 1. Create a MethodHandle to retrieve the bits from Deriver
//            MethodHandle getBits = GET_BITS_HANDLE; // Deriver -> MetalBitSet
//
//            // 2. Create a MethodHandle to test (bitIndex + 1)
//            MethodHandle testPlusOne = MethodHandles.insertArguments(TEST_HANDLE, 1, bitIndex + 1); // MetalBitSet, int -> boolean
//
//            // 3. Create a MethodHandle that takes Deriver, gets bits, and tests (bitIndex +1)
//            MethodHandle condition = MethodHandles.foldArguments(testPlusOne, getBits); // Deriver -> boolean
//
//            // 4. Create a MethodHandle to test (bitIndex)
//            MethodHandle test = MethodHandles.insertArguments(TEST_HANDLE, 1, bitIndex); // MetalBitSet, int -> boolean
//
//            // 5. Create a MethodHandle that takes Deriver, gets bits, and tests (bitIndex)
//            MethodHandle hit = MethodHandles.foldArguments(test, getBits); // Deriver -> boolean
//
//            // 6. Create a bound MethodHandle for the 'miss' method with bitIndex and predicate's MethodHandle
//            MethodHandle missBound = MethodHandles.insertArguments(MISS_HANDLE, 0, bitIndex, predicate.mh()); // (MetalBitSet, Deriver) -> boolean
//
//            // 7. Create a MethodHandle that takes Deriver, gets bits, and calls 'miss'
//            MethodHandle miss = MethodHandles.foldArguments(missBound, getBits); // Deriver -> boolean
//
//            // 8. Use guardWithTest to branch between 'hit' and 'miss'
//            return MethodHandles.guardWithTest(condition, hit, miss); // Deriver -> boolean
//        }
//
//        /**
//         * Helper method invoked when a memoization miss occurs.
//         *
//         * @param bitIndex     The starting bit index for memoization.
//         * @param bits         The MetalBitSet instance from Deriver.
//         * @param d            The Deriver instance being tested.
//         * @param predicateMh  The MethodHandle of the underlying predicate.
//         * @return The result of the predicate evaluation.
//         * @throws Throwable If the predicate evaluation throws an exception.
//         */
//        private static boolean miss(int bitIndex, MetalBitSet bits, Deriver d, MethodHandle predicateMh) {
//            boolean result;
//            try {
//                result = (boolean) predicateMh.invoke(d);
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//
//            if (result)
//                bits.set(bitIndex); // Set bitIndex to indicate true
//            bits.set(bitIndex + 1); // Set (bitIndex +1) to indicate a cache hit
//            return result;
//        }
//
//        // Cached MethodHandles for performance
//        private static final MethodHandle GET_BITS_HANDLE;
//        private static final MethodHandle TEST_HANDLE;
//        private static final MethodHandle SET_HANDLE;
//        private static final MethodHandle MISS_HANDLE;
//
//        static {
//            try {
//                MethodHandles.Lookup lookup = MethodHandles.lookup();
//                // Assuming 'bits' is a public field. If not, use a getter method.
//                GET_BITS_HANDLE = lookup.findGetter(Deriver.class, "bits", MetalBitSet.class);
//                TEST_HANDLE = lookup.findVirtual(MetalBitSet.class, "test", MethodType.methodType(boolean.class, int.class));
//                SET_HANDLE = lookup.findVirtual(MetalBitSet.class, "set", MethodType.methodType(MetalBitSet.class, int.class));
//                // Define the static 'miss' method
//                MISS_HANDLE = lookup.findStatic(Memoize.class, "miss",
//                        MethodType.methodType(boolean.class, int.class, MetalBitSet.class, Deriver.class, MethodHandle.class));
//            } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
//                throw new ExceptionInInitializerError("Failed to initialize MethodHandles in Memoize class: " + e);
//            }
//        }
//
}

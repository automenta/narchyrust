package nars.truth;

import jcog.Is;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.MetalLongSet;
import jcog.math.Intervals;
import jcog.random.SplitMix64Random;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.NALTask;
import nars.TruthFunctions;
import nars.task.util.StampOverlapping;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;

import java.util.Arrays;
import java.util.function.IntFunction;


/**
 * "Stamp" (evidence chain)
 * -- long[]
 * -- cyclic (overlap) detection
 * -- merge (in 2-ary derivations)
 */
public interface Stamp {

    static boolean overlap(NALTask x, NALTask y) {
        if (x.equals(y)) return true;
        try (var s = new StampOverlapping()) {
            return s.set(x).test(y);
        }
    }

    static long[] zip(long[] a, long[] b, Object A, Object B) {
        return zip(a, b, Util.hashCombine(A, B));
    }

    static long[] zip(long[] a, long[] b, Object sampleHash) {
        return zip(a, b, sampleHash, NAL.STAMP_CAPACITY);
    }

    /**
     * Applies a fair, random-removal merge of input stamps.
     *
     * @param a          The first stamp array.
     * @param b          The second stamp array.
     * @param sampleHash Hash to use for sampling
     * @param capacity   Maximum capacity of the resulting stamp array.
     * @return A merged stamp array.
     */
    static long[] zip(long[] a, long[] b, Object sampleHash, int capacity) {
        int aa = a.length, bb = b.length;

        if (aa == 0) {
            if (bb > capacity) throw new TODO();
            return b;
        }
        if (bb == 0 || (aa == bb && ArrayUtil.equals(a, b))) {
            if (aa > capacity) throw new TODO();
            return a;
        }


        var o = overlapCount(a, b);
        if (o > 0 && aa == o) return b;
        if (o > 0 && bb == o) return a;

        var abLen = aa + bb - o;
        if (abLen <= capacity) {
            return o > 0 ? zipFlat(a, b, abLen) : zipDirect(a, b);
        }
        return zipSample(capacity, i -> i == 0 ? a : b, sampleHash, 2, abLen);
    }

    /**
     * Merges two sorted arrays by avoiding duplicate insertion in O(N) time.
     * The arrays 'a' and 'b' are assumed to be already sorted.
     *
     * @param a The first sorted array.
     * @param b The second sorted array.
     * @param abLen The length of the merged output.
     * @return A merged and sorted array containing all unique elements from a and b.
     */
    private static long[] zipFlat(long[] a, long[] b, int abLen) {
        long[] merged = new long[abLen];
        int i = 0, j = 0, k = 0;

        while (i < a.length && j < b.length) {
            if(a[i] < b[j]) {
                if(k == 0 || merged[k - 1] != a[i]){
                    merged[k++] = a[i];
                }
                i++;
            } else if (b[j] < a[i]) {
                if(k == 0 || merged[k - 1] != b[j]) {
                    merged[k++] = b[j];
                }
                j++;
            } else {
                if (k == 0 || merged[k - 1] != a[i]) {
                    merged[k++] = a[i];
                }
                i++;
                j++;
            }
        }
        // add remaining elements
        while(i < a.length) {
            if(k == 0 || merged[k - 1] != a[i]){
                merged[k++] = a[i];
            }
            i++;
        }
        while (j < b.length){
            if(k == 0 || merged[k - 1] != b[j]) {
                merged[k++] = b[j];
            }
            j++;
        }
        return merged;
    }


    /**
     * Simple case: Direct sequential merge with no contention.
     * The arrays 'a' and 'b' are assumed to be already sorted.
     */
    private static long[] zipDirect(long[] a, long[] b) {
        int aa = a.length, bb = b.length, abLength = aa + bb;
        var ab = new long[abLength];
        int ia = 0, ib = 0;
        for (var i = 0; i < abLength; i++) {
            var an = ia < aa ? a[ia] : Long.MAX_VALUE;
            var bn = ib < bb ? b[ib] : Long.MAX_VALUE;
            long abn;
            if (an < bn) {
                abn = an;
                ia++;
            } else {
                abn = bn;
                ib++;
            }
            ab[i] = abn;
        }
        return ab;
    }

    static boolean validStamp(long[] stamp) {
        var n = stamp.length;
        if (n > 1) {
            if (n > NAL.STAMP_CAPACITY)
                return false;
            for (var i = 1; i < n; i++)
                if (stamp[i - 1] >= stamp[i])
                    return false; //out of order or duplicate

        }
        return true;
    }

    static long[] toSetArray(long[] x) {
        return toSetArray(x, x.length);
    }

    static long[] toSetArray(long[] x, int outputLen) {
        var l = x.length;
        return (l < 2) ? x : _toSetArray(outputLen, Arrays.copyOf(x, l));
    }

    static long[] _toSetArray(int outputLen, long[] sorted) {
        Arrays.sort(sorted);

        long lastValue = -1;
        var uniques = 0;

        for (var v : sorted) {
            if (lastValue != v)
                uniques++;
            lastValue = v;
        }

        if ((uniques == outputLen) && (sorted.length == outputLen))
            return sorted;

        var outSize = Math.min(uniques, outputLen);
        var dedupAndTrimmed = new long[outSize];
        var uniques2 = 0;
        long lastValue2 = -1;
        for (var v : sorted) {
            if (lastValue2 != v)
                dedupAndTrimmed[uniques2++] = v;
            if (uniques2 == outSize)
                break;
            lastValue2 = v;
        }
        return dedupAndTrimmed;
    }


    static boolean overlapsAny(Stamp a, Stamp b) {
        return a == b || overlapsAny(a.stamp(), b.stamp());
    }

    /**
     * True if there are any common elements.
     * Assumes the arrays are sorted and contain no duplicates.
     *
     * @param a Evidence stamp in sorted order.
     * @param b Evidence stamp in sorted order.
     */
    static boolean overlapsAny(long[] a, long[] b) {
        if (a == b)
            return true;
        int an = a.length, bn = b.length;
        if (an == 0 || bn == 0)
            return false;
        else if (an == 1 && bn == 1)
            return a[0] == b[0];
        else
            return Intervals.intersectsRaw(a, b) && overlapExhaustive(a, b);
    }

    /**
     * A more efficient way to compute the overlap by short-circuiting the check.
     * @param a Evidence stamp in sorted order.
     * @param b Evidence stamp in sorted order.
     * @return true of there are any overlaps.
     */
    private static boolean overlapExhaustive(long[] a, long[] b) {
        int an = a.length, bn = b.length;
        int i = 0, j = 0;
        while (i < an && j < bn) {
            long ab = a[i] - b[j];
            if (ab == 0)
                return true; //found overlap
            else if (ab < 0)
                i++;
            else
                j++;
        }
        return false; // no overlap found
    }

    /*
        @return
             Integer.MIN_VALUE no match
             0 a == b
            +1 a contains b
            -1 b contains a
     */
    static int containment(long[] a, long[] b) {
        if (a == b) return 0;
        var matches = 0;
        int an = a.length, bn = b.length;
        int i = 0, j = 0;

        if(an == 0 && bn == 0)
            return 0;

        while (i < an && j < bn) {
            if (a[i] == b[j]) {
                matches++;
                i++;
                j++;

            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }

            if (matches == an && matches == bn) {
                return 0;
            } else if (matches == an && bn > matches) {
                return -1;
            } else if (matches == bn && an > matches) {
                return +1;
            }

            if (i >= an || j >= bn) {
                break;
            }
        }

        return Integer.MIN_VALUE; //no overlap
    }

    static long[] zip(int capacity, IntFunction<long[]> t, Object sampleHash, int n) {
        return switch (n) {
            case 0 -> throw new NullPointerException();
            case 1 -> t.apply(0);
            case 2 -> zip(t.apply(0), t.apply(1), sampleHash, capacity);
            default -> zipN(capacity, t, sampleHash, n);
        };
    }

    /**
     * Max possible size estimate for the long hashset because it can be expensive
     */
    private static long[] zipN(int capacity, IntFunction<long[]> t, Object sampleHash, int n) {
        var m = 0;
        for (var i = 0; i < n; i++) m += t.apply(i).length;
        return m > capacity ?
                zipSample(capacity, t, sampleHash, n, m) :
                maybeEqual(zipFlat(t, n, m), t, n);
    }

    /**
     * Try to find an existing equivalent stamp component for the output, and re-use it.
     */
    private static long[] maybeEqual(long[] next, IntFunction<long[]> prevs, int n) {
        for (var i = 0; i < n; i++) {
            var prev = prevs.apply(i);
            if (ArrayUtil.equals(next, prev))
                return prev;
        }
        return next;
    }

    /**
     * Flattens the arrays provided by the given function into one sorted array
     */
    private static long[] zipFlat(IntFunction<long[]> t, int n, int len) {
        var l = new DisposeableLongArrayList(len);
        for (var i = 0; i < n; i++) {
            for (var s : t.apply(i)) {
                var a = l.binarySearch(s);
                if (a < 0)
                    l.addAtIndex(-a - 1, s);
            }
        }
        return l.toArray(); //already sorted
    }


    private static long[] zipSample(int capacity, IntFunction<long[]> t, Object sampleHash, int n, int maxPossibleSize) {
        var ee = eviArray(t, n, maxPossibleSize);
        return ee.length <= capacity ?
                maybeEqual(ee, t, n) :
                zipSample(capacity, hashSeed(sampleHash), n, ee);
    }

    private static int hashSeed(Object sampleHash) {
        return sampleHash instanceof Integer i ? i : sampleHash.hashCode();
        //return NAL.STAMP_HASH_SEED ? sampleHash.getAsInt() : ThreadLocalRandom.current().nextInt();
    }

    private static long[] eviArray(IntFunction<long[]> t, int n, int maxPossibleSize) {
        var evi = new MetalLongSet(maxPossibleSize);
        for (var i = 0; i < n; i++)
            evi.addAll(t.apply(i));
        return evi.toSortedArray();
    }

    @Is({"Block_chain", "Cryptocurrency"})
    private static long[] zipSample(int capacity, int sampleHash, int n, long[] evi) {
        int nab = evi.length;
        var rng = new SplitMix64Random(sampleHash);

        //TODO special case: toRemove < nab/2, invert selection
        var skip = MetalBitSet.bits(nab);
        var toRemove = nab - capacity;
        assert (toRemove > 0);

        int removed = 0;
        while (removed < toRemove){
            int r = rng.nextInt(nab);
            if(!skip.test(r)) {
                skip.set(r);
                removed++;
            }
        }
        return zipSampleCollect(evi, skip, capacity);
    }

    private static long[] zipSampleCollect(long[] x, MetalBitSet skip, int capacity) {
        var y = new long[capacity];
        for (int j = 0, k = 0, length = x.length; j < length; j++) {
            if (!skip.test(j)) {
                y[k++] = x[j];
                if (k >= capacity) break;
            }
        }
        //assert(k==capacity);
        if (y.length > 1)
            Arrays.sort(y);
        return y;
    }


    /**
     * The fraction of components in common divided by the total amount of unique components.
     * <p>
     * How much two stamps overlap can be used to estimate
     * the potential for information gain vs. redundancy.
     * <p>
     * == 0 if nothing in common, completely independent
     * >0 if there is at least one common component;
     * 1.0 if they are equal, or if one is entirely contained within the other
     * < 1.0 if they have some component in common
     * <p>
     * Assumes the arrays are sorted and contain no duplicates
     */
    static float overlapFraction(long[] a, long[] b) {
        int an = a.length, bn = b.length;

        if (an == 1 && bn == 1)
            return (a[0] == b[0]) ? 1 : 0;

        var common = overlapCount(a, b);
        if (common == 0)
            return 0;

        var denom = Math.min(an, bn); //assert (denom != 0);
        return Util.unitizeSafe(((float) common) / denom);
    }

    /**
     * Assumes 'a' and 'b' are sorted
     */
    static int overlapCount(long[] a, long[] b) {
        int an = a.length, bn = b.length;
        int i = 0, j = 0;
        var count = 0;
        while (i < an && j < bn) {
            if (a[i] == b[j]) {
                count++;
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }
        return count;
    }

    public static long[] zip(NALTask a, NALTask b) {
        return zip(a.stamp(), b.stamp(), a, b);
    }

    /**
     * Deduplicated and sorted (increasing) version of the evidentialBase.
     * This can always be calculated deterministically from the evidentialBAse
     * since it is the deduplicated and sorted form of it.
     */
    long[] stamp();

    /**
     * Originality monotonically decreases with evidence length increase.
     * It must always be < 1 (never equal to one) due to its use in the or(conf, originality) ranking
     */
    default float originality() {
        return TruthFunctions.originality(stampLength());
    }

    default int stampLength() {
        return stamp().length;
    }

    final class DisposeableLongArrayList extends LongArrayList {
        DisposeableLongArrayList(int len) {
            super(len);
        }

        @Override
        public long[] toArray() {
            return size == items.length ?
                    items :
                    super.toArray();
        }
    }

}
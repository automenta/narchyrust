package nars.util;

import jcog.WTF;
import jcog.data.list.MetalConcurrentQueue;
import jcog.data.list.MetalRing;
import jcog.math.LongInterval;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.Intervals.intersectsRaw;
import static jcog.math.LongInterval.TIMELESS;
import static nars.Op.ETERNAL;

public class RingIntervalSeries<X extends LongInterval> extends IntervalSeries<X> {

    public final MetalRing<X> q;

    public RingIntervalSeries(MetalRing<X> q) {
        super(q.length());
        this.q = q;
    }

    public RingIntervalSeries(int capacity) {
        this(new MetalConcurrentQueue<>(capacity));
    }

    @Override
    public final boolean remove(X x) {
        return q.remove(x);
    }

    public final X peek(int x) {
        return q.getHead(x);
    }

    @Override
    public final boolean isEmpty() {
        return q.isEmpty();
    }

    @Override
    public final void push(X x) {
        if (!q.offer(x))
            throw new WTF(); //TEMPORAR
    }

    @Override
    public final @Nullable X poll() {
        return q.poll();
    }

    public int indexNear(int head, long when) {
        int n = q.size();
        int cap = q.length();
        int low = 0, high = n - 1, mid = -1;
        while (low <= high) {
            if (low==high) return low;

            mid = (low + high) / 2;

            var midVal = q.getUnsafe(head, mid, cap);
            if (midVal == null)
                return -1;

            long a = midVal.start(), b = midVal.end();
            if (when >= a && when <= b)
                break; //intersection found

            if (when > b)
                low = mid + 1; //assert(when>a);
            else
                high = mid - 1;
        }
        return mid;
    }


//    /**
//     * Find the index of the interval that either contains {@code when},
//     * or if none contains it, then return the index of the closest interval.
//     *
//     * If the ring is empty, returns -1.
//     */
//    public int indexNear(int head, long when) {
//        int n = size();
//        return n == 0 ? -1 : binarySearchClosest(head, when, 0, n - 1);
//
//    }

//    /**
//     * A binary-search–like approach to find the interval that best matches {@code when}.
//     * - If an interval contains {@code when}, return its index.
//     * - Otherwise, return whichever of the bounding intervals is closer to {@code when}.
//     */
//    private int binarySearchClosest(int head, long when, int low, int high) {
//        int cap = q.length();
//
//        int closestIndex = -1;
//        long closestDiff = Long.MAX_VALUE;
//
//        while (low <= high) {
//            int mid = (low + high) >>> 1;
//            X midVal = q.getUnsafe(head, mid, cap);
//            if (midVal == null) {
//                // This *shouldn't* normally happen if size() is correct,
//                // but handle gracefully anyway.
//                break;
//            }
//            long start = midVal.start();
//            long end   = midVal.end();
//
//            // 1) If 'when' lies in [start, end], we have an exact hit.
//            if (start <= when && when <= end) {
//                return mid;
//            }
//
//            // 2) Update "closest" so far
//            long diff = midVal.diff(when);
//            if (diff < closestDiff) {
//                closestDiff = diff;
//                closestIndex = mid;
//            }
//
//            // 3) Standard binary-search branching
//            if (when < start) {
//                high = mid - 1;
//            } else {
//                // when > end
//                low = mid + 1;
//            }
//        }
//
//        // If we exit the loop without finding an exact container,
//        // we return 'closestIndex'. That might be -1 if the ring was full of nulls,
//        // or if some data anomaly happened. In normal conditions, it won't be -1.
//        return closestIndex;
//    }

    /**
     * TODO obey exactRange flag
     */
    @Override
    public boolean whileEach(long s, long e, boolean intersectRequired, Predicate<? super X> each) {
        return whileEach(s, e,
                s /* linear */
                //Fuzzy.mean(s,e) /* radial */
            , intersectRequired, each);
    }

    /** @param w start point, s <= w <= e.  if w==s, conducts linear search: s..e*/
    public boolean whileEach(long s, long e, long w, @Deprecated boolean intersectRequired, Predicate<? super X> each) {
        assert (s != ETERNAL && s != TIMELESS);

        long seriesStart = start();
        if (seriesStart == TIMELESS) return true; //nothing
        long seriesEnd = end();
        if (seriesEnd == TIMELESS) seriesEnd = seriesStart; //HACK

        if (intersectRequired && !intersectsRaw(seriesStart, seriesEnd, s, e))
            return true; //nothing

        int head = q.head();
        int size = q.size();//Math.min(q.length(), this.size());

        boolean linear = s == w;
        return linear ?
            whileEachLinear(s, e, intersectRequired, each, head, size) :
            whileEachRadial(s, e, w, intersectRequired, each, head, size);
    }

    /**
     * Linear iteration from the index that is "near" s onward.
     */
    private boolean whileEachLinear(long qs, long qe,
                                    @Deprecated boolean intersectRequired,
                                    Predicate<? super X> whle,
                                    int head,
                                    int size)
    {
        int p = indexNear(head, qs);
        if (p < 0)
            return true; // No intervals or ring is weirdly empty

        int qCap = q.length();
        long xs = Long.MIN_VALUE;
        for (int i = 0; i < size; i++) {
            var x = q.getUnsafe(head, p++, qCap);
            if (x == null)
                break;

            long xsNext = x.start();
            if (xsNext < xs)
                break; //wrap-around
            xs = xsNext;

            var xe = x.end();
            if (xe < qs)
                continue; //before beginning
            if (xs > qe)
                break; //beyond the end
            if (!intersectRequired || intersectsRaw(xs, xe, qs, qe)) {
                if (!whle.test(x))
                    return false;
            }
        }
        return true;
    }

    /**
     * UNTESTED
     * "Radial" search: expand outward from the interval near {@code queryStart},
     * checking intervals in an increasing and decreasing direction,
     * ensuring we only look up to {@code sz} intervals total.
     */
    private boolean whileEachRadial(long s, long e, long queryStart,
                                    boolean intersectRequired,
                                    Predicate<? super X> whle,
                                    int head,
                                    int sz)
    {
        int center = indexNear(head, queryStart);
        if (center < 0)
            return true; // If we can't find anything, just done

        // We'll expand out from center by r steps.
        int r = 0;
        boolean increase = true;   // move +r
        boolean decrease = true;   // move -r
        int remain = sz;

        var qCap = q.length();

        while ((increase || decrease) && remain > 0) {
            // index +r
            X v = null;
            if (increase) {
                v = q.getUnsafe(head, center + r, qCap);
                if (v == null || (intersectRequired && !v.intersectsRaw(s, e))) {
                    increase = false;
                    v = null;
                }
            }
            // index -r
            X u = null;
            if (decrease) {
                u = q.getUnsafe(head, center - r, qCap);
                if (u == null || (intersectRequired && !u.intersectsRaw(s, e))) {
                    decrease = false;
                    u = null;
                }
            }
            // We advanced r only once after both gets
            r++;

            // Decide which to yield first. Typically you’d yield
            // whichever is “closer” to queryStart. If that’s not
            // important, you can yield in +/- order.
            if (u != null && v != null) {
                long distU = u.diff(queryStart);
                long distV = v.diff(queryStart);
                if (distV < distU) {
                    // v is closer; yield it first
                    if (!whle.test(v)) return false;
                    remain--;
                    // then yield u
                    if (!whle.test(u)) return false;
                    remain--;
                } else {
                    // u is closer or tie
                    if (!whle.test(u)) return false;
                    remain--;
                    if (!whle.test(v)) return false;
                    remain--;
                }
            }
            else {
                // If only one is non-null
                if (v != null) {
                    if (!whle.test(v)) return false;
                    remain--;
                }
                if (u != null) {
                    if (!whle.test(u)) return false;
                    remain--;
                }
            }
        }

        return true;
    }



    @Override
    public int size() {
        return q.size();
    }

    @Override
    public X first() {
        return q.first();
    }

    @Override
    public final X last() {
        return q.last();
    }

    @Override
    public void clear() {
        q.clear();
    }

    @Override
    public Stream<X> stream() {
        return q.stream();
    }

    @Override
    public void forEach(Consumer<? super X> each) {
        q.forEach(each);
    }

    public final void whileEachLinear(long s, long e, Predicate<X> m) {
        whileEach(s, e, s, true, m);
    }
}
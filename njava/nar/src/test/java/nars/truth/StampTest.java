package nars.truth;

import jcog.util.ArrayUtil;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.truth.Stamp.toSetArray;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author me
 */


class StampTest {

    private static long[] a(long... x) {
        return x;
    }

    @Test
    void testOverlap() {


        assertTrue(Stamp.overlapsAny(a(1, 2), a(2)));
        assertTrue(Stamp.overlapsAny(a(1), a(1, 2)));
        assertFalse(Stamp.overlapsAny(a(1), a(2)));
        assertFalse(Stamp.overlapsAny(a(2), a(1)));
        assertFalse(Stamp.overlapsAny(a(1, 2), a(3, 4)));
        assertTrue(Stamp.overlapsAny(a(1, 2), a(2, 3)));
        assertTrue(Stamp.overlapsAny(a(2, 3), a(1, 2)));
        assertFalse(Stamp.overlapsAny(a(2, 3), a(1)));

        assertFalse(Stamp.overlapsAny(a(1), a(2, 3, 4, 5, 6)));
        assertFalse(Stamp.overlapsAny(a(2, 3, 4, 5, 6), a(1)));


    }

    @Test
    void testStampZipForward() {
        assertEquals(
                Arrays.toString(new long[] { 7, 8, 12, 13 }),
                Arrays.toString(zipForward(
                        new long[] { 1, 2, 8, 12},
                        new long[] { 3, 4, 7, 13}, 4))
        );
    }

    @Disabled
    @Test
    void testStampZipReverse() {

        long[] a = {1, 2};
        long[] b = {3, 4};
        int i = 3;
        long[] zip = zipReverse(a, b, i);
        assertArrayEquals(
            new long[] { 1, 2, 3 },
                zip
        );



        assertArrayEquals(
            new long[] { 1, 2, 3, 4 },
                zipReverse(new long[] { 1 }, new long[] { 2, 3, 4}, 4)
        );
        assertArrayEquals(
            new long[] { 1, 2, 3, 4 },
                zipReverse(new long[] { 1,2,3 }, new long[] { 4 }, 4)
        );
        assertArrayEquals(
            new long[] { 0, 1, 2, 4 },
                zipReverse(new long[] { 0, 1,2,3 }, new long[] { 4 }, 4)
        );

        
        assertArrayEquals(
            new long[] { 0, 1, 2, 3 },
                zipReverse(new long[] { 0, 1,2 }, new long[] { 2, 3, 4 }, 4)
        );
    }

    @Test
    void directionInvariance() {
        
        boolean[] both = { false, true };
        for (boolean dir : both) {
            assertArrayEquals(
                    new long[]{1, 2, 3, 4},
                    zip(
                            new long[]{1, 2},
                            new long[]{3, 4}, 0.5f, 4, dir)
            );
        }
    }

    private static long[] zipReverse(long[] a, long[] b, int i) {
        return zip(a, b, 0.5f, i, false);
    }
    private static long[] zipForward(long[] a, long[] b, int i) {
        return zip(a, b, 0.5f, i, true);
    }
    private static long[] zipForward(long[] a, long[] b, float aToB, int i) {
        return zip(a, b, aToB, i, true);
    }

    @Test
    void testStampToSetArray() {
        assertEquals(3, toSetArray(new long[]{1, 2, 3}).length);
        assertEquals(2, toSetArray(new long[]{1, 1, 3}).length);
        assertEquals(1, toSetArray(new long[]{1}).length);
        assertEquals(0, toSetArray(new long[]{}).length);
        assertEquals(Arrays.hashCode(toSetArray(new long[]{3, 2, 1})), Arrays.hashCode(toSetArray(new long[]{2, 3, 1})));
        assertTrue(
                Arrays.hashCode(toSetArray(new long[] { 1,2,3 }))
                !=
                Arrays.hashCode(toSetArray(new long[] { 1,1,3 }))
        );
    }

    @Disabled
    @Test
    void testStampReversePreservesOldestEvidence() {
        assertArrayEquals(
                new long[] { 1, 3 },
                zipReverse(new long[] { 1, 2}, new long[] { 3, 4}, 2)
        );

        assertArrayEquals(
                new long[] { 1, 2, 3, 4 },
                zipReverse(new long[] { 1, 2, 8, 12}, new long[] { 3, 4, 7, 13}, 4)
        );


        long[] a = { 1, 2, 10, 11 };
        long[] b = { 3, 5, 7, 22 };
        assertEquals(
                new LongArrayList(1, 2, 3, 5),
                new LongArrayList(zipReverse(a, b, 4)));
    }


    @Test
    void testStampZipForwardWeighted() {

        long[] a = {1, 2, 8, 12};
        long[] b = {3, 4, 7, 13};

        assertEquals(
                Arrays.toString(new long[] { 7, 8, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.5f, 4))
        );
        assertEquals(
                Arrays.toString(new long[] { 2, 8, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.8f, 4))
        );
        assertEquals(
                Arrays.toString(new long[] { 2, 8, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.95f, 4)) 
        );
        assertEquals(
                Arrays.toString(new long[] { 4, 7, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.2f, 4))
        );

    }

    @Test
    void testStampZipForwardWeighted2() {

        long[] a = {0, 2, 4, 6, 8, 10, 12};
        long[] b = {1, 3, 5, 7, 9, 11, 13};

        assertEquals(
                Arrays.toString(new long[] { 7, 8, 9, 10, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.5f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 4, 6, 8, 10, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.8f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 2, 4, 6, 8, 10, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.95f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 7, 8, 9, 10, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.35f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 3, 5, 7, 9, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.1f, 7))
        );
    }
    @Test
    void testStampZipForwardWeighted3() {

        
        long[] a = {0, 2};
        long[] b = {1, 3, 5, 7};

        assertEquals(
                Arrays.toString(new long[] { 0, 2, 3, 5, 7 }),
                Arrays.toString(zipForward(a,b, 0.5f, 5))
        );
        assertEquals(
                Arrays.toString(new long[] { 0, 2, 3, 5, 7 }), 
                Arrays.toString(zipForward(a,b, 0.8f, 5))
        );
        assertEquals(
                Arrays.toString(new long[] { 1, 2, 3, 5, 7 }),
                Arrays.toString(zipForward(a,b, 0.2f, 5))
        );

    }

    @Test
    void testOverlapFractionIndependent() {
        assertEquals(0f, Stamp.overlapFraction(a(1), a(3)), 0.01f);
        assertEquals(0f, Stamp.overlapFraction(a(1, 2), a(3)), 0.01f);
        assertEquals(0f, Stamp.overlapFraction(a(1, 2), a(3, 4)), 0.01f);
    }
    @Test
    void testOverlapFraction2() {
        assertEquals(1 / 2f, Stamp.overlapFraction(a(1, 2), a(2, 3)), 0.01f);
        assertEquals(1f, Stamp.overlapFraction(a(1, 2), a(1, 2, 3)), 0.01f);
    }
    @Test
    void testOverlapFraction3() {
        
        
        
        assertEquals(1/3f, Stamp.overlapFraction(a(1,2,3), a(3,4,5)), 0.01f);
        assertEquals(2/3f, Stamp.overlapFraction(a(1,2,3,4), a(3,4,5)), 0.01f);

        
        assertEquals(1f, Stamp.overlapFraction(a(1,2), a(2)), 0.01f);
        assertEquals(1f, Stamp.overlapFraction(a(1,2,3,4), a(2,3,4)), 0.01f);
    }

//    @Test
//    void testDetectOverlapAfterZipOverflow() {
//        ObjectFloatPair<long[]> p = Stamp.zip(List.of(
//                NALTask.taskUnsafe($.the("x"), QUESTION, null, 0, 0, new long[]{1, 2, 8, 9}),
//                NALTask.taskUnsafe($.the("y"), QUESTION, null, 0, 0, new long[]{3, 4, 5, 8})
//        ), 2);
//        assertEquals("[8, 9]", Arrays.toString(p.getOne()));
//        assertEquals(1/8f, p.getTwo(), 0.01f);
//    }


    /**
     * Zips two evidentialBase arrays into a new one, with weighted preference given to the new inputs.
     *
     * @param a        The first stamp array (older).
     * @param b        The second stamp array (newer).
     * @param aToB     The weight for newer stamp components.  Between 0..1
     * @param maxLen   The maximum length of the merged array.
     * @param newToOld Whether or not the merge should prefer the new components over the old.
     * @return A new merged array with the elements from a and b.
     */
    @Deprecated static long[] zip(long[] a, long[] b, float aToB, int maxLen, boolean newToOld) {
        int aLen = a.length, bLen = b.length;
        if (aLen == bLen && ArrayUtil.equals(a, b))
            return a;

        int baseLength = Math.min(aLen + bLen, maxLen);
        long[] c = new long[baseLength];
        if (aLen + bLen > maxLen)
            zipChooseSome(a, b, aToB, maxLen, newToOld, aLen, bLen, c, baseLength);
        else
            zipChoose(a, b, baseLength, aLen, bLen, c);
        return toSetArray(c, maxLen);
    }

    @Deprecated private static void zipChooseSome(long[] a, long[] b, float aToB, int maxLen, boolean newToOld, int aLen, int bLen, long[] c, int baseLength) {
        if (!Float.isFinite(aToB)) aToB = 0.5f;

        if (newToOld) {

            int aMin = 0, bMin = 0;

            if (aToB <= 0.5f) {
                int usedA = Math.max(1, (int) (aToB * (aLen + bLen)));
                if (usedA < aLen) {
                    if (bLen + usedA < maxLen)
                        usedA += maxLen - usedA - bLen;
                    aMin = Math.max(0, aLen - usedA);
                }
            } else {
                int usedB = Math.max(1, (int) ((1f - aToB) * (aLen + bLen)));
                if (usedB < bLen) {
                    if (aLen + usedB < maxLen)
                        usedB += maxLen - usedB - aLen;
                    bMin = Math.max(0, bLen - usedB);
                }
            }
            choose(a, b, aLen, bLen, c, baseLength, aMin, bMin);
        } else {
            // TODO: reverse weighted implementation
            // throw new UnsupportedOperationException("reverse weighted not yet implemented");
            aToB = Math.max(0f, Math.min(1f, aToB)); //Force aToB into a valid range
            int aKept = Math.max(0, (int) (aToB * maxLen));
            int bKept = maxLen - aKept;
            int aMin = Math.max(0, aLen - aKept);
            int bMin = Math.max(0, bLen - bKept);


            int ia = 0, ib = 0, k = 0;
            while (ia < aLen || ib < bLen) {
                boolean useA = ia < aLen && (ia < aMin || (ib >= bMin && ((k & 1) == 0)));
                if (useA) {
                    if (k == 0 || c[k - 1] != a[ia])
                        c[k++] = a[ia];
                    ia++;
                } else if (ib < bLen){
                    if (k == 0 || c[k - 1] != b[ib])
                        c[k++] = b[ib];
                    ib++;
                }
                if (k >= baseLength)
                    break;

            }
        }
    }

    @Deprecated private static void choose(long[] a, long[] b, int aLen, int bLen, long[] c, int baseLength, int aMin, int bMin) {
        int ib = bLen - 1, ia = aLen - 1;

        for (var i = baseLength - 1; i >= 0; ) {
            boolean ha = (ia >= aMin), hb = (ib >= bMin);
            long next;
            if (ha && hb) {
                next = (i & 1) > 0 ? a[ia--] : b[ib--];
            } else if (ha) {
                next = a[ia--];
            } else if (hb) {
                next = b[ib--];
            } else
                throw new RuntimeException("stamp fault");
            c[i--] = next;
        }
    }

    @Deprecated private static void zipChoose(long[] a, long[] b, int baseLength, int aLen, int bLen, long[] c) {
        int ib = 0, ia = 0;
        for (var i = 0; i < baseLength; ) {
            boolean ha = ia < aLen, hb = ib < bLen;
            c[i++] = ((ha && hb) ?
                    ((i & 1) > 0) : ha) ?
                    a[ia++] : b[ib++];
        }
    }


}
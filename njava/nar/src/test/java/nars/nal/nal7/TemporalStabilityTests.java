package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * N independent events
 */
@Disabled
class TemporalStabilityTests {

    static final int CYCLES = 100;

    private final NAR nar = NARS.tmp();


    static class T1 extends TemporalStabilityTest {

        /** accept derived tasks only if they occurr at a time of the input tasks */
        private static final boolean ONLY_WHENS = false;

        private final IntSet whens;
        private final IntToObjectFunction<String> eventer;
        private final int minT;
        private final int maxT;
        static final int tolerance = 0;

        T1(IntToObjectFunction<String> eventer, int... whens) {
            this.whens = new IntHashSet(whens).toImmutable();
            minT = this.whens.min();
            maxT = this.whens.max();
            this.eventer = eventer;
        }

        T1(IntToObjectFunction<String> eventer, int[] whens, int minT, int maxT) {
            this.whens = new IntHashSet(whens).toImmutable();
            this.minT = minT;
            this.maxT = maxT;
            this.eventer = eventer;
        }

        @Override
        public boolean validOccurrence(long _o) {
            int o = (int)_o;

            if (ONLY_WHENS && !whens.contains(o))
                return false;

            return (o >= minT - tolerance) && (o <= maxT + tolerance);
        }

        @Override
        public void input(NAR n) {
            int j = 0;
            for (int i : whens.toSortedArray())
                n.inputAt(i, eventer.valueOf(j++) + ". :|:");
        }

    }


    private static final IntToObjectFunction<String> inheritencer = (j) -> {
        char c = (char) ('a' + j);
        return c + ":" + c + c;
    };
    private static final IntToObjectFunction<String> implicator = (j) -> {
        char c = (char) ('a' + j);
        return '(' + (c + "==>" + (c + String.valueOf(c))) + ')';
    };
    private static final IntToObjectFunction<String> atomizer = (j) -> String.valueOf((char) ('a' + j));
    private static final IntToObjectFunction<String> productor = (j) -> '(' + atomizer.apply(j) + ')';
    private static final IntToObjectFunction<String> biproductor = (j) -> {
        char c = (char) ('a' + j);
        return "(" + c + ',' + (c + String.valueOf(c)) + ')';
    };
    private static final IntToObjectFunction<String> linkedproductor = (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j + 1));
        return "(" + c + ',' + d + ')';
    };
    private static final IntToObjectFunction<String> linkedinh = (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j + 1));
        return "(" + c + "-->" + d + ')';
    };
    private static final IntToObjectFunction<String> linkedimpl = (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j + 1));
        return "(" + c + "==>" + d + ')';
    };
    static final IntToObjectFunction<String> conjSeq2 = (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j + 1));
        return "(" + c + " &&+5 " + d + ')';
    };
    static final IntToObjectFunction<String> conjInvertor = (j) -> {
        char c = (char) ('a' + j);
        return "(" + c + " &&+5 (--," + c + "))";
    };


    @Test
    void testTemporalStabilityInh3() {
        new T1(inheritencer, 1, 2, 5).test(CYCLES, nar);
    }

    @Test
    void testTemporalStabilityImpl() {
        new T1(implicator, 1, 2, 5).test(CYCLES, nar);
    }

    @Test
    void testTemporalStabilityAtoms() {
        new T1(atomizer, 1, 3).test(CYCLES, nar);
    }

    @Disabled
    @Test
    void sanity1() {

        NAR n = NARS.tmp();
        n.inputAt(1, "((--,(a)) &&+4 (--,(c))). | %1;0.81%");
        n.inputAt(2, "(b). | %1;0.90%");
        //n.log();
        n.run(24);
    }

    @Test
    void testTemporalStabilityProd() {
        new T1(productor, 1, 2, 5).test(CYCLES, nar);
    }

    @Test
    void testTemporalStabilityBiProd() {
        new T1(biproductor, 1, 2, 5).test(CYCLES, nar);
    }


    @Test
    void testTemporalStabilityLinkedProd_easy() {
        new T1(linkedproductor, 1, 2).test(500, nar);
    }

    @Test
    void testTemporalStabilityLinkedProd() {
        new T1(linkedproductor, 1, 2, 5).test(CYCLES, nar);
    }

    @Test
    void testTemporalStabilityLinkedInh() {
        new T1(linkedinh, 1, 2, 5).test(CYCLES, nar);
    }

    @Test
    void testTemporalStabilityLinkedImpl() {
        new T1(linkedimpl, 1, 2, 5).test(CYCLES, nar);
    }

    @Test
    void testTemporalStabilityLinkedTemporalConjSmall() {
        new T1(conjSeq2, new int[]{1, 6}, 1, 16).test(100, nar);
    }


    @Test
    void testTemporalStabilityLinkedTemporalConj() {
        new T1(conjSeq2, new int[]{1, 6, 11}, 1, 16).test(CYCLES * 2, nar);
    }

    @Test
    void testTemporalStabilityLinkedImplExt() {
        new T1(linkedimpl, 1, 2, 5).test(CYCLES, nar);
    }

    @Test
    void testTemporalStabilityLinkedImplExt2() {


        NAR n = nar;

        T1 a = new T1(linkedimpl, 1, 2, 5, 10);
        T1 b = new T1(linkedinh, 1, 2, 5, 10);

        a.test(-1, n);
        b.test(-1, n);

        n.run(CYCLES);

    }

}
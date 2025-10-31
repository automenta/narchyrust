package nars.action.decompose;

import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.Deriver;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.unify.constraint.StructMatcher;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

import static nars.$.$$c;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DecomposeTest {

    /** filter only conj subterms for deep decompose */
    static final DynamicDecomposer.WeightedDynamicCompoundDecomposer ImplConjLayer2 = new DecomposeN(3) {
        @Override
        @Nullable protected Term sampleDynamic(Compound x, int depthRemain, RandomGenerator rng) {
            if (depthRemain == 2 && !x.CONJ())
                depthRemain--;
            return super.sampleDynamic(x, depthRemain, rng);
        }
    };

    @Test void impl_decompose1() {
        assertDecompose($$c("(--(&&,(a-->b),(c-->d))==>(&&,--(e-->f),(g-->h)))"),
                Decompose2.Decomposer2,
                "[(a-->b), (c-->d), (e-->f), (g-->h)]");
    }
    @Test void impl_decompose2() {

        assertDecompose($$c("(--(&&,(a-->b),(c-->d))==>(&&,--(e-->f),(g-->h)))"),
                ImplConjLayer2,
                "[a, b, c, d, e, f, g, h]");

        assertDecompose($$c("(--(&&,(a-->b),(c-->d))==>x)"),
                ImplConjLayer2,
                "[a, b, c, d, x]");

        assertDecompose($$c("(((a-->b)||(c-->d))==>(&&,--(e-->f),(g-->h)))"),
                ImplConjLayer2,
                "[a, b, c, d, e, f, g, h]");
    }

    @Test void impl_decompose3() {
        assertDecompose($$c("(--(&&,a,b,c)==>(&&,d,--e,f))"),
                Decompose2.Decomposer2,
                "[a, b, c, d, e, f]");

    }

    @Disabled @Test void factored_conj() {
        assertDecomposeCond("(x && (y &&+1 z))",
            "[(y &&+1 z), x]"
            //"[(x&&y), (x&&z), (y &&+1 z), x, y, z]"
        );
    }

    @Disabled @Test void factored_xternal() {
        assertDecomposeCond("(x && (y &&+- z))", "[(y &&+- z), x, y, z]");
    }

    @Test void factored_xternal_neg() {
        assertDecomposeCond("(x && --(y &&+- z))",
            //"[(y &&+- z), x, (x&&y), (x&&z), y, z]" //distributed TODO
            //"[(y &&+- z), x, y, z]"
            "[(y &&+- z), x]"
        );
    }

    private static void assertDecomposeCond(String x, String y) {
        assertDecompose($$c(x),
            DecomposeCond::decomposeCond,
                y
        );
    }


//    @Disabled
//    @Test void impl_subj_disj() {
//        //TODO
//        assertDecompose($$c("(--(x&&y) ==> z)"),
//                new DecomposeStatement(IMPL)::,
//              //..
//        );
//    }

    private static void assertDecompose(Compound x, BiFunction<Compound, RandomGenerator, Term> xy, String y) {
        RandomGenerator rng = new RandomBits(new XoRoShiRo128PlusRandom(1));
        TreeSet<Term> d = new TreeSet();
        for (int i = 0; i < x.complexity()*10; i++) {
            Term z = xy.apply(x, rng);
            if (z!=null && /* HACK */ !z.equals(x))
                d.add(z);
        }
        assertEquals(y, d.toString());
    }

    /** 2nd-layer compound decomposition */
    @Deprecated
    static class Decompose2 extends DecomposeTerm {


        public Decompose2() {
            super();

            //layer > 1 means subterms will contain compounds
            iff(PremiseTask, new StructMatcher.HasSubStruct(Op.Compounds, true));
        }

        @Override
        @Nullable
        public Term decompose(Compound src, Deriver d) {
            return Decomposer2.apply(src, d.rng);
        }

        static final DynamicDecomposer Decomposer2 = new DecomposeN(2);

    }
}
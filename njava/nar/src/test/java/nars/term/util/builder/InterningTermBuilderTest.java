package nars.term.util.builder;

import jcog.data.byt.RecycledDynBytes;
import nars.Op;
import nars.Term;
import nars.io.IO;
import nars.subterm.RemappedPNSubterms;
import nars.subterm.Subterms;
import nars.term.atom.Atomic;
import nars.term.builder.Intermed;
import nars.term.builder.InterningTermBuilder;
import nars.term.builder.MultiInterningTermBuilder;
import nars.term.builder.SmartTermBuilder;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

class InterningTermBuilderTest {

    private static final Term a = Atomic.atomic("a"), b = Atomic.atomic("b");

    @Test
    void test1() {
        InterningTermBuilder t =
                new MultiInterningTermBuilder(SmartTermBuilder.the);
                //new SeparateInterningTermBuilder();
        Term pab = t.compound(PROD, a, b);
        assertEquals( "(a,b)", pab.toString());

//        Function<Intermed.InternedCompoundByComponents, Term> prodCache = (HijackMemoize<Intermed.InternedCompoundByComponents, Term>) t.terms[PROD.id];

//        PriProxy<Intermed.InternedCompoundByComponents, Term> pabEntry = Iterators.get(prodCache.iterator(), 0);
//        assertEquals(pab, pabEntry.get());
        Term pabSame = t.compound(PROD, a, b);
        assertSame(pab, pabSame);

        Term paab = t.compound(PROD, a, t.compound(PROD, a, b));
//        prodCache.print();

        assertSame(pab, paab.sub(1));

        //Huffman h = prodCache.buildCodec();

    }

    @Test void MappedNegBiSubterms() {

        InterningTermBuilder t = new MultiInterningTermBuilder(new SmartTermBuilder(true));
        Subterms s = t.subterms($$("x1").neg(), $$("y1") );
        assertInstanceOf(RemappedPNSubterms.class, s, () -> s.getClass().toString());
        assertEquals("((--,x1),y1)", s.toString());
    }

//    @Test
//    void testImplicationComplexEndToEnd() {
//        //InterningTermBuilder t = new InterningTermBuilder();
//        if (Op.terms instanceof InterningTermBuilder) {
//
//            InterningTermBuilder i = (InterningTermBuilder) Op.terms;
////        System.out.println("impl/conj:");
////        i.terms[Op.IMPL.id].print();
////        i.terms[CONJ.id].print();
//            IMPL.the(a, CONJ.the(b.neg(), CONJ.the(a, 1, CONJ.the(b.neg(), b)).neg()));
////        System.out.println("impl/conj:");
////        i.terms[Op.IMPL.id].print();
////        i.terms[CONJ.id].print();
//        }
//    }

    @Test void KeyConstructionEquivalence() {
        byte[] a = new Intermed.InternedCompoundByComponentsArray(CONJ, 1, InterningTermBuilderTest.a.neg(), b).key.arrayCopy();
		RecycledDynBytes.get().clear();
//        byte[] b = new Intermed.InternedCompoundTransform(new LightDTCompound( new LightCompound(CONJ, this.a.neg(), this.b), 1)).key.arrayCopy();
//		RecycledDynBytes.get().clear();
//        assertArrayEquals(a, b);
//        assertEq(IO.bytesToTerm(a),IO.bytesToTerm(b));
        assertEquals("((--,a) &&+1 b)", IO.bytesToTerm(a).toString());
    }
}
package nars.func;

import nars.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactorizeTest {

    public final Factorize.FactorIntroduction f = new Factorize.FactorIntroduction();

    @Test
    void testConjPar2() {
        assertEq(
                "(member(#1,{a,b})&&f(#1))",
            //"(member($1,{a,b})==>f($1))",
                f.apply($$("(f(a) && f(b))"))
        );
    }
    @Test
    void testConjPar2_neg() {
        assertEq(
            "(member(#1,{(--,b),a})&&f(#1))", f.apply($$("(f(a) && f(--b))"))
        );
    }
    @Test
    void testConjPar2_xternal() {
        assertEq(
            "(member(#1,{a,b})&&f(#1))",
            //"(member($1,{a,b})==>f($1))",
            f.apply($$("(f(a) &&+- f(b))"))
        );
    }
    @Disabled
    @Test
    void testConjPar_in_Seq() {
        assertEquals(
            $$("((f(#1) && member(#1,{a,b})) &&+1 f(c))"),
            f.apply($$("((f(a) && f(b)) &&+1 f(c))"))
        );
    }

    @Test
    void testDisjPar2() {
        assertEquals(
            $$("--(--f(#1) && member(#1,{a,b}))"),
            f.apply($$("(f(a) || f(b))"))
        );
    }

    @Test
    void testConjSeq2() {
        String x = "(f(a) &&+3 f(b))";
        //factoring denied
        assertEq(x, f.apply($$(x)));
    }

    @Test
    void testTriple() {
        assertEq(
                "(member(#1,{a,b,c})&&f(#1))",
                //"(member($1,{a,b,c})==>f($1))",
                f.apply($$("(&&, f(a), f(b), f(c))"))
        );
    }

    @Test
    void testWithSomeNonInvolved() {
        assertEq(
               "(&&,member(#1,{a,b}),f(#1),g)",
            //"(member($1,{a,b})==>(f($1)&&g))",
                f.apply($$("(&&, f(a), f(b), g)"))
        );
    }

    @Test
    void testDoubleCommutive() {
        assertEq(
              "(member(#1,{a,y})&&{x,#1})",
            //"(member($1,{a,y})==>{x,$1})",
                f.apply($$("({a,x} && {x,y})"))
        );
    }

    @Test
    void test2() {
        assertEq(
                "(member(#1,{a,b})&&f(x,#1))",
            //"(member($1,{a,b})==>f(x,$1))",
            f.apply($$("(f(x,a) && f(x,b))"))
        );
    }

    @Test void three() {
        assertUnFactorizable("(&&,(--,isRow(tetris,(15,true),true)),isRow(tetris,(15,false),true),(--,nextColliding(tetris,true)),nextInBounds(tetris,true))");
    }

    @Disabled @Test void commutiveCollapseUnchanged() {
        assertUnFactorizable("((tetris,(5,x))&&(tetris,(8,x)))");
    }
    @Disabled @Test void commutiveCollapseSimUnchanged() {
        assertUnFactorizable("((tetris,(5,false))<->(tetris,(8,true)))");
    }
    @Test void wtf() {
        assertUnFactorizable("(&&,(meta(tetris,b)-->freq),a:z,effort:w)");
    }

    private void assertUnFactorizable(String s) {
        Term t = $$(s);
        assertEquals(
            Null, //unchanged
            f.apply(t)
        );
    }

    @Test
    void testInduction1() {
        assertEq(
               "(member(#1,{a,b,c})&&f(#1))",
            //"(member($1,{a,b,c})==>f($1))",
            f.apply(CONJ.the($$("f(c)"),
                    f.apply($$("(f(a) && f(b))")))).normalize()
        );
    }
}
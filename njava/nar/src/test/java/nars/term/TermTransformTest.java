package nars.term;

import nars.$;
import nars.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.atom.Bool.True;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TermTransformTest {

    @Test
    void testReplaceTemporalCorrectly() {
        assertEq("(((_1,_2)&&(_1,_3)) &&+2 ((_1,_2)&&(_1,_3)))",
                $$("(((_1,_2)&&(_1,_3)) &&+2 ((_1,_2)&&(_1,_3)))"));

        Term x = $$("((((_1,_2)&&(_1,_3)) &&+2 ((_1,_2)&&(_1,_3))) ==>+2 ((_1,_2)&&(_1,_3)))");
        Term y = x.replace($$("((_1,_2)&&(_1,_3))"), $.varDep(1));
        assertEquals("((#1 &&+2 #1) ==>+2 #1)", y.toString());
    }

    @Test void testSimReplaceCollapse() {
        assertEq(True, $$("(x<->y)").replace($$("x"), $$("y")));
    }

    @Test void replaceNeg_a() {
        assertEq(
                "(--,#1)", $$("(--,(x&&y))").replace($$("(x&&y)"), $.varDep(1))
        );
    }

    @Test void replaceNeg_b() {
        assertEq(
                "(#1 &&+1 (--,#1))", $$("((x&&y) &&+1 (--,(x&&y)))")
                        .replace($$("(x&&y)"), $.varDep(1))
        );
    }

}
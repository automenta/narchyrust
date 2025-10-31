package nars.func;

import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$$;
import static nars.func.ArithmeticizeTest.assertArithmetic;
import static nars.func.MathFuncTest.assertEval;
import static nars.term.atom.Bool.False;
import static nars.term.util.Testing.assertEq;

class CmpTest {

    final NAR n = NARS.shell();

    @Test
    void testComparator_Inline() {
        assertEval("-1","cmp(1,2)");
    }

    @Test
    void testComparator_scalar() {
        assertEval("-1", "cmp(1,2)");
    }

    @Test
    void testComparator_scalar_var() {
        assertEval("cmp(#1,x)", "cmp(#1,x)");
    }
    @Test
    void testComparator_scalar_var_sort() {
        assertEval("(cmp(#1,x)=-1)","(cmp(x,#1)=1)");
    }

    @Test
    void testComparator3() {
        assertArithmetic("(f(1)==>f(2))",
                "[((f(#1)==>f(#2))&&(cmp(#1,#2)=-1)), (f(#_1)==>f(add(#_1,1))), (f(add(-1,#_2))==>f(#_2))]");
    }

    @Test
    void testComparator4() {
        assertArithmetic("(f(2)==>f(1))",
                "[((f(#2)==>f(#1))&&(cmp(#1,#2)=-1)), (f(#_2)==>f(add(-1,#_2))), (f(add(#_1,1))==>f(#_1))]");
    }


    @Test
    void testComparatorCondition_1() {
        assertEq("f(4)", n.eval($$("(&&, (cmp(#1,3)=1), f(#1), (#1=4))")));
    }

    @Test
    void testComparatorCondition_2() {
        //backwards solve possible because cmp==0
        assertEval("f(4,4)", "(&&, (cmp(#1,#2)=0), f(#1,#2), (#1=4))");

    }

    @Test
    void testComparatorWithVars_DontEval() {
        assertEq("-1", n.eval($$("cmp(x(1),x(2))"))); //constant
        assertEq("cmp(x(#1),x(#2))", n.eval($$("cmp(x(#a),x(#b))"))); //variable
        assertEq("0", n.eval($$("cmp(x(#a),x(#a))"))); //variable, but equality known
    }

    @Test
    void testCmpReduceToEqual_vec_impossible() {
        assertEq(False, n.eval($$$("(cmp((a,#x),(b,#y))=0)")));
        assertEq(False, n.eval($$$("(cmp((b,#x),(a,#y))=0)")));
    }
    @Test
    void testCmpReduceToEqual_vec_variable_component() {
        assertEq("(#1=#2)", n.eval($$$("(cmp((a,#1),(a,#2))=0)")));
        assertEq("(#1=#2)", n.eval($$$("(cmp((a,#2),(a,#1))=0)")));
    }
    @Test
    void testCmpReduceToInEqual_vec_variable_component() {
        assertEq("(cmp(#1,#2)=1)", n.eval($$$("(cmp((a,#1),(a,#2))=1)")));
    }

    @Test
    void testCmpReduceToEqual1() {
        assertEq("(#1=#2)", n.eval($$("(0=cmp(#x,#y))")));
        assertEq("(#1=#2)", n.eval($$("(0=cmp(#y,#x))")));
    }

    @Test
    void testComparator_to_Equal() {
        assertEq("(x(#1)=y(#1))", n.eval($$("(0=cmp(x(#1),y(#1)))"))); //variable, but equality known
    }

    @Test
    void testComparator_to_NotEqual() {
        assertEq(False, n.eval($$("(0=cmp(x,y))"))); //variable, but equality known
    }

    @Test
    void NonConjArithmeticize() {
        assertArithmetic("x(1,1)", null); //nothing to do
        assertArithmetic("x(1,2)", "[((cmp(#1,#2)=-1)&&x(#1,#2)), x(1,add(1,1)), x(add(-1,2),2)]");
        //assertArithmetic("((1 && --2)-->x)", "[(((#1~add(#1,1))-->x)&&equal(#1,1)), (((#2~#1)-->x)&&cmp(#2,#1,-1))]");
        //assertArithmetic("(((1,1)~(2,3))-->x)", "4 of them");
    }

    @Test
    void testComparator_vector_2d() {
        //TODO check polarity
        assertEval("-1", "cmp((1,1),(1,2))");
        assertEval("1", "cmp((1,2),(1,1))");
        assertEval("0", "cmp((1,1),(1,1))");
    }

}
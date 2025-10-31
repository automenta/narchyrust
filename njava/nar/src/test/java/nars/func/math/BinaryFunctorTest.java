package nars.func.math;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$$;
import static nars.term.util.Testing.assertEq;

class BinaryFunctorTest {


    /** commutitivity */
//    @Nested
//    class Two_ary {
        final NAR n = NARS.shell();

        @Test
        void AddCommutive() throws Narsese.NarseseException {
            assertEq("add(#1,1)", n.eval($.$("add(#x,1)")));
            assertEq("add(#1,1)", n.eval($.$("add(1,#x)")));
        }


        @Test
        void CmpOrderingConstant() {
            assertEq("(#1=1)",  n.eval($$("(cmp(2,1)=#x)")));
            assertEq("(#1=-1)", n.eval($$("(cmp(1,2)=#x)")));
        }
        @Test
        void CmpOrderingVariable() {
            assertEq("(cmp(2,#1)=#2)", n.eval($$("(cmp(2,#1)=#x)")));
            assertEq("(cmp(#1,2)=#2)", n.eval($$("(cmp(#1,2)=#x)")));
        }

        @Test
        void AddOrderingConstant() {
            assertEq("add(#1,3)", n.eval($$("add(1,2,#x)")));
            assertEq("add(#1,3)", n.eval($$("add(2,1,#x)")));
        }

        @Test
        void AddOrderingVar() {
            assertEq("add(#1,#2,3)", n.eval($$("add(#1,#2,3)")));
            assertEq("add(#1,#2,3)", n.eval($$("add(#2,#1,3)")));
        }

        @Test
        void MulOrderingVar() {
            assertEq("mul(#1,#2,3)", n.eval($$("mul(#1,#2,3)")));
            assertEq("mul(#1,#2,3)", n.eval($$("mul(#2,#1,3)")));
        }

        @Test
        void CmpOrderingVar() {
            assertEq("(cmp(#1,#2)=1)", n.eval($$$("(cmp(#2,#1)=-1)")));
            assertEq("(cmp(#1,#2)=-1)", n.eval($$$("(cmp(#1,#2)=-1)")));

//            assertEq("cmp(#1,#2,1)", n.eval($$$("cmp(#1,#2,1)")));
//            assertEq("cmp(#1,#2,-1)", n.eval($$$("cmp(#2,#1,1)")));
        }

        @Test
        void MulOrderingConstant() {
            assertEq("mul(#1,2)", n.eval($$("mul(1,2,#x)")));
            assertEq("mul(#1,2)", n.eval($$("mul(2,1,#x)")));
        }

//        @Test
//        void CmpOrderingSemiConstant() {
//            assertEq("cmp(#1,2,-1)", n.eval($$("cmp(#1,2,-1)")));
//            assertEq("cmp(#1,2,-1)", n.eval($$("cmp(2,#1,1)")));
//        }

        @Test
        void AddOrderingSemiConstant() {
            assertEq("add(?2,#1,2)", n.eval($$("add(#1,2,?2)")));
            assertEq("add(?2,#1,2)", n.eval($$("add(2,#1,?2)")));
        }
//    }

    /** associativity */
//    @Nested
//    class Three_ary {
//        final NAR n = NARS.shell();

        @Test
        void AddOrderingVar2() {
            assertEq("add(?4,#1,#2,#3)", n.eval($$("add(#1,add(#2,#3),?4)")));

            assertEq("add(?4,#1,#2,#3)", n.eval($$("add(#3,add(#1,#2),?4)")));
        }

        @Test
        void AddOrderingConstant2() {
            assertEq("add(#1,6)", n.eval($$("add(1,add(2,3),#x)")));

            assertEq("add(#1,6)", n.eval($$("add(2,add(1,3),#x)")));
        }

        @Test
        void AddOrderingSemi_Constant() {
            assertEq("add(#1,#2,3)", n.eval($$("add(1,add(2,#1),#2)")));
        }
//    }

    /** multi-associativity */
//    @Nested
//    class Four_ary {
//
//        final NAR n = NARS.shell();

        @Test
        void AddOrderingVar3() {
            assertEq("add(?5,#1,#2,#3,#4)", n.eval($$("add(add(#4,#1),add(#2,#3),?5)")));
        }
//    }

}
package nars.func;

import nars.Narsese;
import nars.Op;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$$;
import static nars.func.MathFuncTest.*;
import static nars.term.atom.Bool.*;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EqualTest {
    /**
     * same tautological assumptions should hold in equal(x,y) results
     */
    @Test
    void EqualOperatorTautologies() {
        //TODO finish
//        NAR n = NARS.shell();
        assertEquals(True, Op.EQ.the(True, True));
        assertEquals(False, Op.EQ.the(True, False));
        assertEquals(False /*Null*/, Op.EQ.the(True, Null));
        assertEquals(False /*Null*/, Op.EQ.the(False, Null));
    }


    @Test
    void impossibility() {
        assertEval(False, $$("((#1=a)&&(#1=b))"));
        assertEval("((#1=a)&&(#2=b))", $$("((#1=a)&&(#2=b))"));
        assertEq(False /*"(a=b)"*/, Op.EQ.the($$("a"), $$("b")));
        assertEval(True, Op.EQ.the($$("a"), $$("b")).neg());
    }
    @Disabled
    @Test
    void impossibilityInv() {
        assertEval("(#1=a)", $$("((#1=a) && --(#1=b))")); //elimination

//        assertEq("(y-->x)", Equal.the($$("x:y"), True));
//        assertEq("(--,(y-->x))", Equal.the($$("x:y"), False));

//        assertEquals("[equal(true,true)]", Evaluation.eval($$("equal(true,true)"), n).toString());
//        assertEquals("[equal(false,false)]", Evaluation.eval($$("equal(false,false)"), n).toString());
        //assertEquals("[null]", Evaluation.eval($$("equal(null,null)"), n).toString());
    }

    @Test
    void EqBackSubstitutionAdd() {
        assertEval("((#1,4)&&(#1,3))", "(&&,(#1,add(#2,1)),(#2=3),(#1,#2))");
        //assertSolves("(&&,(#1,add(#2,1)),(#2=3),(#1,#2))", "((#1,4)&&(#1,3))");
    }

    @Test
    void EqBackSubstitutionAddIndirect_2ary() {
        assertEval("2", "(&&,(3=add(#1,1)),#1)");
    }
    @Test
    void EqBackSubstitutionAddIndirect_3ary() {
        assertEval("1", "(&&,(3=add(#1,1,1)),#1)");
    }

    @Disabled @Test
    void EqBackSubstitutionAddIndirect_3ary_2_vars() {
        assertEval("add(mul(#2,-1),2)", "(&&,(3=add(#1,#2,1)),#1)"); //??
    }

    @Test
    void EqBackSubstitutionAdd2() {
        assertEval("((#1,3)&&(#1,4))", "(&&,(#1,add(#2,1)),(#1,#2),(#2=3))");
    }
    @Test
    void EqBackSubstitutionAdd2_solve() throws Narsese.NarseseException {
        assertSolves("((#1,3)&&(#1,4))", "(&&,(#1,add(#2,1)),(#1,#2),(#2=3))");
    }

    @Test
    void EqualSolutionAddInverse() {
        assertEval($$("x(0)"), "(x(#1) && (add(#1,1)=1))");
        assertEval($$("x(0)"), "(x(#1) && (add(1,#1)=1))");
        assertEval($$("x(0)"), "(x(#1) && (1=add(#1,1)))");
        assertEval($$("x(0)"), "(x(#1) && (1=add(1,#1)))");
    }

    @Test
    void EqualSolutionMulInverseA() {
        assertEval($$("x(-2)"), "(x(#1) && (mul(#1,-1)=2))");
        assertEval($$("x(-2)"), "(x(#1) && (mul(-1,#1)=2))");
        assertEval($$("x(-2)"), "(x(#1) && (2=mul(#1,-1)))");
        assertEval($$("x(-2)"), "(x(#1) && (2=mul(-1,#1)))");
    }
    @Test
    void EqualSolutionMulInverseB() {
        assertEval($$("x(1)"), "(x(#1) && (mul(2,#1)=2))");
        assertEval($$("x(1)"), "(x(#1) && (mul(#1,2)=2))");
        assertEval($$("x(1)"), "(x(#1) && (2=mul(#1,2)))");
        assertEval($$("x(1)"), "(x(#1) && (2=mul(2,#1)))");
    }

    @Test
    void EqualSolutionComplex() {
        /*

        (&&,(--,(g(add(#1,1),0,(0,add(add(#1,1),9)))&&equal(add(#1,1),1))),(--,chronic(add(#1,1))),(--,add(#1,1)),(--,down))

        "equal(add(#1,1),1)" ===> (1 == 1+x) ===> (0 == x)

        drastic simplification:
            (&&,(--,(g(add(#1,1),0,(0,add(#1,10))&&equal(#1, 0)),(--,chronic(add(#1,1))),(--,add(#1,1)),(--,down))
            etc (&&,(--,(g(add(#1,1),0,(0,10)),(--,chronic(1)),(--,0),(--,down))
         */

        String t = "(&&,(--,(g(add(#1,1),0,(0,add(add(#1,1),9))) && (add(#1,1)=1))),(--,c(add(#1,1))),(--,add(#1,1)),(--,down))";

        assertEval($$("(&&,(--,g(1,0,(0,10))),(--,c(1)),(--,down),(--,1))"), t);
    }




    @Test
    void AddEqualIdentity() {
        assertEval($$("answer(0)"), "((#x=add(#x,$y))==>answer($y))");
    }
    @Test void productSolve() {
        assertEval($$("f(2)"), "(((1,#x)=(1,2)) && f(#x))"); //numeric
        assertEval($$("f(b)"), "(((a,#x)=(a,b)) && f(#x))"); //non-numeric
        assertEval($$("f(b)"), "(((a,(#x,c))=(a,(b,c))) && f(#x))"); //recursive
    }

    @Test void setInequal_arity() {
        assertEvalFalse("({a}={a,b})");
        assertEvalFalse( "((a)=(a,b))");
        assertEvalFalse("((a)=(a,#b))");
    }
    @Test void setInequal_arity_but_actually_solveable() {
        assertEval("a", "(({a}={a,#b}) && #b)");
    }


    @Test void setInequal_type() {
        assertEvalFalse( "({a,#b}=(a,#b))");
    }

    @Test void setSolve() {
        assertEval("f(2)", "(({1,#x}={1,2}) && f(#x))");
        assertEval("f(b)", "(({a,#x}={a,b}) && f(#x))");
    }
    @Test void setSolve_extra() {
        assertEval("(f(2) && g(2))", "(&&, ({1,#x,#y}={1,2}), f(#x), g(#y))");
    }

    @Test
    void varEquivalence_Elimination() {
        assertEval($$$("(x(#1)<->y(#1))"), "((x(#a)<->y(#b)) && (#a=#b))");
    }
    @Test
    void falseConstant() {
        assertEval(False, "(a=b)");
    }
    @Test
    void trueWithNegationNormalization1() {
        //assertEval($$("(--,equal(--z,y))"), "(equal(--#x,z) && equal(#x,y))");
        assertEval($$("--z"), "(((--#x=z) && (#x=y)) && #x)");
    }
    @Test
    void trueWithNegationNormalization2() {
        assertEval($$$("((--,#1)=#2)"), "((--#x=#z) && (#x=#y))"); //indeterminate
    }

    @Test
    void EqualVector_Decompose_VariableComponent_1() {
        assertEval("(#1=#2)", $$$("((a,#1)=(a,#2))"));
    }
    @Test
    void EqualVector_Decompose_VariableComponent_2() {
        assertEval($$$("((#1,#3)=(#2,#4))"), $$$("((a,#1,#3)=(a,#2,#4))"));
    }
    @Test void equalInSequence_pre() {
        assertEval($$$("(fz-->((race,1)&&(#2,1)))"),
                "((fz-->((race,#2)&&(#3,#2)))&&(#2=1))");
    }
    @Test void equalInSequence1() {
        assertEval($$$("((fz-->((race,1)&&(#2,1))) &&+1 x)"),
                "(((fz-->((race,#2)&&(#3,#2)))&&(#2=1)) &&+1 x)");
    }
    @Test void equalInSequence2() {
        assertEval(
            $$$("((fz-->((race,1)&&(#3,1))) &&+340 (--,(((fz-->#1) &&+2330 (fz-->(race,1))) &&+340 (fz-->(race,1)))))"),
            "(((fz-->((race,#2)&&(#3,#2)))&&(#2=1)) &&+340 (--,(((fz-->#1) &&+2330 (fz-->(race,1))) &&+340 (fz-->(race,1)))))");
    }
    @Test void equalInSequence3() {
        
        assertEval(
                $$$("(((y,(R-->(y,(--,R(tetris,b)))))&(y,Δplan(y,(low-->(tetris,#3)))))-->wonder)"),
                "((--,((--,wonder(y,Δplan(y,(low-->(tetris,#3))))) &&+9820 (&&,wonder(#1,(#2-->(#1,(--,((tetris,b)-->#2))))),(R=#2),(y=#1))))&&wonder(y,(R-->(y,(--,R(tetris,b))))))");

    }
}
package nars.func;

import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Term;
import nars.action.transform.Arithmeticize;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;

class ArithmeticizeTest {
    public static void assertArithmetic(String q, String y) {
        Random rng = new XoRoShiRo128PlusRandom(1);
        Set<String> solutions = new TreeSet();
        Term Q = $$(q);
        for (int i = 0; i < 10; i++) {
            Term s = Arithmeticize.apply(Q, new RandomBits(rng));
            if (s != null) {
                solutions.add(s.toString());
            } else {
                assertNull(y);
                return;
            }
        }
        assertEquals(y, solutions.toString());
    }

    @Test
    void scalar1() {
        assertArith("(2,3)",
                "(#1,add(#1,1))", "(add(#1,-1),#1)"
                //"(2,add(1,2))", "(add(-1,3),3)",
                //"((#1,add(#1,1))&&equal(#1,2))",
                 //"((cmp(#1,#2)=-1)&&(#1,#2))"
        );
    }

    @Test
    void scalar_add() {
        assertArith("x(2,3)",
            "x(#1,add(#1,1))", "x(add(#1,-1),#1)");
    }

    @Test
    void scalar_cmp() {
        assertArith("x(2,3)", "((cmp(#1,#2)=-1)&&x(#1,#2))");
    }

    @Test
    void scalar3() {
        assertArith("(x(2,3) ==> y)",
                "(x(add(#1,-1),#1)==>y)", "(x(#1,add(#1,1))==>y)"
                //,"((x(#1,#2)==>y)&&(cmp(#1,#2)=-1))"
        );
    }

    @Test
    void scalar_negate() {
        assertArith("(2 ==> -2)",
                "(#1==>add(#1,-4))",  "(add(#1,4)==>#1)",
                "(#1==>mul(#1,-1))","(mul(#1,-1)==>#1)"
                //"((cmp(#1,#2)=-1)&&(#2==>#1))"
        );
    }
    @Test
    void vector_2d_1() {
        assertArith("((2,3) ==> (3,4))",
                "(add((-1,-1),#1)==>#1)", "(#1==>add((1,1),#1))"
                //"((2,3)==>add((2,3),#1))&&equal(#1,(1,1)))"
                //"[(((2,3)==>add((2,3),#1))&&equal((1,1),#1)), ((add((3,4),#1)==>(3,4))&&equal((-1,-1),#1))]"
                //"((#_2,#_3)==>add((1,1),(#_2,#_3)))", "(add((-1,-1),(#_3,#_4))==>(#_3,#_4))"
        );
    }
//    @Test
//    void vector_scale_1() {
//        assertArith("(2,2,2)",
//                //"((2,3)==>add((2,3),#1))&&equal(#1,(1,1)))"
//                //"[(((2,3)==>add((2,3),#1))&&equal((1,1),#1)), ((add((3,4),#1)==>(3,4))&&equal((-1,-1),#1))]"
//                "mul((2,2,2),2)"
//        );
//    }

    @Test
    void vector_scale_2() {
        assertArith("((1,2) ==> (2,4))",
                "(add((-1,-2),#1)==>#1)","(#1==>add((1,2),#1))",
                "((#1,#2)==>mul((#1,#2),2))" //"(#1==>mul(#1,2))",

                //"((2,3)==>add((2,3),#1))&&equal(#1,(1,1)))"
                //"[(((2,3)==>add((2,3),#1))&&equal((1,1),#1)), ((add((3,4),#1)==>(3,4))&&equal((-1,-1),#1))]"
                //"((#_1,#_2)==>mul((#_1,#_2),#_2))", "(add((-1,-2),(#_2,#_4))==>(#_2,#_4))"
        );
    }


    static void assertArith(String q, String... p) {
        Random rng = new XoRoShiRo128PlusRandom(1);
        TreeSet<Term> expect = Stream.of(p).map($::$$$).collect(toCollection(TreeSet::new));
        Set<Term> actual = new TreeSet<>();
        Term Q = $$(q);
        int bound = p.length * 8;
        RandomGenerator r = new RandomBits(rng);
        for (int i = 0; i < bound; i++) {
            Term aa = Arithmeticize.apply(Q, r);
            assertNotNull(aa);
            actual.add(aa.normalize());
        }

        assertTrue(actual.containsAll(expect)
                , ()->"got: " + actual + ", wanted: " + expect
        );
    }

    @Test
    void max_min_test() {
        assertArith("x(2,3)",
                "((min(#1,#2)=2)&&x(#1,#2))",
                "((max(#1,#2)=3)&&x(#1,#2))"
        );
    }
//    @Test
//    void gte_test() {
//        assertArith("(5,3)",
//                "((gte(#1,#2)=1)&&(#1,#2))",
//                "(#1,add(#1,-2))"
//        );
//    }
//
//    @Test
//    void lte_test() {
//        assertArith("(3,5)",
//                "((lte(#1,#2)=1)&&(#1,#2))",
//                "(#1,add(#1,2))"
//        );
//    }
//
//    @Test
//    void gt_lt_test() {
//        assertArith("(4,2,6)",
//                "((gt(#1,#2)=1)&&(lt(#1,#3)=1)&&(#1,#2,#3))",
//                "(add(#1,2),#1,add(#1,4))"
//        );
//    }

//    @Test
//    void mod_test() {
//        assertArith("x(2,3)",
//                "x(mod(#1,3),#1)",
//                "x(mod(add(#1,2),3),#1)"
//        );
//    }

//    @Test
//    void div_test() {
//        assertArith("(8,2)",
//                "(#1,div(#1,2))",
//                "(mul(2,#2),#2)"
//        );
//    }

//    @Test
//    void combined_arithmetic_test() {
//        assertArith("(10,5,2)",
//                "((gte(#1,#2)=1)&&(lte(#2,#3)=1)&&(#1,#2,#3))",
//                "(add(#2,#3),#2,div(#2,2))",
//                "(mul(2,#3),#3,mod(#1,3))"
//        );
//    }
//
//    @Test
//    void vector_gte_lte_test() {
//        assertArith("((5,7),(3,4))",
//                "((gte(#1,#3)=1)&&(gte(#2,#4)=1)&&((#1,#2),(#3,#4)))",
//                "((#1,#2),(add(#1,-2),add(#2,-3)))"
//        );
//    }
//
//    @Test
//    void arithmetic_chain_test() {
//        assertArith("(2,4,8,16)",
//                "(#1,mul(#1,2),mul(#1,4),mul(#1,8))",
//                "(div(#2,2),#2,mul(#2,2),mul(#2,4))",
//                "(#1,add(#1,2),add(#1,6),add(#1,14))"
//        );
//    }

}
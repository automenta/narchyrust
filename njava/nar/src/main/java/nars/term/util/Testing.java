/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package nars.term.util;

import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.data.list.Lst;
import nars.Narsese;
import nars.Op;
import nars.Term;
import nars.Truth;
import nars.io.IO;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Bool;
import nars.term.var.Variable;

import java.util.List;
import java.util.function.Supplier;

import static nars.$.*;
import static nars.NAL.truth.FREQ_EPSILON;
import static nars.Op.DTERNAL;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * static utilities for Term and Subterms tests
 */
public enum Testing { ;


    public static void assertEquivalentTerm(String term1String, String term2String) {

        try {


            Termed term1 = $(term1String);
            Termed term2 = $(term2String);


            assertEq(term1.term(), term2.term());

        } catch (Narsese.NarseseException e) {
            fail(e::toString);
        }
    }

    public static Term assertStable(String is) {
        return assertEq(is,is);
    }

    public static Term assertEq(Term x, Term y) {

        Supplier<String> n = ne(x, y);
        if (x instanceof Compound != y instanceof Compound) fail(n);
        assertEquals(x instanceof Neg, y instanceof Neg, n);
        assertEquals(x instanceof Variable, y instanceof Variable, n);

        //noinspection RedundantCast
        assertEq((Termlike)x, ((Termlike)y));

        assertEquals(0, y.compareTo(x), n);
        assertEquals(0, x.compareTo(y), n);
        assertEquals(0, x.compareTo(x), n);
        assertEquals(0, y.compareTo(y), n);



        assertEquals(x.opID(), y.opID());
        assertEquals(y instanceof Neg, x instanceof Neg);

        assertEquals(x.structOp(), y.structOp());
        assertEquals(x.subs(), y.subs());


        Subterms xs = x.subterms();
        Subterms ys = y.subterms();
        assertEqSubterms(xs, ys);



        if (x instanceof Compound)
            assertEquals(orderedRecursion(x), orderedRecursion(y));

        //return assertEq(y, x.toString(), x);
        return x;
    }

    private static Supplier<String> ne(Termlike x, Termlike y) {
        return () -> "not equal:\nactual:\t" + y + "\nexpect:\t" + x;
    }

    public static Term assertEq(String exp, String is) {
        Term t = $$(is);
        return assertEq(exp, is, t);
    }

    private static Term assertEq(Object exp, String is, Term x) {
        assertEquals(exp, exp instanceof String ? x.toString() : x, () -> is + " reduces to " + exp);


        //test for stability:
        Term y = null;
        try {
            y = $(x.toString());
        } catch (Narsese.NarseseException e) {
            //e.printStackTrace();
            fail(() -> x + " -> " + e);
        }
        //assertEquals(u, t, ()-> is + " unstable:\n0:\t" + t + "\n1:\t" + u);
        assertEquals(x, y);

//        try {
//            Term z = y.anon();
//        } catch (RuntimeException e) {
//            fail(e::toString);
//        }

        return x;
    }

    public static Term assertEq(Term z, String x) {
        Term y = $$(x);
        assertEq(z, y);
        assertEq(z, x, y);
        return y;
    }

    public static void assertEq(String exp, Term x) {
        assertEquals(exp, x.toString(), () -> exp + " != " + x);
    }

    private static void assertEq(Termlike x, Termlike y) {
        var n = ne(x, y);
        assertEquals(x instanceof Subterms, y instanceof Subterms, n);


        assertEquals(x, y, n);
        assertEquals(y, x, n);

        assertEquals(x, x, n);
        assertEquals(y, y, n);


        assertEquals(x.toString(), y.toString());
        assertEquals(x.hashCode(), y.hashCode());
        assertEquals(x.complexity(), y.complexity());
        assertEquals(x.height(), y.height());
        assertEquals(x.complexityConstants(), y.complexityConstants());
        assertEquals(x.struct(), y.struct());
        assertEquals(x.structSurface(), y.structSurface());
        assertEquals(x.structSubs(), y.structSubs());
        assertEquals(x.vars(), y.vars());
        assertEquals(x.hasVars(), y.hasVars());
        assertEquals(x.hasVarDep(), y.hasVarDep());
        assertEquals(x.varDep(), y.varDep());
        assertEquals(x.hasVarIndep(), y.hasVarIndep());
        assertEquals(x.varIndep(), y.varIndep());
        assertEquals(x.hasVarQuery(), y.hasVarQuery());
        assertEquals(x.varQuery(), y.varQuery());
        assertEquals(x.hasVarPattern(), y.hasVarPattern());
        assertEquals(x.varPattern(), y.varPattern());


    }

    private static List<Term> orderedRecursion(Term x) {
        List<Term> m = new Lst(x.complexity()*2);
        x.recurseTermsOrdered((t)->{
            m.add(t);
            return true;
        });
        return m;
    }

    public static void assertEq(Compound a, Compound b) {
        assertEq((Term)a, b);
        assertEqSubterms(a, b);
    }
    public static void assertEq(Subterms a, Subterms b) {
        assertEq((Termlike)a, b);
        assertEqSubterms(a, b);
    }

    private static void assertEqSubterms(Subterms a, Subterms b) {

        if (!(a instanceof Compound) && !(b instanceof Compound))
            assertEquals(a.hashCode(), b.hashCode());

        int s = a.subs();
        assertEquals(s, b.subs());
        for (int i = 0; i < s; i++) {
            assertEq(a.sub(i), b.sub(i));
            assertTrue(a.subEquals(i, b.sub(i)));
            assertTrue(b.subEquals(i, a.sub(i)));
            assertEquals(a.subUnneg(i), b.subUnneg(i));
            assertEquals(a.subSafe(i), b.subSafe(i));
            assertEquals(a.subIs(i, Op.ATOM), b.subIs(i, Op.ATOM)); //TODO other ops
        }

        assertTrue(Iterators.elementsEqual(a.iterator(), b.iterator()));

        Term[] aa = a.arrayShared();
        Term[] bb = b.arrayShared();
        assertArrayEquals(aa, bb);
        assertEquals(Op.terms_.compound(PROD, aa), Op.terms_.compound(PROD, bb));
        assertEquals(Op.terms_.compound(PROD, bb), Op.terms_.compound(PROD, aa));
        assertEquals(pFast(a), pFast(b));
        assertEquals(Op.terms_.compound(PROD, DTERNAL, false, aa), Op.terms_.compound(PROD, DTERNAL, false, bb));

        assertEquals(a.hashCodeSubterms(), b.hashCodeSubterms());

        assertEquals(0, Subterms.compare(a, b));
        assertEquals(0, Subterms.compare(a, b));

        boolean aNorm = a.NORMALIZED();
        boolean bNorm = b.NORMALIZED();
        assertEquals(aNorm, bNorm, () -> a + " (" + a.getClass() + ") normalized=" + aNorm + ", " + " (" + b.getClass() + ") normalized=" + bNorm);
        assertEquals(a.subtermsSorted(), b.subtermsSorted());
        assertEquals(a.subtermsSorted(), b.subtermsSorted());

        {
            byte[] bytesExpected = IO.termToBytes(pFast(a));
            byte[] bytesActual = IO.termToBytes(pFast(b));
            assertArrayEquals(bytesExpected, bytesActual);
        }
        {
            if (a.subs() > 0) {
                byte[] bytesExpected = IO.termToBytes(sFast(a));
                byte[] bytesActual = IO.termToBytes(sFast(b));
                assertArrayEquals(bytesExpected, bytesActual);
            }
        }
//        {
//            byte[] bytesExpected = IO.termToBytes(PROD.the(a));
//            byte[] bytesActual = IO.termToBytes(PROD.the(b));
//            assertArrayEquals(bytesExpected, bytesActual);
//        }
//
//        {
//            byte[] bytesExpected = IO.termToBytes(PROD.the(a));
//            byte[] bytesActual = IO.termToBytes(PROD.the(b));
//            assertArrayEquals(bytesExpected, bytesActual);
//        }
//
//        {
//            if (a.subs() > 0) {
//                byte[] bytesExpected = IO.termToBytes(SETe.the(a));
//                byte[] bytesActual = IO.termToBytes(SETe.the(b));
//                assertArrayEquals(bytesExpected, bytesActual);
//            }
//        }

    }

    /** HACK may need to split this into two functions, one which accepts bool result and others which only accept a thrown exception */
    public static void assertInvalids(String... inputs) {
        for (String s : inputs)
            assertInvalid(s);
    }

    public static void assertInvalid(String s) {
        try {
            Term e = Narsese.term(s);
            assertInstanceOf(Bool.class, e, () -> s + " should not be parseable but got: " + e);

        } catch (Narsese.NarseseException | TermException e) {
            //assertTrue(true);
        }
    }

    @Deprecated public static void testParse(String expected, String input) {
        Termed t = null;
        try {
            t = $(input);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
        if (expected == null)
            expected = input;
        assertEquals(expected, t.toString());
    }

    public static void testParse(String s) {
        testParse(null, s);
    }

    public static void assertEq(float f, float c, Truth t) {
        assertTrue(Util.equals(t.freq(), f, FREQ_EPSILON), ()->t + " wrong freq: " + f);
        assertTrue(Util.equals(t.conf(), c, FREQ_EPSILON), ()->t + " wrong conf: " + c);
    }

    public static void assertCanonical(String x) {
        assertEquals(x, $$$(x).toString());
    }

    public static void assertCond(String container, String cond) {
        var CONTAINER = $$c(container);
        var COND = $$$(cond);
        assertTrue(CONTAINER.condOf(COND), ()->"condOf(" + CONTAINER + ","+ COND +")");
    }

    public static void assertNotCond(String container, String cond) {
        var CONTAINER = $$c(container);
        var COND = $$$(cond);
        assertFalse(CONTAINER.condOf(COND), ()->"notCondOf(" + CONTAINER + ","+ COND +")");
    }

}
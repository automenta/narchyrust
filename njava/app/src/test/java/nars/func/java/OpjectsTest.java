package nars.func.java;

import jcog.data.list.Lst;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OpjectsTest {

    @SuppressWarnings("WeakerAccess")
    public static class SimpleClass {
        int v;

        public void set(int x) {
            System.err.println("setAt: " + x);
            this.v = x;
        }

        public int get() {
            return v;
        }

        public boolean bool() {
            return true;
        }
        public boolean boolFalse() {
            return false;
        }
    }


    /**
     * self invocation
     */
    @Test
    void testEvoke() throws Narsese.NarseseException {
        NAR n = NARS.tmp();

        int dur = 1;

        n.time.dur(dur);

        List<Term> evokes = new Lst();
        Opjects objs = new Opjects(n.main()) {
            @Override
            protected boolean evoked(Term method, Object instance, Object[] params) {
                evokes.add(method);
                return super.evoked(method, instance, params);
            }
        };
        n.add(objs);

        n.run();

        SimpleClass x = objs.the("(x)", new SimpleClass());
        StringBuilder sb = new StringBuilder();
        n.main().eventTask.on(sb::append);


        n.input("set((x),1)! |");

        n.run(dur);

        assertEquals(1, evokes.size());

        n.input("get((x),#y)! |");

        n.run(dur);

        assertEquals(2, evokes.size());

        n.run(dur*4);

        assertEquals(2, evokes.size());
    }

    @Test
    void testObjectMethods() throws Narsese.NarseseException {
        NAR n = NARS.tmp();


        Opjects objs = new Opjects(n.main());
        n.add(objs);

        SimpleClass x = objs.a("(x)", SimpleClass.class);
        StringBuilder sb = new StringBuilder();
        n.main().eventTask.on(sb::append);


        n.input("hashCode((x),#h)! |");
        n.run(1);
        n.run(1);
        assertFalse(sb.toString().contains("voke"));
    }
    @Test
    void testBoolMethod() {
        NAR n = NARS.tmp();


        Opjects objs = new Opjects(n.main());
        n.add(objs);

        SimpleClass x = objs.a("(x)", SimpleClass.class);
        StringBuilder sb = new StringBuilder(1024);
        n.log(sb);

        x.bool();
        //n.input("hashCode(x,#h)! |");
        //n.run(1);
        n.run(1);
        assertTrue(sb.toString().contains("bool((x)). 0⋈1 %1.0;.90%"));

        x.boolFalse();
        n.run(1);
        assertTrue(sb.toString().contains("boolFalse((x)). 1⋈2 %0.0;.90%"));
    }

    /**
     * invoked externally (ex: by user)
     */
    @Test
    void testInvokeInstanced() {
        NAR n = NARS.tmp();
        Opjects o = new Opjects(n.main());
        SimpleClass y = o.a("(y)", SimpleClass.class);
        n.add(o);
        testInvoke(n, y);
    }

    static void testInvoke(NAR n, SimpleClass y) {
        StringBuilder sb = new StringBuilder();
        n.main().eventTask.on(sb::append);
        n.run(1);
        {
            y.get();
        }
        n.run(1);
        {
            y.set(1);
        }
        n.run(1);
        String s = sb.toString();
        System.out.println("log:\n" + s);
        assertTrue(s.contains("get((y),0)."));
        assertTrue(s.contains("set((y),1)."));
    }

    @Test
    void testInvokeWrapped() {
        NAR n = NARS.tmp();
        Opjects o = new Opjects(n.main());
        n.add(o);
        SimpleClass y = o.the("(y)", new SimpleClass());
        testInvoke(n, y);
    }

    @Disabled
    @Test
    void learnMethodGoal() throws Narsese.NarseseException {


        NAR n = NARS.tmp();


        Opjects objs = new Opjects(n.main());

        SimpleClass x = objs.a("x", SimpleClass.class);


        n.beliefPriDefault.pri(0.05f);
        n.questionPriDefault.pri(0.05f);
        n.questPriDefault.pri(0.05f);
        n.freqRes.set(0.1f);
        n.time.dur(10);
        n.complexMax.set(30);

//        n.logPriMin(System.out, 0.02f);


        int N = 2;



        int loops = 0, trainingRounds = 4;
        while (x.v != 2) {

            if (loops++ < trainingRounds) {
                for (int i = 0; i < 2; i++) {


                    x.set(i % N);

                    n.run(1);

                    x.get();

                    n.run(1);
                }


                n.input("$1.0 x(get,(),2)!");

                n.run(50);
            }


            n.run(50);
        }


    }
}
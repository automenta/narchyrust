package nars;

import jcog.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.QUESTION;
import static org.junit.jupiter.api.Assertions.*;


@Execution(ExecutionMode.SAME_THREAD)
class InterNARTest {

    @ParameterizedTest
    @ValueSource(ints={1,0})
    void testDiscoverDoesntSelfConnect(int pingSelf) {
        NAR n = NARS.realtime(1f).get();
        InterNAR x = new InterNAR(n);
        x.fps(8f);

        n.synch();
        if (pingSelf==1) {
            x.ping(x.addr());
        }
        for (int i = 0; i < 8; i++) {
            assertFalse(x.peer.them.contains(x.peer.me));
            assertFalse(x.peer.connected(),()->x.peer.them.stream().toList().toString());
            Util.sleepMS(100);
        }

        n.delete();
    }

    static void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<NAR, NAR> afterConnect) {

        final float NET_FPS = 20f;
        final float NAR_FPS = NET_FPS * 2;

        int preCycles = 1;

        NAR a = NARS.realtime(NAR_FPS).withNAL(1, 1).get();
        NAR b = NARS.realtime(NAR_FPS).withNAL(1, 1).get();

        Focus aa = a.main();
        Focus bb = b.main();

        for (int i = 0; i < preCycles; i++) {
            a.run(1);
            b.run(1);
        }

        beforeConnect.accept(a, b);

        InterNAR ai = new InterNAR(0, false, aa);
        InterNAR bi = new InterNAR(0, false, bb);

        ai.fps(NET_FPS);
        bi.fps(NET_FPS);

        a.synch();
        b.synch();

        assertNotSame(ai.id, bi.id);
        assertNotEquals(bi.addr(), ai.addr());
        assertNotEquals(bi.peer.name(), ai.peer.name());


        ai.peer.ping(bi.peer);

        boolean connected = false;
        final int CONNECT_INTERVAL_MS = 30;
        final int MAX_CONNECT_INTERVALS = 200;
        for (int i = 0; !connected && i < MAX_CONNECT_INTERVALS; i++) {
            Util.sleepMS(CONNECT_INTERVAL_MS);
            connected = ai.peer.connected() && bi.peer.connected();
        }
        assertTrue(connected);


        System.out.println("connected. interacting...");

        afterConnect.accept(a, b);

        /* init */
        int duringCycles = 256;
        for (int i = 0; i < duringCycles; i++) {
            a.run(1);
            b.run(1);
        }

        System.out.println("disconnecting..");

        /* init */
        int postCycles = 128;
        for (int i = 0; i < postCycles; i++) {
            a.run(1);
            b.run(1);
        }


        a.delete();
        b.delete();

    }

    /**
     * direct question answering
     */
    @Test
    void testInterNAR1() {
        AtomicBoolean aRecvQuestionFromB = new AtomicBoolean();

        testAB((a, b) -> {

            a.main().onTask(task -> {
                if (task.toString().contains("(?1-->y)"))
                    aRecvQuestionFromB.set(true);
            }, QUESTION);


            try {
                b.believe("(X --> y)");
            } catch (Narsese.NarseseException e) {
                fail(e);
            }

        }, (a, b) -> {

            try {
                a.input("(?x --> y)?");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        });

        assertTrue(aRecvQuestionFromB.get());

    }

    /**
     * cooperative solving
     */
    @Test
    void testInterNAR2() {

        AtomicBoolean recv = new AtomicBoolean();

        testAB((a, b) -> {

            a.complexMax.set(3);
            b.complexMax.set(3);

            b.main().onTask(bt -> {
                if (bt.BELIEF() && bt.term().toString().contains("(a-->d)"))
                    recv.set(true);
            }, BELIEF);

        }, (a, b) -> {


            a.believe($$("(b --> c)"));

            b.believe($$("(a --> b)"));
            b.believe($$("(c --> d)"));
            b.question($$("(a --> d)"));

        });

        assertTrue(recv.get());

    }

}
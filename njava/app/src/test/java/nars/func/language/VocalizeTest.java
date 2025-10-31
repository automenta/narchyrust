package nars.func.language;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VocalizeTest {

    @Test
    void testVocalization1() {
        NAR n = NARS.tmp();
        StringBuilder b = new StringBuilder();
        Vocalize s = new Vocalize(n, 1f, (w) -> b.append(n.time() + ":" + w + ' '));

        s.speak($.atomic("x"), 1, $.t(1f, 0.9f));
        s.speak($.atomic("not_x"), 1, $.t(0f, 0.9f));
        s.speak($.atomic("y"), 2, $.t(1f, 0.9f));
        s.speak($.atomic("z"), 4, $.t(0.95f, 0.9f));
        s.speak($.atomic("not_w"), 6, $.t(1f, 0.9f));
        assertEquals(5, s.vocalize.size()); 
        n.run(5);
        assertEquals("1:x 2:y 4:z ", b.toString());
        assertEquals(1, s.vocalize.size()); 


    }

    @Test
    void testHearGoal() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.complexMax.set(16);
        n.freqRes.set(0.1f);
        n.timeRes.set(50);

//        n.log();
        n.input(
                "$1.0 (hear($1) ==> speak($1)).",
                "$1.0 (speak($1) ==> hear($1)).",
                "$1.0 (hear(#1) && speak(#1))!",
                "$1.0 speak(#1)!",
                "$1.0 speak(?1)@"
        );

        n.startFPS(40f);

        NARHear.hear(n.main(), "a b c d e f g", null, 100, 0.1f);

        Util.sleepMS(5000);

        n.stop().tasks(true, false, true, false).forEach(System.out::println);

        System.out.println();




    }
}
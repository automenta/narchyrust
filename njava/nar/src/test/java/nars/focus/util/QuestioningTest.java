package nars.focus.util;

import nars.*;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestioningTest {

    @Test void questioning_before() {
        q1(true);
    }
    @Test void questioning_after() {
        q1(false);
    }

//    @Test void questioning_unify() {
//        //TODO
//    }


    private static void q1(boolean beforeOrAfter) {
        NAR n = NARS.tmp();
        Focus f = n.main();

        StringBuilder log = new StringBuilder(1024);
        Questioning q = new Questioning(f, 3) {
            @Override protected void answer(NALTask question, NALTask answer) {
                log.append(question).append("\t").append(answer).append("\n");
            }
        };

        Term x = $$("x");
        if (beforeOrAfter) {
            q.ask(x);
            n.believe(x);
        } else {
            n.believe(x);
            q.ask(x);
        }

        //TODO n.believe(x); //duplicate

        assertEquals("$0.0 x?\t$.50 x. %1.0;.90%\n", log.toString());
    }

}
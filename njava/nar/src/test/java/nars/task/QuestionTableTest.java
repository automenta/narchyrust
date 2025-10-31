package nars.task;

import nars.NAR;
import nars.NARS;
import nars.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.ETERNAL;
import static nars.Op.QUESTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuestionTableTest {

    static final Term x = $$("x");

    final NAR n = NARS.tmp();

    @Test void coexist_eternalTemporalQuestions() {

        n.ask(x, QUESTION, ETERNAL, ETERNAL);

        assertEquals(1, n.concept(x).questions().taskCount());

        n.ask(x, QUESTION, 0, 0);

        assertEquals(2, n.concept(x).questions().taskCount());

    }
}
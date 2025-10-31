package nars.term;

import nars.*;
import nars.util.Timed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VariableTest2 {
    @Test
    void testDestructiveNormalization() throws Narsese.NarseseException {
        String t = "<$x --> y>";
        String n = "($1-->y)";
        Timed timed = NARS.shell();
        Termed x = $.$(t);
        assertEquals(n, x.toString());


    }


    @Test
    void varNormTestIndVar() throws Narsese.NarseseException {


        NAR n = NARS.shell();

        String t = "<<($1, $2) --> bigger> ==> <($2, $1) --> smaller>>";

        Termed term = $.$(t);
        Task task = Narsese.task(t + '.', n);

        System.out.println(t);
        assertEquals("(bigger($1,$2)==>smaller($2,$1))", task.term().toString());
        System.out.println(term);
        System.out.println(task);


        Task t2 = n.inputTask(t + '.');
        System.out.println(t2);


        n.run(10);

    }


    @Test void seqConceptTerm() {
        NAR n = NARS.tmp();
        for (String s : new String[] {
                "(((--,Δ#2) &&+870 (--,(#1-->happy))) &&+750 #2)",
                "(a &&+1 b)",
                "(a &&+1 (b &&+1 c))",
                "(a &&+1 (#1 &&+1 c))",
                "(a &&+1 (#1 &&+1 #1))",
                "(a &&+1 (#1 &&+1 #2))"
        }) {
            var x = $.$$(s);
            var y = n.conceptTerm(x, false);
            var t = NALTask.TASKS(y, (byte)0, true);
            assertTrue(t, ()-> x + " -> " + y + " -> not TaskConcept");
        }
    }

}

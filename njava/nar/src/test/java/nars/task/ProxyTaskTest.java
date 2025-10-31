package nars.task;

import nars.$;
import nars.NALTask;
import nars.task.proxy.SpecialNegTask;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProxyTaskTest {

    @Test
    void testNegate() {
        var p = NALTask.task($$("r"), BELIEF, $.t(0.25f, 0.9f), ETERNAL, ETERNAL, new long[] { 0 });
        var n = SpecialNegTask.neg(p);
        assertEquals("$0.0 (--,r). %.75;.90%", n.toString());
        assertEquals("$0.0 r. %.25;.90%", n.the().toString());
    }
}
package nars.concept.dynamic;

import nars.Term;
import nars.TruthFunctions;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.t;
import static org.junit.jupiter.api.Assertions.*;

class DynDeltaTest extends AbstractDynTaskTest {

    @Test
    void test1() {
        n.time.dur(0);

        float c = 0.9f;
        float f0 = 0, f1 = 1;
        long s = 0, e = 1;

        n.believe("a", f0, c, s, s);
        n.believe("a", f1, c, e, e);

        Term dA = $$("/\\a");
        var deltaPoint = n.beliefTruth(dA, s, s);
        assertNull(deltaPoint, "timepoint: no delta available");

        var deltaRange = n.beliefTruth(dA, s, e);
        assertNotNull(deltaRange);

        //range
        float dF = TruthFunctions.delta(f0, f1);
        assertEquals(t(dF,  c*c).toString(), deltaRange.toString());

//        //verify focusing
//        var wider = n.belief(dA, 0, 2);
//        assertEquals(0, wider.start());
//        assertEquals(1, wider.end());
//        assertEquals(t(0, 0.67f).toString(), wider.truth().toString());

    }

}
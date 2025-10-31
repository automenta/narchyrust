package nars.concept.dynamic;

import jcog.Fuzzy;
import nars.Narsese;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TODO update to current <-> semantics */
@Disabled class DynSimTest extends AbstractDynTaskTest {

    @Test void dynSim1()  { assertSim(1f, 1f); }
    @Test void dynSim2()  { assertSim(0f, 1f); }

    private void assertSim(float a, float b)  {
        final long WHEN_KNOWN = ETERNAL;
        long WHEN_QUERY = n.time();

        n.believe("(x-->y)", a, 0.9f, WHEN_KNOWN, WHEN_KNOWN);
        n.believe("(y-->x)", b, 0.9f, WHEN_KNOWN, WHEN_KNOWN);

        final String xSimY = "(x<->y)";

        assertDynamicBeliefTable(xSimY);
        assertEquals(t(Fuzzy.and(a, b), 0.81f), n.beliefTruth($$(xSimY), WHEN_QUERY));
    }

    @Disabled
    @Test void goal() throws Narsese.NarseseException {
        n.want($("(x-->y)"), 1, 0.9f, Tense.Eternal);
        n.want($("(y-->x)"), 1, 0.9f, Tense.Eternal);

        final String xSimY = "(x<->y)";

        assertTrue(isDynamicGoalTable($$(xSimY)));
        assertEquals(t(Fuzzy.and(1, 1), 0.81f), n.goalTruth($$(xSimY), n.time()));
    }
}
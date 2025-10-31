package nars.concept.dynamic;

import nars.NALTask;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** TODO debug */
class DynImplContraTest extends AbstractDynTaskTest {

    @Test
    void contrapositionPosDyn() throws Narsese.NarseseException {
        assertDynContrapos("((--,B)==>A)", "(A ==> B)");
    }

    @Test
    void contrapositionPosTemporal1() throws Narsese.NarseseException {
        assertDynContrapos("((--,B) ==>-1 A)", "(A ==>+1 B)");
    }

    @Test
    void contrapositionPosTemporal2() throws Narsese.NarseseException {
        assertDynContrapos("((--,B) ==>-2 (A &&+1 C))", "((A &&+1 C) ==>+1 B)");
    }

    @Test
    void contrapositionPosTemporal3() throws Narsese.NarseseException {
        assertDynContrapos("((--,(B &&+1 C)) ==>-2 A)", "(A ==>+1 (B &&+1 C))");
    }

    @Test
    void contrapositionPosTemporal4() throws Narsese.NarseseException {
        assertDynContrapos("((--,(B &&+1 C)) ==>-3 (A &&+1 D))", "((A &&+1 D) ==>+1 (B &&+1 C))");
    }

    private void assertDynContrapos(String y, String x) throws Narsese.NarseseException {
        n.believe(x, 0.9f, 0.9f);


        NALTask b = n.belief(y);
        assertNotNull(b);
        assertEquals(y + ". %0.0;.45%", b.toStringWithoutBudget());
    }
}
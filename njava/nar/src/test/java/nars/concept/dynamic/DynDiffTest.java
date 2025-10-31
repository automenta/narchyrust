package nars.concept.dynamic;

import nars.Narsese;
import nars.Term;
import nars.table.BeliefTables;
import nars.table.dynamic.DynTruthBeliefTable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DynDiffTest extends AbstractDynTaskTest {

//    @Test
//    void testRawDifference() throws Narsese.NarseseException {
//        
//        n.believe("x", 0.75f, 0.50f);
//        n.believe("y", 0.25f, 0.50f);
//        n.run(1);
//        Term xMinY = $("(x ~ y)");
//        Term yMinX = $("(y ~ x)");
//        assertDynamicTable(xMinY);
//        assertDynamicTable(yMinX);
//        assertEquals(
//                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
//        );
//        assertEquals(
//                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
//        );
//
//    }

    @Disabled @Test void DiffUnion() throws Narsese.NarseseException {
        
        n.believe("c:x", 0.75f, 0.50f);
        n.believe("c:y", 0.25f, 0.50f);
        n.run(1);
        //Term xMinY = $("c:(x - y)"), yMinX = $("c:(y - x)");
        Term xMinY = $("c:(x & --y)"), yMinX = $("c:(y & --x)");

        assertNotNull(((BeliefTables) n.conceptualize(xMinY).beliefs()).tableFirst(DynTruthBeliefTable.class));
        assertNotNull(((BeliefTables) n.conceptualize(yMinX).beliefs()).tableFirst(DynTruthBeliefTable.class));
        assertEquals(
                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
        );
        assertEquals(
                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
        );
    }
    @Disabled
    @Test void DiffIntersection() throws Narsese.NarseseException {
        
        n.believe("c:x", 0.75f, 0.50f);
        n.believe("c:y", 0.25f, 0.50f);
        n.run(1);
        Term xMinY = $("c:(x - y)"), yMinX = $("c:(y - x)");
        assertNotNull(((BeliefTables) n.conceptualize(xMinY).beliefs()).tableFirst(DynTruthBeliefTable.class));
        assertNotNull(((BeliefTables) n.conceptualize(yMinX).beliefs()).tableFirst(DynTruthBeliefTable.class));
        assertEquals(
                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
        );
        assertEquals(
                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
        );
    }


}
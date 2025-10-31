package nars.concept.dynamic;

import nars.*;
import nars.table.BeliefTables;
import nars.table.dynamic.DynBeliefTable;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractDynTaskTest {
    protected final NAR n = NARS.tmp();

    final boolean isDynamicBeliefTable(String t) {
        return isDynamicBeliefTable($$(t));
    }

    final boolean isDynamicBeliefTable(Term t) {
        return dynTable(n, t, true)  instanceof DynBeliefTable;
    }

    final boolean isDynamicGoalTable(Term t) {
        return dynTable(n, t, false) instanceof DynBeliefTable;
    }

    final void assertDynamicBeliefTable(String s) {
        assertTrue(isDynamicBeliefTable(s));
    }


    static TaskTable dynTable(NAR n, Term t, boolean beliefsOrGoals) {
        Concept c = n.conceptualize(t);
        TaskTable b = beliefsOrGoals ? c.beliefs() : c.goals();
        return b instanceof BeliefTables bb ? bb.tableFirst(DynBeliefTable.class) : b;
    }

}
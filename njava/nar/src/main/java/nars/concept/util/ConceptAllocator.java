package nars.concept.util;

import nars.Concept;
import nars.TaskTable;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.LazyBeliefTable;
import nars.table.eternal.EternalTable;
import nars.term.Termed;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/** sets capacities for the various Concept features */
public final class ConceptAllocator implements Consumer<Concept> {


    private final ToIntFunction<Termed> beliefsEteCapacity;
    private final ToIntFunction<Termed> beliefsTempCapacity;
    private final ToIntFunction<Termed> goalsEteCapacity;
    private final ToIntFunction<Termed> goalsTempCapacity;
    private final ToIntFunction<Termed> questionsCapacity;
    private final ToIntFunction<Termed> questsCapacity;


    public ConceptAllocator(
            ToIntFunction<Termed> beliefsEteCapacity, ToIntFunction<Termed> beliefsTempCapacity,
            ToIntFunction<Termed> goalsEteCapacity, ToIntFunction<Termed> goalsTempCapacity,
            ToIntFunction<Termed> questionsCapacity, ToIntFunction<Termed> questsCapacity) {
        this.beliefsEteCapacity = beliefsEteCapacity;
        this.beliefsTempCapacity = beliefsTempCapacity;
        this.goalsEteCapacity = goalsEteCapacity;
        this.goalsTempCapacity = goalsTempCapacity;
        this.questionsCapacity = questionsCapacity;
        this.questsCapacity = questsCapacity;
    }

    @Override public final void accept(Concept c) {
        if (c instanceof TaskConcept) {
            apply((TaskConcept)c);
        }
    }


    private void setTaskCapacity(TaskConcept c, TaskTable x, boolean beliefOrGoal) {
        if (x instanceof BeliefTables)
            ((BeliefTables)x).tables.forEachWith((xx, C) -> _setTaskCapacity(C, xx, beliefOrGoal), c);
        else
            _setTaskCapacity(c, x, false);
    }

    private void _setTaskCapacity(TaskConcept c, TaskTable t, boolean beliefOrGoal) {
        t.taskCapacity(beliefCap(c, beliefOrGoal,
t instanceof LazyBeliefTable.EternalLazyBeliefTable || t instanceof EternalTable));
    }

    private void apply(TaskConcept c) {
        setTaskCapacity(c, c.beliefs(),true);
        setTaskCapacity(c, c.goals(),false);
        c.questions().taskCapacity(questionCap(c, true));
        c.quests().taskCapacity(questionCap(c, false));
    }


    private int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        return (beliefOrGoal ?
                (eternalOrTemporal ?
                        beliefsEteCapacity : beliefsTempCapacity) :
                (eternalOrTemporal ?
                        goalsEteCapacity : goalsTempCapacity)).applyAsInt(concept);
    }



    private int questionCap(TaskConcept concept, boolean questionOrQuest) {
        return (questionOrQuest ? questionsCapacity : questsCapacity).applyAsInt(concept);
    }


}
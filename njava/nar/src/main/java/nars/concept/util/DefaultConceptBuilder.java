package nars.concept.util;

import nars.BeliefTable;
import nars.Concept;
import nars.NAL;
import nars.Term;
import nars.concept.NodeConcept;
import nars.table.LazyBeliefTable;
import nars.table.LazyQuestionTable;
import nars.table.eternal.EternalTable;
import nars.table.question.HijackQuestionTable;
import nars.table.question.QuestionTable;
import nars.table.temporal.NBTemporalBeliefTable;

import java.util.function.Consumer;

public class DefaultConceptBuilder extends ConceptBuilder {

    private final Consumer<Concept> alloc;

    public DefaultConceptBuilder() {
        this(new ConceptAllocator(
                NAL.concept.beliefEternalCapacity,
                NAL.concept.beliefTemporalCapacity,
                NAL.concept.beliefEternalCapacity,
                NAL.concept.beliefTemporalCapacity,
                NAL.concept.questionCapacity,
                NAL.concept.questionCapacity
        ));
    }

    public DefaultConceptBuilder(Consumer<Concept> allocator) {
        this.alloc = allocator;
    }

    @Override protected Concept nodeConcept(Term t) {
        return new NodeConcept(t);
    }

    @Override public BeliefTable eternalTable(Term t) {
        return new LazyBeliefTable.EternalLazyBeliefTable(EternalTable::new);
        //return new EternalTable(0);
    }

    @Override
    public BeliefTable temporalTable(Term t, boolean beliefOrGoal) {
        return new LazyBeliefTable(
            NAL.belief.TEMPORAL_TABLE_ARRAY ?
                //nars.table.temporal.ArrayTemporalBeliefTable::new :
                NBTemporalBeliefTable::new :
                nars.table.temporal.NavigableMapBeliefTable::new
                //nars.table.temporal.NavigableMapBeliefTable2::new
        );
        //return new NavigableMapBeliefTable();
        //return new RTreeBeliefTable();
    }

    @Override public QuestionTable questionTable() {
        return new LazyQuestionTable(HijackQuestionTable::new);
        //return new HijackQuestionTable();
    }

    @Override
    public void start(Concept c) {
        super.start(c);
        alloc.accept(c);
    }


}
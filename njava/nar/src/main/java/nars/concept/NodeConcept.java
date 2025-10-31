package nars.concept;

import jcog.data.map.CompactArrayMap;
import nars.BeliefTable;
import nars.Concept;
import nars.NALTask;
import nars.Term;
import nars.table.EmptyBeliefTable;
import nars.table.question.QuestionTable;
import nars.term.Termed;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;



/** a 'blank' concept which does not store any tasks */
public class NodeConcept extends Concept {

    public final CompactArrayMap<String, Object> meta = new CompactArrayMap<>();

    public NodeConcept(Term term) {
        super(term);
    }

    @Override public BeliefTable beliefs() { return EmptyBeliefTable.Empty; }

    @Override public BeliefTable goals() { return EmptyBeliefTable.Empty; }

    @Override public QuestionTable questions() { return QuestionTable.Empty; }

    @Override public QuestionTable quests() { return QuestionTable.Empty; }

    @Override
    public final Term term() {
        return term;
    }

    @Override
    public Stream<NALTask> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        return Stream.empty();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || (obj instanceof Termed && term.equals(((Termed) obj).term()));
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final String toString() {
        return term.toString();
    }

    @Override
    public boolean delete() {
        if (this instanceof PermanentConcept)
            return false;

        Object[] c = meta.clearPut(DELETED);
        return c == null || c==DELETED;
    }

    @Override
    public final boolean isDeleted() {
        return !(this instanceof PermanentConcept) && meta.array() == DELETED;
    }

    private static final Object[] DELETED = { null, null };

    @Override
    public <X> X meta(String key, Function<String,X> valueIfAbsent) {
        return (X) meta.computeIfAbsent(key, valueIfAbsent);
    }

    @Override
    public <X> X meta(String key, Supplier<X> valueIfAbsent) {
        return (X) meta.computeIfAbsent(key, valueIfAbsent);
    }

    @Override
    public <X> X meta(String key, Object value) {
        return (X) meta.put(key, value);
    }

    @Override
    public <X> X meta(String key) {
        return (X)meta.get(key);
    }

    public static class FunctorConcept extends NodeConcept implements PermanentConcept {
        public FunctorConcept(Term term) { super(term); }
    }


}
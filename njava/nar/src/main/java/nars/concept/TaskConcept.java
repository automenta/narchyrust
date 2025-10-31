package nars.concept;

import jcog.Research;
import jcog.data.list.Lst;
import nars.*;
import nars.table.LazyQuestionTable;
import nars.table.question.QuestionTable;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.Float.NaN;
import static nars.Op.*;

public class TaskConcept extends NodeConcept  {

    private final BeliefTable/*s*/ beliefs, goals;
    private QuestionTable quests, questions;

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A target corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public TaskConcept(Term term,
                       BeliefTable beliefs, BeliefTable goals,
                       QuestionTable questions, QuestionTable quests) {
        super(term);
        assert(term.CONCEPTUALIZABLE());
        this.beliefs = beliefs;
        this.goals = goals;
        this.questions = questions;
        this.quests = quests;
    }

    public final TaskTable table(byte punc) {
        return table(punc, false);
    }

    public final TaskTable table(byte punc, boolean write) {
        return switch (punc) {
            case BELIEF -> beliefs();
            case GOAL -> goals();
            case QUESTION -> questions(write);
            case QUEST -> quests(write);
            default -> throw new UnsupportedOperationException("what kind of punctuation is: '" + punc + '\'');
        };
    }

    public final boolean remove(NALTask t) {
        return table(t.punc()).remove(t, false);
    }

    @Override
    public final QuestionTable quests() {
        return quests;
    }

    @Override
    public final QuestionTable questions() {
        return questions;
    }

    public final QuestionTable quests(boolean write) {
        var q = quests;
        return write && q instanceof LazyQuestionTable l ? (this.quests = l.get()) : q;
    }

    public final QuestionTable questions(boolean write) {
        var q = questions;
        return write && q instanceof LazyQuestionTable l ? (this.questions = l.get()) : q;
    }

    @Override
    public final BeliefTable beliefs() {
        return this.beliefs;
    }

    @Override
    public final BeliefTable goals() {
        return this.goals;
    }

    public void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, Consumer<Task> each) {
        if (includeConceptBeliefs && beliefs != null) beliefs.forEachTask(each);
        if (includeConceptQuestions && questions != null) questions.forEachTask(each);
        if (includeConceptGoals && goals != null) goals.forEachTask(each);
        if (includeConceptQuests && quests != null) quests.forEachTask(each);
    }

    public void forEachTask(Consumer<? super Task> each) {
        if (beliefs != null) beliefs.forEachTask(each);
        if (questions != null) questions.forEachTask(each);
        if (goals != null) goals.forEachTask(each);
        if (quests != null) quests.forEachTask(each);
    }

    @Override
    public Stream<NALTask> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        int c = 0;
        if (includeBeliefs) c++;
        if (includeGoals) c++;
        if (includeQuestions) c++;
        if (includeQuests) c++;
        assert (c > 0);

        List<TaskTable> tables = new Lst<>(c);

        if (includeBeliefs) tables.add(beliefs());
        if (includeGoals) tables.add(goals());
        if (includeQuestions) tables.add(questions());
        if (includeQuests) tables.add(quests());

        return tables.stream().flatMap(TaskTable::taskStream)
                //.filter(Objects::nonNull)
                ;
    }


    @Override
    public boolean delete() {
        if (super.delete()) {
            if (beliefs != null) beliefs.delete();
            if (goals != null) goals.delete();
            if (questions != null) questions.delete();
            if (quests != null) quests.delete();
            return true;
        }
        return false;
    }

    /** goal satisfaction relative to belief */
    @Research
    public double happy(long start, long end, float dur, NAR nar) {

        double b = beliefs().freq(start, end, dur, nar);
        if (b!=b) return NaN;

        double g = goals().freq(start, end, dur, nar);
        if (g!=g) return NaN;

        return (1 - Math.abs(b - g));
    }

    @Override
    public void print(Appendable out, boolean showbeliefs, boolean showgoals) {
        super.print(out, showbeliefs, showgoals);

        Consumer<Task> printTask = s -> {
            try {
                out.append(printIndent);
                out.append(s.toString());
                out.append(" ");
                out.append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        try {
            if (showbeliefs) {
                out.append(" Beliefs:");
                if (beliefs().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    beliefs().forEachTask(printTask);
                }
                out.append(" Questions:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    questions().forEachTask(printTask);
                }
            }

            if (showgoals) {
                out.append(" Goals:");
                if (goals().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    goals().forEachTask(printTask);
                }
                out.append(" Quests:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    quests().forEachTask(printTask);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
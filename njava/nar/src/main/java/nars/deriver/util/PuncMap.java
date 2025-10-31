package nars.deriver.util;

import nars.$;
import nars.Deriver;
import nars.Task;
import nars.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.PREDICATE;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static nars.Op.*;

/**
 * punc -> punc map
 */
public final class PuncMap<D> extends PREDICATE<Deriver> {

    private static final Atom PUNC = Atomic.atom("punc");

    private final byte belief;
    private final byte goal;
    private final byte question;
    private final byte quest;
    private final byte command;

    public PuncMap(byte belief, byte goal, byte question, byte quest, byte command) {
        super(id(belief, goal, question, quest, command));
        this.belief = belief;
        this.goal = goal;
        this.question = question;
        this.quest = quest;
        this.command = command;
    }

    public static byte p(BytePredicate enable, ByteToByteFunction p, byte b) {
        return enable.accept(b) ? p.valueOf(b) : 0;
    }

    private static Term id(byte belief, byte goal, byte question, byte quest, byte command) {
//        if (belief != 0 && goal != 0 && question!= 0 && quest!= 0 && command!= 0) {
//            return PUNC;
//        } else {
        java.util.Set<Term> s = new UnifiedSet<>(5);
        if (belief != 0) s.add(idTerm(Task.BeliefAtom, belief));
        if (goal != 0) s.add(idTerm(Task.GoalAtom, goal));
        if (question != 0) s.add(idTerm(Task.QuestionAtom, question));
        if (quest != 0) s.add(idTerm(Task.QuestAtom, quest));
        if (command != 0) s.add(idTerm(Task.CommandAtom, command));
        Term tt;
        //HACK
        tt = s.size() == 1 ? s.iterator().next() : SETe.the(s);

        return $.func(PUNC, tt);
//        }
    }

    private static Term idTerm(Atom inPunc, byte value) {
        var outPunc = punc(value);
        return inPunc == outPunc ? inPunc : $.p(inPunc, outPunc);
    }

    /**
     * total # of unique punctuation types accepted
     */
    public int size() {
        var c = 0;
        if (belief != 0) c++;
        if (goal != 0) c++;
        if (question != 0) c++;
        if (quest != 0) c++;
        if (command != 0) c++;
        return c;
    }

    public boolean all() {
        return size() == 5;
    }

    @Override
    public float cost() {
        return 0.002f;// * size; //TODO
    }

    @Override
    public boolean test(Deriver d) {
        var task = d.premise.task();
        return get(task!=null ? task.punc() : COMMAND) != 0;
    }

    public byte get(byte in) {
        return switch (in) {
            case BELIEF -> belief;
            case GOAL -> goal;
            case QUESTION -> question;
            case QUEST -> quest;
            case COMMAND -> command;
            default -> (byte)0;
        };
    }

    public boolean can(byte b) {
        return belief == b || goal == b || question == b || quest == b || command == b;
    }

}
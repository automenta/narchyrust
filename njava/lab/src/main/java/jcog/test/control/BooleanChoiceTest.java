package jcog.test.control;

import nars.$;
import nars.NAR;
import nars.Term;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;

public class BooleanChoiceTest extends MiniTest {

    private float reward;
    boolean prev = false;

    public BooleanChoiceTest(NAR n, BooleanBooleanPredicate goal) {
        this(n, $.atomic("x"), goal);
    }

    public BooleanChoiceTest(NAR n, Term action, BooleanBooleanPredicate goal) {
        super(n);

        actionPushButton(action, (next) -> {
           boolean c = goal.accept(prev, next);
           prev = next;
           reward = c ? 1f : 0f;
           return next;
        });

    }

    @Override
    protected float myReward() {
        float r = reward;
        reward = Float.NaN;
        return r;
    }

}
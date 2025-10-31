package nars.test.condition;

import nars.NALTask;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class LambdaTaskCondition extends TaskCondition {
    private final Predicate<NALTask> tc;

    public LambdaTaskCondition(Predicate<NALTask> tc) {
        this.tc = tc;
    }

    @Override public boolean matches(@Nullable NALTask task) {
        return tc.test(task);
    }

}

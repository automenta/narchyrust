package nars.time;

import java.util.Random;
import java.util.function.Predicate;

class LambdaTimeGraph extends TimeGraph {
    public Predicate<Event> solution;

    LambdaTimeGraph(Random rng) {
        this(null, rng);
    }

    LambdaTimeGraph(Predicate<Event> solution, Random rng) {
        super(rng);
        this.solution = solution;
    }

    @Override
    protected boolean solution(Event s) {
        return solution.test(s);
    }
}
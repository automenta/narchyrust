package jcog.test;

import jcog.agent.Agent;
import jcog.decide.Decide;
import jcog.decide.DecideEpsilonGreedy;
import jcog.random.XoRoShiRo128PlusRandom;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.junit.jupiter.api.TestReporter;

import java.util.Random;

/**
 * Generic MDP test abstraction
 *
 * https://github.com/openai/gym/tree/master/gym/envs
 * https://github.com/jmacglashan/burlap/tree/master/src/main/java/burlap/domain/singleagent
 *
 * TODO just use Game
 */
@Deprecated public abstract class AbstractAgentTest {

    final Random rng = new XoRoShiRo128PlusRandom();
    protected Decide deciding =
            //new DecideSoftmax(0.5f, rng);
            new DecideEpsilonGreedy(0.01f, rng);


    protected abstract void test(IntIntToObjectFunction<Agent> agentBuilder);

    public void after(TestReporter context) {
//        String message = String.format(
//                "%s '%s' took %d ms.",
//                unit, context.getDisplayName(), elapsedTime);
        //context.publishEntry("test" ,"x");

    }

}
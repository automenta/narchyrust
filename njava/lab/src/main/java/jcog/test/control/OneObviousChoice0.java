package jcog.test.control;

import jcog.agent.Agent;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.test.AbstractAgentTest;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OneObviousChoice0 extends AbstractAgentTest {

    static final boolean easy = false;
    @Override
    protected void test(IntIntToObjectFunction<Agent> agentBuilder) {

        int cycles = 4000;
        //for (int cycles : new int[] { 100, 1000, 10000 }) { //TODO further parameterize test
        Agent agent = agentBuilder.value(1, 2);
        assert (agent.inputs >= 1);
        assert (agent.actions == 2);


        Random r = new XoRoShiRo128PlusRandom();
        float nextReward = 0;
        IntIntHashMap acts = new IntIntHashMap();
        for (int i = 0; i < cycles; i++) {

            //TODO parameter for dimension, incl 0
            double[] distraction = {
                easy ? 0 : r.nextFloat()
            };

            int action = agent.actDiscrete(nextReward, distraction, deciding);

            acts.addToValue(action, 1);
            nextReward = switch (action) {
                case 0 -> 0.0f;
                case 1 -> +1.0f;
                default -> throw new UnsupportedOperationException();
            };
        }
        System.out.println(this.getClass().getSimpleName() + '\t' + agent.getClass() + ' ' + agent.summary() + '\n' + acts);
        assertTrue(acts.get(1) > acts.get(0));
        final float minRatio = 2f;
        assertTrue(acts.get(1) / minRatio > acts.get(0));
    }
}

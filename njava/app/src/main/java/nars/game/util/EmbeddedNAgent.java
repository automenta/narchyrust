package nars.game.util;

import jcog.agent.Agent;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Term;
import nars.deriver.impl.SerialDeriver;
import nars.game.Game;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;

import static java.lang.System.arraycopy;
import static java.util.stream.IntStream.range;

/**
 * wraps a complete NAR in an Agent interface for use with generic non-NAR 'Agent' MDP environments
 */
public class EmbeddedNAgent extends Agent {

    public final NAR nar;
    private final Atom id;

    private static NAR defaultNAR() {
//        NAL.DEBUG = true;

        NAR n = NARS.shell();
        n.complexMax.set(32);

        //n.freqResolution.set(0.1f);
        return n;
    }

    static final int DUR_CYCLES = 1;
    static final int iterationsPerCycle = 32;
    final double[] senseValue;
    final double[] q;
    public final Game game;
    private float nextReward = Float.NaN;


    public EmbeddedNAgent(int inputs, int actions) {
        this(defaultNAR(), inputs, actions);
    }

    public EmbeddedNAgent(NAR n, int numIn, int numAct) {
        super(numIn, numAct);

        this.id = Atomic.atom("agent");

        q = new double[numAct];
        senseValue = new double[numIn];

        n.time.dur(DUR_CYCLES);
        n.beliefConfDefault.set(0.5f);
        n.goalConfDefault.set(0.5f);
        //n.confMin.set(0.1f);

        n.add(this.game = new AgentGame(numIn, numAct));

        SerialDeriver d = new SerialDeriver(NARS.Rules.nal(1, 8).core().stm().temporalInduction().compile(n), n);
        d.everyCycle(game.focus());
        d.iter.set(iterationsPerCycle);

        this.nar = n;
//        env.focus().log();

    }


    @Override
    public void apply(double[] inputPrev, double[] actionPrev, float reward, double[] input, double[] actionNext) {

        this.nextReward = reward;

        arraycopy(input, 0, senseValue, 0, senseValue.length);

        game.nar().run(DUR_CYCLES);

//        double dex = env.dexterity();
//        if (dex > 0) {
//            System.out.println(dex + ", " +  Arrays.toString(q).replace("[", "").replace("]", "" ));
//        }

        arraycopy(q, 0, actionNext, 0, q.length);
    }

    private class AgentGame extends Game {
        private final int numIn;
        private final int numAct;


        AgentGame(int numIn, int numAct) {
            super(EmbeddedNAgent.this.id);
            this.numIn = numIn;
            this.numAct = numAct;
        }

        @Override
        protected void init() {
            range(0, numIn).forEach(i -> sense(EmbeddedNAgent.this.sense(i), () -> (float)senseValue[i]));

            for (int i = 0; i < numAct; i++) {
                int I = i;
                //$.p("action", Int.the(i))
                action(EmbeddedNAgent.this.action(i), (b, g) -> q[I] = g == null ? 0 : g.freq());

            }

//            addSensor(new SwitchAction(nar, (a) -> {
//                        nextAction = a;
//                        return true;
//                    }, range(0, numAct).mapToObj(i -> $.inh(id, $.p("action", Int.the(i)))).toArray(Term[]::new))
//            );

            reward(() -> nextReward);
        }


    }

    protected Term action(int i) {
        return $.inh(id,
                $.p("action", Int.i(i))
                //"action" + i
        );
    }

    protected Term sense(int i) {
        return $.inh(id,
                $.p("sense", Int.i(i))
                //"sense" + i
        );
    }
}
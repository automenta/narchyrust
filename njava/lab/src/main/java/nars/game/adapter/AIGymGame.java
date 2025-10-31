package nars.game.adapter;

import jcog.Log;
import jcog.Util;
import jcog.decide.Decide;
import jcog.io.Shell;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.ArraySensor;
import nars.$;
import nars.Player;
import nars.game.Game;
import nars.sensor.BitmapSensor;
import org.json.JSONArray;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static java.lang.Double.isFinite;

/**
 * wrapper for OpenAI Gym
 * https://github.com/Farama-Foundation/Gymnasium
 * <p>
 * TODO not completely working
 * pip install gymnasium[all]
 * pip install autorom[accept-rom-license]
 */
public class AIGymGame extends Game {

    public static void main(String[] args) throws IOException {
        Game g = new AIGymGame(
                "LunarLander-v2"
                //"BipedalWalker-v3"
                //"Pendulum-v1"
                //"Acrobot-v1"
                //"FrozenLake-v1"
                //"CliffWalking-v0"
                //"Taxi-v3"
                //"Blackjack-v1"

                //"CarRacing-v2"

                //"MiniGrid-Empty-6x6-v0"

                //"ALE/Breakout-v5"


                //"BeamRider-ram-v0"

                //"CrazyClimber-ram-v0"
                //"CrazyClimber-v0"

                //"CartPole-v0" //WARNING: infinity
                //"MountainCar-v0"
                //"DoomDeathmatch-v0" //2D inputs
                //"InvertedDoublePendulum-v1"

                //"Pong-v0"
                //"Pong-ram-v0"

                //"Hopper-v1"
                //"MsPacman-ram-v0"
                //"SpaceInvaders-ram-v0" //<---
                //"Hex9x9-v0"

                //https://github.com/Farama-Foundation/HighwayEnv
                //"highway-v0"
                //"parking-v0"
        );

        //RLPlayer.run(g, 0);
        new Player(g).start();
    }


    private static final Logger logger = Log.log(AIGymGame.class);

    private final String environmentID;
    private final PyShell shell;

    private boolean envReady = false, finished = false;

    private int inputs = -1, outputs = -1;
    private double[] low;
    private double[] high;
    private double[] input, output;
    private double reward;
    private double[] lastInput;
    private double[] outputActual;

    public AIGymGame(String environmentID) {
        super(environmentID);
        //super(environmentID, GameTime.durs(4));

        this.environmentID = environmentID;

        try {
            shell = new PyShell();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        shell.inputLines(
            "import gymnasium as gym",
            "import logging, json",
            "import numpy as np",
            "logging.getLogger().setLevel(logging.WARN)", //INFO and LOWER are not yet supported
            "def enc(x):\n\tprint('\\n')\n\treturn json.dumps(x)",
            "def encS(x):\n\tx.pop() ; x[0] = x[0].flatten().tolist() ; x[2] = int(x[2])\n\treturn json.dumps(x)",
            "env = gym.make('" + environmentID + "', render_mode=\"human\")",
            "env = gym.wrappers.flatten_observation.FlattenObservation(env)",
            "env.metadata['render_fps']=10000"
        );

        shell.input(
            "r=env.reset()[0]\n" +
            "r = r.flatten().tolist() if (not(type(r) is tuple)) else r\n" +
            "if (isinstance(env.observation_space, gym.spaces.Box)):\n"+
                "\tenc([env.observation_space.low.flatten().tolist(),env.observation_space.high.flatten().tolist(),str(env.action_space),r])\n" +
            "else:\n" +
                "\tenc([str(env.observation_space),str(env.action_space),r])\n"
//            "r=env.reset()\n" +
//            "enc([list(env.observation_space.values()),str(env.action_space)])\n"

        , this::onInput);
    }

    private boolean onInput(String f) {
        f = f.trim();

        //HACK is there a way to disable this in python
        while (f.startsWith(">>> ")) f = f.substring(4);

        if (f.startsWith("'")) {
            f = f.substring(1, f.length() - 1);

            if (!onJSON(f))
                throw new UnsupportedOperationException("not JSON: " + f);

            return true;
        }

        return false;
    }

    private void nextFrame() {
        System.arraycopy(output, 0, outputActual, 0, output.length);
        shell.input("encS(list(env.step(" +
            actionModel.toEnvironment(outputActual) +
            ")))", this::onInput);
    }

    @Override
    protected void sense() {
        nextFrame();
        if (finished)
            shell.input("env.reset()\n");

        super.sense();
    }

    private ActionModel actionModel;

    @Deprecated interface ActionModel {

        String toEnvironment(double[] o);

        int actions();
    }

    public static class DiscreteActionModel implements ActionModel {
        final int actions;
        private final Decide decide;

        DiscreteActionModel(String a) {
            this.actions = Integer.parseInt(a.substring(9, a.length() - 1));
            var rng = new XoRoShiRo128PlusRandom();
            this.decide =
                //new DecideRoulette(rng);
                //new DecideSoftmax(0.1f, rng);
                Decide.Greedy;
                //new DecideEpsilonGreedy(0.1f, rng);
        }

        @Override
        public String toEnvironment(double[] o) {
            int which = decide.applyAsInt(o);
            for (int i = 0; i < o.length; i++) {
                o[i] = (i == which) ? 1 : 0;
            }
            return Integer.toString(which);
        }

        @Override
        public int actions() {
            return actions;
        }
    }

    public static class BoxActionModel implements ActionModel {

        double[][] bounds;
        final int actions;

        BoxActionModel(String a) {

//            if (a.endsWith(",)")) {
//                int dims = Integer.parseInt(a.substring(4, a.length() - 2));
//                if (dims != 1)
//                    throw new UnsupportedOperationException(a);

            a = a.substring(4, a.length()-1);
            var aa = a.split(", ");
            int dims = Integer.valueOf(aa[2].substring(1, aa[2].length()-2));
            double low = Double.valueOf(aa[0]);
            double high = Double.valueOf(aa[1]);

            actions = dims;
            bounds = new double[dims][2];
            for (int i = 0; i < dims; i++) {
                bounds[i][0] = low;
                bounds[i][1] = high;
            }
        }

        @Override
        public String toEnvironment(double[] o) {
            //NORMALIZE to Box bounds
            double[] oo = o.clone();
            for (int i = 0; i < oo.length; i++)
                oo[i] = Util.lerpSafe(oo[i], bounds[i][0], bounds[i][1]);

            return "np.array(" + Arrays.toString(oo) + ")";
        }

        @Override
        public int actions() {
            return actions;
        }
    }

    private boolean onJSON(String f) {
        JSONArray j = (JSONArray) pyjson(f);
        if (j == null)
            return false;

        if (inputs < 0 && (f.contains("Discrete") || f.contains("Box"))) {
            jsonInit(j);
        } else {
            jsonUpdate(j);
        }

        normalizeInput(input);

        return true;
    }

    private void jsonInit(JSONArray j) {
        //first cycle, determine model parameters
        input = asArray(j, 3);
        inputs = input.length;

        low = asArray(j, 0);
        high = asArray(j, 1);
        //restore +-Infinity HACK
        for (int i = 0; i < inputs; i++) {
            if (low[i] == high[i]) {
                throw new UnsupportedOperationException();
//                    low[i] = Double.NEGATIVE_INFINITY;
//                    high[i] = Double.POSITIVE_INFINITY;
            }
        }

        String a = j.get(2).toString();
        if (a.startsWith("Discrete")) {
            actionModel = new DiscreteActionModel(a);
        } else if (a.startsWith("Box")) {
            actionModel = new BoxActionModel(a);
        } else {
            throw new UnsupportedOperationException("Unknown action_space type: " + a);
        }

        initGym();
    }

    private void jsonUpdate(JSONArray j) {
        //ob, reward, done, _
        //TODO dont re-allocate arrays
        input = asArray(j, 0);
        reward = asDouble(j, 1);
        finished = j.getInt(2) == 1;
    }

    private void initGym() {

        addSensor(new BitmapSensor(new ArraySensor(inputs, false) {
            @Override
            protected float value(int i) {
                return (float) input[i];
            }
        }, id));

        outputs = actionModel.actions();
        output = new double[outputs];
        outputActual = new double[outputs];
        for (int o = 0; o < outputs; o++) {
            int O = o;
            action($.inh(id, $.p("act", o)),
                a -> (float) (output[O] = a),
                f -> (float) outputActual[O]);
        }

        rewardPercentile($.inh(id, "reward"), () -> (float) reward);
        envReady = true;

    }

    private double[] normalizeInput(double[] x) {
        //normalize input to low/high range
        double[] low = this.low;
        double[] high = this.high;
        for (int i = 0; i < x.length; i++) {
            double l = low[i], h = high[i];
            if (isFinite(l) && isFinite(h)) {
                x[i] = Util.lerp(x[i], l, h);
            }
        }
        return x;
    }

    private static Object pyjson(String j) {
        j = j.replace("Infinity", "NaN" /*"1"*/); //HACK

        try {
            //doesnt handle Inf
            return new JSONArray(j);
            //return JSONObject.stringToValue(j);
            //return ((JsonArray) Json.parse(j));
        } catch (Exception e) {
            System.err.println("can not parse: " + j);
            e.printStackTrace();
        }
        return null;

    }

    private static double[] asArray(JSONArray j, int index) {
        return asArray(j.getJSONArray(index));
    }

    private static double[] asArray(JSONArray x) {
        double[] y = new double[x.length()];
        for (int i = 0; i < y.length; i++)
            y[i] = x.getDouble(i);
        return y;
    }

    private static double asDouble(JSONArray j, int index) {
        return j.getDouble(index);
    }

    /** interface to a python sub-process */
    static class PyShell extends Shell {

        @Deprecated private final AtomicReference<Predicate<String>> nextLineConsumer = new AtomicReference(null);

        PyShell() throws IOException {
            super("python",
                "-i" /* interactive */,
                "-q" /* quiet */,
                "-u" /* unbuffered */,
                "-X utf8"
            );
        }

        public void input(String line) {
            println(line);
        }

        /** blocking I/O operation HACK */
        public void input(String line, Predicate<String> result) {
            var prev = nextLineConsumer.getAndSet(result);
            if (prev != null)
                throw new UnsupportedOperationException();

            input(line);

            while (nextLineConsumer.get() != null)
                Thread.yield();
                //Util.sleepMS(1); //HACK
        }

        @Override
        protected void readln(String line) {
            super.readln(line);

            if (nextLineConsumer != null) /* HACK since it can be called from super class constructor */ {
                Predicate<String> c = nextLineConsumer.get();
                if (c != null) {
                    if (c.test(line))
                        nextLineConsumer.set(null);
                }
            }
        }

        public void inputLines(String... lines) {
            for (var x : lines)
                input(x + "\n");
        }

    }

}
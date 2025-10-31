package nars.experiment;

import jcog.Util;
import jcog.exe.Exe;
import jcog.lab.Lab;
import jcog.lab.Optimize;
import nars.$;
import nars.Player;
import nars.control.DefaultBudget;
import nars.game.Game;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;

public enum PlayerOptimize2 {
    ;

    /* TODO input param */ @Deprecated static int runSeconds = 2 * 60;
    /* TODO input param */ @Deprecated static int repeats = 1;
    /* TODO input param */ @Deprecated static float fps = 50;

    private static final int trials = 1;
    private static final int threads = Math.max(1, Runtime.getRuntime().availableProcessors()-1);

    public static void main(String[] args) {
        try {
            // Read JSON parameters from stdin
            /* ex:
            {
                "eternalization": 0.25,
                "durMax": 32,
                "durShift": 48,
                "complexMax": 40,
                "certain": 1.5,
                "focusTasks": 256,
                "focusLinks": 320,
                "clusterCap": 64,
                "confMin": 0.0001,
                "freqRes": 0.01
            }
             */
            var reader = new BufferedReader(new InputStreamReader(System.in));
            var jsonInput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }
            var params = new JSONObject(jsonInput.toString().trim());
            System.out.println(params);

            Function<String, Object> par = params::get;
            var e = new Lab<>(() -> player(
                new Tetris.Dotris($.$$("t"), 5, 12).fallTime(1)
                //new Tetris().fallTime(3)
                //new PoleCart($.$$("p"), false)
            ))
                    //.sense("concepts", (Player x) -> x.nar.memory.size())
                    //.sense("cmplexMean", (Player x) -> x.nar.concepts().mapToInt(z -> z.term.complexity()).average().getAsDouble())
                    //.sense("confMean", (Player x) -> x.nar.tasks(true, false, true, false).mapToDouble(Truthed::conf).average().getAsDouble())
                    .is("eternalization", par, (Player p, float v) -> p.eternalization = v)
                    .is("durMax", par, (Player p, int v) -> p.durMax = v)
                    .is("durShift", par, (Player p, int v) -> p.durShift = v)
                    .is("complexMax", par, (Player p, int c) -> p.complexMax = c)
                    .is("certain", par, (Player p, float v) -> p.ready(N -> p.games(g -> ((DefaultBudget)g.focus().budget).certain.set(v))))
                    .is("focusConcepts", par, (Player p, int v) -> p.focusConcepts = v)
                    .is("confMin", par, (Player p, float v) -> p.confMin = v)
                    .is("freqRes", par, (Player p, float v) -> p.freqRes = v)
                    .is("clusterCap", par, (Player p, int v) -> p.clusterCap = v);

            var o = e.optimize(repeats, PlayerOptimize2::experiment, new Optimize.OptimizationStrategy() {
                        @Override
                        public void run(Optimize oo) {
                            oo.eval(oo.mid); //run the midpoint of all values
                        }
                    })
                    .run(trials);
            var r = o.data.row(0);

            // Output results as JSON to stdout
            var result = new JSONObject();
            result.put("score", r.getDouble("score"));
            result.put("eternalization", r.getDouble("eternalization"));
            result.put("durMax", r.getDouble("durMax"));
            result.put("durShift", r.getDouble("durShift"));
            result.put("complexMax", r.getDouble("complexMax"));
            result.put("certain", r.getDouble("certain"));
            result.put("focusTasks", r.getDouble("focusTasks"));
            result.put("focusLinks", r.getDouble("focusLinks"));
            result.put("confMin", r.getDouble("confMin"));
            result.put("freqRes", r.getDouble("freqRes"));
            result.put("clusterCap", r.getDouble("clusterCap"));

            //sensors
//            result.put("complexMean", r.getDouble("complexMean"));
//            result.put("concepts", r.getDouble("concepts"));
//            result.put("confMean", r.getDouble("confMean"));

            //meta
            result.put("timestamp", System.currentTimeMillis());
            result.put("machine", System.getProperty("os.name") + "-" + Runtime.getRuntime().availableProcessors());

            System.err.println("EXPERIMENT_OUTPUT=" + result);

        } catch (Exception e) {
            // Output error as JSON to stdout
            var errorResult = new JSONObject();
            errorResult.put("error", e.getMessage());
            System.err.println(errorResult);
            System.exit(1);
        } finally {
            ((ForkJoinPool) Exe.executor()).shutdownNow();
            System.exit(0);
        }
    }

    private static Player player(Game g) {
        var p = new Player(g).fps(fps);
        p.exeAffinity = false;
        p.uiTop = false;
        if (threads > 0)
            p.threads = threads;
        return p;
    }

    private static double experiment(Supplier<Player> P) {
        try (var p = P.get()) {
            p.start();
            Util.sleepS(runSeconds);
            return p.rewardMean();
        }
    }
}

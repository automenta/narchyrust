package nars;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import jcog.data.list.Lst;
import jcog.io.Serials;
import jcog.table.DataTable;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.Source;
import tech.tablesaw.io.csv.CsvReader;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.stream.Collectors.toSet;

/**
 * Player performance metaheuristic search
 */
public class PlayerPlayer {

    static final String outputPath = "/home/me/narl";
    static final String jar = "/home/me/narl/n.jar";
    private static final String JAVA_HOME =
        "/home/me/jdk-21";
        //"/home/me/graalvm-ce-java17-22.3.0-dev";

    static String[] ignore = { "rewardtracefile" };
    static final String target = "rewardMean";

    public static void main(String[] args) {

        int repeats = 4;
        int timeLimitMinutes = 10;
        String RAM =
            //"2g";
            "1g";
            //"512m";

        int threads = 8;
        float df =
            0.01f;
            //0.02f;
        int uifps = 2;

        boolean META = true;

        boolean PROCEDURAL = true;
        boolean DELTA = true;
        boolean STRUCTURAL = true;
        boolean STRUCTURAL_optional = false;

        /** validation */
        boolean REASON_optional =
            true;
            //false;

        String[] games = {
            "PoleCart",
            "Tetris$One",
            "ArkaNAR",
            "NARio",
            //"Gradius",

//            "Tetris$Multi"

            //"Tetris$MultiEasy",
            /*"Chimera"*/
        };

        for (int i = 0; i < repeats; i++) {

            for(String game : games) {

                for (boolean reason : REASON_optional ? new boolean[]{true, false} : new boolean[] { true } ) {
                for(int volmax : new int[] { 28 /*28, 20, 36*/ }) {
                    for (float ete : new float[]{ 1/50f })
                        /*for (float durMax : new float[] { 32, 1, 2, 16, 64, 128, 256 })*/
                        //for (int taskbagderiver_subiter : new int[] { 4, 8, 12, 16, 20, 24 })
                    /*for (float patience : new float[]{ 32 })*/ {
                        for (float beliefConf : new float[]{ 0.9f /*0.25f, 0.5f, 0.75f, 0.9f*/}) {
                            for (float goalConf : new float[]{ 0.9f /*0.25f, 0.5f, 0.75f, 0.9f*/}) {

                                boolean procedural = reason && PROCEDURAL;
                                for (boolean structural : reason && STRUCTURAL ?
                                        (STRUCTURAL_optional ? new boolean[]{true, false} : new boolean[]{true})
                                        :
                                        new boolean[]{false}
                                ) {//STRUCTURAL ? new boolean[]{STRUCTURAL && procedural /*true, false*/} : new boolean[]{false}) {
                                    for (boolean delta : new boolean[]{reason && DELTA}) {

                                        ProcessBuilder b = new ProcessBuilder();

                                        var e = b.environment();

                                        //e.put("jarhash", hash(jar));
                                        e.put("jartime", Long.toString(time(jar)));

                                        e.put("procedural", Boolean.toString(procedural));
                                        e.put("structural", Boolean.toString(structural));
                                        e.put("delta", Boolean.toString(delta));

                                        e.put("beliefconf", Float.toString(beliefConf));
                                        e.put("goalconf", Float.toString(goalConf));

                                        e.put("volmax", Integer.toString(volmax));

                                        //e.put("TaskBagDeriver.SUBITER", Integer.toString(taskbagderiver_subiter));
                                        //e.put("durMax", Float.toString(durMax));

                                        //e.put("patience", Float.toString(patience));

                                        //e.put("goalspread", Boolean.toString(goalSpread));

                                        boolean meta = reason && META;
                                        e.put("meta", Boolean.toString(meta));

                                        e.put("eternalization", Float.toString(ete));

                                        e.put("uifps", Integer.toString(uifps));
                                        e.put("df", Float.toString(df));

                                        e.put("threads", Integer.toString(threads));

                                        int timelimitMS = timeLimitMinutes * 60 * 1000;
                                        e.put("timelimit", Integer.toString(timelimitMS));

                                        e.put("rewardtrace", Boolean.toString(true)); //NECESSARY
                                        e.put("rewardtracefile", outputPath + "/" + game + "." + System.currentTimeMillis() + ".json");

                                        try {
                                            b.command(
                                                    JAVA_HOME + "/bin/java",
                                                    "-da", "-dsa",

                                                    "-XX:MaxGCPauseMillis=20",
                                                    //"-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",

                                                    "-Xmx" + RAM,
                                                    "-cp", jar,
                                                    "nars.experiment." + game
                                            );
                                            //System.out.println(b.command());
                                            //System.out.println(e);
                                            //                    b.redirectError(new File("/tmp/e"));
                                            //                    b.redirectOutput(new File("/tmp/o"));
                                            b.inheritIO();
                                            Process p = b.start();
                                            //System.out.println("starting " + p.pid());
                                            int result = p.waitFor();

                                        } catch (IOException | InterruptedException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    private static String hash(String jar) {
        try {
            return Hashing.sha512().hashBytes(Files.readAllBytes(Path.of(jar))).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static long time(String jar) {
        return Path.of(jar).toFile().lastModified();
    }

//    public static void main(String[] args) throws Exception {
//        var d = new CsvReader().read(new Source(new File(
//                "/home/me/d/t.csv")));
//        System.out.println(d.printAll());
//        d = d.removeColumns("rewardtracefile","structural", "procedural");
//        var t = new TableDecisionTree(d, "rewardMean",
//                3, 2);
//        //t.print();
//        t.printExplanations();
//    }

    static class PlayerPlayerAnalyzer {


        static final CsvWriteOptions csv = CsvWriteOptions
                .builder(System.out)
                //.usePrintFormatters(true)
                .quoteChar('"')
                .quoteAllFields(true)
                .build();

        public static void main(String[] args) throws IOException {
            File f = new File(outputPath);

            List<JsonNode> experiments = new Lst();
            for (File experimentFiles : f.listFiles()) {
                if (experimentFiles.getName().endsWith(".json")) {
                    try {
                        String x = Files.readString(experimentFiles.toPath());
                        var j = Serials.jsonNode(x);
                        System.out.println(j);
                        if (j.isEmpty())
                            continue;
                        experiments.add(j);
                    } catch (IOException e) {
                        e.printStackTrace();
                        //throw new RuntimeException(e);
                    }
                }
            }
            Set<String> cols = new TreeSet();
            for (JsonNode x : experiments)
                x.fieldNames().forEachRemaining(cols::add);

            Set<String> colsInteresting = cols.stream().filter((String z) -> {
                Object first = null;
                for (var x : experiments) {
                    JsonNode next = x.get(z);
                    if (first == null) {
                        first = next;
                    } else {
                        if (!first.equals(next))
                            return true; //some difference
                    }
                }
                return false; //all equal
            }).collect(toSet());

            int colsInterestingCount = colsInteresting.size();
            if (colsInterestingCount < 2)
                throw new UnsupportedEncodingException();

            var s = new StringBuilder()
                .append(Joiner.on(",").join(colsInteresting)).append('\n');

            for (var x : experiments) {
                int i = 0;
                for (String c : colsInteresting) {
                    s.append(colName(x.get(c)));
                    if (++i < colsInterestingCount)
                        s.append(',');
                }
                s.append('\n');
            }
//            System.out.println(s);

            var d = new CsvReader().read(new Source(new StringReader(s.toString())));
            d = d.removeColumns(ignore);

            try {
                for (var ddd : d.splitOn("games").getSlices())
                  analyzeTable(ddd.asTable());
            } catch (IllegalStateException e) {
                analyzeTable(d);
            }
        }

        private static void analyzeTable(Table d) {
            //TODO maybe split into N separate tables by Game
            d = DataTable.collapseEqualColumns(d).sortDescendingOn(target);

            System.out.println(d.printAll());
            //new CsvWriter().write(d/*D*/, csv);

//            if (d.rowCount() > 2) {
//                var t = new TableDecisionTree(d, target, 3, 2);
//                t.print();
//                t.printExplanations();
//            }

            //window(TsneRenderer.view(d, MetalBitSet.bits(4).set(0,2,3)), 800, 800);
            System.out.println();
        }

        private static String colName(JsonNode n) {
            String nn = n == null ? "" : n.toString();

            nn = nn.replaceAll("\"","")
                   .replaceAll(",","");

            if (nn.contains(" ")) //TODO other characters
                nn = "\"" + nn + "\""; //quote it
            return nn;
        }
    }
}
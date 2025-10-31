//package nars;
//
//import com.google.common.reflect.Reflection;
//import jcog.Log;
//import jcog.Str;
//import jcog.TODO;
//import jcog.decision.TableDecisionTree;
//import jcog.lab.Optimize;
//import jcog.table.DataTable;
//import jcog.util.ArrayUtil;
//import org.gridkit.nanocloud.CloudFactory;
//import org.gridkit.nanocloud.RemoteNode;
//import org.junit.jupiter.engine.JupiterTestEngine;
//import org.junit.platform.engine.TestExecutionResult;
//import org.junit.platform.engine.TestSource;
//import org.junit.platform.engine.support.descriptor.MethodSource;
//import org.junit.platform.launcher.Launcher;
//import org.junit.platform.launcher.TestExecutionListener;
//import org.junit.platform.launcher.TestIdentifier;
//import org.junit.platform.launcher.TestPlan;
//import org.junit.platform.launcher.core.LauncherConfig;
//import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
//import org.junit.platform.launcher.core.LauncherFactory;
//import org.slf4j.Logger;
//import tech.tablesaw.api.*;
//import tech.tablesaw.columns.Column;
//import tech.tablesaw.columns.numbers.NumberColumnFormatter;
//
//import java.io.File;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadLocalRandom;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;
//import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
//import static tech.tablesaw.aggregate.AggregateFunctions.countFalse;
//import static tech.tablesaw.aggregate.AggregateFunctions.mean;
//
/// /import org.slf4j.Logger;
/// /import org.slf4j.LoggerFactory;
//
////import org.slf4j.Logger;
//
////import org.pitest.classinfo.ClassName;
//
///** periodically, or event-triggered: runs unit tests and broadcasts results */
//public class SelfTest {
//
//
//    /** local launcher */
//    public static void main(String[] args) {
//        SelfTest s = new SelfTest();
//        s.unitTestsByPackage("nars.nal.nal1");
//        s.unitTestsByPackage("nars.nal.nal2");
//        s.unitTestsByPackage("nars.nal.nal3");
//        s.unitTestsByPackage("nars.nal.nal4");
//        s.unitTestsByPackage("nars.nal.nal5");
//        s.unitTestsByPackage("nars.nal.nal6");
//        s.unitTestsByPackage("nars.nal.nal7");
//        s.unitTestsByPackage("nars.nal.nal8");
//        s.run(16);
//    }
//
//
//    //TODO make this a Bag
//    //final FastCoWList<Supplier> experiments = new FastCoWList<>(Supplier[]::new);
//    private final CopyOnWriteArrayList<Supplier> experiments = new CopyOnWriteArrayList();
//
//    SelfTest() {
//
//    }
//
//    private final JupiterTestEngine engine = new JupiterTestEngine();
//    //https://junit.org/junit5/docs/current/user-guide/#launcher-api-listeners-reporting
//    private final LauncherConfig launcherConfig = LauncherConfig.builder()
//            .enableTestEngineAutoRegistration(false)
//            //.enableTestExecutionListenerAutoRegistration(false)
//            //.addTestEngines(new CustomTestEngine())
//            .addTestEngines(engine)
//            //new LegacyXmlReportGeneratingListener(reportsDir, out)
//            .build();
//
//
//    /** ex: "./nar/target/test-classes/" */
//    void addClassPath(File classes) {
//        experiments.add(unitTests(b-> b.selectors(
//                selectClasspathRoots(Set.of(classes.getAbsoluteFile().toPath()))
//        )));
//    }
//
//    private void unitTestsByPackage(String... packages) {
//        experiments.add(unitTests(b->{
//            for (String pkg : packages)
//                b.selectors(selectPackage(pkg));
//        }));
//    }
//
//    private Supplier<Table> unitTests(Consumer<LauncherDiscoveryRequestBuilder> selector) {
//
//        Launcher lf = LauncherFactory.create(launcherConfig);
//
//        LauncherDiscoveryRequestBuilder b = LauncherDiscoveryRequestBuilder.request();
//        selector.accept(b);
//        TestPlan tp = lf.discover(b.build());
//
//        return ()->{
//            DataTable out = newTable();
//            lf.execute(tp, new MyTestExecutionListener(out));
//            return out;
//        };
//    }
//
////    static private int _jupiterPrefixToRemove = "[engine:junit-jupiter]/[class:".length();
////
////    protected static String sid(TestIdentifier id) {
////        String uid = id.getUniqueId();
////        try {
////            return "(" + uid.substring(_jupiterPrefixToRemove).replace("]/[method:", "/").replace("]", "").replace("()","")
////                    .replace("/",",").replace(".",",") + ")";
////        } catch (Exception e) {
////            return uid;
////        }
////
////    }
//
//
////    public static class TestMetrics implements Serializable {
////
////        /** unixtime */
////        public final long when;
////
////        public final String id;
////        public long wallTimeNS;
////
////        public boolean success, error;
////
////        public TestMetrics(TestIdentifier testIdentifier) {
////            this.when = System.currentTimeMillis();
////            this.id = sid(testIdentifier);
////
////        }
////
////        @Override
////        public String toString() {
////            return id +
////                    (success ? " sccs " : " fail ") + (error ? " err " : " ") +
////                    " @ " + new Date(when) + " .. " +
////                    Texts.timeStr(wallTimeNS);
////        }
////    }
//
//
//    private static DataTable newTable() {
//        DataTable d = new DataTable();
//        d.addColumns(
//                StringColumn.create("package"),
//                StringColumn.create("class"),
//                StringColumn.create("method"),
//                LongColumn.create("start"),
//                BooleanColumn.create("success"),
//                BooleanColumn.create("error"),
//                LongColumn.create("wallTimeNS")
//        );
//        return d;
//    }
//
//    private static class MyTestExecutionListener implements TestExecutionListener {
//
//        final Table out;
//        transient long startNS;
//        transient long endNS;
//        private long startUnixTime;
////        transient private TestMetrics m;
//
//        MyTestExecutionListener(Table results) {
//            this.out = results;
//        }
//
//        @Override
//        public void executionStarted(TestIdentifier testIdentifier) {
//
//            this.startUnixTime = System.currentTimeMillis();
//            this.startNS = System.nanoTime();
//        }
//
//        @Override
//        public void executionFinished(TestIdentifier id, TestExecutionResult result) {
//
//            this.endNS = System.nanoTime();
//
//            boolean success = false, error = false;
//            long wallTimeNS = (endNS - startNS);
//            switch (result.getStatus()) {
//                case SUCCESSFUL -> {
//                    success = true;
//                    error = false;
//                }
//                case FAILED -> {
//                    success = false;
//                    error = false;
//                }
//                case ABORTED -> {
//                    success = false;
//                    error = true;
//                }
//            }
//
//
//
//            TestSource src = id.getSource().orElse(null);
//            if (src==null)
//                return;
//
//            if (src instanceof MethodSource m) {
//                //ClassName.fromString(m.getClassName()).getNameWithoutPackage().toString();
//                String cl = m.getClassName();
//
//                assert(!cl.isEmpty());
//                String pk = Reflection.getPackageName(m.getClassName());
//                assert(!pk.isEmpty());
//                String me = m.getMethodName() +
//                        (!m.getMethodParameterTypes().isEmpty() ? ('(' + id.getDisplayName() + ')') : "");
//                assert(!me.isEmpty());
//                synchronized (out) {
//                    ((DataTable)out).add(pk, cl, me, startUnixTime, success, error, wallTimeNS);
//                }
//            } else {
//                //TODO / ignore
//            }
//
//
//
//        }
//    }
//
//
//
//
//
//
//
//    void run(int repeats) {
//
////        static {
//
//            Log.off();
////        }
//
//        Table all = newTable();
//
//        Runtime.getRuntime().addShutdownHook(new Thread(()->{
////            try {
//                synchronized (all) {
//                    report(all);
////                    save(all, "/home/me/d/tests1.csv");
//                }
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
//        }));
//
//        ExecutorService exe =
//            //ForkJoinPool.commonPool();
//            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//
//        Supplier<Table>[] experiments = this.experiments.toArray(new Supplier[0]); //array();
//        for (int i = 0; i < repeats; i++) {
//
//            ArrayUtil.shuffle(experiments, ThreadLocalRandom.current());
//
//            for (Supplier<Table> experiment : experiments) {
//                exe.execute(() -> {
//                    Table d = experiment.get();
//                    List<Column<?>> cols = d.columns();
//                    int colsSize = cols.size();
//
////                    exe.execute(()->{
//                        synchronized (all) {
//                            for (int c = 0; c < colsSize; c++) //HACK
//                                all.column(c).append((Column) cols.get(c));
//                        }
////                    });
//                });
//            }
//        }
//
//        try {
//            exe.shutdown();
//            exe.awaitTermination(3600, null);
//            //exe.awaitTermination(3600, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//
//    }
//
//    private static final Logger logger = Log.log(SelfTest.class);
//
////    private static void save(Table d, String filename) throws IOException {
////        synchronized (d) {
////            FileOutputStream fos = new FileOutputStream(filename, true);
////            GZIPOutputStream os = new GZIPOutputStream(new BufferedOutputStream(fos, 64 * 1024));
////            logger.info("appending {} rows to {} ({})", d.rowCount(), filename, os.getClass());
////            d.write().csv(os);
////            os.flush();
////            os.close();
////            logger.info("saved {}", filename);
////
////
////        }
////
////    }
//
//    private void report(Table all) {
//
//        String[] testID = all.columns(0, 1, 2).stream().map(Column::name).toArray(String[]::new);
//
//        //https://jtablesaw.github.io/tablesaw/userguide/reducing
//        try {
//
//            Table walltimes = all.summarize("wallTimeNS", mean/*, stdDev*/).by(testID)
//                    .sortDescendingOn("Mean [wallTimeNS]")
//                    ;
//
//            ((NumberColumn)walltimes.column(3)).setPrintFormatter(new NSTimeFormatter());
//
//            System.out.println(walltimes.printAll());
//
//        } catch (RuntimeException t) {
//            //TODO why might it happen
//            t.printStackTrace();
//        }
//
//
//        Table ffAll = all.summarize("success", countFalse).by(testID);
//
//        Table ff = ffAll.where(((NumberColumn)ffAll.column(3)).isGreaterThan(0));
//
//        Table fails = ff.sortDescendingOn(ff.column(3).name());
//
//        System.out.println(fails.printAll());
//        System.out.println("failures: " + ff.summary());
//
//
//        TableDecisionTree tr = Optimize.tree(
//            ff,
//            2, 6, 3);
//        System.out.println(tr);
////        tr.print();
//        tr.printExplanations();
//
////        NAR n = NARS.tmp();
////        n.log();
////        Atom SUCCESS = Atomic.atom("success");
////        d.forEach(r->{
////            String traw = r.getString(0);
////            //String t = traw.substring(1, traw.length()-1);
////            Term tt = $.$$(traw);
////            switch (r.getString(2)) {
////                case "Success":
////                    Task b = n.believe($.inh(tt, SUCCESS));
////                    System.out.println(b);
////                    break;
////                case "Fail":
////                    n.believe($.inh(tt, SUCCESS).neg());
////                    break;
////            }
////        });
////        n.run(1000);
//    }
//
//
//    public static class RemoteLauncher {
//        public static void main(String[] args) {
//            var cloud = CloudFactory.createCloud();
//
//            RemoteNode rn = RemoteNode.at(cloud.node("eus")).useSimpleRemotingForLegacyEngine();
//            rn.setRemoteAccount("me");
//            rn.setRemoteJavaExec("/home/me/jdk/bin/java");
//            rn.setRemoteJarCachePath("/aux/me/tmp");
//            rn.setProp("debug", "true");
//
//
//            Table s = cloud.node(/*"**"*/ "eus").exec(() -> {
//
//                //return new TestServer("nars.nal.nal1").unitTestsByPackage();
//                throw new TODO();
//            });
//
//            s.write().csv(System.out);
//
//            cloud.shutdown();
//
//        }
//    }
//
//    private static class NSTimeFormatter extends NumberColumnFormatter {
//        public String format(double value) {
//            return Str.timeStr(Math.round(value));
//        }
//    }
//}
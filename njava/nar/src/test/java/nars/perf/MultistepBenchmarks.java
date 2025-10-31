//package nars.perf;
//
//import nars.NAR;
//import nars.NARS;
//import nars.Op;
//import nars.term.util.builder.MegaHeapTermBuilder;
//import nars.term.util.builder.MemoizingTermBuilder;
//import nars.term.util.builder.SeparateInterningTermBuilder;
//import nars.term.util.builder.SimpleHeapTermBuilder;
//import nars.test.impl.DeductiveMeshTest;
//import org.junit.jupiter.api.Disabled;
//import org.openjdk.jmh.annotations.*;
//import org.openjdk.jmh.runner.RunnerException;
//
//import static nars.perf.NARBenchmarks.perf;
//
////import nars.SelfTest;
//
//
//@State(Scope.Thread)
//@AuxCounters(AuxCounters.Type.EVENTS)
//@Disabled
//public class MultistepBenchmarks {
//
//    @Param("2000")
//    private
//    String cycles;
//
//    /*, "24"*/
//    @Param("12")
//    private
//    String termVolumeMax;
//
//    @Param({"0","1","2","3","4","5"})
//    private
//    String termBuilder;
//
//    private NAR n;
//
//    public static void main(String[] args) throws RunnerException {
//        perf(MultistepBenchmarks.class, o -> {
//
//            o.warmupIterations(1);
//            o.measurementIterations(2);
//            o.threads(1);
//            o.forks(1);
//
//        });
//    }
//
//    @Setup
//    public void start() {
////        Function<Term[], Subterms> h = null;
//
//
//        n = NARS.tmp();
//        n.volMax.set(Integer.parseInt(termVolumeMax));
//
//        switch (termBuilder) {
//            case "0" -> Op.terms = SimpleHeapTermBuilder.the;
//            case "1" -> Op.terms = MegaHeapTermBuilder.the;
//            case "2" -> Op.terms = new SeparateInterningTermBuilder(SimpleHeapTermBuilder.the);
//            case "3" -> Op.terms = new SeparateInterningTermBuilder(MegaHeapTermBuilder.the);
//            case "4" -> Op.terms = new MemoizingTermBuilder(SimpleHeapTermBuilder.the);
//            case "5" -> Op.terms = new MemoizingTermBuilder(MegaHeapTermBuilder.the);
//        }
//
//    }
//
////    @TearDown
////    public void end() {
////        long concepts = n.memory.size();
////    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.SingleShotTime/*AverageTime*/)
//    public void deductiveMeshTest1() {
//        new DeductiveMeshTest(n, 6, 6);
//        n.run(Integer.parseInt(cycles));
//
////        SelfTest s = new SelfTest();
////        s.unitTestsByPackage("nars.nal.nal1");
//////        s.unitTestsByPackage("nars.nal.nal2");
////        s.run(3);
//    }
//
//}

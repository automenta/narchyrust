//package nars.exe.impl;
//
//import nars.NAR;
//import nars.exe.Exec;
//
//import java.util.function.Consumer;
//
///**
// * single thread executor used for testing
// */
//public class UniExec extends Exec {
//
//    public UniExec() {
//        this(1);
//    }
//
//    protected UniExec(int concurrencyMax) {
//        super(concurrencyMax);
//    }
//
//    @Override protected void execute(Consumer<NAR> t) {
//        t.accept(nar);
//    }
//
//    @Override
//    public void execute(Runnable r) {
//        r.run();
//    }
//}

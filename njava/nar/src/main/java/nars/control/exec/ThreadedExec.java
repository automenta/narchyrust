//package nars.exe.impl;
//
//import jcog.Log;
//import jcog.event.Off;
//import jcog.exe.AffinityExecutor;
//import jcog.exe.Exe;
//import nars.NAR;
//import nars.derive.reaction.ReactionModel;
//import nars.time.clock.RealTime;
//import org.slf4j.Logger;
//
//import java.util.concurrent.Semaphore;
//import java.util.function.Function;
//import java.util.function.Supplier;
//
///**
// * N independent asynchronously looping worker threads
// */
//public abstract class ThreadedExec extends WorkQueueExec {
//
//    private static final Logger logger = Log.log(ThreadedExec.class);
//
//    private final boolean affinity;
//    private final AffinityExecutor exe;
//
//    protected ThreadedExec(int maxThreads, boolean affinity, Function<NAR, ReactionModel> model) {
//        super(maxThreads, model);
//
//        this.exe = new AffinityExecutor(maxThreads);
//        exe.set(worker(), affinity);
//
//        this.affinity = affinity;
//
//        logger.info("{} threads, {} queueSize", maxThreads, queueSize());
//    }
//
//    protected abstract Supplier<Worker> worker();
//
//    protected sealed interface Worker extends Runnable, Off permits WorkerExec.WorkPlayLoop {  }
//
//    @Override
//    public final synchronized void synch() {
//        Semaphore r = exe.running;
//        if (r.tryAcquire(exe.maxThreads)) {
//            assert (this.exe.size() == 0);
//            try {
//                super.synch();
//            } finally {
//                r.release(exe.maxThreads);
//            }
//        }
//    }
//
//    @Override
//    public void starting(NAR n) {
//        if (!(n.time instanceof RealTime))
//            throw new UnsupportedOperationException("non-realtime clock not supported");
//
//        super.starting(n);
//
//        Exe.runLater(exe::runAll);
//
////        if (!executorGlobal)
////            Exe.setExecutor(this); //ready
//    }
//
//    @Override
//    protected void stopping(NAR nar) {
//        exe.exceptionRespawn = false;
//        exe.shutdownNow();
//        super.stopping(nar);
//    }
//
//
//    @Override
//    public boolean delete() {
//        if (super.delete()) {
//
////            if (Exe.executor() == this) //HACK
////                Exe.setExecutor(ForkJoinPool.commonPool()); //TODO use the actual executor replaced by the start() call instead of assuming FJP
//
//            exe.shutdownNow();
//
//            flush();
//
//            return true;
//        }
//        return false;
//    }
//
//
//
//
//}
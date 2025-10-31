//package nars.exe.impl;
//
//import jcog.Log;
//import jcog.exe.WorkQueue;
//import nars.NAR;
//import nars.derive.reaction.ReactionModel;
//import nars.exe.Exec;
//import org.slf4j.Logger;
//
//import java.util.function.Consumer;
//import java.util.function.Function;
//
//public abstract class WorkQueueExec extends Exec implements Consumer {
//    static final int inputQueueCapacityPerThread = 64;
//    protected final WorkQueue in;
//    private static final Logger logger = Log.log(WorkQueueExec.class);
//
//    protected final Function<NAR, ReactionModel> model;
//
//    protected WorkQueueExec(int concurrencyMax, Function<NAR, ReactionModel> model) {
//        super(concurrencyMax);
//        this.model = model;
//        int queueSize = inputQueueCapacityPerThread * concurrencyMax;
//        in = new WorkQueue<>(queueSize);
//    }
//
//    @Override
//    public synchronized void synch() {
//        in.acceptAll(this);
//    }
//
//    public final int work() {
//        return work(
//                1
//                //1f/concurrencyMax
//        );
//    }
//
//    /**
//     * called by a thread to do pending work.  completeness, in 0..1.0
//     * determines the max amount of pending burden the callee will be responsible for
//     */
//    public final int work(float completeness) {
//        return in.accept(this, completeness);
//    }
//
//    /**
//     * in-thread synchronous executions
//     */
//    public final void accept(Object runnable) {
//        if (runnable instanceof Consumer c)
//            c.accept(nar);
//        else
//            ((Runnable) runnable).run();
//    }
//
//    @Override
//    public final void execute(Runnable async) {
//        if (!in.offer(async))
//            async.run(); //inline
//    }
//
//    @Override
//    protected final void execute(Consumer<NAR> x) {
//        if (!in.offer(x))
//            executeJammed(x);
//    }
//
//    private void executeJammed(Consumer<NAR> x) {
//
////        //experimental: help drain queue
////        Object helping = in.poll();
////        if (helping!=null) {
////            logger.error("{} queue jam help={}", this, helping);
////            executeNow(helping);
////        }
//
//        //if (!in.offer(x)) { //try again
//        logger.error("{} queue blocked offer={}", this, x);
//        //TODO print queue contents, but only from one thread and not more than every N seconds
//        accept(x); //else: execute (may deadlock)
//        //}
//    }
//
//    public final int queueSize() {
//        return in.size();
//    }
//
//    protected void flush() {
//        Object next;
//        while ((next = in.poll()) != null) accept(next);
//    }
//
//    protected ReactionModel model(NAR n) {
//        return model.apply(n);
//    }
//
//}

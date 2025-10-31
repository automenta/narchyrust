//package nars.exe.impl;
//
//import jcog.Log;
//import jcog.Util;
//import jcog.data.list.FastCoWList;
//import jcog.random.RandomBits;
//import jcog.random.XoRoShiRo128PlusRandom;
//import nars.NAR;
//import nars.Deriver;
//import nars.derive.impl.TaskBagDeriver;
//import nars.derive.reaction.ReactionModel;
//import nars.Focus;
//import nars.time.part.DurLoop;
//import org.jctools.queues.SpmcArrayQueue;
//import org.slf4j.Logger;
//
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Function;
//import java.util.function.Supplier;
//import java.util.random.RandomGenerator;
//
//public class WorkerExec extends ThreadedExec {
//
//    /**
//     * TODO refactor this into a ProxyBag or ProxySampler for a bag which can be optionally used
//     */
//    @Deprecated private final SpmcArrayQueue<Focus> focusNext;
//
//    private final AtomicBoolean sampleQueueBusy = new AtomicBoolean(false);
//    private final FastCoWList<WorkPlayLoop> loops = new FastCoWList<>(WorkPlayLoop.class);
//    private ReactionModel reaction;
//    private transient long periodNS = 0;
//
//    private DurLoop update;
//
//    public WorkerExec(int threads, Function<NAR, ReactionModel> model) {
//        this(threads, false, model);
//    }
//
//    public WorkerExec(int threads, boolean affinity, Function<NAR, ReactionModel> model) {
//        super(threads, affinity, model);
//        focusNext = new SpmcArrayQueue<>(concurrencyMax * 3);
//    }
//
//    @Override
//    public void starting(NAR n) {
//        this.reaction = model(n);
//
//        super.starting(n);
//
//        update = n.onDur(this::schedule);
//    }
//
//    @Override
//    protected void stopping(NAR nar) {
//        loops.forEach(WorkPlayLoop::close);
//        super.stopping(nar);
//    }
//
//
//    @Override
//    protected Supplier<Worker> worker() {
//        return () -> {
//            WorkPlayLoop l = new WorkPlayLoop(this::deriver);
//            loops.add(l);
//            return l;
//        };
//    }
//
//    private void schedule() {
//
//        periodNS = nar.loop.periodNS();
//
//        WorkPlayLoop[] loop = loops.array();
//        int n = loop.length;
//
//        float throttle = nar.throttle();
//        float runningIdeal = n * throttle;
//        int running = (int) runningIdeal;
//
//        //int partialSleep = (throttleSleepRemainder > 0) ? nar.random().nextInt(running) : -1;
//        for (int i = 0; i < n; i++) {
//            float idle;
//            if (i >= running)
//                idle = 1; //fully asleep
//            else if (i == running - 1)
//                idle = Util.max(0, runningIdeal - running); //fraction
//            else
//                idle = 0; //fully awake
//
//            loop[i].idle = idle;
//        }
//        focusBufferClear();
//    }
//
//    protected Deriver deriver(Focus f) {
//        return
//                //new TaskBagDeriver.CompleteDeriver(reaction, nar);
//                new TaskBagDeriver(reaction, nar);
//        //new FairDeriver(reaction, nar);
//        //new QueueDeriver(reaction, nar);
//
//        //new SerialDeriver(reaction, nar);
//        //new CachedSerialDeriver(reaction, nar, 1 * 1024);
//        //new MixDeriver(reaction, nar);
//        //new EqualizerDeriver(reaction, nar);
//        //new BagDeriver(reaction, nar);
//        //new OptiTreeDeriver(reaction, nar);
//    }
//
//    private Focus focus(RandomGenerator rng) {
//        return focusBuffered(rng);
//        //return focusDirect(rng);
//    }
//
//    private Focus focusBuffered(RandomGenerator rng) {
//        Focus f = focusNext.poll();
//        if (f == null) {
//            if (Util.enterAlone(sampleQueueBusy)) {
//                try {
//                    f = focusBufferFill(rng) ? focusNext.poll() : null;
//                } finally {
//                    Util.exitAlone(sampleQueueBusy);
//                }
//            }
//        }
//
//        //last resort, if other thread is busy filling
//        return f == null ? focusDirect(rng) : f;
//    }
//
//    private void focusBufferClear() {
////        if (sampleQueueBusy.compareAndSet(false, true)) {
////            try {
//        focusNext.clear();
////            } finally {
////                sampleQueueBusy.set(false);
////            }
////        }
//    }
//
//    private boolean focusBufferFill(RandomGenerator rng) {
//        if (nar.focus.isEmpty()) return false;
//        nar.focus.sample(rng, focusNext.capacity(),
//            f -> focusNext.offer(f.id)
//        );
//        return true;
//    }
//
//    private Focus focusDirect(RandomGenerator rng) {
//        var p = nar.focus.sample(rng);
//        return p != null ? p.id : null;
//    }
//
//    public final class WorkPlayLoop implements ThreadedExec.Worker {
//
//        private static final Logger logger = Log.log(WorkPlayLoop.class);
//
//        private final RandomBits RNG = new RandomBits(new XoRoShiRo128PlusRandom());
//        final Deriver deriver;
//        public volatile float idle = 0;
//        public boolean alive = true;
//
//        WorkPlayLoop(Function<Focus, Deriver> deriverBuilder) {
//            deriver = deriverBuilder.apply(null);
//        }
//
//        @Override
//        public void run() {
//            //Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//            while (alive) {
//                try {
//                    if (work() == 0) {
//                        if (idle())
//                            sleep(0);
//                        else
//                            play();
//                    }
//                } catch (Exception e) {
//                    logger.error("run", e);
//                }
//            }
//            stop();
//        }
//
//        private boolean idle() {
//            var idle = this.idle;
//            return idle > 0 && RNG.nextBooleanFast8(idle);
//        }
//
//        private void sleep(float throttle) {
//            if (throttle < 1)
//                Util.sleepNS((int) (periodNS * (1 - throttle)));
//        }
//
//        private void stop() {
//            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
//            loops.remove(this);
//        }
//
//        private boolean play() {
//            Focus f;
//            if ((f = WorkerExec.this.focus(RNG)) == null)
//                return false;
//
//            deriver.next(f);
//            return true;
//        }
//
//        @Override
//        public void close() {
//            alive = false;
//        }
//    }
//
//}
//package nars.exe.impl;
//
//import jcog.random.XoRoShiRo128PlusRandom;
//import nars.NAR;
//import nars.Deriver;
//import nars.derive.impl.TaskBagDeriver;
//import nars.derive.reaction.ReactionModel;
//import nars.focus.util.TaskBagAttentionSampler;
//import nars.Premise;
//import nars.NALTask;
//import nars.time.part.DurLoop;
//
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.function.Function;
//
///** TODO */
//public class FiberExec extends WorkQueueExec {
//
//    final List<Fiber> fibers = new CopyOnWriteArrayList<>();
//    final List<Thread> fiberThreads = new CopyOnWriteArrayList<>();
//
//    float threadsPerConcurrency =
//        //1;
//        1.5f;
//        //2;
//        //4;
//        //16;
//
//    boolean threadsVirtual = true;
//    int depth =
//        //4;
//        6;
//        //8;
//
//    int batchSize = 8;
//
//    final Random rng = new XoRoShiRo128PlusRandom();
//    private DurLoop update;
//
//    public FiberExec(int concurrencyMax, Function<NAR, ReactionModel> model) {
//        super(concurrencyMax, model);
//    }
//
//    private class Fiber implements Runnable {
//
//        boolean alive = true;
//        @Override
//        public void run() {
//
//            while (alive) {
//                try {
//                    work();
//                    play();
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                }
//            }
//        }
//
//        final Deriver deriver;
//        final TaskBagDeriver.PremiseQueue queue;
//        final TaskBagAttentionSampler tasks = new TaskBagAttentionSampler();
//
//        private Fiber(ReactionModel m) {
//            this.deriver = new Deriver(m, nar) {
//                @Override
//                protected void acceptTask(NALTask x) {
//                    super.acceptTask(x);
//                    queue.onDerived(x);
//                }
//
//                @Override
//                protected void acceptPremise(Premise p) {
//                    queue.add(p, this.premise);
//                }
//
//                @Override
//                protected void next() {
//                }
//
//            };
//            queue = new TaskBagDeriver.PrioritySetPremiseQueue();
//        }
//
//        private void play() {
//            var F = nar.focus.sample(rng);
//            if (F==null) {
//                Thread.yield();
//                return;
//            }
//            var f = F.id;
//
//            deriver.next(f);
//
//            tasks.seed(f, batchSize);
//            for (var task : tasks.tasks) {
//                if (task != null)
//                    iter(task);
//            }
//        }
//
//        private void iter(NALTask t) {
//            queue.runSeed(t, depth, deriver);
//        }
//    }
//
//    @Override
//    public void starting(NAR nar) {
//        this.update = nar.onDur(this::update);
//
//        ReactionModel rxn = model(nar);
//
//        var tb = threadsVirtual ? Thread.ofVirtual() : Thread.ofPlatform();
//        //tb.uncaughtExceptionHandler()
//        @Deprecated int numThreads = Math.max(1, Math.round(concurrencyMax * threadsPerConcurrency));
//        for (int i = 0; i < numThreads; i++) {
//            var f = new Fiber(rxn);
//            fibers.add(f);
//            var t = tb.start(f);
//            fiberThreads.add(t);
//        }
//
//
//    }
//
//    protected void update() {
//        //TODO apply nar.loop.throttle
//    }
//
//    @Override
//    protected void stopping(NAR nar) {
//        for (Fiber t : fibers)
//            t.alive = false;
//    }
//
//}

package nars.focus.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.Util;
import jcog.decide.Roulette;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import jcog.random.RandomBits;
import nars.Focus;
import nars.NAL;
import nars.NALTask;
import nars.task.util.PuncBag;
import nars.task.util.TaskBag;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static jcog.pri.Prioritized.EPSILON;
import static nars.Op.*;

public class TaskAttention implements Consumer<NALTask>, Iterable<PLink<NALTask>> {

    public final TaskAttentionModel model;
    public final Focus focus;

    public TaskAttention(boolean puncBag, Focus f) {
        model = puncBag ? new PuncBagAttentionModel() : new BagAttentionModel();
        this.focus = f;
    }

    @Override
    public final Iterator<PLink<NALTask>> iterator() {
        return model.iterator();
    }

    public final Stream<PLink<NALTask>> stream() {
        return model.stream();
    }

    public final void forEach(byte punc, Consumer<NALTask> c) {
        if (punc==0) throw new TODO();
        model.forEach(punc, c);
    }

    public final int capacity() {
        return model.capacity();
    }

    public final void setCapacity(int c) {
        model.setCapacity(c);
    }

    public void seed(TaskBagAttentionSampler sample, int n, PuncBag punc, RandomGenerator rng) {
        model.seed(sample, n, punc, rng);
    }

    public final void commit() {
        model.commitIfModified();
    }

    public final int size() {
        return model.size();
    }

    public void clear() {
        model.clear();
    }

    private static Consumer<? super PLink<NALTask>> updater(MyTaskBag b) {
        return Util.compose(z -> { if (z.id.isDeleted()) z.delete(); }, b.forget(1));
    }

    public static class MyTaskBag extends TaskBag {
        private final AtomicBoolean modified = new AtomicBoolean(false);

        public MyTaskBag(PriMerge merge) {
            super(merge);
        }

        @Override
        public PLink<NALTask> put(NALTask p, float pri) {
            var y = super.put(p, pri);
            Util.once(modified);
            return y;
        }

        public void commitIfModified() {
            if (Util.next(modified))
                commit(updater(this));
        }

    }

    @Override
    public final void accept(NALTask t) {
        model.add(t, pri(t));
    }

    private float pri(NALTask t) {
        var f = focus;
        return f.budget.priIn(t, f);
    }

    public final boolean isEmpty() {
        return model.isEmpty();
    }

    public interface TaskAttentionModel {
        Iterator<PLink<NALTask>> iterator();

        void sampleUnique(int n, RandomGenerator rng, Consumer<PLink<NALTask>> each);

        void add(NALTask t, float pri);

        Stream<PLink<NALTask>> stream();

        int capacity();

        void setCapacity(int c);

        void commitIfModified();

        boolean isEmpty();

        static MyTaskBag newBag() {
            return new MyTaskBag(NAL.taskBagMerge);
        }

        void seed(TaskBagAttentionSampler sample, int n, PuncBag punc, RandomGenerator rng);

        /** punc=0: not filtered by punc */
        Iterator<NALTask> iterator(byte punc);

        @Deprecated private static Iterator<NALTask> unwrap(Iterator<PLink<NALTask>> tt) {
            return Iterators.transform(tt, z -> z.id);
        }

        default void sample(TaskBag bag, RandomGenerator rng, int n, TaskBagAttentionSampler sampler) {
            if (n > 0) {
                sampler.itersRemain = bag.size();
                sampler.samplesRemain = n;
                bag.sample(rng, sampler.sample);
            }
        }

        default Stream<NALTask> stream(boolean b, boolean g, boolean q, boolean Q) {
            return stream().map(x -> x.id).filter(t -> switch(t.punc()) {
                case BELIEF -> b;
                case GOAL -> g;
                case QUESTION -> q;
                case QUEST -> Q;
                default -> false;
            });
        }

        int size();

        default void forEach(byte punc, Consumer<NALTask> c) {
            iterator(punc).forEachRemaining(c);
        }

        void clear();
    }

    /** holds tasks in one combined bag */
    public static class BagAttentionModel extends MyTaskBag implements TaskAttentionModel {

        public BagAttentionModel() {
            super(NAL.taskBagMerge);
        }

        @Override
        public void add(NALTask t, float pri) {
            put(t, pri);
        }

        @Override
        public void seed(TaskBagAttentionSampler sampler, int n, PuncBag ignored, RandomGenerator rng) {
            sample(this, rng, n, sampler);
        }

        @Override
        public void sampleUnique(int n, RandomGenerator rng, Consumer<PLink<NALTask>> each) {
            if (n > 0) {
                for (var it = sampleUnique(rng); --n >= 0 && it.hasNext(); )
                    each.accept(it.next());
            }
        }

        @Override public Iterator<NALTask> iterator(byte punc) {
            var ii = iterator();
            var ff = punc!=0 ? Iterators.filter(ii, (PLink<NALTask> z) -> z.id.punc() == punc) : ii;
            return TaskAttentionModel.unwrap(ff);
        }

        @Override
        public void forEach(byte punc, Consumer<NALTask> c) {
            forEach(z -> { var zz = z.id; if (zz.punc()==punc) c.accept(zz); });
        }
    }

    /** holds tasks in separate bags, for each punctuation */
    public static class PuncBagAttentionModel implements TaskAttentionModel {
        public final MyTaskBag
            beliefs = TaskAttentionModel.newBag(),
            goals = TaskAttentionModel.newBag(),
            questions = TaskAttentionModel.newBag(),
            quests = TaskAttentionModel.newBag();

        @Override
        public Iterator<NALTask> iterator(byte punc) {
            return TaskAttentionModel.unwrap(bag(punc).iterator());
        }

        @Override
        public void clear() {
            beliefs.clear();
            goals.clear();
            questions.clear();
            quests.clear();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<PLink<NALTask>> iterator() {
            return Iterators.concat(beliefs.iterator(), goals.iterator(), questions.iterator(), quests.iterator());
        }

        @Override
        public void sampleUnique(int n, RandomGenerator rng, Consumer<PLink<NALTask>> each) {
            throw new TODO();
        }

        @Override
        public void add(NALTask t, float pri) {
            bag(t.punc()).put(t, pri);
        }

        @Override
        public final Stream<PLink<NALTask>> stream() {
            return Streams.concat(beliefs.stream(), goals.stream(), questions.stream(), quests.stream());
        }

        @Override
        public int capacity() {
            return beliefs.capacity() * 4;
        }

        @Override
        public void setCapacity(int c) {
            var cc = Math.max(1, c/4);
            beliefs.capacity(cc);
            goals.capacity(cc);
            questions.capacity(cc);
            quests.capacity(cc);
        }

        private MyTaskBag bag(byte punc) {
            return switch (punc) {
                case BELIEF -> beliefs;
                case GOAL -> goals;
                case QUESTION -> questions;
                case QUEST -> quests;
                default -> null;
            };
        }

        /**
         * update procedure, every dur
         */
        @Override
        public void commitIfModified() {
            beliefs.commitIfModified();
            goals.commitIfModified();
            questions.commitIfModified();
            quests.commitIfModified();
        }

        @Override
        public boolean isEmpty() {
            return beliefs.isEmpty() && goals.isEmpty() && questions.isEmpty() && quests.isEmpty();
        }

        @Override
        public void seed(TaskBagAttentionSampler sample, int n, PuncBag punc, RandomGenerator rng) {
            var bProb = puncProb(BELIEF, beliefs, punc);
            var qProb = puncProb(QUESTION, questions, punc);
            var gProb = puncProb(GOAL, goals, punc);
            var QProb = puncProb(QUEST, quests, punc);

            var probSum = bProb + gProb + qProb + QProb;
            if (probSum < EPSILON * 4)
                probSum = bProb = gProb = qProb = QProb = 1; //FLATTEN

            int beliefNum, goalNum, questionNum, questNum;
            if (n > 1) {
                beliefNum =   puncNum(bProb, n, probSum, rng);
                questionNum = puncNum(qProb, n, probSum, rng);
                goalNum =     puncNum(gProb, n, probSum, rng);
                questNum =    puncNum(QProb, n, probSum, rng);
            } else {
                beliefNum = questionNum = goalNum = questNum = 0;
                switch (Roulette.selectRoulette(rng, new float[]{bProb, qProb, gProb, QProb})) {
                    case 0 -> beliefNum = 1;
                    case 1 -> questionNum = 1;
                    case 2 -> goalNum = 1;
                    case 3 -> questNum = 1;
                }
            }
            sample(beliefs, rng, beliefNum, sample);
            sample(goals, rng, goalNum, sample);
            sample(questions, rng, questionNum, sample);
            sample(quests, rng, questNum, sample);
        }

        private static int puncNum(float puncProb, int iter, float probSum, RandomGenerator rng) {
            return RandomBits.nextFloor(iter * (puncProb / probSum), rng);
        }

        private static float puncProb(byte punc, TaskAttention.MyTaskBag bag, PuncBag prob) {
            return bag.isEmpty() ? 0 : prob.apply(punc);
        }

    }


}

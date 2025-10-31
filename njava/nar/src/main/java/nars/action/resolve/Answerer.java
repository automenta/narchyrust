package nars.action.resolve;

import jcog.Util;
import nars.*;
import nars.deriver.reaction.NativeReaction;
import nars.focus.time.TaskWhen;
import nars.table.question.QuestionTable;
import nars.term.Neg;
import nars.time.Tense;
import nars.truth.evi.EviInterval;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public abstract class Answerer extends NativeReaction {

    /** default instance; warning has mutable params that will be shared */
    public static final TaskResolver AnyTaskResolver =
            new DirectTaskResolver(false);
//    public static final TaskResolver OccSpecTaskResolver =
//            new DirectTaskResolver(true, false, false);
    public static final TaskResolver BestTaskResolver =
            new DirectTaskResolver(false) {
                @Override
                protected int answerCapacity(Deriver d) {
                    return 1;
                }
            };

    public final TaskResolver resolver;
    public final TaskWhen timing;

    protected Answerer(TaskWhen timing, TaskResolver resolver) {
        this.timing = timing;
        this.resolver = resolver;
    }

    //    public static final AbstractTaskResolver InquisitiveTaskResolver = new InquisitiveTaskResolver(Revise);
    public static class DirectTaskResolver extends TaskResolver {

        private static final boolean timeRes = true;
        public final boolean occSpecific;

        public DirectTaskResolver(boolean occSpecific) {
            this.occSpecific = occSpecific;
        }

        /** force conceptualization: if false, dynamic tables are still used if concept doesnt exist */
        protected boolean conceptualize(byte punc) {
            return false;
            //return (punc==BELIEF || punc==GOAL); //not questions
            //return true;
            //return conceptualize;
        }

        @Override @Nullable public final NALTask resolveTask(Term x, byte punc, Object/*Supplier<long[]>*/ occ, Deriver d, @Nullable Predicate<NALTask> filter) {
            if (x instanceof Neg)
                throw new UnsupportedOperationException("use resolveTaskPN");

            NAR n = d.nar;

            var t = table(x, punc, n);
            if (t == null || t.isEmpty())
                return null;

            var answerCapacity = answerCapacity(d);

            boolean beliefOrQuestion = !(t instanceof QuestionTable);
            try (Answer a = new Answer(x, beliefOrQuestion, occ(occ, d), answerCapacity, n)) {
                return a
                    .filter(filter)
                    .eviMin(d.eviMin)
                    .random(d.rng)
                    .match(t)
                    .task(Math.min(answerCapacity, revisionCapacity(d)), occSpecific);
            }
        }

        private static EviInterval occ(Object occ, Deriver d) {
            long[] se = Util.get(occ);
            return new EviInterval(timeRes ? Tense.dither(se, d.timeRes()) : se, d.dur());
        }

        private int revisionCapacity(Deriver d) {
            return NAL.answer.REVISION_CAPACITY;
            //return 1 + d.rng.nextInt(NAL.answer.PROJECTION_CAPACITY);
        }

        protected int answerCapacity(Deriver d) {
            return
                NAL.answer.ANSWER_CAPACITY;
                //1 + d.randomInt(NAL.answer.ANSWER_CAPACITY-1); //TODO abstract distribution interface
                //d.randomBoolean() ? 1 : NAL.answer.ANSWER_CAPACITY;
        }

        protected @Nullable TaskTable table(Term x, byte punc, NAR n) {
            return n.table(x, punc, conceptualize(punc));
        }

    }


//        private static void termSpecific(Answer a) {
//            Term template = a.term();
//            if (template.TEMPORALABLE()) {
//                if (template.TEMPORAL_VAR())
//                    throw new TODO();
//                else
//                    a.filter(template.equalsTermed());
//            }
//        }

//        private static boolean ensureTermSpecific(Answer a) {
//            var template = a.term();
//            if (template.TEMPORALABLE()) {
//                var tasks = a.tasks;
//                int n = tasks.size();
//                var tEq = template.equals();
//                for (int i = n - 1; i >= 0; i--) {
//                    if (!tEq.test(tasks.get(i).term()))
//                        tasks.remove(i);
//                }
//                if (a.isEmpty()) return false;
//            }
//            return true;
//        }

//    /**
//     * TODO
//     */
//    public static class CachedTaskResolver implements TaskResolver {
//
//        final TaskResolver resolver;
//
//        public CachedTaskResolver(TaskResolver resolver) {
//            this.resolver = resolver;
//        }
//
//        //TODO for cache wrapper:
//        //record TaskLookup(TaskTable t, Term x, Longerval l) { }
//
//        @Override
//        @Nullable
//        public NALTask resolveTask(Term x, byte punc, Object occ, Deriver d, @Nullable Predicate<NALTask> filter) {
//            throw new TODO();
//        }
//
//        static class TaskLookup {
//            public final TaskTable t;
//            public final Term x;
//            public final long start, end;
//            private final int hash;
//
//            public TaskLookup(TaskTable t, Term x, long start, long end) {
//                this.t = t;
//                this.x = x;
//                this.start = start;
//                this.end = end;
//                this.hash = hashCombine(hashCombine(System.identityHashCode(t), x), start, end);
//            }
//
//            @Override
//            public boolean equals(Object o) {
//                if (this == o) return true;
//                if (!(o instanceof TaskLookup that)) return false;
//                return
//                        hash == that.hash &&
//                                start == that.start && end == that.end &&
//                                t == that.t &&
//                                x.equals(that.x);
//            }
//
//            @Override
//            public int hashCode() {
//                return hash;
//            }
//        }
//    }


//    @Skill("Flashback_(psychology)")
//    public static class FlashbackTaskResolver extends DirectTaskResolver {
//
//        FlashbackTaskResolver(boolean conceptualize, boolean template) {
//            super(conceptualize, template);
//        }
//
//        @Override
//        public @Nullable NALTask resolveTask(Term x, byte punc, long s, long e, Deriver d, @Nullable Predicate<NALTask> filter) {
//            NALTask z = super.resolveTask(x, punc, s, e, d, filter);
//
//            if (z != null && !z.ETERNAL() && apply(z)) {
//                //FLASHBACK REPLAY shift
//                NALTask z2 = task(z, d);
//                NALTask.copyMeta(z2, z);
//                z = z2;
//            }
//
//            return z;
//        }
//
//        protected NALTask task(NALTask z, Deriver d) {
//            long now = d.now;
//            return SpecialOccurrenceTask.proxy(z, now, now + (z.end() - z.start()));
//        }
//
//        protected boolean apply(NALTask z) {
//            //return true;
//            return !(z instanceof SerialTask) && z.BELIEF();
//            //return z.term().TEMPORAL();
//        }
//    }

//    public static class EternalTaskResolver extends FlashbackTaskResolver {
//
//        EternalTaskResolver(boolean conceptualize, boolean template) {
//            super(conceptualize, template);
//        }
//
//        @Override
//        protected NALTask task(NALTask z, Deriver d) {
//            return SpecialOccurrenceTask.proxy(z, ETERNAL, ETERNAL);
//        }
//    }


//    public static class InquisitiveTaskResolver extends DirectTaskResolver {
//
//        InquisitiveTaskResolver() {
//            super(false, true);
//        }
//
//        @Override
//        protected @Nullable TaskTable table(Term x, byte punc, Deriver d) {
//            TaskTable t = super.table(x, punc, d);
//            if (t == null || t.isEmpty()) {
//                byte punc2 = switch (punc) {
//                    case BELIEF -> QUESTION;
//                    case GOAL -> QUEST;
//                    case QUESTION -> x.hasAny(VAR_QUERY) ? (byte) 0 : BELIEF;
//                    case QUEST -> x.hasAny(VAR_QUERY) ? (byte) 0 : GOAL;
//                    default -> (byte) 0;
//                };
//                if (punc2 != 0)
//                    return super.table(x, punc2, d);
//            }
//            return t;
//        }
//    }
}
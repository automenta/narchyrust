package nars;

import jcog.TODO;
import nars.table.eternal.EternalTable;
import nars.table.temporal.NavigableMapBeliefTable;
import nars.time.Moment;
import nars.truth.MutableTruthInterval;
import nars.truth.proj.TruthProjection;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static java.lang.Float.NaN;

/**
 * A model storing, ranking, and projecting beliefs or goals (tasks with TruthValue).
 * It should iterate in top-down order (highest ranking first)
 * <p>
 * TODO make an abstract class not interface
 */
public interface BeliefTable extends TaskTable {

    BeliefTable[] EmptyBeliefTableArray = new BeliefTable[0];

    static double eternalTaskValue(NALTask eternal) {
        return eternal.evi();
    }

    static double eternalOriginality(NALTask eternal) {
        return eternalTaskValue(eternal) * eternal.originality();
    }

    static Predicate<BeliefTable> mutableTable(boolean eternal) {
        return z -> eternal ? z instanceof EternalTable : z instanceof NavigableMapBeliefTable;
    }

    @Deprecated @Nullable
    default Truth truth(long s, long e, NAR nar) { return truth(s, e, nar.dur(), nar); }

    @Nullable
    default Truth truth(Moment w, NAR nar) {
        return truth(w.s, w.e, w.dur, nar);
    }

    @Deprecated @Nullable
    default Truth truth(long start, long end, float dur, NAR nar) {
        return truth(start, end, null, null, dur, nar);
    }

    @Nullable
    default Truth truth(MutableTruthInterval se, Term template, float dur, NAR n) {
        return truth(se.s, se.e, template, null, dur, n);
    }

    @Nullable
    default Truth truth(long start, long end, @Nullable Term template, @Nullable Predicate<NALTask> filter, float dur, NAR n) {
        return isEmpty() ? null : answer(start, end, template, dur, n).filter(filter).truth();
    }

    default Answer answer(long start, long end, @Nullable Term template, float dur, NAR n) {
        return _answer(start, end, template, dur, n).match(this);
    }

    default Answer answer(long start, long end, @Nullable Term template, float dur, Predicate<NALTask> filter, NAR n) {
        return _answer(start, end, template, dur, n).filter(filter).match(this);
    }

    private static Answer _answer(long start, long end, @Nullable Term template, float dur, NAR n) {
        return new Answer(template, true,
                start, end, dur,
                NAL.answer.ANSWER_CAPACITY, n);
    }

    default float freq(long start, long end, float dur, NAR nar) {
        var t = answer(start, end, null, dur, nar).truth(false);
        return t != null ? t.freq() : NaN;
    }

    default double coherency(long s, long e, float dur, NAR nar) {
        @Nullable TruthProjection t = null;
        if (!isEmpty())
            t = answer(s, e, null, dur, nar).truthProjection();

        throw new TODO(t.toString());
        //return t == null || !t.commit(false) ? Double.NaN : t.coherency();
    }

}

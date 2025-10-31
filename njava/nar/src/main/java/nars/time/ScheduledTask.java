package nars.time;

import com.google.common.primitives.Longs;
import nars.NAR;
import nars.Term;
import nars.term.Termed;

import java.util.function.Consumer;

/** event schedled for a specific time in the past or present (run as soon as possible), or delayed until
 * the future */
public abstract class ScheduledTask implements Consumer<NAR>, Comparable<ScheduledTask>, Termed {

    /** TODO atomic ops on this using VarHandle */
    public volatile long next = Long.MIN_VALUE;

    @Override
    public abstract Term term();

    @Override
    public final String toString() {
        return "@" + next + ':' + super.toString();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int compareTo(ScheduledTask that) {
        if (this == that) return 0;

        int t = Longs.compare(next, that.next);
		return t != 0 ? t : Integer.compare(System.identityHashCode(this), System.identityHashCode(that));
    }

}
package nars.time;

import static nars.Op.ETERNAL;
import static nars.Op.TIMELESS;

/**
 * fff
 * -->-->      span dt terminates at start of TO
 * ttt
 */
public record TimeSpan(long dt) {

    public static final TimeSpan TS_ZERO = new TimeSpan(0);

    public static TimeSpan the(long dt) {
        if (dt == 0) {
            return TS_ZERO;
        } else {
            assert (dt != ETERNAL && dt != TIMELESS);
            //assert (dt != TIMELESS && dt!= XTERNAL && dt!=DTERNAL): "bad timespan"; //TEMPORARY
            //assert (dt != XTERNAL) : probably meant to use TIMELESS";
            //assert (dt != DTERNAL) : "probably meant to use ETERNAL";
            return new TimeSpan(dt);
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(dt);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ||
                obj instanceof TimeSpan(long dt1) && dt == dt1;
    }

    @Override
    public String toString() {
        return //(dt == ETERNAL ? "E" :
                (dt >= 0 ? ("+" + dt) : ("-" + (-dt)));
    }
}
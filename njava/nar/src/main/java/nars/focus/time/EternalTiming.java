package nars.focus.time;

import nars.Deriver;
import nars.NALTask;

import static nars.Op.ETERNAL;

public class EternalTiming implements TaskWhen {

    @Override
    public long[] whenAbsolute(Deriver d) {
        return new long[] { ETERNAL, ETERNAL };
    }

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {
        return whenAbsolute(d);
    }
}
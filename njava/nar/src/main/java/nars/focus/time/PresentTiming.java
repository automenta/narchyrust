package nars.focus.time;

import nars.Deriver;
import nars.NALTask;

public class PresentTiming implements TaskWhen {

    @Override
    public long[] whenAbsolute(Deriver d) {
        return d.focus.when();
    }

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {
        return whenAbsolute(d);
    }

}
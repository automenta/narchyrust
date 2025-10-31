package nars.focus.time;

import nars.Deriver;
import nars.NALTask;

public class ProxyTiming implements TaskWhen {
    final TaskWhen delegate;

    public ProxyTiming(TaskWhen delegate) {
        this.delegate = delegate;
    }

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {
        return delegate.whenRelative(t, d);
    }

    @Override
    public long[] whenAbsolute(Deriver d) {
        return delegate.whenAbsolute(d);
    }
}
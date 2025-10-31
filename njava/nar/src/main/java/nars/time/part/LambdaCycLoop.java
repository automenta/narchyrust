package nars.time.part;

import jcog.event.Off;
import nars.$;
import nars.NAR;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.util.NARPart;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class LambdaCycLoop extends NARPart implements Consumer<NAR> {
    private final Consumer<NAR> each;

    private static final Atom onCycle = Atomic.atom("onCycle");
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private Off off;

    LambdaCycLoop(Consumer<NAR> each) {
        super($.func(onCycle, $.identity(each)));
        this.each = each;
    }

    public static LambdaCycLoop the(Consumer<NAR> each) {
        return new LambdaCycLoop(each);
    }

    protected void run(NAR n) {
        each.accept(n);
    }

    @Override
    protected void starting(NAR nar) {
        assert(off == null);
        off = nar.onCycle(this);
    }

    @Override
    protected void stopping(NAR nar) {
        @Nullable Off o = off;
        if (o!=null) {
            this.off = null;
            o.close();
        }
    }

    @Override
    public void accept(NAR nar) {
        if (busy.compareAndSet(false, true)) {
            try {
                run(nar);
            } finally {
                busy.set(false);
            }
        }
    }
}

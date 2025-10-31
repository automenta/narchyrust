package nars.gui;

import nars.util.NARPart;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

/** for use with ToggleButton, etc */
public class NARPartEnabler implements BooleanProcedure {
    private final NARPart p;
    private transient Runnable resume;

    public NARPartEnabler(NARPart p) {
        this.p = p;
        resume = p::on;
    }

    @Override
    public synchronized void value(boolean x) {
        if (x) {
            Runnable r = resume;
            resume = null;
            if (r != null)
                r.run();
        } else {
            if (resume == null)
                resume = p.pause();
        }
    }
}
package nars.term.util;

import nars.term.Termlike;
import nars.util.SoftException;

public class TermTransformException extends SoftException {

    private final Termlike x, y;

    public TermTransformException(String reason, Termlike x, Termlike y) {
        super(reason);
        this.x = x; this.y = y;
    }

    @Override
    public String getMessage() {
        String m = super.getMessage();
        String xh = "\tx: ";
        return String.valueOf(getClass()) + '\n' +
                xh +
                x + "\n\ty: " +
                y + '\n' +
                m;
    }
}
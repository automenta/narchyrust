package nars.io;

import nars.NALTask;

/**
 * immediate, no buffering
 */
public class DirectTaskInput extends TaskInput {

    @Override protected void remember(NALTask x) {
        rememberNow(x);
    }


}

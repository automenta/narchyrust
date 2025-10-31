package nars.focus.time;

import nars.Deriver;
import nars.NALTask;

/** the deriver uses TimeFocus implementations to determine TWO specific time intervals,
 * the second depending indirectly on the first:
 *   1) at the start of a derivation process, TimeFocus determines when in time that
 *      a TaskLink should resolve Tasks in
 *   2) during premise formation, in which a premise containing a Task resulting from (1),
 *      the TimeFocus determines when to match belief tasks and answer questions.
 * */
public interface TaskWhen {

	long[] whenRelative(NALTask t, Deriver d);

	long[] whenAbsolute(Deriver d);

}
package nars.table.dynamic;

import nars.BeliefTable;
import nars.NAL;
import nars.NALTask;
import nars.Truth;
import nars.action.memory.Remember;
import nars.table.BeliefTables;
import nars.task.SerialTask;
import org.jetbrains.annotations.Nullable;

/**
 * special belief tables implementation
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 */
public class SensorBeliefTables extends BeliefTables {

	public final @Nullable SerialBeliefTable sensor;

	public SensorBeliefTables(BeliefTable mutableTemporalTable, BeliefTable sensorTable) {
		super(sensorTable);
		tables.add(mutableTemporalTable);
		sensor = tableFirst(SerialBeliefTable.class);
	}

	@Override
	public final void remember(Remember r) {
        var x = r.input;
		if (!(x instanceof SerialTask) && accept(r, x))
			super.remember(r);
	}

	private boolean accept(Remember r, NALTask x) {
		if (NAL.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS_ON_REMEMBER && sensor.ignore(r.input)) {
			r.unstore(x);
			x.delete();
			return false;
		}
		return true;
	}

	@Nullable public final SerialTask add(Truth next, SerialBeliefTable.SerialUpdater su) {
		return sensor.add(next, tables, su);
	}

}
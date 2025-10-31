package nars.table.temporal;

import jcog.math.LongInterval;
import nars.NALTask;
import nars.task.util.TaskOccurrence;

import java.util.Comparator;

public final class TaskRegionComparator implements Comparator<LongInterval> {

    static final TaskRegionComparator the = new TaskRegionComparator();

    private TaskRegionComparator() {
    }
    @Override
    public int compare(LongInterval a, LongInterval b) {
        // First compare by start times
        int startComparison = Long.compare(a.start(), b.start());
        if (startComparison != 0)
            return startComparison;

        // If start times are equal, compare by end times
        int endComparison = Long.compare(a.end(), b.end());
        if (endComparison != 0)
            return endComparison;

        // If temporal bounds are identical, maintain consistency with additional properties
        if (a instanceof TaskOccurrence || b instanceof TaskOccurrence) {
            // Handle TaskOccurrence consistently with other cases
            return compareEquality(a, b);
        }

        return compareEquality(a, b);
    }

    static int compareEquality(LongInterval a, LongInterval b) {
        int m = Integer.compare(a.hashCode(), b.hashCode());
        if (m != 0) return m;
        if (a.equals(b)) return 0;

        return NALTask.compareSerialized((NALTask) a, (NALTask) b);
    }
//    @Override
//    public int compare(LongInterval a, LongInterval b) {
//        if (a instanceof TaskOccurrence aa)
//            return +compareTaskOccurrence(aa, b);
//        else if (b instanceof TaskOccurrence bb)
//            return -compareTaskOccurrence(bb, a);
//        else
//            return compareTaskTask(a, b);
//    }
//
//    static int compareTaskTask(LongInterval a, LongInterval b) {
//        int mid = Intervals.compare(a, b);
//        return mid != 0 ? mid : compareEquality(a, b);
//    }
//
//    static int compareEquality(LongInterval a, LongInterval b) {
//        //this naive hashcode compare is not 100% bulletproof but should work for 99.99+% cases where truth, stamp or term causes the hash to differ
//
//        int m = Integer.compare(a.hashCode(), b.hashCode());
//        if (m != 0) return m;
//        if (a.equals(b)) return 0;
//
//        return NALTask.compareSerialized((NALTask) a, (NALTask) b);
//        //return compareIdentity(a, b); //last resort
//    }
//
//    static int compareTaskOccurrence(TaskOccurrence x, LongInterval y) {
//        long w = x.start();
//        if (w < y.start()) return -1;
//        else if (w > y.end()) return +1;
//        else return 0; //intersect
//    }


}
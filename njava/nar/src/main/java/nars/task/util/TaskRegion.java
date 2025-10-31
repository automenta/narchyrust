package nars.task.util;

import jcog.Util;
import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import nars.NAL;
import nars.NALTask;
import nars.Task;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static jcog.Util.maybeEqual;

/**
 * 3d cuboid region:
 * time            start..end              64-bit signed long
 * frequency:      min..max in [0..1]      32-bit float
 * confidence:     min..max in [0..1)      32-bit float
 */
public interface TaskRegion extends HyperRegion, LongInterval {

//    static Consumer<TaskRegion> asTask(Consumer<? super NALTask> each) {
//        return r -> {
//            NALTask x = _task(r);
//            if (x != null) each.accept(x);
//        };
//    }
//
//    static Predicate<TaskRegion> asTask(Predicate<? super NALTask> each) {
//        return r -> {
//            NALTask x = _task(r);
//            return x == null || each.test(x);
//        };
//    }

//    static NALTask _task(TaskRegion r) {
//        return (NALTask) (r instanceof NALTask ? r : r.task());
//    }

    static TaskRegion mbr(TaskRegion x, NALTask y) {
		return //x == y ? x :
            mbr(x, y.start(), y.end(), y.freq(), (float) y.conf());
    }

    static TasksRegion mbr(TaskRegion r, long xs, long xe, float _ef, float _ec) {

        long S = r.start();

        assert(xs!=ETERNAL && xs!=TIMELESS && xe!=ETERNAL && xe!=TIMELESS);

        long E = r.end();
        int f = r.freqMinI(), F = r.freqMaxI();
        int c = r.confMinI(), C = r.confMaxI();

        int ef = TasksRegion.freqI(_ef), ec = TasksRegion.confI(_ec);
//        if (r instanceof TasksRegion) {
//            if (xs >= S && xe <= E)
//                if (ec >= c && ec <= C)
//                    if (ef >= f && ef <= F)
//                        return (TasksRegion) r; //conttained
//        }

        return new TasksRegion(
            min(S, xs), max(E, xe),
            min(f, ef), max(F, ef),
            min(c, ec), max(C, ec)
        );

        //return r instanceof TasksRegion ? Util.maybeEqual((TasksRegion) r, y) : y;
    }

    @Override
    boolean equals(Object obj);


//    @Override
//    default double cost() {
//        double x = timeCost() * freqCost() * confCost();
//        assert (x == x);
//        return x;
//    }

//    @Override
//    default double perimeter() {
//        return timeCost() + freqCost() + confCost();
//    }

    @Override
    int hashCode();

//    default float expectation() {
//        return (float) TruthFunctions.expectation(freqMean(), confMin());
//    }

//    @Override
//    default double cost(int dim) {
//        switch (dim) {
//            case 0:
//                return range(0);
//                //return Math.log(range(0)) * TIME_COST;
//                //return Math.sqrt(range(0)) * TIME_COST;
//            case 1:
//                return range(1);
//            case 2:
//                return range(2);
//        }
//        throw new UnsupportedOperationException();
//    }

    @Override
    default double range(int dim) {
        return switch (dim) {
            case 0 -> (end() - start());
            case 1 -> (freqMaxI() - freqMinI()) * NAL.truth.FREQ_EPSILON;
            case 2 -> (confMaxI() - confMinI()) * NAL.truth.TASK_REGION_CONF_EPSILON;
            default -> throw new UnsupportedOperationException();
        };

    }

    @Override
    default int dim() {
        return 3;
    }

    @Override
    default TaskRegion mbr(HyperRegion r) {
        if (this == r || r == null) return this;

        if (r instanceof NALTask) {
            //assert (!(this instanceof Task)) : "mbr(task,task) should force creation of TasksRegion";
            return mbr(this, (NALTask) r);
        } else {
            TaskRegion R = (TaskRegion) r;
//            if (contains(r))
//                return this;
//            else if (r.contains(this))
//                return R;

            return maybeEqual(new TasksRegion(
                min(start(), R.start()), max(end(), R.end()),
                min(freqMinI(), R.freqMinI()), max(freqMaxI(), R.freqMaxI()),
                min(confMinI(), R.confMinI()), max(confMaxI(), R.confMaxI())
            ), this, R);
//            //may only be valid for non-Tasks
//            if (this instanceof TasksRegion && z.equals(this))
//                return this; //contains or equals y
//            else if (r instanceof TasksRegion && z.equals(r))
//                return (TaskRegion) r; //contained by y
//            else
//                return z; //enlarged (intersecting or disjoint)
        }
    }

    @Override
    default /* final */ boolean intersects(HyperRegion _y) {
        if (_y == this) return true;

        TaskRegion y = (TaskRegion) _y;
        if (intersectsRaw(/*(LongInterval)*/y)) {

            int xca = confMinI(), ycb = y.confMaxI();
            if (xca <= ycb) {
                int xfa = freqMinI(), yfb = y.freqMaxI();
                if (xfa <= yfb) {


                    boolean xt = this instanceof Task, yt = _y instanceof Task;
                    if (xt && yt)
                        return true; //HACK shortcut since tasks currently only have one flat freq but could change with piecewise linear truth

                    int xcb = xt ? xca : confMaxI();
                    int yca = yt ? ycb : y.confMinI();
                    if (xcb >= yca) {
                        int xfb = xt ? xfa : freqMaxI();
                        int yfa = yt ? yfb : y.freqMinI();
                        return (xfb >= yfa);
                    }
                }
            }
        }
        return false;
    }

    @Override
    default /* final */ boolean contains(HyperRegion x) {
        if (x == this) return true;
        TaskRegion t = (TaskRegion) x;
        if (containsRaw(t)) {
            return
                confMinI() <= t.confMinI() &&
                confMaxI() >= t.confMaxI() &&
                freqMinI() <= t.freqMinI() &&
                freqMaxI() >= t.freqMaxI()
                ;
        }
        return false;
    }

    default double coord(int dimension, boolean maxOrMin) {
        return switch (dimension) {
            case 0 -> maxOrMin ? end() : start();
            case 1 -> (maxOrMin ? freqMaxI() : freqMinI()) * (NAL.truth.FREQ_EPSILON);
            case 2 -> (maxOrMin ? confMaxI() : confMinI()) * (NAL.truth.TASK_REGION_CONF_EPSILON);
            default -> Double.NaN;
        };
    }

    default double center(int dimension) {
        return switch (dimension) {
            case 0 -> mid();
            case 1 -> (freqMinI() + freqMaxI()) * NAL.truth.FREQ_EPSILON_half;
            case 2 -> (confMinI() + confMaxI()) * (0.5 * NAL.truth.TASK_REGION_CONF_EPSILON);
            default -> Double.NaN;
        };
    }

    default float freqMean() {
        return (freqMin() + freqMax()) / 2;
    }
    default float confMean() {
        return (confMin() + confMax()) / 2;
    }

    float freqMin();
    float freqMax();
    float confMin();
    float confMax();




    private int i(boolean freqOrConf, boolean maxOrMin) {
        return Util.toInt(
            freqOrConf ?
                (maxOrMin ? freqMax() : freqMin()) :
                (maxOrMin ? confMax() : confMin()),

            NAL.truth.FREQS// : NAL.truth.CONFS
        );
    }

    default int freqMinI() {
        return i(true, false);
    }
    default int freqMaxI() {
        return i(true, true);
    }
    default int confMinI() {
        return i(false, false);
    }
    default int confMaxI() {
        return i(false, true);
    }

}
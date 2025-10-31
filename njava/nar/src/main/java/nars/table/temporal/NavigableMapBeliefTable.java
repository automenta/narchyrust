package nars.table.temporal;

import jcog.TODO;
import jcog.data.map.ConcurrentSkipListMap2;
import jcog.math.LongInterval;
import nars.Answer;
import nars.NALTask;
import nars.action.memory.Remember;
import nars.task.util.ClusterRevise;
import nars.task.util.TaskOccurrence;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;


/**
 * TODO eval https://github.com/yahoo/Oak
 */
public non-sealed class NavigableMapBeliefTable extends TemporalBeliefTable  /*, TODO TimeRangeBag<NALTask,NALTask> */ {

    private final NavigableMap<LongInterval, NALTask> map;

    public NavigableMapBeliefTable(NavigableMap<LongInterval, NALTask> map) {
        this.map = map;
    }

    public NavigableMapBeliefTable() {
        this(
            new TaskRegionNALTaskConcurrentSkipListMap()
            //new ConcurrentSkipListMap<>(cmp
            //Maps.synchronizedNavigableMap(new TreeMap<>(cmp)) //<- doesnt work
            //Collections.synchronizedNavigableMap(new TreeMap<>(cmp)) //<- doesnt work
        );
    }

    private static boolean scanNearest(long x, NALTask b, NALTask t, Predicate<NALTask> a) {
        long db = b.diff(x), dt = t.diff(x);
        return dt == db ? scanNearestDir(a) : dt < db;
    }

    private static boolean scanNearestDir(Predicate<NALTask> a) {
        return a instanceof Answer A ? A.rng().nextBoolean() : ThreadLocalRandom.current().nextBoolean();
    }





//    private static boolean includesStamp(long[] existing, long[] incoming) {
//        //return Arrays.equals(existing, incoming); //strict
//
//        //whether they equal or contain each other
//        int xys = Stamp.equalsOrContains(existing, incoming);
//        return xys == 0 || xys == +1 || xys == -1;
//    }

    @Override
    protected boolean insertInternal(NALTask x, @Nullable Remember r) {
        var y = map.putIfAbsent(x, x);

        if (y == null) {
            ensureCapacity(r);
            y = x;
        } else {
            //equal, merged
        }

        if (r != null) r.store(y);

        return y == x;
    }


    @Override
    protected void compress(int preferred, int capIgnored, Remember r) {
        new ClusterRevise(NavigableMapBeliefTable.this, r, preferred, capacity());
    }

    @Override
    public void removeIfInternal(Predicate<NALTask> o, long values, long e) {
        throw new TODO();
    }

    @Override
    public void removeAllInternal(Iterable<NALTask> toRemove) {
        for (var x : toRemove)
            remove(x, false);
    }

    @Override
    public NALTask firstTask() {
        return (NALTask) map.firstKey();
    }

    @Override
    public NALTask lastTask() {
        return (NALTask) map.lastKey();
    }

    /**
     * revise a subset of tasks
     * TODO add a window that this zips along in batches
     */
    @Override
    public int taskCount() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    protected boolean removeInternal(NALTask x, boolean delete) {
        var y = map.remove(x);
        if (y != null) {
            if (delete)
                y.delete();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Stream<? extends NALTask> taskStream() {
//        Collection<NALTask> s = map.values();
//        return s.isEmpty() ? Stream.empty() : s.stream();
        return map.values().stream();
    }

    @Override
    public void match(Answer a) {
        var size = taskCount();
        if (size > 0) {
            if (size == 1 || size <= a.ttl)
                whileEach(a); //ALL
            else
                scanNear(TemporalBeliefTable.scanStart(a), a); //SOME
        }
    }

    public final void scanNear(long x, Predicate<NALTask> a) {
        scanNear(x, true, false, a);
    }

    /**
     * outwards-proceeding scan,
     * alternating between upwards and downwards to next nearest task in either direction,
     * centered at 'x'
     */
    public final void scanNear(long x, boolean inclusiveBelow, boolean inclusiveAbove, Predicate<NALTask> a) {
        LongInterval m = TaskOccurrence.at(x);
        scanNear(x, head(inclusiveBelow, m), tail(inclusiveAbove, m), a);
    }

    private static void scanNear(long x, Iterator<NALTask> bot, Iterator<NALTask> top, Predicate<NALTask> a) {
        NALTask b = null, t = null;
        NALTask next;
        do {
            if (b == null && bot != null) {
                if ((b = hasNextOrNull(bot)) == null) bot = null;
            }
            if (t == null && top != null) {
                if ((t = hasNextOrNull(top)) == null) top = null;
            }

            if (b == null && t == null)
                break;

            var tOrB = b != null && t != null ?
                    scanNearest(x, b, t, a) :
                    (t != null);

            if (tOrB) {
                next = t;
                t = null;
            } else {
                next = b;
                b = null;
            }

        } while (a.test(next));
    }

    @Nullable
    private static NALTask hasNextOrNull(Iterator<NALTask> i) {
        return i.hasNext() ? i.next() : null;
    }

    private Iterator<NALTask> head(boolean inclusiveBelow, LongInterval m) {
        return iter(
            map instanceof ConcurrentSkipListMap2 M ?
                M.headMapDescending(m, inclusiveBelow) :
                map.headMap(m, inclusiveBelow).descendingMap()
        );
    }

    private Iterator<NALTask> tail(boolean inclusiveAbove, LongInterval m) {
        return iter(map.tailMap/*Ascending*/(m, inclusiveAbove));
    }

    private static Iterator<NALTask> iter(NavigableMap<LongInterval, NALTask> s) {
        return s.values().iterator();
    }

    @Override
    public void forEachTask(Consumer<? super NALTask> x) {
        map.values().forEach(x);
    }

    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super NALTask> x) {
        intersecting(minT, maxT).forEach(x);
    }

    @Override
    public void removeIf(Predicate<NALTask> remove, long s, long e) {

        var ii = intersecting(s, e).iterator();
        while (ii.hasNext()) {
            if (remove.test(ii.next()))
                ii.remove();
        }

    }

    @Override
    public void whileEach(Predicate<? super NALTask> each) {
        for (var x : map.values()) {
            if (!each.test(x))
                return;
        }
    }

    private NavigableMap<LongInterval, NALTask> subMap(long minT, long maxT) {
        //assert(minT <= maxT);

        if (minT == MIN_VALUE && maxT == MAX_VALUE)
            return map; //everything

        LongInterval a = TaskOccurrence.at(minT);
        var b = (maxT == minT) ? a : TaskOccurrence.at(maxT);
        return map.subMap(
                a, true,
                b, true);
    }

    @Override protected Iterable<NALTask> intersecting(long minT, long maxT) {
        return subMap(minT, maxT).values();
    }


//    protected String summary() {
//        return taskCount() + "/" + capacity() + " tasks, range=" + range();
//    }

//    private static boolean acceptMerge(MutableTruthProjection a, NALTask merged) {
//
//        if (NAL.DEBUG) {
//            if (!merged.term().equalConcept(a.getFirst().term()))
//                return false; //concept change
//        }
//
//        double bValue = -value(victim);
//        double aValue = value(merged) /* * a.density()*/ - a.sumOfDouble(NavigableMapBeliefTable::value);
//        return aValue >= bValue;
//    }


//    @Nullable
//    protected static NALTask revise(TruthProjection t, @Nullable Remember r) {
////        if (r != null) {
////            //test revision options
////            final NALTask fir = t.get(0);
////            Term x = fir.term();
////            if (x.IMPL()) {
////                //TODO > 2
////                if (t.size() == 2) {
////                    final NALTask sec = t.get(1);
////                    if (fir.stampOverlapping().test(sec)) {
////                        final Term y = sec.term();
////                        final Term s = x.sub(0);
////                        final Term ys = y.sub(0);
////                        if (s.equals(ys)) {
////
////                            int dt0 = x.dt();
////                            if (dt0 == DTERNAL) dt0 = 0; //HACK
////                            int dt1 = y.dt();
////                            if (dt1 == DTERNAL) dt1 = 0; //HACK
////                            if (Math.abs(dt0 - dt1) >= r.nar().dur()) { //different DT //TODO how different
////                                final Term xp = x.sub(1), yp = y.sub(1);
////                                final boolean n0 = fir.NEGATIVE();
////                                final boolean n1 = sec.NEGATIVE();
////                                Term template = IMPL.the(s, XTERNAL, CONJ.the(xp.negIf(n0), XTERNAL, yp.negIf(n1)));
////                                if (template.volume() <= r.nar().volMax()) {
////                                    DynTaskify d = new ReviseTaskify(DynImplConj.DynImplConjPred, (Compound) template, t, r);
////                                    d.add(n0 ? new SpecialNegTask(fir) : fir);
////                                    d.add(n1 ? new SpecialNegTask(sec) : sec);
////                                    NALTask z = d.taskClose();
////                                    if (z != null)
////                                        return z;
////                                    else {
////                                        //else: fail, continue below
////                                    }
////                                }
////                            }
////                        }
////                    }
////                }
////            } else {
////                //TODO conj, etc..
////            }
////
////        }
//
//        //TODO implement intermpolation revision when revision fails (ex: stamps overlap)
//
//        return t.task();
//    }

    @Override
    public void evictForce(int toRemove, DoubleFunction<NALTask> w) {

//        if (map.size() <= toRemove) {
//            //EMERGENCY GENOCIDE
//            map.clear();
//            return;
//        }

        while (toRemove > 0) {
            //TODO compare first and last entry and remove weaker, or weighted random by their strength

            var first = firstTask();
            if (first == null)
                break; //map is empty

            var last = lastTask();
            if (first == last) {
                map.pollFirstEntry();
                break;
            } else {
                //compare
                if (w.doubleValueOf(first) > w.doubleValueOf(last))
                    map.pollFirstEntry();
                else
                    map.pollLastEntry();
            }
            toRemove--;
        }
    }

    private static final class TaskRegionNALTaskConcurrentSkipListMap extends ConcurrentSkipListMap2<LongInterval, NALTask> {
        @Override
        public int compare(LongInterval a, LongInterval b) {
            return TaskRegionComparator.the.compare(a, b);
//            if (a instanceof TaskOccurrence aa)
//                return +compareTaskOccurrence(aa, b);
//            else if (b instanceof TaskOccurrence bb)
//                return -compareTaskOccurrence(bb, a);
//            else
//                return compareTaskTask(a, b);
        }
    }




}
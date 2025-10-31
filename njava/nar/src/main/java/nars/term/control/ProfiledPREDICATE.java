package nars.term.control;

import jcog.Str;
import jcog.data.list.Lst;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.lang.System.nanoTime;

public class ProfiledPREDICATE<X> extends PREDICATE<X> {
    private static final Map<String, PrefProf> PROFILES_BY_TYPE = new ConcurrentHashMap<>();

    private final PREDICATE<X> delegate;

    private ProfiledPREDICATE(PREDICATE<X> delegate) {
        super(delegate.ref);
        this.delegate = delegate;
    }

    @Override
    public boolean test(X x) {
        long startTime = nanoTime();
        boolean result = delegate.test(x);
        long duration = nanoTime() - startTime;

        var profile = PROFILES_BY_TYPE.computeIfAbsent(
                id(unwrapDelegate(delegate)), z -> new PrefProf());

        profile.record(result, duration);

        return result;
    }

    private static String id(PREDICATE<?> p) {
        String s = p.getClass().toString();
        if (p instanceof TermMatching tm)
            s += "/" + tm.match.name();
        //TODO other decompositions
        return s;
    }

    @Override
    public float cost() {
        return delegate.cost();
    }

    public static <X> Function<PREDICATE<X>, PREDICATE<X>> wrap() {
        return p -> p instanceof ProfiledPREDICATE || p instanceof AND || p instanceof FORK ?
                p : new ProfiledPREDICATE<>(p);
    }

    public static void printStats() {
        System.out.println("Profiled Predicate Statistics:");

        // Sort the entries by descending mean time
        var sorted = new Lst<>(PROFILES_BY_TYPE.entrySet());
        sorted.sort((a, b) ->
            Double.compare(b.getValue().timeStatsUS.copy().getMean(),
                           a.getValue().timeStatsUS.copy().getMean()));

        for (var entry : sorted) {
            var c = entry.getKey();
            var p = entry.getValue();

            Histogram tts = p.timeStatsUS.copy();

            System.out.printf("Type: %s Count: %d, True Ratio: %.2f, Time: %s +- %s%n",
                c, (p.getTrueCount() + p.getFalseCount()), p.getTrueRatio(),
                Str.timeStr(1000 * tts.getMean()),
                Str.timeStr(1000 * tts.getStdDeviation())
            );
        }
    }
    private static PREDICATE<?> unwrapDelegate(PREDICATE<?> predicate) {
        return predicate instanceof NOT n ? n.cond : predicate;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ProfiledPREDICATE::printStats));
    }

    private static final class PrefProf {
        private final AtomicLong trueCount = new AtomicLong();
        private final AtomicLong falseCount = new AtomicLong();
        final static int highestTrackableValue = 1_000_000;
        final static int numberOfSignificantValueDigits = 3;
        private final Histogram timeStatsUS =
            new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
            //new ConcurrentHistogram(highestTrackableValue, numberOfSignificantValueDigits);

        long getTrueCount() {
            return trueCount.get();
        }

        long getFalseCount() {
            return falseCount.get();
        }

        double getTrueRatio() {
            long totalCount = getTrueCount() + getFalseCount();
            return totalCount > 0 ? (double) getTrueCount() / totalCount : 0.0;
        }

//        double getFalseRatio() {
//            long totalCount = getTrueCount() + getFalseCount();
//            return totalCount > 0 ? (double) getFalseCount() / totalCount : 0.0;
//        }

        synchronized void record(boolean result, long duration) {
            (result ? trueCount : falseCount).incrementAndGet();

            timeStatsUS.recordValue(duration /1000 /* ns -> ms */);
        }


    }
}
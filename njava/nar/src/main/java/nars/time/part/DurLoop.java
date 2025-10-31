package nars.time.part;

import jcog.Util;
import jcog.signal.FloatRange;
import nars.$;
import nars.NAR;
import nars.Term;
import nars.time.ScheduledTask;
import nars.util.NARPart;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static jcog.Str.n2;

/**
 * a part that executes a given procedure once every N durations (approximate)
 * N is an adjustable duration factor (float, so fractional values are ok)
 * the part is retriggered asynchronously so it is not guaranteed consistent
 * timing although some adjustment is applied to help correct for expectable
 * amounts of lag, jitter, latency, etc.
 *
 * these are scheduled by the NAR's Time which holds a priority queue of
 * temporal events.  at any given time it will contain zero or one of this
 * Dur's immutable and re-usable AtDur event.
 */
public abstract class DurLoop extends NARPart implements Consumer<NAR> {

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    public final FloatRange durations = new FloatRange(1, 0.5f, 16*1024);

    private final WhenDur at = new WhenDur();

    @Override
    public final WhenDur event() {
        return at;
    }
    
    protected DurLoop(Term id) {
        super(id);
    }

    protected DurLoop() {
        super();
    }


    /**
     * set period (in durations)
     */
    public DurLoop durs(float durations) {
        this.durations.setSafe(durations);
        return this;
    }

    @Override protected void starting(NAR nar) {
        at.next = Long.MIN_VALUE; //intial trigger
        nar.runAt(at);
    }

    public int durCyclesInt(NAR nar) {
        return Math.max(1, Math.round(durCycles(nar)));
    }

    public float durCycles(NAR nar) {
        return (float) (durations.doubleValue() * nar.dur());
    }

    public static final class DurRunnable extends DurLoop {
        private final Runnable r;

        public DurRunnable(Runnable r) {
            super($.identity(r));
            this.r = r;
        }

        @Override
        public void accept(NAR n) {
            r.run();
        }

    }

    private final class WhenDur extends ScheduledTask {

        /** Proportional gain (tune for stability and responsiveness) */
        private static final float kP = 0.2f;

        /**
         * when the last cycle ended
         */
        private long started = Long.MIN_VALUE, startedPrev = started;

        private final AtomicBoolean busy = new AtomicBoolean(false);
        static final private boolean trace = false;

        @Override
        public void accept(NAR n) {
            if (isOn())
                if (Util.enterAlone(busy)) {
                    before();
                    try {
                        DurLoop.this.accept(n);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        Util.exitAlone(busy);
                        after(n);
                    }
                }
                //else System.err.println("busy?"); //TEMPORARY
        }

        private void after(NAR nar) {
            next = next(nar);
            nar.runAt(this);
        }

        private void before() {
            this.startedPrev = this.started;
            this.started = nar.time();
        }

        private long next(NAR nar) {
            var dur = durCyclesInt(nar);

            var now = nar.time();
            var slow = (now - started) - dur;
            if (slow > 0 && trace)
                traceLag(slow, dur);

            if (this.startedPrev == Long.MIN_VALUE)
                this.startedPrev = this.started - dur;

            var startIdeal = startedPrev + dur;
            var late = started - startIdeal;
            nar.emotion.durLoop(DurLoop.this, Math.max(late, 0), Math.max(slow, 0), dur);

            return nextTimeAdaptive(now, startIdeal, dur);
            //return nextTime0(dur, startIdeal);
            //return nextTime1(dur);
        }

        private void traceLag(float slow, int dur) {
            var slowDurs = slow / dur;
            logger.info("{} slow {}%", DurLoop.this, n2(100 * slowDurs));
        }

        /** fixed rate, with correctional shift period, so that it attempts to maintain a steady rhythm and re-synch even if a frame is lagged*/
        private static long nextTimeAdaptive(final long now, final long startIdeal, int dur) {

            long nextIdeal = startIdeal + dur;
            long error = now - nextIdeal;

            // Adaptive Rescheduling:
            // If behind by more than one cycle, skip missed cycles and reset startIdeal.
            if (error > dur) {
                long missedCycles = error / dur;
                nextIdeal = (startIdeal + (missedCycles) * dur) + dur;
                error = now - nextIdeal;  // Recalculate error after rescheduling
            }

            // Proportional Control Adjustment:
            // Calculate adjustment, ensuring it's within safe bounds.
            long adjustment = (long) (error * kP);
            adjustment = Util.clampSafe(adjustment, -dur/2, +dur/2);

            // Calculate the next time, applying the adjustment.
            long nextTime = nextIdeal + adjustment;

            // Robustness Check:
            // Ensure the next time is always in the future. If the adjustment was too large (which shouldn't happen
            // with the current kP and bounds, but we check for extra robustness), fall back to a safe minimum delay.
            if (nextTime <= now) {
                nextTime = now + 1;
                //nextTime = now + Math.min(1, dur / 10); // Ensure at least 1 cycle or 10% of dur, whichever is smaller
            }

            return nextTime;
        }

        /** fixed delay */
        private long nextTime1(int dur) {
            return nar.time() + dur;
        }

        /** fixed rate, with correctional shift period, so that it attempts to maintain a steady rhythm and re-synch even if a frame is lagged*/
        private long nextTime0(int dur, long startedIdeal) {
            var nextIdeal = startedIdeal + dur;

            //TODO refine
            return (nextIdeal < started) ?
                started + dur //restart
                :
                nextIdeal; //ok soon enough to catch up
        }


        @Override
        public final Term term() {
            return DurLoop.this.term();
        }
    }
}
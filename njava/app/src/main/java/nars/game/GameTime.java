package nars.game;

import jcog.TODO;
import jcog.event.Off;
import jcog.exe.InstrumentedLoop;
import jcog.signal.IntRange;
import nars.NAR;
import nars.time.clock.RealTime;
import nars.time.part.DurLoop;
import org.eclipse.collections.api.block.function.primitive.DoubleToFloatFunction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** model for timing a game */
public abstract class GameTime {

    public static FPS fps(float fps) { return new FPS(fps); }
    public static Durs durs(float durs) { return new Durs(durs); }

    public static GameFrames frames(Game game) {
        return frames(game, 1);
    }

    public static GameFrames frames(Game game, int frameRateDivisor) {
        return new GameFrames(game, frameRateDivisor);
    }

    /** estimate the time of the next cycle */
    @Deprecated public abstract long next(long now);

    private final transient AtomicBoolean paused = new AtomicBoolean(false);

    public final void pause(boolean pause) {
        if (paused.getAndSet(pause)!=pause) {
//            synchronized(paused) {
//                if (pause) {
//                    stop();
//                } else {
//                    throw new TODO();
//                    //start(this);
//                }
//            }
        }
    }

    protected abstract void start(Game game);

    protected abstract void stop();

    /** in cycles; can change dynamically */
    public abstract float dur();


    /** measured in realtime
     * TODO async loop for extended sleep periods
     * */
    public static class FPS extends GameTime {

        private transient float fps;
        private transient InstrumentedLoop loop;
        private transient Runnable g;
        private transient DoubleToFloatFunction secToDur = (x)-> (float) x;


        public FPS(float fps) {
            this.fps = fps;
        }

        @Override
        protected void start(Game game) {
            this.g = game::next;
            this.secToDur = s -> Math.max(1, (float) ((RealTime) game.nar.time).secondsToUnits(s));

            loop = new InstrumentedLoop() {
//                @Override
//                public void execute(AbstractTimer ignored) {
//                    game.nar.exe.execute(this);
//                }

                @Override public boolean next() {
                g.run();
                return true;
                }
            };
            loop.fps(fps);
        }

        @Override
        protected void stop() {
            fps = loop.fps();
            loop.stop();
            loop = null;
            g = ()->{};
            //children.forEach(c -> c.pause(true));
        }


        //        final FastCoWList<FPS> children = new FastCoWList<>(FPS.class);

//        @Override
//        public GameTime chain(float periodMultiplier) {
//            FPS parent = this;
//            return new FPS(initialFPS /* HACK */ / periodMultiplier) {
//
//                {
//                    children.add(this);
//                }
//
//                @Override
//                public float dur() {
//                    return parent.dur * periodMultiplier;
//                }
//            };
//        }

//        @Override
//        public float period() {
////            TreeMap m = new TreeMap();
////            loop.stats("", m);
////            System.out.println(this.g + " " + m);
//            return period;
//        }

        @Override
        public long next(long now) {
            return now + Math.max(1, (int) dur());
        }

        @Override
        public float dur() {
            return secToDur.valueOf(loop.periodSec());
        }

//
//        @Override protected Off clock(Game g) {
////            if (!(g.nar().time instanceof RealTime))
////                throw new UnsupportedOperationException("realtime clock required");
//
//            this.g = g;
//
//            return new NARSubLoop();
//        }
//
//        private final class NARSubLoop extends NARPart {
//            @Override
//            protected void starting(NAR nar) {
//                loop.fps(fps);
//            }
//
//            @Override
//            protected void stopping(NAR nar) {
//                loop.stop();
//            }
//        }
    }

    /** measured in # of perceptual durations */
    public static class Durs extends GameTime {

        private final transient float durPeriod;

        public DurLoop loop;
        private Game g;
//        private float cycles = 1;
//        private Runnable resume = null;

        Durs(float durPeriod) {
            this.durPeriod = durPeriod;
        }

        @Override
        public float dur() {
            //Game g = this.g;
            //return g !=null ? Math.max(1, loop.cyclesFromLastDur) /*loop.durCycles(g.nar)*/ : 1;
            return loop.durCyclesInt(loop.nar);
        }

        @Override
        protected void start(Game g) {
            assert(this.g == null);
            assert(loop==null);
            loop = new DurLoop.DurRunnable((this.g = g)::next);
            loop.nar = g.nar; //HACK
            loop.durs(durPeriod);
            g.nar.add(loop);
        }

        @Override
        protected void stop() {
            loop.close();
            loop.nar = null;
            loop = null;
        }

        @Override
        public long next(long now) {
            DurLoop l = this.loop;
            NAR n = l.nar;
            return n != null ? now + l.durCyclesInt(n) : now;
        }
    }

    /** chains a game's clock to another, divided by an integer number of frames >= 1*/
    private static class GameFrames extends GameTime {

        public final IntRange divisor = new IntRange(1, 1, 16);
        private final Game superGame;
        private Off onFrame;

        GameFrames(Game superGame, int frameRateDivisor) {
            super();
            this.superGame = superGame;
            this.divisor.set(frameRateDivisor);
        }

        @Override
        public void start(Game subGame) {
            onFrame = superGame.afterFrame(new Consumer<>() {

                private int frame = 0;

                @Override
                public void accept(Game sg) {
                    if (frame++ % divisor.intValue() == 0)
                        subGame.next();
                }
            });
        }

        @Override
        public long next(long now) {
            throw new TODO();
        }


        @Override
        protected void stop() {
            onFrame.close();
            onFrame = null;
        }

        @Override
        public float dur() {
            return superGame.clock.dur() * divisor.floatValue();
        }

    }

//    public static Cycs cycles(float cycles) { return new Cycs(cycles); }
}
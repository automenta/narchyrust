package nars.game.sensor;

import jcog.TODO;
import jcog.Util;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.Term;
import nars.Truth;
import nars.game.Game;
import nars.game.action.AbstractAction;
import nars.game.action.AbstractGoalAction;
import nars.game.action.BiPolarAction;
import nars.sensor.BitmapSensor;
import nars.term.Termed;

import java.util.List;

import static nars.game.NAct.NEG;
import static nars.game.NAct.POS;

/**
 * TODO not finished
 *
 * random access memory read/[write] I/O device
 *
 * param:
 *   word length
 *   num words
 *   read-only?
 *   read momentum, write momentum
 *   address momentum
 *   read-only bits per word (ex: can prefix each word with header bits)
 *
 * Actions:
 *
 *   address + -
 *
 *   current word goals
 *
 * Sensors
 *
 *   current word beliefs (accessed through any VectorSensor perceptual impl: 1D, 2D etc)

 *   optional: current address signal (absolute position via DigitizedScalar)
 *
 */
public class BitSensor extends AbstractSensor {

    public final int words, width;
    private final BiPolarAction addr;
    public final AbstractSensor addrPct;
    private final AbstractAction writingAction;
    @Deprecated private final Game game;
    private float writing = 1;
    float momentum =
        //0;
        0.5f;
        //0.9f;
        //0.99f;
        //0.1f;

    public final float get(int x, int y) {
        return bit(x, y).get();
    }

    public Bit bit(int x, int y) {
        return data[y].bits[x];
    }

    @Override
    public int size() {
        throw new TODO();
    }


    public static class Bit {

        private volatile float value = Float.NaN;
        boolean writable = true;

        public void set(float v) {
            if (writable)
                _set(v);
        }

        void _set(float v) {
            this.value = v;
        }

        public float get() {
            return value;
        }
    }

    /** current values of the active word as read by the vector sensor */
    final float[] active, activeNext;

    public class Word {
        public final Bit[] bits;

        Word() {
            this.bits = new Bit[width];
            for (int i = 0; i < width; i++)
                bits[i] = new Bit();
        }

        public void load() {
            for (int i = 0; i < width; i++)
                activeNext[i] = active[i] = bits[i].get();
        }

        public void save() {
            float res = resolution();
            for (int i = 0; i < width; i++)
                bits[i].set(Truth.freq(activeNext[i], res));
        }
    }

    public final AbstractGoalAction[] write;
    public final VectorSensor read;
    public final Word[] data;


    volatile int address = 0, nextAddress = 0;

    public BitSensor(Term id, int width, int words, Game g) {
        super(id);
        this.words = words;
        this.width = width;

        active = new float[width];
        activeNext = new float[width];

        data = new Word[words];
        for (int i = 0; i < words; i++)
            data[i] = new Word();

        this.read = g.addSensor(new BitmapSensor(new Bitmap2D() { //HACK
            @Override
            public int width() {
                return width;
            }

            @Override
            public int height() {
                return 1;
            }

            @Override
            public float value(int x, int y) {
                return active[x];
            }
        }, $.p("read", id)));

        Term WRITING = $.p("write", id);
        this.write = Util.arrayOf(i -> {
            Term bitID = $.inh(WRITING, $.the(i));
            return g.action(bitID, j -> {
                write(i, j);
                return j;
            });
        }, new AbstractGoalAction[width]);


//        this.addr = g.actionMutex($.inh(id, $.p("addr", NEG)), $.inh(id, $.p("addr", POS)), (l)->{
//            if (address - 1 < 0) address = sensor.length - 1;
//            else address --;
//        }, (r)->{
//            if (address + 1 == sensor.length) address = 0;
//            else address++;
//        });

        this.writingAction = g.action(WRITING, x->{
            this.writing = x==x ? x : 0;
        });

        if (words > 1) {
            //TODO optional wrap-around
            Term ADDR = $.p("addr", id);
            this.addr = g.actionToggle($.inh(ADDR, NEG), $.inh(ADDR, POS), () -> {
                nextAddress = address - 1;
                if (nextAddress < 0) nextAddress = 0;
            }, () -> {
                nextAddress = address + 1;
                if (nextAddress >= words) nextAddress = words - 1;
            });

            this.addrPct = g.sense($.inh(ADDR, "is"), ()->((float)address)/(words-1));

        } else {
            this.addr = null;
            this.addrPct = null;
        }


        this.game = g;

        g.beforeFrame(this);
    }

    public void write(int bit, float next) {
        activeNext[bit] = next(next, active[bit]);
    }

//    @Override
//    public void start(Game g) {
//        super.start(g);
//
//        NAR nar = g.nar;
//        nar.runLater(()->{ //HACK
//        {
//            PriTree pri = nar.pri;
//            PriNode sensors = new PriAmp(write) {
//                @Override
//                public PriAmp amp(float a) {
//                    return super.amp(a/width);
//                }
//            };
//            pri.add(sensors);
////            for (AbstractGoalAction s : write) {
////                PriAmp ss = s.concept.sensor.pri;
////                //unlink
////                pri.remove(ss); //HACK just to clear edges
////                pri.add(ss);
////                //re-link
////                pri.link(ss, sensors);
////            }
//            pri.link(sensors, game.actions.pri);
//        }
//        });
//
//    }

    /** can implement any kind of interpolation or blending of incoming with existing value */
    protected float next(float next, float prev) {
        if (prev != prev && next==next) return next; //ON
        else if (next!=next) return Float.NaN; //OFF
        else return Util.lerpSafe((1 - momentum) * writing, prev, next); //MIX
    }

    @Override
    public Iterable<? extends Termed> components() {
        return List.of(write);
    }

    @Override
    public void accept(Game g) {

        //update sensor/action resolution
        float res = sensing.freqRes;
        for (AbstractGoalAction w : write)
            w.freqRes(res);
        read.freqRes(res);

        Word w = data[address];

        w.save();

        if (nextAddress!=address)
            w = data[address = nextAddress];

        w.load();
    }

}
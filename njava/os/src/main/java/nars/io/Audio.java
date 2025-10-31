package nars.io;

import jcog.signal.FloatRange;
import jcog.signal.wave1d.SignalInput;
import nars.$;
import nars.NAR;
import nars.game.Game;
import nars.game.sensor.FreqVectorSensor;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import spacegraph.audio.AudioSource;

import javax.sound.sampled.LineUnavailableException;

/**
 * global audio input (mixed by the sound system)
 */
public class Audio extends Device {

    public final SignalInput in;
    /**
     * updates per time unit
     */
    private final FloatRange rate = new FloatRange(30, 0.5f, 120);
    @Deprecated
    public final FreqVectorSensor hear;
    private final AudioSource audio;

    private static final Atom FREQ = Atomic.atom("freq"); //TODO ?
    private static final Atom PHASE = Atomic.atom("phase"); //TODO angle or theta

    public Audio(AudioSource a, NAR n, float fps) {
        /*HACK*/
        super($.quote(a.name()));

        SignalInput src = new SignalInput();
        src.set(a, 0.04f);
        this.audio = a;

        this.in = src;
        this.rate.set(fps);

        Game h = new Game($.inh(id, "hear"));


        //            CircularFloatBuffer hearBuf = new CircularFloatBuffer(8192);
        int bands = 32;

        hear = new FreqVectorSensor(bands, 2048, 120, 1024,
                f -> $.inh(id, $.p(Int.i(f%bands),
                        f >= bands ? PHASE : FREQ)
                ), n);

        hear.on(src);

        h.addSensor(hear);

        n.add(h);
    }


    @Override
    protected void stopping(NAR nar) {
        audio.stop();
        in.stop();
        super.stopping(nar);
    }

    @Override
    protected void starting(NAR nar) {
        try {
            audio.start();
        } catch (LineUnavailableException e) {
            logger.warn("{} {}", audio, e);
            stop(nar);
            return;
        }

        super.starting(nar);
        in.setPeriodMS(Math.round(1000f / rate.floatValue()));
    }
}
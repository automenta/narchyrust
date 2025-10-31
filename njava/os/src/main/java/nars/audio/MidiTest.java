package nars.audio;

import jcog.Util;
import nars.*;
import nars.deriver.impl.SerialDeriver;
import nars.game.Game;
import nars.gui.graph.TasksView;
import nars.time.Tense;
import nars.time.clock.RealTime;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.audio.midi.MIDISynth;

import javax.sound.midi.MidiUnavailableException;

import static nars.$.$$;
import static nars.Op.GOAL;
import static spacegraph.SpaceGraph.window;

public class MidiTest extends Game {

    private final MIDISynth ctrl;

    public MidiTest() throws MidiUnavailableException {
        super("midi");
        ctrl = new MIDISynth();
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            int key = 45 + i;
            actionPushButton($.inh(id, String.valueOf((char)('a'+i))), new BooleanProcedure() {
                boolean pressed = false;
                @Override
                public void value(boolean o) {
                    if (o) {
                        if (!pressed) {
                            ctrl.press(key, 127);
                            pressed = true;
                        }
                    } else {
                        if (pressed) {
                            ctrl.release(key, 127);
                            pressed = false;
                        }
                    }
                }
            }).goalDefault($.t(0, 0.1f), nar);
        }

    }

    public static void main(String[] args) throws MidiUnavailableException, Narsese.NarseseException {
        NAR n = //NARS.realtime(30f).get();
            new NARS.DefaultNAR(0, 0, true).time(new RealTime.MS()).get();
        n.complexMax.set(10);
        n.freqRes.set(0.1f);
        n.timeRes.set(20);
        n.time.dur(50);


        n.startFPS(50f);

        MidiTest g = new MidiTest();
//        ((Curiosity)g.actions.curiosity).enable.set(false);

        n.add(g);

//        d.everyCycle(g.focus());
        g.focus().log();
//        g.focus().logTo(System.out, t -> t instanceof NALTask && ((NALTask)t).GOAL());

//        n.startFPS(20f);
//        new Thread(()->n.start()).start();

//        Util.sleepMS(2500);

        Deriver d = new SerialDeriver(NARS.Rules.nal(6, 8).core().stm().temporalInduction().compile(n), n);
        //d.iter = 1;
        d.next(g.focus());
        new Thread(()->{
            while (true) {
                d.next(null);
                Util.sleepMS(500);
            }
        }).start();


        window(TasksView.timeline(()->n.tasks().iterator(), 0, n.time()+1)
                .withControls()
                , 800, 600);

        for (int i = 0; i < 32; i++) {
            key(n, g, 1);
            key(n, g, 3);
            key(n, g, 5);
            key(n, g, 7);
            key(n, g, 9);
        }




    }

    private static void key(NAR n, MidiTest g, int i) {

        Term note = $$("(midi-->" + (char) (i + 'a') + ")");
        int dur = 250;
        play(n, g, note, 1, dur);
        play(n, g, note, 0, dur);
    }

    private static void play(NAR n, MidiTest g, Term note, int f, int dur) {

        float conf = f > 0.5f ? 0.5f : 0.25f;

        g.focus().remember(NALTask.task(note,
                GOAL, $.t(f, conf),
                Tense.dither(n.time(), n.timeRes()),
                Tense.dither(n.time()+ dur, n.timeRes()),
                n.evidence()).withPri(1));

        Util.sleepMS(dur);
    }
}
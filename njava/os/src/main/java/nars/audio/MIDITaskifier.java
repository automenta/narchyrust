package nars.audio;

import jcog.data.list.Lst;
import jcog.exe.Loop;
import nars.*;
import nars.game.Game;
import nars.game.action.AbstractAction;
import nars.game.action.GoalAction;
import nars.gui.NARui;
import nars.time.Tense;
import spacegraph.SpaceGraph;
import spacegraph.audio.synth.SineWave;

import javax.sound.midi.*;
import java.util.Arrays;
import java.util.List;

import static nars.Op.*;


/**
 * generic MIDI input interface
 */
public class MIDITaskifier extends Game {

    List<AbstractAction> keys = new Lst<>();
	float[] volume = new float[128];

	public MIDITaskifier() {
	    super("midi");
    }

    @Override
    protected void init() {

        MidiInReceiver midi = MIDI();

		Arrays.fill(volume, Float.NaN);

		SoNAR s = new SoNAR(nar);

		for (int i = 36; i <= 51; i++) {
			Term key = channelKey(9, i);

			Term keyTerm = $.p(key);

			var c = new GoalAction(keyTerm, (b, d) -> {

				if (d == null)
					return Float.NaN;

				float v = d.freq();
				if (v > 0.55f)
					return v;
				else if (b != null && b.freq() > 0.5f)
					return 0;
				else
					return Float.NaN;
			});


			nar.input(NALTask.taskUnsafe(c.term(), BELIEF, $.t(0f, 0.35f), ETERNAL, ETERNAL, nar.evidence()));
			nar.input(NALTask.taskUnsafe(c.term(), GOAL, $.t(0f, 0.1f), ETERNAL, ETERNAL, nar.evidence()));



			keys.add(c);


			s.listen(c.concept, (k) -> new SineWave((float) (100 + Math.random() * 1000)));

		}
		onFrame(()->{
            for (int i = 36; i <= 51; i++) {

                float v = volume[i];

                if (v == 0) {
                    volume[i] = Float.NaN;
                }


//                int dur = n.dur();
                //c.update(n.time()-dur, n.time(), null);
//                keys.get(i-36).accept(null);
            }
        });




	}

	public static void main(String[] arg) {

        NAR nar = NARS.threadSafe();
        nar.complexMax.set(16);

        MIDITaskifier midi = new MIDITaskifier();
        nar.add(midi);

//        nar.onTask(t -> {
//            if (t instanceof DerivedTask && t.isGoal()) {
//
//                System.err.println(t.proof());
//            }
//        });


        new Loop(2f) {

            final Term now = $.p("now");

            @Override
            public boolean next() {
                nar.believe(now, Tense.Present);
                return true;
            }
        };


        SpaceGraph.window(NARui.beliefCharts(midi.keys, nar), 900, 900);


        nar.startFPS(60f);
    }

	public static boolean receive(MidiDevice device) {
		return device.getDeviceInfo().getName().startsWith("MPD218");
	}

	public static Term channelKey(int channel, int key) {
		return $.the(key);
	}

	public MidiInReceiver MIDI() {

		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

		for (MidiDevice.Info info : infos) {
			try {
				MidiDevice.Info ii = info;

				MidiDevice device = MidiSystem.getMidiDevice(ii);

				System.out.println(device + "\t" + device.getClass());
				System.out.println("\t" + device.getDeviceInfo());
				System.out.println("\ttx: " + device.getTransmitters());
				System.out.println("\trx: " + device.getReceivers());

				if (receive(device)) {
					return new MidiInReceiver(device);
				}

                /*if (device instanceof Synthesizer) {
                    synthInfos.addAt((Synthesizer) ii);
                } else if (device instanceof MidiDevice) {
                    midis.addAt((MidiDevice) ii);
                }*/
			} catch (MidiUnavailableException e) {

			}
		}

		return null;
	}

	public class MidiInReceiver implements Receiver {


		public MidiInReceiver(MidiDevice device) throws MidiUnavailableException {

			if (!device.isOpen()) {
				device.open();
			}

			device.getTransmitter().setReceiver(this);
		}

		@Override
		public void send(MidiMessage m, long timeStamp) {


			if (m instanceof ShortMessage s) {
				int cmd = s.getCommand();
				switch (cmd) {
					case ShortMessage.NOTE_OFF:
						if ((volume[s.getData1()] == volume[s.getData1()]) && (volume[s.getData1()] > 0))
							volume[s.getData1()] = 0;


						break;
					case ShortMessage.NOTE_ON:
						volume[s.getData1()] = 0.6f + 0.4f * s.getData2() / 128f;


						break;
					default:

						break;

				}
			}

		}


		@Override
		public void close() {

		}
	}



}
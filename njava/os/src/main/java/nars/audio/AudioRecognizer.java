package nars.audio;

import com.google.common.io.Files;
import jcog.exe.Loop;
import jcog.markov.MarkovSampler;
import jcog.markov.MarkovSentence;
import jcog.signal.wave1d.SignalInput;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.func.Commentary;
import nars.func.language.NARHear;
import nars.func.language.Vocalize;
import nars.game.Game;
import nars.game.sensor.FreqVectorSensor;
import nars.gui.FocusUI;
import nars.gui.sensor.VectorSensorChart;
import nars.time.clock.RealTime;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.audio.speech.NativeSpeechDispatcher;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.textedit.TextEdit;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.jogamp.newt.event.KeyEvent.VK_ENTER;
import static nars.Op.BELIEF;
import static nars.game.GameTime.fps;
import static spacegraph.space2d.container.grid.Containers.grid;

public class AudioRecognizer {

	final SignalInput in;

	/**
	 * updates per time unit
	 */
	//private final FloatRange rate = new FloatRange(30, 0.5f, 120);
	public AudioRecognizer() throws LineUnavailableException {
		float fps = 15f;

		NAR nar = new NARS.DefaultNAR(8, true).time(new RealTime.MS().durFPS(fps)).get();
		nar.complexMax.set(64);

		AudioSource audio = AudioSource.all().get(0); //HACK
		in = new SignalInput();
		in.set(audio, 2f/fps);
		in.fps(fps);
		audio.start();

		//this.rate.set(fps);

		Game h = new Game("that", fps(fps));

		FreqVectorSensor hear = new FreqVectorSensor(32, 2048, 0, 2048, f -> $.inh(h.id, $.the(f)), nar);
		hear.on(in);

		h.addSensor(hear);

		nar.add(h);

        h.sensors.pri.amp(0.1f);

		TextEdit input = new TextEdit(20, 4);
        //                new VectorSensorChart(hear, n),
        //spectrogram(hear.buf, 0.1f,512, 16),
        //			new ConceptListView(h.what(), 32),
        //			new TaskListView(h.what(), 16),
        //						try {
        /*keyboard*/
        //							nar.input("$1.0 hear" +
        //								//$.p(wokenize.tokenize(msg).tokenize(i)) +
        //								":" + $.quote(i) +
        //								". |");
        //						} catch (Narsese.NarseseException e) {
        //							e.printStackTrace();
        //						}
        SpaceGraph.window(grid(
                new VectorSensorChart(hear, nar).withControls(),
//                new VectorSensorChart(hear, n),
                //spectrogram(hear.buf, 0.1f,512, 16),
                FocusUI.focusUI(h.focus()),
//			new ConceptListView(h.what(), 32),
//			new TaskListView(h.what(), 16),
                new ObjectSurface(hear), input.onKeyPress(ke -> {
                    if (ke.getKeyCode() == VK_ENTER) {
                        String i = input.text().trim();
                        input.clear();
                        if (!i.isEmpty()) {
//						try {
                            NARHear.hear(i, "ok" /*keyboard*/, nar);
//							nar.input("$1.0 hear" +
//								//$.p(wokenize.tokenize(msg).tokenize(i)) +
//								":" + $.quote(i) +
//								". |");
//						} catch (Narsese.NarseseException e) {
//							e.printStackTrace();
//						}
                        }
                    }
                })), 400, 400);

        nar.startFPS(fps);

		nar.add(new Commentary(nar.main()));

		Vocalize v = new Vocalize(nar, 1f, new NativeSpeechDispatcher()::speak);
		MarkovSentence m = new MarkovSentence();
		try {
			m.learnTokenize(3, Files.toString(new File("/tmp/t.txt"), Charset.defaultCharset()));
		} catch (IOException e) { e.printStackTrace(); }

		MarkovSampler<String> mm = new MarkovSampler<>(m);
		Loop.of(()->{
			String s = mm.next(true);
			v.speak($.quote(s), nar.time(), $.tt(1,nar.confDefault(BELIEF)));
			NARHear.hear(s, "uh" /*inner monolog*/, nar);
		}).fps(2f);
	}

	public static void main(String[] args) throws LineUnavailableException {

		new AudioRecognizer();

	}


}
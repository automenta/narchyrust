package nars.io;

import com.github.sarxos.webcam.Webcam;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import nars.GUI;
import nars.NAR;
import nars.NARS;
import nars.Term;
import nars.gui.NARPartEnabler;
import nars.gui.sensor.VectorSensorChart;
import nars.util.NARPart;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.SignalView;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.text.Labelling;
import spacegraph.video.VideoSurface;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * hardware abstraction for accessing devices
 */
public class Devices extends NARPart {

    private final ConcurrentFastIteratingHashMap<Term, Device> devices = new ConcurrentFastIteratingHashMap<>(new Device[0]);

    public static void main(String[] args)  {

        NAR n = NARS.tmp();
        n.complexMax.set(30);
//        n.log();

        Devices h = new Devices();
        n.add(h);

        n.startFPS(60f);

        GUI.gui(n);



        SpaceGraph.window(new Gridding(h.devices.valueStream().map(d -> Labelling.the(d.term().toString(), d instanceof Audio ?
                audioWidget((Audio) d) :
                videoWidget((Video) d)
        ))), 800, 800);
    }

    private static Surface videoWidget(Video v) {
        return new VideoSurface(v.c);
    }

    private static Surface audioWidget(Audio a) {
        return new Splitting(
            new Splitting(
                    new VectorSensorChart(a.hear, a.nar).withControls(),
                    0.5f,
                    new SignalView(a.in)
            ).resizeable(),
            0.9f,
            new Gridding(

                /** TODO extract PartEnableToggleButton class */
                new CheckBox("+", new NARPartEnabler(a)
            ).set(a.isOn() /* TODO intermediate state */),new ObjectSurface(a.hear))
        ).horizontal().resizeable();

        //            WaveBitmap hearView = new WaveBitmap(hearBuf, 300, 64);
        //            h.onFrame(hearView::update);

        //                new VectorSensorChart(hear, n),
        //spectrogram(hear.buf, 0.1f,512, 16),

    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        Stream.<Supplier<Stream<Runnable>>>of(this::startAudio, this::startVideo)
            .parallel()
            .flatMap(Supplier::get)
            .forEach(Runnable::run); //HACK
    }


    private Stream<Runnable> startVideo() {
        return Webcam.getWebcams().parallelStream().map(z->
                ()->webcamStart(z));
    }

    private Stream<Runnable> startAudio() {
        return AudioSource.all().parallelStream().map(a ->
            ()->start(new Audio(a, nar, 30f))
        );
    }

    private void webcamStart(Webcam w) {
        try {
              start(new Video(w));
        } catch (RuntimeException e) {
            logger.warn("{} {}", w.getDevice().getName(), e.getMessage());
        }
    }


    private void start(Device d) {
        nar.add(d);
        devices.put(d.id, d);
    }


}
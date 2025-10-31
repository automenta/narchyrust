package nars.video;

import com.jogamp.opengl.GL2;
import jcog.cluster.NeuralGasNet;
import jcog.data.Centroid;
import jcog.math.FloatSupplier;
import jcog.math.normalize.FloatNormalized;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.Term;
import nars.game.Game;
import nars.term.atom.Atomic;
import nars.util.Timed;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.video.Draw;

import java.util.function.Consumer;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraGasNet<P extends Bitmap2D> implements Consumer<Game> {

    private final Timed timed;

    private final P src;


    final NeuralGasNet net;

    public CameraGasNet(Atomic root, P src, Game agent, int blobs) {

        this.src = src;

        this.timed = agent.nar();

        this.net = new NeuralGasNet(3, (short)blobs) {
            @Override
            public Centroid newCentroid(int i, int dims) {
                return new Centroid(i, dims);
            }
        };

        int width = src.width();
        int height = src.height();

        for (int j = 0; j < blobs; j++) {
            int i = j;
            Term base = $.func("blob", $.the(i), root);

            FloatSupplier v2 = () -> {
                Centroid node = net.node(i);
                if (node != null)
                    return (float) node.getEntry(0);
                else
                    return Float.NaN;
            };
            agent.sense($.inh(base, Atomic.atomic("x")), new FloatNormalized(v2));
            FloatSupplier v1 = () -> {
                Centroid node = net.node(i);
                if (node != null)
                    return (float) node.getEntry(1);
                else
                    return Float.NaN;
            };
            agent.sense($.inh(base, Atomic.atomic("y")), new FloatNormalized(v1));
            FloatSupplier v = () -> {
                Centroid node = net.node(i);
                if (node != null)
                    return (float) node.getEntry(2);
                else
                    return Float.NaN;
            };
            agent.sense($.inh(base, Atomic.atomic("c")), new FloatNormalized(v));


        }


        agent.onFrame(this);

        SpaceGraph.window(new PaintSurface() {
            @Override
            protected void paint(GL2 gl, ReSurface reSurface) {
                int nodes = net.size();
                for (int i = 0; i < nodes; i++) {
                    Centroid n = net.node(i);
                    float e = (float) ((1f + n.localError()));
                    float x = (float) n.getEntry(0);
                    float y = (float) n.getEntry(1);
                    float c = (float) n.getEntry(2);
                    gl.glColor4f(c, 0, (0.25f * (1f - c)), 0.75f);
                    float r = 0.1f / (1f + e);
                    Draw.rect(x, 1f - y, r, r, gl);
                }
            }
        }, 500, 500);
    }



    @Override
    public void accept(Game g) {

        src.updateBitmap();

        int width = src.width();
        int height = src.height();

        int pixels = width * height;

        net.alpha.set(0.005f);

        net.setLambdaPeriod(64);
        
        net.setWinnerUpdateRate(0.05f, 0.01f);


        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                float color = src.value(w, h);
                if (timed.random().nextFloat() - 0.05f <= color)
                
                    net.put(w/((float)width), h/((float)height), color );
            }
        }
    }

}
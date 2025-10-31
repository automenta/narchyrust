package nars.video;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.func.IntIntToFloatFunction;
import jcog.io.BinTxt;
import jcog.math.GilbertCurve;
import jcog.signal.FloatRange;
import jcog.signal.wave2d.Bitmap2D;
import jcog.tensor.model.AbstractAutoencoder;
import jcog.tensor.Tensor;
import jcog.tensor.model.VanillaAutoencoder;
import nars.$;
import nars.Term;
import nars.game.Controlled;
import nars.game.Game;
import nars.game.sensor.SignalComponent;
import nars.game.sensor.VectorSensor;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * similar to a convolutional autoencoder
 * TODO pixelConf should be used in Signal truther
 * TODO encode multiple source feature channels besides just brightness, ex: hue
 */
public class AutoClassifiedBitmap extends VectorSensor implements Controlled<Game> {

    private static final Logger logger = LoggerFactory.getLogger(AutoClassifiedBitmap.class);

    public final AbstractAutoencoder ae;

    /** learning rate */
    public final FloatRange alpha = new FloatRange(
            //5E-3f //SGD
            2E-5f //ADAM
            , 0, 0.02f);

    public final float[][] pixRecon;
//    public final FloatRange confResolution = new FloatRange(0, 0, 1);
    private final Lst<SignalComponent> signals;
//    @Deprecated private final MetaBits metabits;

    /** per-pixel feature inputs */
    private final IntIntToFloatFunction[] pixIn;

    /**
     * interpret as the frequency component of each encoded cell
     */
    private final double[][][] encoded;
    private final double[] ins;

    /** superpixel's dimensions in pixels */
    private final int sw, sh;

    /** output dimensions in superpixels */
    private final int nw, nh;

    /** input dimensions in pixels */
    private final int pw, ph;

    private final Game game;
    private final Term[] feature;
    private final Term idPattern;

    public Bitmap2D src;
    public boolean reconstruct = false;


//    /** pre-dithers the autoencoder's encodings to the
//     *  specified (frequency) resolution prior to backprop.
//     *  as if it were a clue to the autoencoder that the extra
//     *  precision will be discarded so it should settle on a
//     *  discretized result instead.
//     */
//    private final boolean preDither = false;
    public int learnRandom = 0;
    public boolean learnTiles = true;

    @Deprecated public AutoClassifiedBitmap(Term root, float[][] pixIn, int sw, int sh, int states, Game game) {
        this(root, null,
                new IntIntToFloatFunction[] { (x, y) -> pixIn[x][y] },
                pixIn.length, pixIn[0].length,
                sw, sh, states, game);
    }


    public AutoClassifiedBitmap(Term root, Bitmap2D src, int superPixelsWide, int superPixelsHigh, int states, Game game) {
        this(root, src, superPixelsWide, superPixelsHigh, states, game, src::value);
    }

    public AutoClassifiedBitmap(Term root, Bitmap2D src, int superPixelsWide, int superPixelsHigh, int states, Game game, IntIntToFloatFunction... pixIn) {
        this(root, src, pixIn,
                src.width(), src.height(),
                src.width()/superPixelsWide, src.height()/superPixelsHigh,
                states, game);
    }

    @Override
    public void controlStart(Game g) {
        if (src instanceof Controlled c)
            c.controlStart(g); //HACK proxy to the source bitmap
    }

    /**
     * @param pw (& ph) dimensions of input bitmap
     * @param sw (& sh) dimensions of each superpixel
     *
     * metabits must consistently return an array of the same size, since now the size of this autoencoder is locked to its dimension
     */
    private AutoClassifiedBitmap(@Nullable Term id, Bitmap2D src, IntIntToFloatFunction[] pixIn, int pw, int ph, int sw, int sh, int features, Game game) {
        super(id, (pw / sw) * (ph / sh) * features);
        this.src = src;

        if (pw % sw != 0)
            throw new UnsupportedOperationException("input width must be divisible by superpixel width");
        if (ph % sh != 0)
            throw new UnsupportedOperationException("input height must be divisible by superpixel height");

        this.idPattern = id; //since it may differ from super(id)'s

//        this.metabits = metabits;
        this.game = game;

        assert(pixIn.length > 0);
        this.pixIn = pixIn;

        this.sw = sw;
        this.sh = sh;
        this.pw = pw;
        this.ph = ph;
        this.nw = pw / sw;
        this.nh = ph / sh;

        int ins = sw * sh * pixIn.length;// + metabits.get(0, 0).length;
        this.ins = new double[ins];

        this.encoded = new double[nh][nw][features];

        //this.pixConf = new float[nw][nh];

        this.pixRecon = new float[ph][pw];

        this.feature = new Term[features];
        for (int i = 0; i < features; i++)
            feature[i] = $.quote(BinTxt.uuid64()); //HACK TODO check for repeat though extremely rare

        this.signals = new Lst<>(tileCount());

        {
            ae = new VanillaAutoencoder(ins,
                    new int[] { } //NO hidden layer
                    //new int[] { features }
                    //new int[] { features, features }
                    //new int[] { features, features, features }
                    //new int[] { features+1 }
                    //new int[] { features*3/2 }
                    //new int[] { features*2 }
                    //new int[] { ins+features }
                    //new int[] { Fuzzy.mean(ins, features) }
                    //new int[] { Fuzzy.mean(ins, features), Fuzzy.mean(ins, features) }
                , features, alpha);
        }

        {

//        ae = new ClassicAutoencoder(ins, features, alpha, new XoRoShiRo128PlusRandom());

        //((ClassicAutoencoder)ae).corruption.set(0.01f);

//        ((ClassicAutoencoder)ae).activation =
//            ReluActivation.the; //fast
//            //LeakyReluActivation.the; //fast
//            //SigmoidActivation.the;
//            //SoftPlusActivation.the;
//            //SigLinearActivation.the; //fast
        }
        ae.latent = (x, y, z) -> {
            int p = superPixelCurrent;
            if (p >= 0) {
                int xx = p % nw, yy = p / nw;
                storeEncoding(xx, yy, y);

                if (reconstruct)
                    reconstructEncoding(xx, yy, z);
            }
        };


        game.addSensor(this);
    }

    @Override
    public void start(Game game) {
        super.start(game);

        int features = features();
        GilbertCurve.gilbertCurve(nw, nh, (x, y)->{
            for (int f = 0; f < features; f++) {
                int ff = f;
                signals.add(component(term(idPattern, x, y, f),
                        () -> (float) encoded[y][x][ff], game.nar));
            }
        });
        logger.info("{} pixels {}x{} ({}) => encoded {}x{} x {} features ({})", this, pw, ph, (pw * ph), nw, nh, features, signals.size());

    }

    public final int features() {
        return feature.length;
    }

    public AutoClassifiedBitmap alpha(float alpha) {
        this.alpha.set(alpha);
        return this;
    }

    @Override
    public final void accept(Game g) {
        this.updateAutoclassifier();
        super.accept(g);
    }

    @Override
    public Iterator<SignalComponent> iterator() {
        return signals.iterator();
    }

    /**
     * @param root
     * @param x    x coordinate
     * @param y    y coordinate
     * @param feature    feature
     */
    protected Term term(Term root, int x, int y, int feature) {
        Term f = this.feature[feature];
        Atomic X = $.the(x), Y = $.the(y);
        if (root.varDep() == 3) {
            return root.replace(Map.of(
                $.varDep(1), X,
                $.varDep(2), Y,
                $.varDep(3), f
            ));
        } else {
            /* default pattern */
            return $.inh(root, $.p($.p(X, Y), f));
            //return $.inh(root, $.p(X, Y, f));
            //return $.inh($.p(root, f),$.p((short) x, (short) y));
            //return $.inh(root,$.p($.p((short) x, (short) y),f));
            //return $.inh(root, $.p($.p((short) x, (short) y), ff));
            //return $.p(ff, $.p((short) x, (short) y), root);
                    //: $.inh($.p((short) x, (short) y), ff);
            //return root!=null ? $.inh($.p(root, $.p(x, y)),feature[f]) : $.inh($.p(x, y), feature[f]);
            //return $.inh(component, feature[f]);
            //return $.inh($.p($.p($.the(x), $.the(y)), root), feature[f]);
            //return $.inh($.p(feature[f], $.p($.the(x), $.the(y))), root);
            //return $.funcImageLast(root, $.p($.the(x), $.the(y)), feature[f]);
        }
    }

    private void updateAutoclassifier() {
        if (src != null)
            src.updateBitmap();

        learn();
    }

    /** current superpixel being trained */
    int superPixelCurrent = -1;

    private final Tensor.GradQueue queue = new Tensor.GradQueue();

    protected void learn() {
        if (ae instanceof VanillaAutoencoder v) {
            v.queue = queue.div(learnRandom + tileCount());
        }

        if (learnRandom >0)
            learnRandomly(learnRandom);

        if (learnTiles)
            learnTiles();

        if (ae instanceof VanillaAutoencoder v)
            v.commit();
    }

    private void learnRandomly(int iterations) {
        //superPixelCurrent = -1;
        //assert(metabits.get(0,0).length==0);

        var rng = ae.rng;
//        if (rng == null)
//            rng = game.random();

        int iw = src.width(), ih = src.height();
        for (int i = 0; i < iterations; i++) {
            int x1 = rng.nextInt(iw - sw);
            int y1 = rng.nextInt(ih - sh);
            int x2 = x1 + sw, y2 = y1 + sh;
            ae.put(loadPixels(ins, x1, y1, x2, y2));
        }
    }

    private void learnTiles() {
        for (double[] ii : tileIterator)
            ae.put(ii);
    }

    private void reconstructEncoding(int x, int y, double[] z) {
        int is = x * sw;
        int js = y * sh;
        int ie = is + sw; //Math.min(pw, is + sw);
        int ce = js + sh; //Math.min(ph, js + sh);
        for (int j = js, p = 0; j < ce; j++) {
            for (int i = is; i < ie; )
                pixRecon[j][i++] = ((float) z[p++]);
        }
    }


    final Iterable<double[]> tileIterator = (new LoadIterator())::reset;

    public int tileCount() {
        return nw*nh;
    }

    public Game game() {
        return game;
    }

    private final class LoadIterator implements Iterator<double[]> {
        int zMax;

        public LoadIterator reset() {
            superPixelCurrent = -1;
            zMax = tileCount();
            return this;
        }

        @Override
        public boolean hasNext() {
            return superPixelCurrent < zMax-1;
        }

        @Override
        public double[] next() {
            int z = ++superPixelCurrent;
            return loadSuperPixel(z % nw, z / nw);
        }
    }

    private double[] loadSuperPixel(int x, int y) {
        int sw = this.sw, sh = this.sh;
        int is = x * sw;
        int ie = Math.min(pw, is + sw);
        int js = y * sh;
        int je = Math.min(ph, js + sh);

        return loadPixels(ins, is, js, ie, je);
    }

    private double[] loadPixels(double[] input, int xs, int ys, int xe, int ye) {
        assert(ye > ys && xe > xs);

        int p = 0;
        for (int j = ys; j < ye; j++)
            for (int i = xs; i < xe; i++) {
                for (var f : pixIn)
                    input[p++] = f.value(i, j);
        }

//        //TODO do this once for all inputs
//        float[] metabits = this.metabits.get(x, y);
//        for (float m : metabits) input[p++] = m; //TODO arraycopy

        return input;
    }

//    @FunctionalInterface
//    public interface MetaBits {
//        float[] get(int subX, int subY);
//    }

//    public class MyAutoencoder extends /*StackedAutoencoder*/ Autoencoder {
//
//        MyAutoencoder(int ins, int features) {
//            super(ins,
//                    features, new XoRoShiRo128PlusRandom()
//                    //features
//                    //features*2, features
//            );
//
//        }
//
////        @Override
////        @Research
////        public double encodePost(double x) {
////            //dither autoencoder encoding to match signal range and resolution
////            x = super.encodePost(x);
////            if (preDither)
////                x = Util.unitizeSafe(Util.round(x, resolution()));
////            return x;
////        }
//    }

    /**
     * save encoding for current learning example
     * @param x superpixel x coord
     * @param y superpixel y coord
     * @param f autoencoder encoded output feature vector
     */
    private void storeEncoding(int x, int y, double[] f) {
        double[] features = encoded[y][x];
        System.arraycopy(f, 0, features, 0, features.length);
        Util.unitizeSafe(features);
    }

}
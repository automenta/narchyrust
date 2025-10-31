package nars.func;

import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave2d.Bitmap2D;
import jcog.tensor.ClassicAutoencoder;

/**
 * Autoencoder dimensional reduction of bitmap input
 */
public class AutoEncodedBitmap implements Bitmap2D {

    private static final float LEARNING_RATE = 0.05f;
    private static final float NOISE_LEVEL = 0.005f;

    final Bitmap2D source;
    private final float[] output;

    public final float[] input;

    final ClassicAutoencoder ae;
    private final int sx;
    private final int sy;
    private final int w;
    private final int h;

    public AutoEncodedBitmap(Bitmap2D b, int sx, int sy, int ox, int oy) {
        this.source = b;

        int w = b.width();
        int h = b.height();
        this.sx = sx;
        this.sy = sy;


        int i = sx * sy;
        this.input = new float[i];
        this.ae = new ClassicAutoencoder(i, ox * oy, ()->0.001f, new XoRoShiRo128PlusRandom(1));

        this.w = w / sx * ox;
        this.h = h / sy * oy;
        this.output = new float[this.w * this.h];
    }

    @Override
    public void updateBitmap() {
        source.updateBitmap();

        
        int w = source.width();
        int h = source.height();

        int k = 0;
        float alpha = LEARNING_RATE / (w*h);

        ae.noise.set(NOISE_LEVEL);

        for (int Y = 0; Y < h; Y+=sy) {
            for (int X = 0; X < w; X+=sx) {

                int j = 0;
                for (int y = 0; y < sy; y++) {
                    for (int x = 0; x < sx; x++) {
                        float b = source.value(X + x, Y + y);
                        if (b!=b)
                            b = 0.5f;
                        input[j++] = b;
                    }
                }
                assert(j==input.length);

                ae.put(input);


                for (double anO : ae.output())
                    output[k++] = (float) anO;

            }
        }
        assert(k == output.length);

    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public float value(int x, int y) {
        return output[y * w + x];
    }



}
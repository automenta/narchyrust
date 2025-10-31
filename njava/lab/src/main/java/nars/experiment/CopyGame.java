package nars.experiment;

import jcog.Util;
import jcog.data.DistanceFunction;
import jcog.random.RandomBits;
import jcog.signal.wave2d.ArrayBitmap2D;
import nars.$;
import nars.game.Game;
import nars.sensor.BitmapSensor;

import static jcog.Util.toDouble;

public class CopyGame extends Game {

    int updatePeriod = 8;

    float res =
        1;
        //0.5f;

    final float[] in;
    final float[] out;

    public CopyGame(String id, int length) {
        super(id);

        in = new float[length];
        out = new float[length];

        addSensor(new BitmapSensor(new ArrayBitmap2D(
                new float[][] { in }), $.p(id, "i")))
                .freqRes(res);

        for (int i = 0; i < length; i++) {
            int I = i;
            action($.inh($.p(id, "o"), $.the(i)), x->{
                if (x!=x) x = 0; //HACK
                x = Util.round(x, res); //HACK
                out[I] = x;
            }).freqRes(res);
        }

        reward(this::equality);

        afterFrame(()->{
            if (changeInput())
                updateInput();
        });
    }

    private boolean changeInput() {
        return rng().nextBoolean(1f/updatePeriod);
    }

    private void updateInput() {
        randomizeInput();
        //TODO other models
    }

    private void randomizeInput() {
        RandomBits r = rng();
        for (int i = 0; i < in.length; i++)
            in[i] = Util.round(r.nextFloat(), res);
    }

    public float equality() {
        return (float) (1 - (DistanceFunction.distanceManhattan(toDouble(in), toDouble(out))/in.length));
    }
}
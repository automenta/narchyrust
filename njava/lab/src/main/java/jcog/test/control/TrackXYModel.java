package jcog.test.control;

import jcog.Util;
import jcog.func.IntIntToFloatFunction;
import jcog.signal.FloatRange;
import jcog.signal.MutableEnum;
import jcog.signal.wave2d.ArrayBitmap2D;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static java.lang.Math.abs;
import static jcog.Util.sqr;
import static jcog.Util.unitize;

/* 1D and 2D grid tracking */
public class TrackXYModel {

    public final ArrayBitmap2D grid;

    public final int W, H;

    /** target coordinates: to be observed by the experiment TODO use V2 */
    public volatile float tx, ty;

    /** current coordinates: to be moved by the experiment TODO use V2 */
    public volatile float cx, cy;

    public final FloatRange controlSpeed = new FloatRange(0.2f, 0.02f, 0.5f);

    public final FloatRange targetSpeed = new FloatRange(0.1f, 0, 1);

    public final FloatRange visionContrast = new FloatRange(4, 0, 4);

    public final MutableEnum<TrackXYMode> mode = new MutableEnum<>(
        //TrackXYMode.RandomTarget
        TrackXYMode.CircleTarget
    );


    public TrackXYModel(int W, int H) {
        this.grid = new ArrayBitmap2D(this.W = W, this.H = H);
    }

    public static Random random() {
        return ThreadLocalRandom.current();
    }

    public void randomize() {
        this.tx = random().nextInt(grid.width());
        //this.sx = random().nextInt(view.width());
        if (grid.height() > 1) {
            this.ty = random().nextInt(grid.height());
            //this.sy = random().nextInt(view.height());
        } else {
            this.ty = this.cy = 0;
        }
    }

    final IntIntToFloatFunction binary = (x, y) -> (Util.equals(tx,x,0.5f) && Util.equals(ty,y,0.5f)) ? 1 : 0;

    final IntIntToFloatFunction fuzzy() { return (x, y) -> {


        float distOther = (float) (sqr((tx - x) / ((double) W)) + sqr((ty - y) / ((double) H)));
        //return distOther > visionContrast.floatValue() ? 0 : 1;
        //float distSelf = (float) Math.sqrt(Util.sqr(sx - x) + Util.sqr(sy - y));
        return unitize(1 - distOther * Math.max(W, H) * visionContrast.floatValue());
//                return Util.unitize(
//                        Math.max(1 - distOther * visionContrast,
//                                1 - distSelf * visionContrast
//                        ));

    };
    }

    public void act() {


        mode.get().accept(this);

        grid.set(
            fuzzy()
            //binary
        );
    }

    public float distMax() {
        return (float) Math.sqrt(sqr(W-1) + sqr(H-1));
    }

    /** linear distance from current to target */
    public float dist() {
        //return (float) Math.sqrt(Util.sqr(Math.max(0.5f,Math.abs(tx - cx))-0.5f) + Util.sqr(Math.max(0.5f,Math.abs(ty - cy))-0.5f));
        return (float) Math.sqrt(distXsq() + distYsq());
    }


    public float distX() {
        return abs(tx - cx);
    }
    public float distY() {
        return abs(ty - cy);
    }

    public float distXsq() {
        return sqr(distX());
    }

    public float distYsq() {
        return sqr(distY());
    }




    public enum TrackXYMode implements Consumer<TrackXYModel> {

        FixedCenter {
            float x;

            @Override
            public void accept(TrackXYModel t) {
                x += t.targetSpeed.floatValue();
                t.tx = t.W / 2f;
                t.ty = t.H / 2f;
            }
        },

        RandomTarget {
            final float momentum = 0.5f;
            float x, y;
            @Override
            public void accept(TrackXYModel t) {
                float targetSpeed = t.targetSpeed.floatValue();
                float ty;
                float tx = Util.clamp(t.tx + 2 * targetSpeed * (TrackXYModel.random().nextFloat() - 0.5f), 0, t.W - 1);
                if (t.H > 1) {
                    ty = Util.clamp(t.ty + 2 * targetSpeed * (TrackXYModel.random().nextFloat() - 0.5f), 0, t.H - 1);
                } else {
                    ty = 0;
                }
                x = Util.lerpSafe(momentum, x, tx);
                y = Util.lerpSafe(momentum, y, ty);

                t.tx = x;
                t.ty = y;
            }
        },

        CircleTarget {
            float theta;

            @Override
            public void accept(TrackXYModel t) {
                theta += t.targetSpeed.floatValue();

                t.tx = (((float) Math.cos(theta) * 0.5f) + 0.5f) * (t.W - 1);

                if (t.H > 1)
                    t.ty = (((float) Math.sin(theta) * 0.5f) + 0.5f) * (t.H - 1);
                else
                    t.ty = 0;

            }
        }

    }


}
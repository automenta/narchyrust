package nars.sensor;

import jcog.math.v2;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.ColorMode;
import nars.$;
import nars.Term;
import nars.game.Controlled;
import nars.game.Game;

import static jcog.Util.*;

/**
 * 2D flat Raytracing Retina
 */
public class PixelBag /* TODO extends ProxyBitmap2D */ implements Bitmap2D, Controlled<Game> {

    private final int px, py;

    /**
     * Z = 0: zoomed in all the way
     * = 1: zoomed out all the way
     */
    public final v2 pos = new v2(0.5f, 0.5f);
    private final v2 posNext = new v2(pos);
    private final Bitmap2D src;

    /** zoom is in 0..1 and it lerps between min and max zoom */
    public float Z;
    private float Znext = Z;


    protected float panRate = 1;
    protected float zoomRate =
        1;
        //0.75f;
    boolean inBoundsOnly = true;


    public final float[][] pixels;

    /** measured in a % of image visibility (0..1) */
    public float maxZoom;

    /**
     * measured in a % of image visibility (0..1)
     * increase >1 to allow zoom out beyond input size (ex: thumbnail size)
     */
    private float minZoom =  1;

    private boolean actionHorizontal;
    private boolean actionVertical;
    private boolean actionZoom;
    private float actionRes;
    private Term actionRoot;

    public PixelBag(Bitmap2D src, int px, int py) {
        this.src = src;
        this.px = px;
        this.py = py;
        this.pixels = new float[px][py];
        this.maxZoom = 1f / Math.min(px, py); //one pixel observation
    }

    @Override
    public ColorMode mode() {
        return src.mode();
    }

    /**
     * source width, in pixels
     */
    public int sw() {
        return src.width();
    }

    /**
     * source height, in pixels
     */
    public int sh() {
        return src.height();
    }

    @Override
    public void updateBitmap() {

        src.updateBitmap();

        int sw = sw(), sh = sh();


        pos.move(posNext, panRate);


        float X = pos.x, Y = pos.y;
        var Z = this.Z = lerpSafe(zoomRate, this.Z, Znext); //TODO zoom lerp

        var visibleProportion = (float) lerp(Math.sqrt(1 - Z), maxZoom, minZoom);
        var ew = visibleProportion * sw;
        var eh = visibleProportion * sh;

        float minX, maxX, minY, maxY;
        if (inBoundsOnly) {
            var mw = ew > sw ? 0 : sw - ew;
            var mh = eh > sh ? 0 : sh - eh;
            minX = X * mw;
            maxX = minX + ew;
            minY = Y * mh;
            maxY = minY + eh;
        } else {
            minX = X * sw - ew / 2;
            maxX = X * sw + ew / 2;
            minY = Y * sh - eh / 2;
            maxY = Y * sh + eh / 2;
        }

        updateClip(sw, sh, minX, maxX, minY, maxY);
    }

    private void updateClip(int sw, int sh, float minX, float maxX, float minY, float maxY) {

        float px = this.px, py = this.py;

        float radX = (maxX - minX) / px / 2,
              radY = (maxY - minY) / py / 2;

        for (var oy = 0; oy < py; oy++) {
            var sy = lerpSafe(oy / (py-1), minY, maxY);
            var y1 = clampPix(sy - radY, sh);
            var y2 = clampPix(sy + radY, sh);
            for (var ox = 0; ox < px; ox++) {

                //TODO optimize sources which are already gray (ex: 8-bit grayscale)

                var sx = lerpSafe(ox / (px-1), minX, maxX);
                var x1 = clampPix(sx - radX, sw);
                var x2 = clampPix(sx + radX, sw);
                pixels[ox][oy] = brightness(x1, x2, y1, y2);
            }
        }
    }

    private static float clampPix(float xy, int limit) {
        return clampSafe(xy, 0, limit - 1);
    }

    /** bilinear */
    float brightness(float x1, float x2, float y1, float y2) {
        var startX = (int) x1;
        var endX = (int) Math.ceil(x2);
        var startY = (int) y1;
        var endY = (int) Math.ceil(y2);

        double totalBrightness = 0, totalWeight = 0;
        for (var y = startY; y <= endY; y++) {
            var yWeight = (y < y2 ? y + 1 : y2) - y1;
            for (var x = startX; x <= endX; x++) {
                var xWeight = (x < x2 ? x + 1 : x2) - x1;
                var weight = xWeight * yWeight;
                totalBrightness += weight * src.value(x, y);
                totalWeight += weight;
            }
        }
        return (float)(totalWeight < Float.MIN_NORMAL ? 0 :
            totalBrightness / totalWeight);
    }


//    /**
//     * cheap sampling approximation
//     */
//    private static float kernelFade(float dpx, float dpy) {
//        float manhattan = Math.abs(dpx) + Math.abs(dpy);
//        return 1/(1 + manhattan);
//
////        float cartesian = (float) Math.sqrt((dpx*dpx) + (dpy*dpy));
////        return 1/(1+cartesian);
//
////        float cartesianSq = ((dpx*dpx) + (dpy*dpy));
////        return 1/(1+cartesianSq);
//    }

    protected float missing() {
        return Float.NaN;
        //return rng.nextFloat();
    }

    public void setMaxZoom(float visibilityPct) {
        this.maxZoom = visibilityPct;
    }

    public void setMinZoom(float visibilityPct) {
        this.minZoom = visibilityPct;
    }

    @Override
    public int width() {
        return px;
    }

    @Override
    public int height() {
        return py;
    }

    @Override
    public float value(int x, int y) {
        return pixels[x][y];
    }

    public final PixelBag setZoom(float zoomLevel) {
        Znext = round(unitize(zoomLevel), actionRes);
        return this;
    }

    public final PixelBag setXRelative(float f) {
        posNext.x = f;
        return this;
    }
    public final PixelBag setYRelative(float f) {
        posNext.y = f;
        return this;
    }

    private PixelBag setXRelativeFromAction(float f) {
        return setXRelative(round(f, actionRes));
    }

    private PixelBag setYRelativeFromAction(float f) {
        return setYRelative(round(f, actionRes));
    }

    public final PixelBag actions(Term actionRoot, boolean horizontal, boolean vertical, boolean zoom, float freqRes) {
        this.actionRoot = actionRoot;
        this.actionHorizontal = horizontal;
        this.actionVertical = vertical;
        this.actionZoom = zoom;
        this.actionRes = freqRes;
        return this;
    }

    @Override
    public void controlStart(Game g) {

        if (actionHorizontal)
            g.action($.inh(actionRoot, "panX"), this::setXRelativeFromAction).freqRes(actionRes);
        else
            pos.x = posNext.x = 0.5f;

        if (actionVertical)
            g.action($.inh(actionRoot, "panY"), this::setYRelativeFromAction).freqRes(actionRes);
        else
            pos.y = posNext.y = 0.5f;

        if (actionZoom) {
            g.action($.inh(actionRoot, "zoom"),
                    this::setZoom
                    //(float z) -> this.setZoom(z*z)
            ).freqRes(actionRes);
            //minZoom = 1.5f; //expand to allow viewing the entire image as summary
        }
    }
}
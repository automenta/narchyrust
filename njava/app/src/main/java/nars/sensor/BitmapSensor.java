package nars.sensor;

import jcog.Util;
import jcog.math.v2i;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.Term;
import nars.concept.TaskConcept;
import nars.game.Game;
import nars.game.sensor.SignalComponent;
import nars.game.sensor.VectorSensor;
import nars.term.atom.Int;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

import static java.lang.Math.max;

/** TODO generalize beyond any 2D specific that this class was originally designed for */
public class BitmapSensor<P extends Bitmap2D> extends VectorSensor {

    @Deprecated public Bitmap2DConcepts<P> concepts;
    public final P src;

    public final int width, height;
    private final IntIntToObjectFunction<Term> pixelTerm;
    private final float defaultFreq;

    //    private FloatFloatToObjectFunction<Truth> mode;

    public BitmapSensor(P src, @Nullable Term root) {
        this(src, src.height() > 1 ?
                        /* 2D default */ RadixProduct(root, src.width(), src.height(), /*RADIX*/1) :
                        /* 1D default */ (x, y) -> root != null ? $.inh(root, $.the(x)) : $.the(x)    //y==1
        );
    }

    public BitmapSensor(P src, @Nullable IntIntToObjectFunction<Term> pixelTerm) {
        this(src, pixelTerm, Float.NaN);
    }

    public BitmapSensor(P src, @Nullable IntIntToObjectFunction<Term> pixelTerm, float defaultFreq) {
        super(pixelTerm.value(-1,-2)
                    .replace(Map.of(
                        Int.i(-1), $.varDep(1),
                        Int.i(-2), $.varDep(2))), src.width() * src.height());

        this.pixelTerm = pixelTerm;
        this.width = src.width();
        this.height = src.height();
        this.src = src;
        this.defaultFreq = defaultFreq;



//        if (src instanceof PixelBag) {
//            //HACK sub-pri the actions for this attn group
//            for (ActionSignal aa : ((PixelBag) src).actions) {
//                n.control.addTo(aa.pri, pri);
//            }
//        }

//        /* modes */
//        SET = (p, v) ->
//                Signal.SET.apply(() ->
//                        nar.confDefault(BELIEF)).value(p, v);
//
//        DIFF = (p, v) ->
//                Signal.DIFF.apply(() ->
//                        nar.confDefault(BELIEF)).value(p, v);
//        mode = SET;
    }

//
//    public Bitmap2DSensor<P> mode(FloatFloatToObjectFunction<Truth> mode) {
//        this.mode = mode;
//        return this;
//    }

    @Override
    public void start(Game game) {
        super.start(game);
        this.concepts = new Bitmap2DConcepts<>(src, defaultFreq, this, pixelTerm, game.nar);
    }

//    final FloatFloatToObjectFunction<Truth> SET;
//    final FloatFloatToObjectFunction<Truth> DIFF;

    public static IntIntToObjectFunction<Term> XY(Term root) {
        return (x, y) -> $.inh($.p((short)x, (short)y), root);
    }

    public static IntIntToObjectFunction<Term> XY(Term root, int radix, int width, int height) {
        return (x, y) ->
                $.p(root, $.pRadix(x, radix, width), $.pRadix(y, radix, height));
    }

    public static IntIntToObjectFunction<Term> RadixProduct(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term xy = radix > 1 ?
                    $.p(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p((short)x, (short)y);
            return root == null ? xy :
                    //$.p(root, coords);
                    $.inh(root, xy);
        };
    }

    public static IntIntToObjectFunction<Term> RadixRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.pRecurse(true, zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p((short)x, (short)y);
            return root == null ? coords : $.p(root,coords);
        };
    }

    public static IntIntToObjectFunction<Term> InhRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.inhRecurse(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p((short)x, (short)y);
            return root == null ? coords : $.p(root,coords);
        };
    }

    private static Term[] zipCoords(Term[] x, Term[] y) {
        int m = max(x.length, y.length);
        Term[] r = new Term[m];
        int sx = m - x.length;
        int sy = m - y.length;
        int ix = 0, iy = 0;
        for (int i = 0; i < m; i++) {
            Term xy;
            char levelPrefix = (char) ('a' + (m - 1 - i));

            if (i >= sx && i >= sy) {

                xy = $.p($.the(x[ix++]), $.the(levelPrefix), $.the(y[iy++]));
            } else if (i >= sx) {

                xy = $.p($.the(levelPrefix), $.the(x[ix++]));
            } else {

                xy = $.p($.the(y[iy++]), $.the(levelPrefix));
            }
            r[i] = xy;
        }
        return r;
    }


    public static Term coord(char prefix, int n, int max) {
        return $.p($.the(prefix), $.p($.radixArray(n, 2, max)));
    }

    public static Term[] coord(int n, int max, int radix) {
        return $.radixArray(n, radix, max);
    }

//    public Bitmap2DSensor<P> diff() {
//        mode = DIFF;
//        return this;
//    }
    @Override
    public void accept(Game g) {
        src.updateBitmap();
        super.accept(g);

    }


    public final TaskConcept get(int x, int y) {
        return concepts.get(x, y);
    }


    @Override
    public final Iterator<SignalComponent> iterator() {
        return concepts.iterator();
    }

    @Override
    public Iterator<SignalComponent> inputIterator() {
        return concepts.iterSpace.iterator();
    }

    public SignalComponent newPixel(Term sid, int x, int y, NAR nar) {
        return new PixelSignal(sid, x, y, nar);
    }

    public final class PixelSignal extends SignalComponent {

        public final v2i pos;

        PixelSignal(Term sid, int x, int y, NAR nar) {
            super(sid, nar);
            pos = new v2i(x, y);
        }

        public int x() { return pos.x; }
        public int y() { return pos.y; }

        @Override
        public float value(Game g) {
//            return Bitmap2DConcepts.this.nextValue(x, y);

            float ff = src.value(x(), y());
//                                        float prev = this.prev;
//                                        this.prev = ff;
//                                        if (Util.equals(ff, prev) && Util.equals(prev, defaultFreq))
//                                            return Float.NaN;
//                                        return ff;
            return Util.equals(ff, concepts.defaultFreq, NAL.truth.FREQ_EPSILON)
                    ? Float.NaN : ff;
        }

    }
}
//    public void link(Game g) {
//        float basePri = this.pri.pri();
//        double sur = surprise();
//        float pri = (float) sur * basePri;
//
//        AbstractTaskLink tl = newLink();
//        tl.priMerge(BELIEF, pri, NAL.tasklinkMerge); //TODO * preamp?
////            tl.priMax(QUEST, surprise);
////            tl.priMax(GOAL, surprise*(1/4f));
//        g.what().link(tl);
//
//    }

//    private class PixelSelectorTaskLink extends DynamicTaskLink {
//        PixelSelectorTaskLink() {
//            super(term());
//        }
//
//
//        @Override public Termed src(When<NAR> when) {
//            return Bitmap2DSensor.this.get(when.x.random());
//        }
//
//
//        /** saccade shape */
//        @Override public Term target(Task task, Derivation d) {
//            //return task.term();
//            //return randomPixel(d.random).term();
//
//            //random adjacent cell
//            Bitmap2DConcepts.PixelSignal ps = (Bitmap2DConcepts.PixelSignal) d.nar.concept(task.term());
//            if (ps!=null) {
//                int xx = clamp(ps.x + d.random.nextInt(3) - 1, 0, width-1),
//                        yy = clamp(ps.y + d.random.nextInt(3) - 1, 0, height-1);
//                return concepts.get(xx, yy).term();
//            } else
//                return null;
////
////        Term[] nn;
////        Term center = pixelTerm.apply(xx, yy);
////        if (linkNESW) {
////            List<Term> neighbors = new FasterList(4);
////            if (xx > 0)
////                neighbors.add(pixelTerm.apply(xx - 1, yy));
////            if (yy > 0)
////                neighbors.add(pixelTerm.apply(xx, yy - 1));
////            if (xx < width - 1)
////                neighbors.add(pixelTerm.apply(xx + 1, yy));
////            if (yy < height - 1)
////                neighbors.add(pixelTerm.apply(xx, yy + 1));
////
////            nn = neighbors.toArray(EmptyTermArray);
////        } else {
////            nn = EmptyTermArray;
////        }
////        return TemplateTermLinker.of(center, nn);
////    }
//        }
//    }

//    private class ConjunctionSuperPixelTaskLink extends DynamicTaskLink {
//
//        final static int MAX_WIDTH = 2, MAX_HEIGHT = 2;
//
//        ConjunctionSuperPixelTaskLink() {
//            super(term());
//        }
//
//        @Override
//        public Termed src(When<NAR> when) {
//            return superPixel(when.x.random());
//        }
//
//
//        @Override
//        public Term target(Task task, Derivation d) {
//            return task.term();
//        }
//
//
//        @Nullable
//        private Term superPixel(Random rng) {
//            int batchX = max(rng.nextInt(Math.min(width, MAX_WIDTH)+1),1),
//                batchY = max(rng.nextInt(Math.min(height, MAX_HEIGHT)+1), 1);
//            int px = rng.nextInt(concepts.width - batchX+1);
//            int py = rng.nextInt(concepts.height - batchY+1);
//            TermList subterms = (batchX*batchY > 1) ? new TermList(batchX * batchY) : null;
//            for (int i = px; i < px+batchX; i++) {
//                for (int j = py; j < py+batchY; j++) {
//                    Signal ij = concepts.get(i, j);
//                    Term ijt = ij.term();
//                    if (subterms==null)
//                        return ijt; //only one pixel, unnegated
//                    Task current = ((SensorBeliefTables) (ij.beliefs())).current();
//                    if (current!=null) {
//                        subterms.add(ijt.negIf(current.isNegative()));
//                    }
//                }
//            }
//            switch (subterms.size()) {
//                case 0: return null;
//                case 1: return subterms.get(0);
//                default: return CONJ.the(0, (Subterms) subterms);
//            }
//        }
//
//    }
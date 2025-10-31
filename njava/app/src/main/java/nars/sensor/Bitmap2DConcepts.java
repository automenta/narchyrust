package nars.sensor;

import jcog.data.iterator.Array2DIterable;
import jcog.signal.wave2d.Bitmap2D;
import nars.NAR;
import nars.Term;
import nars.Truth;
import nars.game.sensor.SignalComponent;
import nars.game.sensor.SignalConcept;
import nars.table.eternal.EternalDefaultTable;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * rectangular region of pixels
 */
public class Bitmap2DConcepts<P extends Bitmap2D> implements Iterable<SignalComponent> {

    /** [y][x] */
    public final SignalComponent[][] matrix;
    public final int width;
    public final int height;
    public final int area;
    public final P src;

    public final Array2DIterable<SignalComponent> iter;
    final Array2DIterable<SignalComponent> iterSpace;

    final float defaultFreq;

    protected Bitmap2DConcepts(P src, float defaultFreq, BitmapSensor<?> s, @Nullable IntIntToObjectFunction<Term> pixelTerm, NAR nar) {

        this.width = src.width();
        this.height = src.height();
        this.area = width * height;
        assert (area > 0);

        this.src = src;

        this.matrix = new SignalComponent[height][width];


        this.defaultFreq = defaultFreq;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                Term sid = pixelTerm.value(x, y);
                SignalComponent sc = s.newPixel(sid, x, y, nar);
                if (defaultFreq==defaultFreq)
                    EternalDefaultTable.add(sc, defaultFreq, nar);


                matrix[y][x] = sc;
            }
        }

        this.iter = new Array2DIterable<>(matrix, false);
        this.iterSpace = new Array2DIterable<>(matrix, true);
    }




    /**
     * iterate columns (x) first, then rows (y)
     */
    @Override
    public final Iterator<SignalComponent> iterator() {
        return iter.iterator();
    }

    public Iterator<SignalComponent> iterator(int s, int e) {
        return iterSpace.iterator(s, e);
    }


    /**
     * crude ASCII text representation of the current pixel state
     */
    public void print(long when, PrintStream out, NAR nar) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                Truth b = matrix[j][i].beliefs().truth(when, when, nar);
                out.print(b!=null ? (b.freq() >= 0.5f ? '+' : '-') : ' ' );
            }
            out.println();
        }
    }

    public SignalConcept getSafe(int x, int y) {
        return matrix[y][x];
    }

    public @Nullable SignalConcept get(int x, int y) {
        return (x < 0) || (y < 0) || (x >= width || y >= height) ? null :
            getSafe(x, y);
    }



    public final int size() { return area; }




}
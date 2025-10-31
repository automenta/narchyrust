package nars.sensor;

import jcog.signal.wave2d.Bitmap2D;
import nars.NAR;
import nars.game.Game;
import nars.game.sensor.SignalComponent;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/** has progressive/adaptive updating abilities
 * TODO needs tested, particularly the modulo start/stop inclusiveness is bugged
 */
public class AdaptiveBitmap2DSensor extends BitmapSensor {

	private int ptrStart = 0, ptrEnd;

	public AdaptiveBitmap2DSensor(NAR n, Bitmap2D src, @Nullable IntIntToObjectFunction pixelTerm) {
		super(src, pixelTerm);
	}
	@Override
	public void accept(Game g) {

		int p = Math.min(concepts.area, pixelsToUpdate());
		if (p == 0)
			return;

		ptrEnd = (ptrStart + p);

		super.accept(g);

		ptrStart = ptrEnd;
	}
	@Override
	public Iterator<SignalComponent> inputIterator() {
		int a = concepts.area;
		return concepts.iterator(ptrStart % a, ptrEnd % a);
	}
	public int pixelsToUpdate() {
		return
			Math.round(
				(concepts.area) *
					1 //all
				//value()
				//pri.amp.floatValue()
				//Util.sqrt(pri.amp.floatValue() /*pri()*/)

			)
			;
	}
}
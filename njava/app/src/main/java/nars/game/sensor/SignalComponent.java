package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Term;
import nars.game.Game;

/** one of potentially several component signals in a vector sensor */
public abstract class SignalComponent extends SignalConcept {

	protected SignalComponent(Term componentID, NAR nar) {
		super(componentID, true, nar);
	}

	public float value;

	public float updateValue(Game g) {
		return this.value = value(g);
	}
	protected abstract float value(Game g);

	public static final class LambdaSignalComponent extends SignalComponent {

		private final FloatSupplier f;

		LambdaSignalComponent(Term id, FloatSupplier f, NAR nar) {
			super(id, nar);
			this.f = f;
		}

		@Override
		public float value(Game g) {
			return f.asFloat();
		}

	}
}
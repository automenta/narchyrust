package nars.term.util.transform;

import jcog.data.map.UnifriedMap;
import nars.Op;
import nars.Term;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;

import java.util.function.Function;

/**
 * Variable normalization
 * <p>
 * Destructive mode modifies the input Compound instance, which is
 * fine if the concept has been created and unreferenced.
 * <p>
 * The target 'destructive' is used because it effectively destroys some
 * information - the particular labels the input has attached.
 */
public class VariableNormalization extends VariableTransform {

	final VariableMap map;

	protected VariableNormalization() {
		this(1, 0);
	}

	public VariableNormalization(int size /* estimate */, int offset) {
		this.map = new VariableMap(size, offset);
	}

	public int offset() {
		return map.offset;
	}

	@Override
	protected Term applyVariable(Variable x) {
		return map.variable(x);
	}

	static final class VariableMap extends UnifriedMap<Variable,Variable> implements Function<Variable,Variable> {
		private int offset;

		private VariableMap(int size, int offset) {
			super(size);
			this.offset = offset;
		}

		public void shift(NormalizedVariable z, int varBits) {
			if (z.isAny(varBits)) {
				offset = Math.max(offset, z.id());
				assert(offset < Byte.MAX_VALUE);
			}
		}

		@Override
		public Variable apply(Variable x) {
			int i = this.offset++;
			return i <= Byte.MAX_VALUE ?
				x.normalize((byte) i) :
				applyOOB(x, i);
		}

		private static UnnormalizedVariable applyOOB(Variable x, int i) {
			return x instanceof UnnormalizedVariable u ?
				u :
				new UnnormalizedVariable(x.opID(), i + x.toString().substring(1) /* HACK */);
		}

		private Term variable(Variable x) {
			return x instanceof UnnormalizedVariable && Op.VarAuto.equals(x) ?
				apply(x) :
				computeIfAbsent(x, this);
		}
	}

}
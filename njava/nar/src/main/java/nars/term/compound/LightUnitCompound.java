package nars.term.compound;

import nars.Op;
import nars.Term;
import nars.subterm.Subterms;

public class LightUnitCompound extends UnitCompound {

	private /*final*/ Term sub;

	public LightUnitCompound(Op op, Term sub) {
		this(op.id, sub);
	}

	public LightUnitCompound(int op, Term sub) {
		super(op);
		this.sub = sub;
	}

	@Override
	public boolean equalExhaustive(Subterms s) {
		if (s instanceof LightUnitCompound Y) {
			Term x = sub, y = Y.sub;
			if (x==y)
				return true;
			else if (x.equals(y)) {
				if (System.identityHashCode(x) < System.identityHashCode(y))
					Y.sub = x;
				else
					this.sub = y;
				return true;
			} else
				return false;
		} else
			return super.equalExhaustive(s);
	}

	@Override
	public final Term sub() {
		return sub;
	}


}
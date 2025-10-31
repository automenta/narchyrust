package nars.term.builder;

import nars.Op;
import nars.Term;
import nars.subterm.ArraySubterms;
import nars.subterm.Subterms;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;

public class SimpleTermBuilder extends HeapTermBuilder {

	public static final TermBuilder the = new SimpleTermBuilder();

	@Override
	public Term compound1New(Op o, Term x) {
		return new CachedUnitCompound(o, x);
		//return compoundNNew(o, DTERNAL, new Term[] { x}, null);
	}

	@Override
	public Term compoundNew(Op o, int dt, Subterms subs) {
		return CachedCompound.the(o, dt, subs);
	}

	@Override
	public Subterms subtermsNew(Term... t) {
		return new ArraySubterms(t);
	}

}
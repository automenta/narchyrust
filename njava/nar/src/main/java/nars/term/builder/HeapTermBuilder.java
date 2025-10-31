package nars.term.builder;

import nars.Term;
import nars.term.Neg;
import nars.term.anon.Intrin;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;

/** abstract impl of stateless on-heap Object-based term instantiation */
public abstract class HeapTermBuilder extends TermBuilder {

	@Override
	public final Atomic atomNew(String id) {
		return new Atom(id);
	}

	@Override protected final Term negNew(Term u) {
		short i = Intrin.id(u);
        return i != 0 ?
			new Neg.NegIntrin(i) :
			new Neg.NegLight(u);
			//NAL.NEG_CACHE_VOL_THRESHOLD <= 0 || (u.volume() > NAL.NEG_CACHE_VOL_THRESHOLD) ? new Neg.NegLight(u) : new Neg.NegCached(u);
	}
}
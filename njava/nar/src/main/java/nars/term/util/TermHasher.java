package nars.term.util;

import jcog.bloom.hash.DynBytesHasher;
import jcog.data.byt.DynBytes;
import nars.Term;

class TermHasher extends DynBytesHasher<Term> {

	TermHasher() {
		super(1024);
	}

	@Override
	protected void write(Term t, DynBytes d) {
        t.write(d);
    }
}
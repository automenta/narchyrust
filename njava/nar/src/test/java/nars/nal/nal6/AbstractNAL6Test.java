package nars.nal.nal6;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;

public class AbstractNAL6Test extends NALTest {

	static final int cycles = 20;


	@Override
	protected NAR nar() {
		NAR n = NARS.tmp(
				8);
//				6, 8);
		n.complexMax.set(13);
		n.confMin.set(0.3f);
		return n;
	}

}
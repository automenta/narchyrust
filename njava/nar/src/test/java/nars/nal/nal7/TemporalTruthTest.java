package nars.nal.nal7;

import nars.NALTask;
import nars.NAR;
import nars.NARS;
import nars.Term;
import nars.term.atom.Atom;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;

public class TemporalTruthTest {

	@Disabled
	@Test
	public void coherentOscillation1() {
		Term x = Atom.atom("x");
		NAR n = NARS.tmp();

//		NAL.DEBUG = true;
//		n.log();

		int seqStart, seqEnd;
		{
			//input sequence
			seqStart = 0;
			seqEnd = 20;
			n.believe(x, 0);
			n.believe(x.neg(), 5);
			n.believe(x, 10);
			n.believe(x.neg(), 15);
		}
		n.main().onTask(_t -> {
			NALTask t = (NALTask)_t;
			if (!t.ETERNAL() && t.term().equals(x)) {
				boolean pn = t.POSITIVE();
				long w = t.mid();
				if (t.range() < 5) {
					if (pn != Math.abs((w % 5) - 5) > Math.abs((w % 5))) {
						System.err.println("wrong: " + t);
					}
				} else {
					//TODO
				}
			}
		}, BELIEF);
		n.run(1000);

		//sample belief at every point and beyond
		for (int t = seqStart; t < seqEnd + (seqEnd - seqStart)*4; t++) {
			System.out.println(t + "\t" + n.belief(x, t));
		}
	}
}
package nars.test.impl;

import nars.$;
import nars.Term;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.test.TestNAR;
import org.jetbrains.annotations.Nullable;


public class DeductiveChainTest {

	public static final @Nullable IndexedStatementBuilder inh = (x, y) ->
		$.inh(a(x), a(y));
	public static final @Nullable IndexedStatementBuilder sim = (x, y) ->
		$.sim(a(x), a(y));
	public static final @Nullable IndexedStatementBuilder impl = (x, y) ->
		$.impl(a(x), a(y));
	public final Term q;
	public final Term[] beliefs;


	public DeductiveChainTest(TestNAR n, int length, int timeLimit, IndexedStatementBuilder b) {
		beliefs = new Compound[length];
		for (int x = 0; x < length; x++)
			beliefs[x] = b.apply(x, x + 1);

		q = b.apply(0, length);

		for (Term belief : beliefs)
			n.nar.believe(belief);

		n.nar.question(q);

		n.mustBelieve(timeLimit, q.toString(), 1.0f, 1.0f, 0.01f, 1.0f);

	}

	static Atomic a(int i) {
		return Atomic.atomic((char)('a' + i));
	}


	@FunctionalInterface
	public interface IndexedStatementBuilder {
		Term apply(int x, int y);
	}


}
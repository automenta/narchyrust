package nars.util;

import jcog.Log;
import jcog.data.list.Lst;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import jcog.signal.FloatRange;
import nars.*;
import nars.term.Functor;
import nars.time.part.DurLoop;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * debounced and atomically/asynchronously executable operation
 * TODO support asych executions that delay or stretch feedback until they complete
 */
public class OpExec implements BiFunction<Task, NAR, Task> {

	private static final Logger logger = Log.log(OpExec.class);

	static final int ACTIVE_CAPACITY = 16;
	/**
	 * expectation threhold (in range 0.5..1.0 indicating the minimum expectation minimum
	 * for desire, and the 1-exp expectation maximum of belief necessary to invoke the action.
	 * <p>
	 * 1.00 - impossible
	 * 0.75 - mid-range
	 * 0.50 - hair-trigger hysterisis
	 */
	public final FloatRange exeThresh;

	final BiConsumer<Term, Timed> exe;
	final ArrayBag<Term, PriReference<Term>> active = new PriReferenceArrayBag<>(PriMerge.max, ACTIVE_CAPACITY);

	private final AtomicReference<DurLoop> onCycle = new AtomicReference(null);

	public OpExec(@Nullable BiConsumer</*TODO: Compound*/Term, Timed> exe, float exeThresh) {
		this(exe, new FloatRange(exeThresh, 0.5f, 1.0f));
	}

	public OpExec(@Nullable BiConsumer<Term, Timed> exe, FloatRange exeThresh) {
		this.exe = exe == null ? ((BiConsumer) this) : exe;
		active.setCapacity(ACTIVE_CAPACITY);
		this.exeThresh = exeThresh;
	}

	/**
	 * implementations can override this to prefilter invalid operation patterns
	 */
	protected Task exePrefilter(Task x) {
		return Functor.args(x.term()).hasAny(Op.AtomicConstant) ? x : null;
	}


	public void update(NAR n) {


		int s = active.size();
		if (s > 0) {
			long now = n.time();
			long start = now - Math.round(n.dur());
			Collection<Term> dispatch = new Lst(s);
			float exeThresh = this.exeThresh.floatValue();

			for (PriReference<Term> x : active) {
				Term xx = x.get();

				Concept c = n.concept(xx);
				if (c == null)
					continue;



				Truth goalTruth = c.goals().truth(start, now, n);
				if (goalTruth == null || goalTruth.expectation() <= exeThresh)
					continue;


				Truth beliefTruth = c.beliefs().truth(start, now, n); /* assume false with no evidence */
				if (beliefTruth != null && beliefTruth.expectation() >= exeThresh)
					continue;


				logger.info("{} EVOKE (b={},g={}) {}", n.time(), beliefTruth, goalTruth, xx);
				dispatch.add(xx);

				x.delete();
			}

			for (Term tt : dispatch)
				exe.accept(tt, n);


			active.commit();
			s = active.size();
		}

		if (s == 0)
			disable(n);
	}

	@Override
	public @Nullable Task apply(Task x, NAR n) {


		Task y = exePrefilter(x);
		if (y == null)
			return x;
		if (y != x)
			return y;

		x = y;

		Term xx = x.term();
		if (x.COMMAND()) {

			exe.accept(xx, n);
			return null;
		} else {

			active.put(new PLink<>(xx.concept() /* incase it contains temporal, we will dynamically match task anyway on invocation */,
				x.priElseZero()
			));


			enable(n);


			return x;
		}
	}

	/**
	 * operator goes into active probing mode
	 */
	protected void enable(NAR n) {
		DurLoop d = onCycle.getOpaque();
		if (d == null) {
			onCycle.updateAndGet(x -> x == null ? n.onDur(this::update) : x);
		} else {
			n.add(d);
		}
	}

	/**
	 * operator leaves active probing mode
	 *
	 * @param n
	 */
	protected void disable(NAR n) {
		DurLoop d;
		if ((d = onCycle.getOpaque()) != null) {
			n.remove(d);

		}
	}


}
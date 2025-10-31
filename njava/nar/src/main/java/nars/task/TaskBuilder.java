package nars.task;

import jcog.data.array.LongArrays;
import jcog.pri.UnitPri;
import nars.*;
import nars.task.util.TaskException;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termed;
import nars.truth.Truthed;
import nars.util.Timed;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

import static nars.$.t;
import static nars.Op.*;

/**
 * Default Task implementation
 * TODO move all mutable methods to TaskBuilder and call this ImTaskBuilder
 * <p>
 * NOTE:
 * if evidence length == 1 (input) then do not include
 * truth or occurrence time as part of the hash, equality, and
 * comparison tests.
 * <p>
 * this allows an input task to modify itself in these two
 * fields without changing its hash and equality consistency.
 * <p>
 * once input, input tasks will have unique serial numbers anyway
 */
@Deprecated
public class TaskBuilder extends UnitPri implements Function<NAL, Task>, Truthed, Termed {


	private Term term;

	protected final byte punc;

	private @Nullable Truth truth;

	private @Nullable long[] evidence = LongArrays.EMPTY_ARRAY;

	private long creation = ETERNAL;
	private long start = ETERNAL;
    private long end = ETERNAL;


	public TaskBuilder(Term t, byte punct, float freq, NAL nar) throws TaskException {
		this(t, punct, t(freq, nar.confDefault(punct)));
	}

	public TaskBuilder(Term term, byte punct, @Nullable Truth truth) throws TaskException {
		this(term, punct, truth, Float.NaN);
	}

	public TaskBuilder(Term term, byte punctuation /* TODO byte */, @Nullable Truth truth, float p) throws TaskException {
		super(p);

		this.punc = punctuation;


		Term tt = term.term();
		if (tt instanceof Neg) {
			Term nt = tt.sub(0);
			if (nt instanceof Compound) {
				tt = nt;

				if (punctuation == BELIEF || punctuation == GOAL)
					truth = truth.neg();
			} else {
				throw new TaskException("Top-level negation", tt);
			}
		}


		this.truth = truth;
		this.term = tt;
	}


	public boolean isInput() {
		return evidence().length <= 1;
	}

	@Override
	public NALTask apply(NAL n) throws TaskException {

		Term t = term;

		byte punc = punc();
		if (punc == 0)
			throw new TaskException("Unspecified punctuation", this);

		Term cntt = t.normalize();//.the();
		if (cntt == null)
			throw new TaskException("Failed normalization", t);

		if (!NALTask.TASKS(cntt, punc, !isInput() && !NAL.DEBUG))
			throw new TaskException("Invalid content", cntt);

		if (cntt != t) {
			this.term = cntt;
		}


		switch (punc()) {
			case BELIEF:
			case GOAL:
				if (truth == null) {

					setTruth(t(1, n.confDefault(punc)));
				} else {

					float confLimit = 1f - NAL.truth.FREQ_EPSILON;
					if (!isInput() && conf() > confLimit) {

						setTruth(t(freq(), confLimit));
					}
				}

				break;
			case QUEST:
			case QUESTION:
				if (truth != null)
					throw new RuntimeException("quests and questions must have null truth");
				break;
			case COMMAND:
				break;

			default:
				throw new UnsupportedOperationException("invalid punctuation: " + punc);

		}


		if (evidence.length == 0)
			setEvidence(n.time.nextStamp());


		if (creation() == ETERNAL) {
			this.creation = n.time();
		}


		float pp = priElseNeg1();
		if (pp < 0) {
			pri(n.priDefault(punc));
		}


		Truth tFinal;
		tFinal = truth != null ? truth.dither(n) : null;

		NALTask i = NALTask.taskUnsafe(term, punc, tFinal, start, end, evidence);
		i.pri(this.pri());
		return i;
	}


	@Override
	public final Term term() {
		return term;
	}

	public float freq() { return truth.freq(); }

	@Override
	public double evi() {
		return truth.evi();
	}

	public boolean isBeliefOrGoal() {
		return punc == BELIEF || punc == GOAL;
	}

	public boolean isCommand() {
		return punc == COMMAND;
	}

	public final @Nullable Truth truth() {
		return truth;
	}

	private void setTruth(@Nullable Truth t) {

		if (t == null && isBeliefOrGoal())
			throw new TaskException("null truth for belief or goal", this);

		if (!Objects.equals(truth, t)) {
			truth = t;
		}
	}


	/**
	 * the evidence should be sorted and de-duplicaed prior to calling this
	 */

	private TaskBuilder setEvidence(@Nullable long... evidentialSet) {
		this.evidence = evidentialSet;
		return this;
	}

	public final byte punc() {
		return punc;
	}


	public final long[] evidence() {
		return this.evidence;
	}

	public final long creation() {
		return creation;
	}

	public final long start() {
		return start;
	}


	private TaskBuilder setCreationTime(long creationTime) {


		this.creation = creationTime;


		return this;
	}

	/**
	 * TODO for external use in TaskBuilder instances only
	 */
	private void setStart(long o) {
		this.start = o;
	}

	/**
	 * TODO for external use in TaskBuilder instances only
	 */
	private void setEnd(long o) {
		if (o != end) {
			if (start == ETERNAL && o != ETERNAL)
				throw new RuntimeException("can not setAt end time for eternal task");
			if (o < start)
				throw new RuntimeException("end must be equal to or greater than start");

			this.end = o;
		}
	}


	@Override
	public final int hashCode() {
		throw new UnsupportedOperationException();


	}

	/**
	 * To check whether two sentences are equal
	 * Must be consistent with the values calculated in getHash()
	 *
	 * @param that The other sentence
	 * @return Whether the two sentences have the same content
	 */
	@Override
	public final boolean equals(@Nullable Object that) {
		throw new UnsupportedOperationException();
	}






    /*
    @Override
    public void delete() {
        super.delete();






    }*/


	/**
	 * end occurrence
	 */
	public final long end() {

		return end;


	}


	public final TaskBuilder present(Timed timed) {
		return time(timed.time());
	}


	public final TaskBuilder time(Timed timed, int dt) {
		return time(timed.time() + dt);
	}


	public final TaskBuilder time(long when) {
		start = when;
		setEnd(when);
		return this;
	}

	public final TaskBuilder time(long start, long end) {
		this.start = start;
		setEnd(end);
		return this;
	}

	public TaskBuilder time(long creationTime, long start, long end) {
		setCreationTime(creationTime);
		this.start = start;
		setEnd(end);
		return this;
	}


	public final TaskBuilder occurr(long occurrenceTime) {
		start = occurrenceTime;
		setEnd(occurrenceTime);
		return this;
	}


	public TaskBuilder eternal() {
		start = ETERNAL;
		setEnd(ETERNAL);
		return this;
	}


	public final TaskBuilder evidence(long... evi) {
		setEvidence(evi);
		return this;
	}

	public final TaskBuilder evidence(NALTask evidenceToCopy) {
		return evidence(evidenceToCopy.stamp());
	}

//	@Override
//	public TaskBuilder withPri(float p) {
//		pri(p);
//		return this;
//	}


}
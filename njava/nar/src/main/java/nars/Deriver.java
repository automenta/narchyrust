package nars;

import jcog.Log;
import jcog.data.bit.MetalBitSet;
import jcog.data.map.UnifriedMap;
import jcog.event.Off;
import jcog.exe.flow.Feedback;
import jcog.memoize.MemoGraph;
import jcog.pid.IterationTuner;
import jcog.pri.Prioritized;
import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.FloatRange;
import nars.deriver.reaction.ReactionModel;
import nars.deriver.reaction.Reactions;
import nars.deriver.util.DeriverBuiltin;
import nars.deriver.util.NALTaskEvaluation;
import nars.link.MutableTaskLink;
import nars.link.TaskLink;
import nars.premise.DerivingPremise;
import nars.premise.NALPremise;
import nars.task.SerialTask;
import nars.term.Compound;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.conj.CondMatch;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.var.Variable;
import nars.time.Every;
import nars.time.clock.RealTime;
import nars.truth.MutableTruthInterval;
import nars.truth.func.TruthFunction;
import nars.unify.Unify;
import nars.unify.UnifyConstraint;
import nars.unify.UnifyTransform;
import nars.util.NARPart;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;

import static nars.Op.VAR_PATTERN;
import static nars.term.atom.Bool.Null;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 * instantiated threadlocal, and recycled mutably
 *
 * TODO extract common "Context" super-interface which Answer also implements
 * there are enough similarities that they can be used interchangeably in several
 * instances
 */
public abstract class Deriver extends NARPart {

	protected static final Logger logger = Log.log(Deriver.class);

	public final ReactionModel model;

	public final RandomBits rng = new RandomBits(new XoRoShiRo128PlusRandom());

	public MetalBitSet bits;

	/**
	 * main premise derivation unifier
	 */
	public final PremiseUnify unify = new PremiseUnify();
	private final MyUnifyTransform unifyTransform = new MyUnifyTransform();


	/** executed at the end of the iteration */
	public final MemoGraph later = new MemoGraph();


//	@Nullable public final DeriverMeter meter =
//			null;
//			//new DeriverMeter.GraphDeriverMeter();

	/**
	 * populates retransform map
	 */
	public final NAR nar;
	public transient double eviMin = NAL.truth.EVI_MIN;

	public Focus focus;

	private final MutableMap<Atomic, Term> derivationFunctors;

	public transient Premise premise;

	public transient int complexMax = NAL.term.COMPOUND_VOLUME_MAX;

	@Nullable private transient Off off;

	protected Deriver(Reactions r, NAR n) {
		this(r.compile(n), n);
	}

	protected Deriver(ReactionModel model, NAR nar) {
		this.nar = nar;

		this.model = model;

		this.derivationFunctors = DeriverBuiltin.get(this);

        reseedRNG();

		nar.add(this);
	}

	private void priAuto(NALTask x) {
		x.pri(focus.budget.priDerived(x, this));
	}

	private void priDerived(NALTask x) {
		x.pri(focus.budget.priDerived(x, null, null, this));
	}

	public final void add(NALTask x) {
		add(x, true);
	}

	public final void add(NALTask x, boolean priAuto) {
		if (invalidDerived(x))
			return;

		if (x.uncreated()) {
			if (priAuto)
				priAuto(x);
			else
				priDerived(x);
		}

		if (NAL.derive.FILTER_PRIORITY_UNDERFLOW && x.priElseZero() < Prioritized.EPSILON)
			return; //TODO eliminate causes of this waste

		addLater(x);
		//addLaterDirect(x);
		//_add(x);
	}

	private void addLater(NALTask x) {
		var f = focus;
		if (x instanceof SerialTask)
			later.once(x, f.ACTIVATE);
		else
			later.chain(x, f.PRE_REMEMBER, f.REMEMBER);
	}

	private void addLaterDirect(NALTask x) {
		later.once(x, this::_add);
	}

	/** entry point for derived tasks */
	@Deprecated private boolean _add(NALTask x) {
		focus.remember(x);
//		_onAdd(x);
		onAdd(x);
		return true;
	}

	/** for subclass impl */
	@Deprecated protected void onAdd(NALTask x) {

	}

//	protected void _onAdd(NALTask x) {
//		if (meter!=null) meter.task(premise, x);
//		if (NAL.DEBUG_DERIVED_NALTASKS)
//			trace(x);

//        var dt = nar.emotion.derivedTask;
//		if (NAL.derive.CAUSE_PUNC) {
//			//TODO detailed per-punc feedback
//			Feedback.is(switch (x.punc()) {
//				case '.' -> "derive.";
//				case '!' -> "derive!";
//				case '?' -> "derive?";
//				case '@' -> "derive@";
//				default -> throw new UnsupportedOperationException();
//			}, dt);
//		} else
//			dt.increment();
//	}

//	private /*synchronized*/ void trace(NALTask x) {
//		System.out.println(x + "\n\t" + premise);
//	}

	/** queues premise for possible execution */
	public final void add(Premise p) {
		if (NAL.DEBUG && invalid(p)) {
			rejected(p);
			return;
		}

        p.setParent(this.premise);

        //		if (p.isDeleted()) priAuto(p);
        //		if (NAL.causeCapacity.intValue()==0)
        //			nar.emotion.derivedPremise.increment();
        //		else Feedback.is("derive.premise", nar.emotion.derivedPremise);

        acceptPremise(p);

        //if (meter!=null) meter.premise(premise, p);
    }

//	private final BeliefResolve beliefResolve = new BeliefResolve(
//		true, true, true, true,
//		new FocusTiming(),
//		Answerer.AnyTaskResolver
//	);

	//	private void priAuto(Premise p) {
//		p.pri((float) focus.budget.priPremise(p, this));
//	}

	private void rejected(Premise p) {
		Feedback.is("derive.premise.invalid", nar.emotion.derivedPremiseInvalid);
//			throw new WTF();
	}

	private boolean invalid(TaskLink l) {
		if (l.priElseZero() <= 0) {
			//throw new UnsupportedOperationException("unbudgeted TaskLink");
			return true;
		}

		int complexMax = this.complexMax;
        var f = l.from();
		if (f.complexity() > complexMax)
			return true;
        var t = l.to();
		return t != f && (t.complexity() > complexMax);
	}

	public final boolean link(MutableTaskLink l) {
		if (invalid(l)) {
			Feedback.is("derive.tasklink.invalid", nar.emotion.derivedPremiseInvalid);
			return false;
		}

		return focus.link(l);
	}

	protected abstract void acceptPremise(Premise p);
	
	public boolean invalidVol(Term t) {
		//assert(!(t instanceof Neg));
		return t.complexity() > complexMax;
	}

	public boolean invalid(NALTask x) {
		return invalidVol(x.term()) || (x.BELIEF_OR_GOAL() && invalidBelief(x));
	}

	private boolean invalidBelief(NALTask x) {
//		if (x.freq()!=Truth.freq(x.freq(), nar.freqRes.floatValue()))
//			throw new WTF();

//		if (NAL.derive.FILTER_SUB_EVI_NALTASK && x.BELIEF_OR_GOAL() && invalidBelief(x)) {
////			if (NAL.truth.EVI_STRICT) {
//			Feedback.is("derive.NALTask.eviUnderflow", nar.emotion.deriveFailTruthUnderflow);
//			return true; //TODO DEBUG and avoid these
////			} else {
////				throw new TODO();
////			}
//		}
		return x.evi() < eviMin;
	}

	private boolean invalidDerived(NALTask x) {
//		if (NAL.DEBUG) {
//			if (!x.isInput()) {
//				Term why = x.why();
//				if (why == null)
//					throw new TaskException("cause missing", x);
//			}
//		}

		if (invalid(x)) {
			Feedback.is("derive.NALTask.invalid");
			return true;
		}

//		if (premise instanceof NALPremise rp && rp.same(x, nar)) {
//			Feedback.is("derive.NALTask.same");
//			return true;
//		}

		return false;
	}

	private boolean invalid(Premise x) {
        if (x instanceof TaskLink)
            throw new UnsupportedOperationException();

        var p = this.premise; //the parent

        if (p.getClass() == x.getClass() && p.equals(x))
            throw new UnsupportedOperationException();

        if (x instanceof NALPremise np)
            return np.invalid(this);

        return false;
	}

	/** @param w if null, doesnt switch */
	public final /*synchronized*/ void next(Focus w) {
		focus(w);
		try {
			next();
			later.run();
		} finally {
			later.clear();
		}
	}

	private void focus(Focus f) {
		var switched = false;
		var prev = this.focus;
		if (f!=null) {
			if (prev!=f) {
				this.focus = f;
				switched = true;
			}
		} else
			f = prev;

		if (f.commit(nar.time()) || switched)
			_commit(f);
	}

	private void _commit(Focus f) {
		var n = nar;
		unify.dur = unifyDur(f, n);
		eviMin = n.eviMin();
		complexMax = f.complexMax();
		onCommit();
	}

	/** called approximately each new duration */
	protected void onCommit() {

	}

	/** called if focus changed */
	protected void start(Focus f) {
	}

	protected abstract void next();

	protected static int unifyDur(Focus f, NAR n) {
		return Math.round(
			f.durSys * n.unifyDurs.floatValue() //SOLID
			//f.dur() * n.unifyDurs.floatValue()  //FLUID (depends on Focus's mutable temporal focus)
			//f.dur() * n.unifyDurs.floatValue()
		);
	}

	private void reseedRNG() {
		rng.setSeed(nar.random().nextLong());
	}


	/** recycles the same instance; must be closed when finished. dont use recursively */
	public MyUnifyTransform unifyTransform(int ttl) {
		if (ttl >= 0) unifyTransform.reset(ttl);
		return unifyTransform;
	}

	public final Off every(Focus w, Every e, float amount) {
		focus(w);
		return on(nar.on(e, amount, nn -> next(null)));
	}

	public final Off everyCycle(Focus w) {
		return every(w, Every.Cycle, 1);
	}

	private synchronized Off on(Off o) {
		if (this.off != null) {
			this.off.close(); //clear previous on
		}
		this.off = o;
		return o;
	}

	public final float dur() {
		return focus.dur();
	}

	public final Term polarize(Term arg, boolean taskOrBelief) {
        var t = taskOrBelief ?
				premise.task().truth() :
				(premise.belief() != null ? premise.belief().truth() : null);

		return arg.negIf(t != null ? t.NEGATIVE() : randomBoolean());
	}

	public final boolean randomBoolean() {
		return rng.nextBoolean();
	}

	public final boolean randomBoolean(float prob) {
		return rng.nextBooleanFast16(prob);
	}

	public final boolean randomBoolean(FloatRange f) {
		return randomBoolean(f.asFloat());
	}

	public final Term cond(boolean before, boolean matched, boolean after, boolean during, Term c, Term e, boolean unify, boolean fast) {

		if (!c.CONDS()) return Null;

		try (var u = unifyTransform(NAL.derive.TTL_CONJ_MATCH)) {
			if (!unify) u.vars = 0;

            var y = CondMatch.match((Compound) c, e, fast, before, matched, after, during, u);

			assert((before && matched && after) || !y.equals(c)): "should have returned Null";

			if (y != Null) u.postUnified();

			return y;
		}

	}

	/** computes the premise component truth values, seen across time */
	public final boolean truth(TruthFunction f, MutableTruthInterval w) {
        var dur = focus.durSys;
		var eviMin = this.eviMin;

		Truth T;
		if (f.taskTruthSignificant()) {
            var T1 = project(premise.task(), w, dur, eviMin);
			T = T1 != null ? f.preTask(T1) : T1;
			if (T==null) return false;
        } else
			T = null;

		Truth B;
		if (!f.single() && f.beliefTruthSignificant()) {
			var B1 = project(premise.belief(), w, dur, eviMin);
			B = B1 != null ? f.preBelief(B1) : B1;
			if (B==null) return false;
        } else
			B = null;

		var tb = f.truth(w, T, B, eviMin);

//		boolean normalize = false;
//		if (w.is() && normalize) {
//			double confMin = (T!=null ? T.conf() : 1) * (B!=null ? B.conf() : 1);
//			if (confMin >= 1) throw new UnsupportedOperationException();
//			w.conf(Math.max(w.conf(), confMin));
//		}

		return tb;
	}

	private Truth project(NALTask t, MutableTruthInterval when, float dur, double eviMin) {
		return when.project(t, dur, nar.eternalization.floatValueOf(t), eviMin);
	}

	public final Term eval(Term x) {
		if (x.complexity() <= complexMax) {
            var y = nar.eval(x);
			if (x == y || y.complexity() <= complexMax)
				return y;
		}

		return Null;
	}

//	public final void can(short id) {
//		hows[howsCount++] = id;
//	}

	/**
	 * current NAR time, set at beginning of derivation
	 */
	public final long now() {
		return focus.time();
	}

	public final int timeRes() {
		return nar.timeRes();
	}

	public final void run(Premise p) {
		//		boolean f = Feedback.start(this);
		try {
			p = model.pre(p, this);
			this.premise = p;
			p.run(this);
		} catch (RuntimeException e) {
			err(e, p);
		} finally {
			//if (f) Feedback.end();
			this.premise = null;
		}

	}

	private static void err(Exception e, Premise p) {
        var s = p.toString();
		var r = p.reaction();
		if (r!=null) s = r + ":" + s;
		logger.error(s, e);
		//p.delete();
	}

//	private void _runProfiled(Premise p) {
//		long t = Util.timeNS(()->_run(p));
//		System.out.println(t + "\t" + p.reactionType() + p.term() );
//	}


	/**
	 * main premise unification instance
	 */
	public class PremiseUnify extends Unify {
		transient int tasksRemain = -1;
		//public transient PatternReaction.DeriveTaskAction.DeriveTask deriving;
		public transient DerivingPremise p;

		/**
		 * TODO can ignore common variables
		 */
		public final Map<Term, Term> retransform = new UnifriedMap<>();

		PremiseUnify() {
			super(VAR_PATTERN, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
		}

		public void unify(Term Tp, Term Bp, Term T, Term B, boolean f, boolean s, DerivingPremise p) {
			s = s && T.equals(B);
			if (unify(f ? Tp : Bp, f ? T : B, s ? p : null)) {
				if (!s && live())
					unify(f ? Bp : Tp, f ? B : T, p);
			}
		}


		private boolean unify(Term x, Term y, @Nullable DerivingPremise p) {
            var finish = p!=null;

			this.p = finish ? p : null;

            var unified = unify(x, y, finish);

			if (finish) this.p = null; //release

			return unified;
		}

		/**
		 * should be created whenever a different NAR owns this Derivation instance, if ever
		 */
		public final RecursiveTermTransform transformDerived = new RecursiveTermTransform() {

			@Override
			public Term applyAtomic(Atomic x) {
                return switch (x) {
                    case Variable vx -> resolveVar(vx);
                    case Atom a -> applyAtom(a);
                    case null, default -> x;
                };
			}

			private Term applyAtom(Atom x) {
				if (x.equals(Premise.TaskInline))
					return premise.from();
				else if (x.equals(Premise.BeliefInline))
					return premise.to();
				else
					return derivationFunctors.getIfAbsentValue(x, x);
			}

			@Override
			public boolean evalInline() {
				return true;
			}

		};

		@Override protected final boolean match() {
            var p = this.p;
			if (p == null) return false; /* done */
			this.p = null;
			p.taskify(Deriver.this);
			return true;
		}

		/**
		 * resolve a target (ex: task target or belief target) with the result of 2nd-layer substitutions
		 */
		public Term retransform(Term x) {
			return x.replace(retransform);
		}

		public void clear(UnifyConstraint<PremiseUnify>[] constraints) {
			this.retransform.clear();
			this.clear();
			this.setTTL(NAL.derive.TTL_UNISUBST);
			this.tasksRemain = NAL.derive.PREMISE_UNIFICATION_TASKIFY_LIMIT;
			this.constrain(constraints);
		}

		public Deriver deriver() {
			return Deriver.this;
		}
	}

	@Deprecated
	public final class MyUnifyTransform extends UnifyTransform implements AutoCloseable {

		private MyUnifyTransform() {
			super(rng);
		}

		public void postUnified() {
			unify.retransform.putAll(xy);
		}

		@Override
		public void close()  {
			clear();
		}

		public void reset(int ttl) {
			this.ttl = ttl;
			this.vars = Op.Variables;
			this.dur = unify.dur;
			this.novel = false;
			//u.volMax = Deriver.this.volMax;
		}
	}

	private final class MyNALTaskEvaluation extends NALTaskEvaluation {

		MyNALTaskEvaluation() {
			super(Deriver.this);
		}

		/** deterministic solutions only */
		@Override protected boolean termutable() {
			return false;
		}

		@Override
		public void accept(NALTask y) {
			add(y.copyMeta(this.task), false);
		}
	}

	protected abstract class DeriverIterationTuner extends IterationTuner {

		long targetIterationPeriodNS;
		private final RealTime t;

		protected DeriverIterationTuner(int itersStart) {
			if (!(nar.time instanceof RealTime r))
				throw new UnsupportedOperationException();
            super(new RandomBits(new XoRoShiRo128PlusRandom()));
			this.t = r;
            this.iters = itersStart;
			update();
		}

		/** target context switch granularity. smaller=more responsive, bigger=more throughput */
		private static final float dutyCycle =
			1/4f;
			//1/6f;
			//1/3f;
			//1/2f;
			//1/8f;

		@Override public long targetPeriodNS() {
			return targetIterationPeriodNS;
		}

		public void update() {
			targetIterationPeriodNS = (long) (t.durNS() * dutyCycle * nar.cpuThrottle.floatValue());
		}
	}
}
//private final IntConsumer modelRunner;

//	/** offset for virtual addressing scheme for rules */
//	@Deprecated public final int howOffset;

//	public final transient short[] hows;
//	public transient int howsCount;
//	@Deprecated public final MetalDibitSet predMemo = new MetalDibitSet(384 /* increase as necessary */);

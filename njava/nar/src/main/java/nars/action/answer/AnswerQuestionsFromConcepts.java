package nars.action.answer;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.data.set.ArrayHashSet;
import jcog.signal.IntRange;
import nars.*;
import nars.action.resolve.Answerer;
import nars.action.resolve.TaskResolver;
import nars.focus.time.TaskWhen;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Termed;
import nars.unify.Unify;
import nars.unify.UnifyTransform;

import java.util.List;
import java.util.function.Predicate;

import static nars.Op.VAR_QUERY;

/** performs exhaustive search of concept index for unifiable answers to variable-containing questions and quests */
public abstract class AnswerQuestionsFromConcepts extends AnswerQuestions {

	public final boolean ACCEPT_REFINED_QUESTION = false;
	public final IntRange maxAnswersPerQuestion = new IntRange(1, 1, 1024);

	protected AnswerQuestionsFromConcepts(TaskWhen timing, TaskResolver resolver) {
		super(timing, true,true, resolver);
	}

	protected abstract Iterable<Term> source(Task q, Deriver d);

	public static class AnswerQuestionsFromConceptIndex extends AnswerQuestionsFromConcepts {

		public AnswerQuestionsFromConceptIndex(TaskWhen timing) {
			super(timing, Answerer.AnyTaskResolver);
		}

		@Override
		protected Iterable<Term> source(Task q, Deriver d) {
			return d.nar.concepts().map(Termed::term)::iterator;
		}

		@Override
		protected int tries(Deriver d) {
			return Integer.MAX_VALUE;
		}
	}

//	public static class AnswerQuestionsFromTaskLinks extends AnswerQuestionsFromConcepts {
//
//		public AnswerQuestionsFromTaskLinks(TaskWhen timing) {
//			super(timing, Answerer.AnyTaskResolver);
//			hasAny(PremiseTask, Op.AtomicConstant);
//		}
//
//		@Override
//		protected Iterable<Term> source(Task Q, Deriver d) {
//			return new TaskTermMatchIterator(d, Q);
//		}
//
//		@Override
//		protected int tries(Deriver d) {
//            return (int) Math.max(1, d.focus.links.size() * 0.5f); //TODO refine
//		}
//
//		private static final class TaskTermMatchIterator extends AbstractIterator<Term> implements Iterable<Term> {
//
//			/** tested after determining novel */
//			private final Predicate<Term> filter;
//			private final Iterator<? extends Premise> i;
//
//			TaskTermMatchIterator(Deriver d, Task Q) {
//				Term q = Q.term();
//
//				filter = q instanceof Compound ?
//						AtomSet.ContainingAllAtomsFrom.containsAllAtomsFrom((Compound) q)
//						:
//						z -> z instanceof Compound zz && zz.contains(q);
//
//                i = d.focus.links.sampleUnique(d.rng);
//			}
//
//			@Override protected Term computeNext() {
//
//				while (i.hasNext()) {
//					Term x = i.next().from();
//					if (filter.test(x))
//						return x;
//				}
//
//				return endOfData();
//			}
//
//
//			@Override
//			public Iterator<Term> iterator() {
//				return this;
//			}
//		}
//	}

	@Override
	protected void run(NALTask q, Deriver d) {
		int sourceTries = tries(d);
		int targetTries = sourceTries*sourceTries;

		int[] remain = {maxAnswersPerQuestion.intValue()};
		new Answering(q, source(q, d), d, answer->{
			react(answer, d);
			return --remain[0] > 0;
		}, sourceTries, targetTries, NAL.derive.TTL_UNISUBST);
	}

	protected abstract int tries(Deriver d);

	private class Answering  {

		private final Predicate<Term> unifiable;

		private final NALTask Q;
		private final Term q;


		final float qPri;
		private final int unifyTTL;
		private final Deriver d;
//		private final int volMax;
//		private final int structureNecessaryAny;
//		private final int structureNecessaryAll;

		private transient Unify u;
		private transient ArrayHashSet<Term> targets;

//		private static final boolean exhaustiveConjEventDecompose = false;

//		/** HACK stupid novelty filter */
//		final MRUMap<Term,Term> invalidTargets;

		Answering(NALTask question, Iterable<Term> source, Deriver d, Predicate<NALTask> each, int sourceTries, int targetTries, int unifyTTL) {
			this.d = d;
			Q = question;
			//TODO use caching concept indexer
			q = question.term();
			qPri = question.priElseZero();
			this.unifyTTL = unifyTTL;
//			volMax = d.volMax;

//			structureNecessaryAny = Op.AtomicConstant;// | q.opBit();
//			structureNecessaryAll = q.structure() & ~(Op.Variables);

			unifiable = q.unifiable(Op.Variables, (int) Math.ceil(d.dur()));

			int opExclude = question.QUEST() ? Op.IMPL.bit : 0;

//			invalidTargets = new MRUMap<>(Math.min(128, targetTries));

			var _timing = Util.once(()->timing.whenRelative(Q,d));
			
			for (Term a : source) {

					if (/*a.volume() < volMax &&*/ /*structured(a.structure()) &&*/ !q.equals(a)) {
						if (opExclude == 0 || ((a.structOp() & opExclude) == 0)) {

						for (Term b : tryAll(a)) {

							if (u == null)
								u = new UnifyTransform(this.d.rng) {
									@Override
									protected boolean filter(Term y) {
										return y.unneg().TASKABLE();
									}
								};

							Term qq = ((UnifyTransform)(u.clear(this.unifyTTL))).unifySubst(q, b, a);
							if (qq != null) {

								boolean lookup = !qq.hasAny(VAR_QUERY);

//								if (when == null)
//                                    when = timing.whenRelative(question, d);

								/* etc */
								NALTask qa = answer(Q, qq, lookup, ACCEPT_REFINED_QUESTION,
										_timing, d);
									/*else {
										LiveTaskLink link = LiveTaskLink.link(q, aa);
										link.priSet(question.punc(), qPri); //TODO divide priority among the generated tasklinks
										return link;
									}*/
								if (qa != null && !each.test(qa))
									return; //done
							} else {
								//invalidTargets.put(b,b); //not necessarily always un-unifiable, just in this instance but it's likely universal
							}

							if (--targetTries <= 0)
								return;
						}
					}
				}

				if (--sourceTries <= 0)
					return;
			}

		}

//		private boolean structured(int as) {
//			return Op.hasAny(as, structureNecessaryAny) && Op.hasAll(as, structureNecessaryAll);
//		}


		private List<Term> tryAll(Term x) {
			if (targets ==null)
				targets = new ArrayHashSet<>(x.complexity());
			else
				targets.clear();

			tryTerm(x);

			switch (x.op()) {
				case IMPL -> {
					Subterms xx = x.subterms();
					Term subj = xx.subUnneg(0), pred = xx.sub(1);
					tryTerm(subj);
					if (subj.CONJ()) tryDecomposeConj(subj);
					tryTerm(pred);
					if (pred.CONJ()) tryDecomposeConj(pred);
				}
				case INH, SIM -> tryTerms(x.subterms());
				case CONJ -> tryDecomposeConj(x);
			}

			Lst<Term> tt = targets.list;

			if (tt.size() > 1)
				tt.shuffleThis(d.rng);

			return tt;
		}

		private void tryTerms(Subterms subterms) {
			for (Term z : subterms)
				tryTerm(z.unneg());
		}

		private void tryTerm(Term x) {
			if (unifiable.test(x))
				targets.add(x);
		}

		private void tryDecomposeConj(Term x) {
			((Compound)x).conds((what) -> tryTerm(what.unneg()), true, true);
		}

	}
}
package nars.action.answer;

import nars.Deriver;
import nars.NAL;
import nars.NALTask;
import nars.Term;
import nars.action.resolve.TaskResolver;
import nars.deriver.op.Taskify;
import nars.deriver.reaction.TaskReaction;
import nars.focus.time.TaskWhen;
import nars.task.proxy.SpecialTermTask;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public abstract class AnswerQuestions extends TaskReaction {

	private final TaskResolver resolver;
	final TaskWhen timing;

//	private boolean priBoostAnswer = false;

	protected AnswerQuestions(TaskWhen timing, boolean questions, boolean quests, TaskResolver resolver) {
		super(false, false, questions, quests);
		hasBeliefTask(false);
		this.timing = timing;
		this.resolver = resolver;
	}

	protected final NALTask answer(NALTask Q, Term q, boolean lookup, boolean acceptRefinedQuestion, Supplier<long[]> when, Deriver d) {
		assert(lookup || acceptRefinedQuestion);

		q = q.unneg(); //HACK

		NALTask x = lookup ? resolver.resolveTask(q, Q.QUESTION() ? BELIEF : GOAL, when, d, null) : null;

		return x != null || !acceptRefinedQuestion ? x : answerWithRefinedQuestion(Q, q, acceptRefinedQuestion);
	}

	@Nullable
	private static NALTask answerWithRefinedQuestion(NALTask Q, Term q, boolean acceptRefinedQuestion) {
		return q.unneg().equals(Q.term()) ? null :
			SpecialTermTask.proxy(Q, Taskify.questionSalvage(q), !NAL.DEBUG);
	}

//	@Override
//	protected void reacting(NALTask answer, Deriver d) {
//		super.reacting(answer, d);
//		if (priBoostAnswer) {
//			NALTask question = d.premise.task(); assert (question.QUESTION_OR_QUEST());
//			answer.priMax(question.priElseZero()); //boost A's priority by Q's
//		}
//	}

//	@Override
//	protected boolean discountComplexity() {
//		return true;
//	}
}
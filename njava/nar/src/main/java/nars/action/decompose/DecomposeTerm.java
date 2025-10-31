package nars.action.decompose;

import jcog.Is;
import nars.*;
import nars.action.resolve.TaskResolver;
import nars.deriver.reaction.NativeReaction;
import nars.focus.time.TaskWhen;
import nars.link.MutableTaskLink;
import nars.premise.NALPremise;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termed;
import nars.unify.constraint.TermMatch;
import org.jetbrains.annotations.Nullable;

/** acts as Compound-term 'feature detector' by extracting a sub-component
 *   (or derivative thereof) in forming Premise's
 */
@Is({"Feature_detection_(nervous_system)", "Solvent"})
public abstract class DecomposeTerm extends NativeReaction {

	private int dir = +1;

	private TaskResolver resolver = null;

	private TaskWhen timing = null;

	public abstract @Nullable Term decompose(Compound src, Deriver d);

	DecomposeTerm() {
		this(true);
	}

	DecomposeTerm(boolean single) {
		super();

//		if (seedLinksOrTasks)
//			tasklink();
//		else {
			taskPunc(true, true, true, true);
			hasBeliefTask(false);
//		}

		if (single) {
			taskEqualsBelief();
			iff(PremiseTask, new TermMatch.SubsMin((short) 1));
		} else {
			neq(PremiseTask, PremiseBelief);
		}
	}

	public final DecomposeTerm bidi(TaskResolver r, TaskWhen w) {
        dir(r, w, 0);
		return this;
	}

	public final DecomposeTerm reverse(TaskResolver r, TaskWhen w) {
		dir(r, w, -1);
		return this;
	}

	private void dir(TaskResolver r, TaskWhen w, int d) {
		this.dir = d;
		this.resolver = r;
		this.timing = w;
	}

	@Override
	protected final void run(Deriver d) {

        var x = term(d.premise);

        var y = decompose((Compound)x, d);
		if (y == null)
			return;

		assert(!(y instanceof Neg)): "decomposed is Neg";

		activate(x, y, d);
	}

	protected void activate(Term x, Term y, Deriver d) {
        var DIR = this.dir;

		if (DIR!=1 && !y.TASKABLE())
			DIR = 1; //force forward

        activate(x, y, switch (DIR) {
            case +1 -> true;
            case -1 -> false;
            default -> d.randomBoolean();
        }, d);
	}

	protected Term term(Premise p) {
		return p.from();
		//return Image.imageNormalize(p.from());
	}

	protected void activate(Term x, Term y, boolean direction, Deriver d) {
		if (NAL.Decompose_LinkOrPremise)
			link(x, y, direction, d);
		else
			premise(y, direction, d);
	}

	private void premise(Term y, boolean direction, Deriver d) {
        var X = d.premise.task();
		if (direction)
			premise(X, y, d);
		else
            premiseReverse(y, X, d);
	}

	private void premise(NALTask task, Termed belief, Deriver d) {
		var p = NALPremise.the(task, belief, false);
		if (structural())
			((NALPremise.NonSeedTaskPremise)p).structural = true;
		d.add(p);
	}

	protected boolean structural() {
		return false;
	}

	/** try to resolve a task for use with */
	private void premiseReverse(Term y, NALTask X, Deriver d) {
        var x = X.BELIEF() ? X : X.term();
        var p = X.punc();
        var Y = resolver.resolveTask(y, p, timing, d);
        if (Y != null)
            premise(Y, x, d);
    }

	private static void link(Term x, Term y, boolean direction, Deriver d) {
		if (!direction) {
            var z = NALTask.taskTerm(y, Op.COMMAND, true);
			if (z!=null) {
				y = x;
				x = z;
			}
		}
		d.link(MutableTaskLink.link(x, y, d));
	}



}
package nars.action.link;

import jcog.TODO;
import jcog.pri.PriReference;
import jcog.signal.FloatRange;
import nars.Deriver;
import nars.NALTask;
import nars.Op;
import nars.Term;
import nars.action.link.index.AdjacentTerms;
import nars.deriver.reaction.NativeReaction;
import nars.link.MutableTaskLink;
import nars.premise.NALPremise;

import java.util.Iterator;


public class TermLinking extends NativeReaction {

	/** forward or reverse balance */
	public final FloatRange direction = FloatRange.unit(
		1 //forward only
		//0.95f
		//0.5f //balanced
	);

	private final AdjacentTerms adj;

	private static final int triesMax = Integer.MAX_VALUE;

	/** max links or premises to generate per iteration */
	private static final int linksProvidedMax = 1;

	/** for only constant terms, ex: if not testing unification */
	private static final boolean variables = true;

	/** adjacency among ONLY image-normalized targets */
	private static final boolean images = false;

	/** experimental: keeps Belief (task) as generated premise's Task (reverse) */
	private static final boolean reverseCreatesPremisesFromBeliefTask = false;

	private static final boolean restricted =
		true;
		//false;

	public TermLinking(AdjacentTerms adj) {
		this.adj = adj;

		taskPunc(true, true, true, true, false);
		if (restricted) {
			taskEqualsBelief(); //necessary?
			hasBeliefTask(false); //necessary?
		}

		isAny( PremiseBelief, Op.Conceptualizables);
		hasAny(PremiseBelief, Op.AtomicConstant); //dont bother with variable-only terms

		if (!variables) {
			hasAny(PremiseTask,   Op.Variables, false);
			hasAny(PremiseBelief, Op.Variables, false);
		}

		if (!images)
			pre(NonImages);

//		if (TASK_VOL_GREATER_THAN_BELIEF_VOL)
//			constrain(new VolumeCompare(PremiseTask, PremiseBelief, false, -1).neg()); //belief.volume <= task.volume
	}


	@Override
	protected final void run(Deriver d) {
        var p = d.premise;

		Term _x = p.from(), _y = p.to();

        var direction = _x.equals(_y) || d.randomBoolean(this.direction);
		Term x, y;
		if (direction) {
			x = _x;  y = _y; //forward
		} else {
			x = _y;  y = _x; //reverse
		}

		var z = adj.adjacencies(x, y, d);
        if (z != null)
			link(x, z, direction, d);
    }

	private static void link(Term x, Iterator<PriReference<Term>> yy, boolean direction, Deriver d) {
		int triesRemain = triesMax, linksRemain = linksProvidedMax;
		while (triesRemain-- > 0 && linksRemain > 0 && yy.hasNext()) {
            var y = yy.next().get();
			if (link(y, direction, d))
				linksRemain--;
		}
	}

	protected static boolean link(Term a, boolean dir, Deriver d) {
		var p = d.premise;
		Term FROM = p.from(), TO = p.to();
		Term x = dir ? FROM : a;
		Term y = dir ? a   : TO;

		var createPremiseOrLink =
			dir || (reverseCreatesPremisesFromBeliefTask && p.belief()!=null);
			//dir; // && !NAL.TermLinking_Reaction_LinkOrPremise;
		if (createPremiseOrLink) {
            return dir ?
				premise(p.task(), y, TO, FROM, d) :
				premise(p.belief(), x, FROM, TO, d);
        } else
			return termLink(dir, x, FROM, TO, y, d);
	}

	private static boolean termLink(boolean dir, Term x, Term FROM, Term TO, Term y, Deriver d) {
		if (dir)
			throw new TODO();

        if (x.equalsRoot(FROM) || x.equalsRoot(TO))
			return false;
        else {
			d.link(MutableTaskLink.link(x, y, d));
			return true;
		}
	}

	private static boolean premise(NALTask t, Term x, Term TO, Term FROM, Deriver d) {
        if (x.equalsRoot(TO) || x.equalsRoot(FROM)) return false;
        else {
			d.add(NALPremise.the(t, x, true));
			return true;
		}
	}


//	private boolean link(Term x, Term y, Deriver d) {
//		var dirOut = d.randomBoolean(directionOut);
//		return dirOut ?
//			_link(x, y, d) :
//			_link(y, x, d);
//	}

}
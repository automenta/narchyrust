package nars.action.transform;

import nars.Deriver;
import nars.NALTask;
import nars.Term;
import nars.action.TaskTransformAction;
import nars.action.decompose.DynamicDecomposer;
import nars.link.AtomicTaskLink;
import nars.task.proxy.SpecialTermTask;
import nars.term.Compound;
import nars.term.util.Image;
import nars.unify.constraint.StructMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

import static nars.Op.*;
import static nars.term.util.Terms.recCom;

/** dynamic image transform
 * may not be useful at all
 * */
public class ImageUnfold extends TaskTransformAction {
	final private boolean generateTaskOrLink;


//	final boolean taskOrBelief;

	public ImageUnfold(boolean generateTaskOrLink) {
		super();
		this.generateTaskOrLink = generateTaskOrLink;

//		this.taskOrBelief = taskOrBelief;
//		if (taskOrBelief) {
			single();
			//single(true,true,false,false);
//		} else {
//			neq(PremiseTask, PremiseBelief);
//			taskPunc(true, true, true, true);
//			hasBeliefTask(false);
//		}

		Term x = PremiseTask; //taskOrBelief ? PremiseTask : PremiseBelief;

		is(x, INH);
		iff(x, new StructMatcher.HasSurfaceStruct(PROD));
		iffNot(x, new StructMatcher.HasSubStruct(IMG)); //TODO optional
	}

	@Override
	protected @Nullable NALTask transform(NALTask x, Deriver d) {

		Term t = x.term(); //taskOrBelief ? d.premise.from() : d.premise.to();

		Term s = t.sub(0), p = t.sub(1);
		if (recCom(s, p))
			return null; //prevent loop

		//shuffle order
		int k = d.rng.nextBooleanAsInt();
		for (int j = 0; j < 2; j++) {
			boolean i = (j + k) % 2 == 0;
			if ((i ? s : p).PROD()) {
				NALTask y = unfold(x, s, p, i, d);
				if (y!=null)
					return y;
			}
		}
		return null;
	}

	@Override
	protected boolean copyMeta() {
		return true;
	}

	/** doesnt unnegate subterms */
	private static final DynamicDecomposer DecomposeOneLayerPolarized = new DynamicDecomposer.WeightedDynamicCompoundDecomposer() {
		@Override
		public @Nullable Term apply(Compound t, RandomGenerator rng) {
			return sampleDynamic(t, 1, rng);
		}

		@Override
		protected boolean unneg() {
			return false;
		}
	};


	@Nullable private NALTask unfold(NALTask T, Term t0, Term t1, boolean subjOrPred, Deriver d) {
		Compound p = (Compound) (subjOrPred ? t0 : t1);
		Term forward = DecomposeOneLayerPolarized.apply(p, d.rng); //TODO if t0 && t1 choose randomly
		if (forward != null) {
			Term t = T.term();
			Term y = subjOrPred ? Image.imageExt(t, forward) : Image.imageInt(t, forward);
			if (y instanceof Compound && y.CONCEPTUALIZABLE()) {
				if (generateTaskOrLink) {
					return SpecialTermTask.proxy(T, y, true);
				} else {
					d.link(AtomicTaskLink.link(y));
				}
			}
		}
		return null;
	}

}
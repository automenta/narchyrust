package nars.action.link.index;

import jcog.pri.PriReference;
import jcog.pri.bag.Sampler;
import nars.Deriver;
import nars.Focus;
import nars.NAL;
import nars.Term;
import nars.link.TermLinkBag;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import static jcog.pri.op.PriMerge.max;

/**
 * stores a bag of ranked reverse atom termlinks in each concept meta table
 */
public class BagAdjacentTerms implements AdjacentTerms {


	@Nullable @Override
	public Iterator<PriReference<Term>> adjacencies(Term from, Term to, Deriver d) {
//		TermLinkBag cached = Snapshot.get(
//			id(f), to,
//			(int) updatePeriod(to, f),
//			(c, b) ->
//				commit(b == null ? new TermLinkBag() : b, c.term(), d),
//			f);

		var bag = Focus.termBag(from, d.focus);
		if (bag==null || bag.isEmpty()) return null;
		bag.commit();
		return sample(from, d.rng, bag);
	}


	//	private TermLinkBag commit(TermLinkBag bag, Term x, Deriver d) {
//		return commit(bag, x, d.focus, d.rng);
//	}
//
//	private TermLinkBag commit(TermLinkBag b, Term x, Focus f, RandomGenerator rng) {
//		var src = f.links;
//		b.capacity(capacity(x, f));
//		var tries = tries(x, src.capacity());
//		if (tries > 0)
//			b.commit(x,
//				src.sampleUnique(rng),
//				Math.min(src.size(), tries),
//				pri(x, b.capacity()),
//				NAL.derive.TERMLINK_and_CLUSTERING_FORGET_RATE);
//		return b;
//	}

	/** insertion priority */
	private static float pri(Term t, int capTgt) {
		if (TermLinkBag.Merge == max)
			return 0.5f;
		else {
			//merge == plus:

			return 2f/capTgt;
//			return (float) Math.pow(1 + capTgt,
//					-1 / 2f
//					//-1/4f
//			);
			//return 1 / Math.sqrt(1 + capTgt);
			//return (capTgt / (1f + src.capacity()));
		}
	}

	private Iterator<PriReference<Term>> sample(Term e, RandomGenerator rng, Sampler<PriReference<Term>> b) {
		return b.sampleUnique(rng);
		//((Bag)b).print(); System.out.println();
		//return PriReference.get(b.sampleUnique(rng));
	}

	/** cache update period, in cycles */
	private float updatePeriod(Term to, Focus f) {
		return f.durSys;
		//return to.volume() * f.dur();
		//return to.volume() * d.nar.dur();
	}


	/** bag scan rate, in max # of items to test from Bag */
	public int tries(Term x, int srcCap) {
		return Math.round(srcCap * NAL.premise.TERMLINK_DENSITY);
		//return Math.round(Fuzzy.mean((float)capTgt, srcCap) * NAL.premise.termLinkTrying);
		//return capTgt;
		//return Math.max(1, (int) Util.sqrt(capTgt));
		//return Integer.MAX_VALUE; //exhaustive
		//return b.size()/2; //semi-exhaustive
		//return Math.max(1, (int) Util.sqrt(b.capacity()));
	}





//	/** experimental */
//	private double priHebbian(Term t, int capTgt, Bag<Premise, Premise> src) {
//		/* hebbian-like 'fire-together': use the priority of the concept to scale the priorities of termlinks */
//		var tgtLink = src.get(MutableTaskLink.link(t));
//		float tgtPri = tgtLink!=null ? tgtLink.priElseZero() : 0;
//		float bagMax = src.priMax(); //normalizing factor
//		if (bagMax > Prioritized.EPSILON) tgtPri = Math.min(1, tgtPri / bagMax);
//		return Util.max(1.0/ capTgt, tgtPri);
//	}
}
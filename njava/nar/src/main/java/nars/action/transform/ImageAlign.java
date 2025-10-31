package nars.action.transform;

import jcog.util.ArrayUtil;
import nars.*;
import nars.deriver.reaction.NativeReaction;
import nars.premise.NALPremise;
import nars.task.proxy.SpecialTermTask;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.control.PREDICATE;
import nars.term.util.Image;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

import static nars.Op.INH;
import static nars.Op.PROD;

/**
 * TODO make recursive version that aligns images in subterms of potentially non-inh compounds
 * ex:
 *
 *  x(a,b) + (a(b,x) ==> y)
 * 		|-
 * 	a(\,x) + (a(b,x) ==> ...   //TODO check the image in this example
 *
 * */
public abstract class ImageAlign extends NativeReaction {

	ImageAlign() {
		neq/*neqRoot*/(PremiseTask, PremiseBelief);
		//single(true, true, true, true);
		noOverlap();
		taskPunc(true,true,true,true);
	}

	private static final PREDICATE<Deriver> ImageAlignable = new PREDICATE<>($.p("image", "alignable")) {

		@Override
		public float cost() {
			return 0.15f;
		}

		@Override
		public boolean test(Deriver d) {
			Premise p = d.premise;
			return Image.alignable(p.from(), p.to());
		}
	};


	/** require both task and belief to be "-->" */
	protected void inhinh() {
		for (Term x : new Term[] {PremiseTask, PremiseBelief}) {
			is(x, INH);
//			isAny(x,INH, SIM);
//			isAny(x, INH, SIM, IMPL);
//			hasAny(x, INH); //TODO more specific to match what is actually searched - if only first layer
		}
		pre(ImageAlignable);
	}

	@Override protected void run(Deriver d) {
		Premise p = d.premise;

		Compound T = (Compound) p.from(), B = (Compound) p.to();

		Compound t = component(T, B, true,  d); if (t  == null) return;
		Compound b = component(T, B, false, d); if (b  == null) return;

		Term[] tb = align(t, b, d); if (tb == null) return;

		Term ti = tb[0]; if (ti.unneg().complexity() > d.complexMax) return;
		Term bi = tb[1]; if (bi.unneg().complexity() > d.complexMax) return;

		boolean te = t.equals(ti), be = b.equals(bi); if (te && be) return;

//		if (premiseOrTaskLink)
			deriveNALPremise(T, t, te, ti, B, b, be, bi, d);
//		else
//			d.add(AtomicTaskLink.link(ti, bi));

	}

	private static void deriveNALPremise(Compound T, Compound t, boolean te, Term ti, Compound B, Compound b, boolean be, Term bi, Deriver d) {
		Premise p = d.premise;
		NALTask YT;
		NALTask pt = p.task();
		if (!te) {
			YT = copyMeta(SpecialTermTask.proxy(pt, taskTerm(T, t, ti), true), pt);
			if (YT==null)
				return; //TODO why??
		} else
			YT = pt;

		Termed YB;
		NALTask pb = p.belief();
		if (!be) {
			Term yb = taskTerm(B, b, bi);
			YB = pb != null ?
					SpecialTermTask.proxy(pb, yb, true) :
					yb.unneg();
			if (YB instanceof NALTask YBT)
				YB = copyMeta(YBT, pb);
		} else
			YB = pb!=null ? pb : B;

		d.add(
			NALPremise.the(YT, YB, false)
		);
	}

	private static NALTask copyMeta(NALTask YT, NALTask pt) {
		return YT!=null ? YT.copyMeta(pt).setCreation(pt.creation()) : null;
	}

	/** HACK weak validation; only normalizes before proxyUnsafe */
	private static Term taskTerm(Compound src, Compound x, Term y) {
		return src.replace(x, y);//.normalize();
	}


	@Nullable static Compound[] alignTarget(Compound t, Compound b, RandomGenerator r) {
		//HACK shuffled ordering
		byte[] order = new byte[4];
		for (int i = 0; i < order.length; i++) order[i] = (byte) i;
		ArrayUtil.shuffle(order, r);

		for (byte j : order) {
			switch (j) {
				case 0 -> {
					Compound bb = Image.alignTo(t, b, r);
					if (bb != null) return new Compound[]{t, bb};
				}
				case 1 -> {
					Compound tt = Image.alignTo(b, t, r);
					if (tt != null) return new Compound[]{tt, b};
				}
				case 2 -> {
					Compound tt = Image.alignTo(b, t, r);
					if (tt != null) return new Compound[]{tt, b};
				}
				case 3 -> {
					Compound bb = Image.alignTo(t, b, r);
					if (bb != null) return new Compound[]{t, bb};
				}
			}
		}

		return null;
	}

	/** selects the task component */
	protected abstract Compound component(Compound t, Compound b, boolean taskOrBelief, Deriver d);

	@Nullable protected abstract Compound[] align(Compound t, Compound b, Deriver d);

	public static class ImageAlignBidi extends ImageAlign {

		{
			inhinh();
		}

		@Override
		protected Compound component(Compound t, Compound b, boolean taskOrBelief, Deriver d) {
			return taskOrBelief ? t : b;
			//return (Compound) Image.imageNormalize(taskOrBelief ? t : b);
		}

		@Override
		protected Compound[] align(Compound t, Compound b, Deriver d) {
			return Image.align(t, b, d.rng, NOVEL_REQUIRED);
		}


		/**
		 * requires that the two statements do not already share equal
		 * subterms.
		 *
		 * if novelty isnt required then a lot of obvious spam is generated
		 * in permuting the images
		 *
		 * probably should always be true.
		 */
		private static final boolean NOVEL_REQUIRED = true;

	}


	public static class ImageAlignUni_Root extends ImageAlign {

		private static final boolean allowVars = false;

		{
			inhinh();

			if (!allowVars) {
				hasAny(PremiseTask, Op.Variables, false);
				hasAny(PremiseBelief, Op.Variables, false);
			}
		}

		@Override
		protected Compound component(Compound t, Compound b, boolean taskOrBelief, Deriver d) {
			return taskOrBelief ? t : b; //as-is
		}

		@Override
		protected Compound[] align(Compound t, Compound b, Deriver d) {
			return alignTarget(t, b, d.rng);
		}
	}


	/** TODO include opposite task,belief order */
	public static class ImageAlignUni_InCompound extends ImageAlign {

		private final boolean fwd;

		public ImageAlignUni_InCompound(boolean fwd) {

			this.fwd = fwd;

			Variable a, b;
			if (fwd) {
				a = PremiseTask; b = PremiseBelief;
			} else {
				a = PremiseBelief; b = PremiseTask;
			}

			inhinh();
//			is(a, INH);
//			isAny(b, Op.Statements);
			hasAll(b, INH.bit | PROD.bit, true); //TODO test in substructure only
		}

		@Override
		protected Compound component(Compound t, Compound b, boolean taskOrBelief, Deriver d) {
			if (fwd)
				return taskOrBelief ? t : ((Compound)statementSub(b, d));
			else
				return taskOrBelief ? ((Compound)statementSub(t, d)) : b;
		}

		@Nullable static Term statementSub(Term x, Deriver d) {
			Term s = x.sub(0).unneg(), p = x.sub(1).unneg();
			boolean S = inhSubterm(s), P = inhSubterm(p);

			if (S && P) return d.randomBoolean() ? s : p;
			else if (S) return s;
			else if (P) return p;
			else return null;
		}

		private static boolean inhSubterm(Term x) {
			return x.INH() && x.subterms().OR(Term::PROD);
		}

		@Override
		protected Compound[] align(Compound t, Compound b, Deriver d) {
			return alignTarget(t, b, d.rng);
		}
	}

//	private static Term[] choose(Term t, Term b, RandomBits rng) {
//		final boolean ti = t.INH();
//		final boolean bi = b.INH();
//		if (!ti && !bi) return null; //give up; require >=1
//
//		if (ti && bi) return new Term[] { t, b }; //TODO only maybe, and look deeper otherwise
//
//		Term[] tb = new Term[2];
//		if (ti) tb[0] = t;
//		if (bi) tb[1] = b;
//		if (!ti) {
//			//find inh in t
//			if ((tb[0] = rng.get(t.subterms().subs(z -> z.INH() && !b.equalsPN(z))))==null)
//				return null;
//		}
//		if (!bi) {
//			//find inh in b
//			if ((tb[1] = rng.get(b.subterms().subs(z -> z.INH() && !t.equalsPN(z))))==null)
//				return null;
//		}
//
//		return tb;
//	}




}
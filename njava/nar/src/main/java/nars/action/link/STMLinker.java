package nars.action.link;

import jcog.TODO;
import jcog.Util;
import nars.Deriver;
import nars.NALTask;
import nars.Task;
import nars.Term;
import nars.action.transform.TemporalComposer;
import nars.deriver.reaction.NativeReaction;
import nars.link.AtomicTaskLink;
import nars.premise.NALPremise;
import nars.term.Termed;
import nars.unify.constraint.TermMatch;
import org.jctools.queues.MpmcArrayQueue;
import org.jetbrains.annotations.Nullable;

import static nars.term.util.Image.imageNormalize;

public class STMLinker extends NativeReaction implements TemporalComposer {

//	/** whether to override the resulting premise's priority with a local heuristic, rather than rely on the deriver's budget impl */
//	private static final boolean BUDGET_OVERRIDE = false;

	private static final boolean premiseOrLink = true; //TODO subclass
	private static final boolean swapOnlyBeliefToBelief = true;
	private static final boolean allowSeq = true;

//	/** for link: */
//	public final AtomicBoolean bidi = new AtomicBoolean(false);
	//TODO 'FloatRange balance' if bidi



	private final int capacity;

	private static final boolean bidirectional = false;


	public STMLinker(boolean b, boolean g, boolean q, boolean Q) {
		this(1, b, g, q, Q);
	}

	public STMLinker(int capacity, boolean b, boolean g, boolean q, boolean Q) {
		super();
		single();
		taskPunc(b, g, q, Q);

		if (!allowSeq)
			iffNot(PremiseTask, TermMatch.SEQ);

		this.capacity = capacity;
	}

	@Override
	protected final void run(Deriver d) {

        var q = d.focus.local(this, s->
			new MpmcArrayQueue<NALTask>(Math.max(2, capacity))
			//new MetalConcurrentQueue<>(capacity)
		);


		boolean novel;
        var x = d.premise.task();
        var next = q.peek();
		if (next == null) {
			novel = true;
		} else if (capacity == 1) {
			//optimized 1-ary case
			novel = link(x, next, d);
		} else {
			throw new TODO();
//			//TODO test
//			novel = true;
//            int h = q.head();
//			for (int i = 0; novel && i < capacity; i++)
//				novel &= link(x, q.get(h, i), d);
		}

		if (novel && keep(x)) {
			if (next!=null)  q.poll();
            var accepted = q.offer(x);
		}

	}

	private static boolean keep(Task x) {
		return true;
	}

	/** returns if novel */
	private boolean link(NALTask next, @Nullable NALTask prev, Deriver d) {
		if (prev == null)
			return true;

		return premiseOrLink ?
			taskPremise(prev, next, d) :
			taskLink(next, prev, bidirectional, d);
	}

	private static boolean taskPremise(NALTask a, NALTask b, Deriver d) {
		if (a.equals(b))
			return false;

		if (swapOnlyBeliefToBelief) {
			if (!b.BELIEF() && a.BELIEF()) {
				//swap so that 'a' is the belief task to non-belief 'b'
                var c = a;
				a = b;
				b = c;
			}
		}

		Termed B;
		if (!b.BELIEF()) {
			B = imageNormalize(b.term());
			if (B.equals(a.term()))
				return false; //will be a useless single premise
		} else
			B = b;

		d.add(NALPremise.the(a, B, true));

		return true;
	}


	private static boolean taskLink(Task next, Task prev, boolean bidi, Deriver d) {
		//TODO imageNormalize?
		Term n = imageNormalize(next.term()).concept(),
			 p = imageNormalize(prev.term()).concept();
		if (n.equals(p))
			return false;


		//TODO use d.focus.budget's method priTasks
        var pri = (float) (Util.mean(prev.priElseZero(), next.priElseZero()) * (bidi ? 0.5 : 1));

		taskLink(p, n, prev.punc(), pri, d);

		if (bidi)
			taskLink(n, p, next.punc(), pri, d);

		return true;
	}

	private static boolean taskLink(Term a, Term b, byte punc, float pri, Deriver d) {
        d.link(AtomicTaskLink.link(a, b).priPunc(punc, pri));
		return true;
	}


//	/** stm budget function */
//	protected static float pri(NALTask a, NALTask b) {
//		//return Util.sqr(Util.mean(a.pri() , b.pri()));
//		return Fuzzy.and(a.priElseZero() , b.priElseZero())/2;
//	}

//	@Override public float prob(Deriver d) {
//		return (float) d.pri.probGrow(d.taskTerm.volume() >= d.beliefTerm.volume() ? d.taskTerm : d.beliefTerm, d);
//	}
	//original tasklink version:

}
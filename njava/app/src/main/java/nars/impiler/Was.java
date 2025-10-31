//package nars.impiler;
//
//import jcog.data.graph.MapNodeGraph;
//import nars.NAR;
//import nars.Task;
//import nars.Term;
//import nars.Concept;
//import nars.Focus;
//
//import java.util.stream.Stream;
//
///** snapshot of a What */
//public class Was extends MapNodeGraph<Term, Task> {
//
//	NAR nar = null;
//
//	public Was() {
//
//
//	}
//
//	public void add(Focus w) {
//		this.nar = w.nar; //HACK
//		add(w.concepts());
//	}
//
//	public void add(Stream<? extends Concept> concepts) {
//		Impiler.load(concepts, nar).forEach(n -> {
//			nodes.put(n.id, n);
//		});
//	}
//
//
//
////	public void add(Concept x) {
////
////		addAll(
////			Stream.of(x).peek(c ->{ //HACK TODO dont do this
////			//impl
////			if (Impiler.filter(c.term()))
////				c.beliefs().taskStream().forEach(t -> Impiler.load(t, nar));
////			})
////			.map(c -> Impiler.node(c, false))
////			.filter(Objects::nonNull)
////			.flatMap(n -> Streams.stream(n.edges(false, true)))
////			.map(FromTo::id));
////	}
//
////	public void add(Task x) {
////
////
////		assert(x.term().IMPL());
////		Subterms xx = x.term().subterms();
////		Term s = xx.sub(0);
////		Term p = xx.sub(1);
////		Term pp = p.negIf(x.isNegative());
////		//for (Term xxx : xx) {
////			edge(s, p, x);
////		//}
////
//////		boolean sSeq = Conj.isSeq(s), pSeq = Conj.isSeq(p);
//////
//////		if (sSeq) {
//////			final Term[] prev = {null};
//////			long[] lPrev = {0};
//////			s.eventsAND((when,what)->{
//////				if (prev[0] !=null)
//////					conjEdge(x, prev[0], what, Tense.occToDT(when - lPrev[0]));
//////				prev[0] = what;
//////				//if (first[0] == null) first[0] = what;
//////				lPrev[0] = when;
//////				return true;
//////			}, 0, false, false);
//////			s = prev[0];
//////		}
//////		if (pSeq) {
//////
//////			final Term[] first = {null}, prev = {null};
//////			long[] lPrev = {0};
//////			s.eventsAND((when,what)->{
//////				if (prev[0] !=null)
//////					conjEdge(x, prev[0], what, Tense.occToDT(when - lPrev[0]));
//////				prev[0] = what;
//////				if (first[0] == null) first[0] = what;
//////				lPrev[0] = when;
//////				return true;
//////			}, 0, false, false);
//////			pp = first[0];
//////		}
////
//////		if (sSeq || pSeq) {
//////			if (!s.equals(pp)) { //HACK
//////				int dt = x.dt();
//////				addEdge(s, pp, proxy(x, IMPL.the(s, dt, pp)));
//////			}
//////		} else {
////			addEdge(s, pp, x);
//////		}
////
////	}
////	protected void conjEdge(Task t, Term x, Term y, int dt) {
////		if (!x.equals(y)) //HACK
////			addEdge(x, y, proxy(t, CONJ.the(x, dt, y)));
////	}
////
////	private NALTask proxy(Task t, Term newTerm) {
////		return NALTask.task(newTerm, QUESTION,null, t.creation(), t.start(), t.end(), t.stamp());
////	}
////	public void addAll(Stream<Task> ss) {
////		ss.forEach(this::add);
////	}
//
//	/** bfs grow layer - add all tangent concepts */
//	public void grow(NAR nar) {
////		java.util.Set<Term> queue = new HashSet();
////		nar.concepts().map(x -> x.term()).filter(x -> x instanceof nars.term.Compound).forEach(x -> {
////			if (x.IMPL()) {
////				Term S = x.sub(0); boolean hasS = node(S)!=-1; //TODO Sneg?
////				Term P = x.sub(1); boolean hasP = node(P)!=-1;
////				if (hasS ^ hasP) {
////					//add the other
////					//if (hasS) queue.add(P);
////					//else queue.add(S);
////					queue.add(x);
////				}
////			}
////		});
////		if (!queue.isEmpty())
////			queue.stream().map(nar::conceptualize).forEach(this::add);
//	}
//}
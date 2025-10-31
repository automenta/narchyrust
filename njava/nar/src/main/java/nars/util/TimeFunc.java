//package nars.util;
//
//import nars.Term;
//
//import static nars.Op.XTERNAL;
//import static nars.term.atom.Bool.Null;
//
///** temporal quantifiers
// *  TODO
// * */
//public enum TimeFunc {
//	;
//
//	abstract static class TemporalAccessor {
//		/** discovers the 'other' events besides 'relativeTo', and returns as a term (a sequence if necessary) */
//		public abstract Term otherEvents(Term t, Term relativeTo, boolean inclBefore, boolean includeIt, boolean includeAfter);
//
//		/** measures delta time between events in 't' , or XTERNAL if uncomputable */
//		public abstract int dt(Term t, Term x, Term y);
//	}
//
//	static final TemporalAccessor ConjAccessor = new TemporalAccessor() {
//
//		@Override
//		public Term otherEvents(Term t, Term relativeTo, boolean inclBefore, boolean includeIt, boolean includeAfter) {
//			return Null;  //TODO
//		}
//
//		@Override
//		public int dt(Term t, Term x, Term y) {
//			return XTERNAL; //TODO
//		}
//	};
//	static final TemporalAccessor ImplAccessor = new TemporalAccessor() {
//
//		@Override
//		public Term otherEvents(Term t, Term relativeTo, boolean inclBefore, boolean includeIt, boolean includeAfter) {
//			return Null; //TODO
//		}
//
//		@Override
//		public int dt(Term t, Term x, Term y) {
//			return XTERNAL;  //TODO
//		}
//	};
//
////	public static class before extends AbstractInlineFunctor {
////
////		public before() {
////			super("before");
////		}
////
////		@Override
////		public Term apply(Evaluation e, Subterms args) {
////			if (args.subs()!=3)
////				return Null;
////
////			Term temporal = args.sub(0);
////			TemporalAccessor a;
////            switch (temporal.op()) {
////                case CONJ -> a = ConjAccessor;
////                case IMPL -> a = ImplAccessor;
////                default -> {
////                    return Null;
////                }
////            }
////
////			Term x = args.sub(1), y = args.sub(2);
////			//TODO
////			return Null;
////		}
////	}
//}
//
///** temporal introduction
// * TODO
// * generalize Arithmetic introduction to work with abstract target matchers, and introducers capable
// * of comparing events within a task.
// *
// * --before, during, after, etc..
// * --comparing ratios of time durations, ex:
// *      ((x &&+1 y) ==> (w &&+10 z))
// *              |-
// *      ((#a ==> #b) && (equals(durRatio(#b,#a),10))
// */
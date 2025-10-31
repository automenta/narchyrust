//package nars.derive.util;
//
//import jcog.data.map.UnifriedMap;
//import nars.$;
//import nars.Op;
//import nars.Term;
//import nars.Deriver;
//import nars.term.control.PREDICATE;
//import nars.term.control.SWITCH;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Map;
//import java.util.function.Function;
//
//import static nars.Premise.Belief;
//import static nars.Premise.Task;
//
//
//public class OpSwitch extends SWITCH<Deriver> {
//
//	public final boolean taskOrBelief;
//
//	public OpSwitch(boolean taskOrBelief, Map<Op, PREDICATE<Deriver>> cases) {
//		super($.func("op", taskOrBelief ? Task : Belief,
//			$.p(cases.entrySet().stream().map(e ->
//				$.p($.quote(e.getKey().toString()), e.getValue().term())).toArray(Term[]::new))));
//		this.taskOrBelief = taskOrBelief;
//	}
//
//	@Override
//	public PREDICATE<Deriver> transformPredicate(Function<PREDICATE<Deriver>, PREDICATE<Deriver>> f) {
//		//TODO EnumMap
//		Map<Op, PREDICATE<Deriver>> e2 = new UnifriedMap<>(cases);
//		boolean[] changed = {false};
//		e2.replaceAll(((k, x) -> {
//			PREDICATE<Deriver> y = x.transformPredicate(f);
//			if (y != x)
//				changed[0] = true;
//			return y;
//		}));
//		return changed[0] ? new OpSwitch(taskOrBelief, e2) : this;
//	}
//
//	public @Nullable Op branch(Deriver m) {
//		//return swtch[(taskOrBelief ? m.taskTerm : m.beliefTerm).opID()];
//		return (taskOrBelief ? m.premise.from() : m.premise.to()).op();
//	}
//
//}
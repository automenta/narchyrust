package nars.action.link.index;

import jcog.TODO;
import jcog.pri.PriReference;
import nars.Deriver;
import nars.Premise;
import nars.Term;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * samples the tasklink bag for a relevant reversal
 * memoryless, determined entirely by tasklink bag, O(n)
 *
 * this finds the first matching result via Bag.sampleUnique
 */
public class EqualTangent extends AbstractAdjacentTerms {

	public static final EqualTangent the = new EqualTangent();

	private EqualTangent() {}

	public Iterator<PriReference<Term>> adjacencies(Term from, Term to, Deriver d) {
		throw new TODO();
//        Iterator<? extends Premise> i = d.focus.links.sampleUnique(d.rng);
//        return Util.nonNull(Iterators.transform(i,
//                test(from, to)::apply));
    }

	@Override protected Function<Premise, Term> test(Term from, Term to) {
		int fromHash = from.hashShort(), toHash = to.hashShort();
		Predicate<Term> fromEq = from.equals(), toEq = to.equals();
		//return Y -> Y.equalReverse(fromEq, fromHash, toEq, toHash);
		return x -> {
			Term y = x.equalReverse(fromEq, fromHash, toEq, toHash);
			if (y==null) return null;
			if (from.INH()) {
				if (y.equals(from.sub(0)) || y.equals(from.sub(1)))
					return null;//filter
			}
			return y;
		};
	}

}
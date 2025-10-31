package nars.deriver.reaction.compile;

import jcog.data.set.ArrayHashSet;
import nars.Deriver;
import nars.NAR;
import nars.deriver.reaction.Reaction;
import nars.deriver.reaction.ReactionModel;
import nars.term.control.NOT;
import nars.term.control.PREDICATE;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

/**
 * high-level interface for compiling premise deriver rules
 */
abstract public class ReactionCompiler {

    abstract public ReactionModel compile(ArrayHashSet<Reaction<Deriver>> reactions, NAR n);

    static ObjectFloatHashMap<PREDICATE> freq(Iterable<Reaction<Deriver>> reactions) {
        var freq = new ObjectFloatHashMap<PREDICATE>(512);
        for (var R : reactions)
            for (PREDICATE<Deriver> x : R.pre) {
                var c = x.cost();
                if (c != Float.POSITIVE_INFINITY)
                    freq.addToValue(_unneg(x), 1);
            }
        var costs = freq.keysView().summarizeFloat(PREDICATE::cost);
        var costMax =
            //costs.getMax();
            costs.getMax() + 1.0/(freq.size());
            //costs.getMax() * 2;
        freq.updateValues((x, count)->
            (float) (count * (1 - x.cost()/costMax))
            //count / (1 + x.cost())
        );
        return freq;
    }

    static PREDICATE _unneg(PREDICATE a) {
        return a instanceof NOT ? __unneg(a) : a;
    }

    static PREDICATE __unneg(PREDICATE x) {
        return ((NOT)x).cond;
    }

}

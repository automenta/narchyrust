package nars.action.link;

import nars.Deriver;
import nars.Premise;
import nars.Term;
import nars.action.link.index.AdjacentTerms;
import nars.term.control.PREDICATE;

/**
 * a more restrictive TermLinking: for use in combination with CompoundDecompose.
 *
 * requires premise to be a 'decomposed compound' result (contains recursively)
 * and additionally, on the back-end, requires the result to be NOT a 'decomposed compound'
 * to prevent cyclic activity.
 */
public class DecomposedTermLinking extends TermLinking {

    public DecomposedTermLinking(AdjacentTerms adj) {
        super(adj);
        pre(structural);
    }

    private static final PREDICATE<Deriver> structural = new PREDICATE<>("termLinkable") {

        @Override
        public boolean test(Deriver d) {
            Premise p = d.premise;
            Term x = p.from(), y = p.to();
            if (x == y) return true;
            int xv = x.complexity(), yv = y.complexity();
            if (xv > yv) return x.containsRecursively(y);
            else if (yv > xv) return y.containsRecursively(x);
            return false;
        }

        @Override
        public float cost() {
            return 0.5f;
        }
    };

}
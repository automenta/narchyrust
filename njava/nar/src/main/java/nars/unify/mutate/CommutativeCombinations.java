package nars.unify.mutate;

import jcog.TODO;
import nars.$;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.unify.Unify;

public class CommutativeCombinations extends Termutator {

    private final Subterms x, y;
    private final int choose;

    /**
     * NOTE X and Y should be pre-sorted using Terms.sort otherwise diferent permutations of the same
     * values could result in duplicate termutes HACK
     */
    /* TODO private */
    public CommutativeCombinations(Subterms X, Subterms Y, int choose) {
        super(COMMUTIVE_COMBINATIONS, $.pFast(X), $.pFast(Y));
        this.x = X;
        this.y = Y;
        this.choose = choose;
//            if (x.subs() != y.subs())//  < 2)
//                throw new WTF();
    }


    @Override
    public boolean apply(Termutator[] chain, int current, Unify u) {
        throw new TODO();
//            int start = u.size();
//            ShuffledSubterms p = new ShuffledSubterms(x, u.random);
//
//            while (p.shuffle()) {
//
//                if (Unifier.unifyLinear(p, y, u)) {
//                    if (!u.tryMutate(chain, current)) break;
//                }
//
//                if (!u.revertLive(start))
//                    break;
//            }
    }

    static final Atom COMMUTIVE_COMBINATIONS = $.the(CommutativeCombinations.class);

}

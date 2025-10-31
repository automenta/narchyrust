package nars.unify.mutate;

import nars.$;
import nars.NAL;
import nars.Term;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.unify.AbstractUnifier;
import nars.unify.Unifier;
import nars.unify.Unify;

/**
 * Created by me on 12/22/15.
 */
public final class CommutivePermutations extends Termutator implements AbstractUnifier {

    private final Subterms x,y;

    public CommutivePermutations(Subterms X, Subterms Y) {
        super(COMMUTIVE_PERMUTATIONS, X, Y);
        assert(X.subs()==Y.subs());
        this.x = X;
        this.y = Y;
    }


    @Override
    public boolean apply(Termutator[] chain, int current, Unify u) {
//        @Nullable TermList xx = u.resolveListIfChanged(x); Subterms x = xx != null ? xx : this.x;
//        @Nullable TermList yy = u.resolveListIfChanged(y); Subterms y = yy != null ? yy : this.y;

        int start = u.size();
        ShuffledSubterms xs = new ShuffledSubterms(x, u.random);

        boolean kont;
        do {
            if (!xs.shuffle())
                break; //exhausted all permutations

            kont = u.use(NAL.derive.TTL_COST_TRY); //pre-pay

            if (Unifier.unifyLinear(xs, y, u)) {
                if (!u.match(chain, current))
                    return false; //cut
            }

            u.revert(start);

        } while (kont);

        return true;
    }

    private static final Atom COMMUTIVE_PERMUTATIONS = $.the(CommutivePermutations.class);

    @Override
    public final boolean apply(Term x, Term y, Unify u) {
        u.termute(this);
        return true;
    }

//    @Override
//    public Termutator commit(Unify u) {
//
//        if (xx != null || yy != null) {
//            xx = xx == null ? (x instanceof TermList ? (TermList) x : x.toList()) : xx;
//            xx.commuteThis();
//            yy = yy == null ? (y instanceof TermList ? (TermList) y : y.toList()) : yy;
//            yy.commuteThis();
//            switch (possiblyUnifiableWhileEliminatingEqualAndConstants(xx, yy)) {
//                case -1:
//                    return null;
//                case +1:
//                    return ELIDE;
//                default: {
//                    int xs = xx.subs();
//                    int ys = yy.subs();
//                    if (xs == ys) {
//                        if (xs == 1 && ys == 1)
//                            return xx.sub(0).unify(yy.sub(0), u) ? ELIDE : null;
//                        else {
//                            if (null == Unifier.unifyPossibleSubterms(Op.SETe.id, xx, yy, u.vars, u.dur))
//                                return null;
//                            return new CommutivePermutations(xx, yy);
//                        }
//                    } else
//                        return null;
//                }
//            }
//
//        } else
//            return this;
//
//    }


    //    @Override
//    public Termutator preprocess(Unify u) {
//		TermList x = u.resolveListIfChanged(this.x, true);
//		TermList y = u.resolveListIfChanged(this.y, true);
//        if (x == null && y == null)
//            return this;
//
//        if (x == null) x = this.x.toList();
//        if (y == null) y = this.y.toList();
//        boolean preUnified = false;
//        switch (CompoundUnifier.possiblyUnifiableWhileEliminatingEqualAndConstants(x, y, u)) {
//            case -1:
//                return null; //impossible
//            case 1:
//                preUnified = true;
//                break;
//            case 0: {
//
//                break;
//
//            }
//        }
//
//        return preUnified ? Termutator.ELIDE : new CommutivePermutations(x, y);
//    }

}
package nars.unify.constraint;

import nars.Term;
import nars.term.atom.Int;
import nars.term.util.conj.Cond;
import nars.term.var.Variable;
import nars.unify.Unify;

public final class CommonSubEventConstraint extends RelationConstraint {

    private final int polarity;

    public CommonSubEventConstraint(Variable x, Variable y, int polarity) {
        super(CommonSubEventConstraint.class, x, y, Int.i(polarity));
        this.polarity = polarity;
    }

    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new CommonSubEventConstraint(newX, newY, polarity);
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return !Cond.commonSubCond(x, y, false, polarity);
    }


    @Override
    public float cost() {
        return 0.75f;
    }


//    public static final class EventCoNeg extends RelationConstraint {
//
//        public EventCoNeg(Variable x, Variable y) {
//            super(EventCoNeg.class.getSimpleName(), x, y);
//        }
//
//        @Override
//        protected RelationConstraint newMirror(Variable newX, Variable newY) {
//            return new EventCoNeg(newX, newY);
//        }
//
//        @Override
//        public boolean invalid(Term x, Term y, Unify context) {
//            if (!validConjXY(x, y))
//                return true;
//
//            if (x.volume() > y.volume()) {
//                Term z = y;
//                y = x;
//                x = z;
//            }
//
//            Compound Y = (Compound) y;
//            return !((Compound) x).eventsOR((when, xx) -> Y.eventOf(xx, -1),
//                    ETERNAL, true, false);
//        }
//
//
//
//        @Override
//        public float cost() {
//            return 0.75f;
//        }
//    }
//
//    private static boolean validConjXY(Term x, Term y) {
//        return !(!x.CONJ() || !y.CONJ() || !Op.hasCommon(x.subterms(), y.subterms()));
//    }
}
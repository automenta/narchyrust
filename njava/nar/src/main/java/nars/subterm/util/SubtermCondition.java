package nars.subterm.util;

import nars.Term;
import nars.term.Compound;

import java.util.function.BiPredicate;

/**
 * tests various potential relations between a containing target and a subterm
 */
public enum SubtermCondition implements BiPredicate<Compound, Term> {


    Subterm() {
        @Override
        public boolean test(Compound container, Term x) {
            return container.contains(x);
        }

        @Override
        public boolean testN(Compound container, Term x) {
            return container.containsNeg(x);
        }

        @Override
        public boolean testPN(Compound container, Term x) {
            return container.containsPN(x);
        }
        //TODO testPN

        public float cost() {
            return 0.4f;
        }
    },

    SubtermNeg() {
        @Override
        public boolean test(Compound container, Term x) {
            return Subterm.testN(container, x);
        }

        @Override
        public boolean testN(Compound container, Term x) {
            return Subterm.test(container, x);
        }

        @Override
        public boolean testPN(Compound container, Term x) {
            return Subterm.testPN(container, x);
        }

        public float cost() {
            return 0.41f;
        }
    },

    Recursive() {
        @Override
        public boolean test(Compound container, Term x) {
            return container.containsRecursively(x);
        }

        public float cost() {
            return 0.6f;
        }
    },


    /** eventOf, or inh/eventOf */
    Cond() {
        @Override
        public boolean test(Compound container, Term x) {
            return container.condOf(x, +1);
        }

        @Override
        public boolean testN(Compound container, Term x) {
            return container.condOf(x, -1);
        }

        @Override
        public boolean testPN(Compound container, Term x) {
            return container.condOf(x, 0);
        }

        public float cost() {
            return 0.8f;
        }
    },

//    /** includes --&& (disj) cases */
//    RCond() {
//        @Override
//        public boolean test(Compound c, Term x) {
//            Term u = c.unneg();return u instanceof Compound && Cond.test(((Compound)u), x);
//        }
//
//        @Override
//        public boolean testN(Compound c, Term x) {
//            Term u = c.unneg();return u instanceof Compound && Cond.testN(((Compound)u), x);
//        }
//
//        @Override
//        public boolean testPN(Compound c, Term x) {
//            Term u = c.unneg();return u instanceof Compound && Cond.testPN(((Compound)u), x);
//        }
//
//        public float cost() {
//            return 0.8f;
//        }
//    },

    CondFirst() {
        @Override
        public final boolean test(Compound container, Term event) {
            return container.condStartEnd(event, true, true);
        }

        public float cost() {
            return 0.85f;
        }
    },

    CondStart() {
        @Override
        public final boolean test(Compound container, Term event) {
            return container.condStartEnd(event, true, false);
        }

        public float cost() {
            return 0.85f;
        }
    },

    CondEnd() {
        @Override
        public final boolean test(Compound container, Term event) {
            return container.condStartEnd(event, false, false);
        }

        public float cost() {
            return 0.85f;
        }
    },

    CondLast() {
        @Override
        public final boolean test(Compound container, Term event) {
            return container.condStartEnd(event, false, true);
        }
        public float cost() {
            return 0.85f;
        }
    }

//    /**
//     * conj containment of another event, or at least one event of another conj
//     */
//    EventsAny() {
//        @Override
//        public boolean testContainer(Compound container) {
//            return container.op()==CONJ;
//        }
//        @Override
//        public boolean test(Compound container, Term x) {
//            if (container.op() != CONJ)
//                return false;
//
//            if (Conj.containsEvent(container, x))
//                return true;
//
//            if (x.op()==CONJ) {
//                return !x.eventsWhile((when,xx) ->
//                    xx==x || !Conj.containsOrEqualsEvent(container, xx)
//                , 0, true, true, true, 0);
//            }
//
//            return false;
////            if (container.op() != CONJ || container.volume() <= xx.volume() || !Term.commonStructure(container, xx))
////                return false;
////
////            boolean simpleEvent = xx.op() != CONJ;
////            if (simpleEvent) {
////                if (Tense.dtSpecial(container.dt())) { //simple case
////                    return container.contains(xx);
////                } else {
////                    return !container.eventsWhile((when, what) -> !what.equals(xx),
////                            0, true, true, true, 0);
////                }
////            } else {
////                Set<Term> xxe = xx.eventSet();
////                container.eventsWhile((when, what) ->
////                                !xxe.remove(what) || !xxe.isEmpty(),
////                        0, true, true, true, 0);
////                return xxe.isEmpty();
////            }
//        }
//
//        public float cost() {
//            return 2f;
//        }
//    }
    ;

    public abstract float cost();

    boolean testPN(Compound container, Term content) {
        return test(container, content) || test(container, content.neg());
    }

    boolean testN(Compound container, Term content) {
        return test(container, content.neg());
    }

    public final boolean test(Compound container, Term content, int polarityCompare) {
        return (switch (polarityCompare) {
            case +1 ->   test(container, content);
            case -1 ->   testN(container, content);
            case  0 ->   testPN(container, content);
            default -> throw new UnsupportedOperationException();
        });
    }
}
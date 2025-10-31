//package nars.game.action;
//
//import nars.Term;
//import nars.game.Game;
//import nars.term.Compound;
//import nars.term.Termed;
//
///** replacement for old Operator API */
//public class PatternAction implements AbstractAction {
//
//    final Compound pattern;
//
//    public PatternAction(Compound pattern) {
//        assert(pattern.varPattern() > 0);
//        this.pattern = pattern;
//    }
//
//    @Override
//    public double dexterity() {
//        return 0;
//    }
//
//    @Override
//    public Iterable<? extends Termed> components() {
//        return null;
//    }
//
//    @Override
//    public void accept(Game g) {
//
//    }
//
//    @Override
//    public Term term() {
//        return pattern;
//    }
//
//}
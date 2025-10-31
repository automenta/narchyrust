//package nars.game.action;
//
//import jcog.TODO;
//import nars.Term;
//import nars.game.Game;
//import nars.Truth;
//import org.jetbrains.annotations.Nullable;
//
///** TODO freq-domain spectrum set of actions */
//public class SpectAction extends CompoundAction<SpectAction.SpectComponentAction> {
//
//
//    public SpectAction(SpectComponentAction[] concepts) {
//        super(concepts);
//    }
//
//    @Override
//    public void accept(Game game) {
//
//    }
//
//    @Override
//    public Term term() {
//        throw new TODO();
//    }
//
//    public class SpectComponentAction extends AbstractGoalAction {
//
//        public SpectComponentAction(Term term) {
//            super(term);
//        }
//
//        @Override
//        protected @Nullable Truth update(@Nullable Truth beliefTruth, @Nullable Truth goalTruth, Game g) {
//            return null;
//        }
//    }
//}
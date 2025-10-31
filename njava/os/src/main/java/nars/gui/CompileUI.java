//package nars.gui;
//
//import jcog.exe.Exe;
//import nars.game.Game;
//import nars.game.reward.ScalarReward;
//import spacegraph.space2d.Surface;
//import spacegraph.space2d.container.grid.Gridding;
//import spacegraph.space2d.widget.button.PushButton;
//
//public enum CompileUI { ;
//
//    public static Surface compileUI(Game g) {
//        return new Gridding(g.rewards.rewards.stream()
//            .filter(z -> z instanceof ScalarReward).map(R ->
//                scalarRewardCompileButton((ScalarReward) R)
//            ));
//    }
//
//    private static PushButton scalarRewardCompileButton(ScalarReward r) {
//        return new PushButton(r.id.toString(),
//            () -> Exe.runUnique(r, r::plan)/*g.nar.runLater(r::plan)*/);
//    }
//}
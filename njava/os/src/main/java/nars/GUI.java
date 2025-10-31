package nars;

import nars.gui.NARui;
import spacegraph.SpaceGraph;

/**
 * main UI entry point
 */
public class GUI {


    public static void main(String[] args) {

        NAR n = null; //NARchy.ui();

        gui(n);

        n.startFPS(10f);

//        demo(n);

//        SpaceGraph.window( FocusUI.focusUI(n.focus()), 1200, 800);
        //window(new TaskListView(n.what(), 32), 1200, 800);
        //window(new ConceptListView(n.what(), 32), 1200, 800);

//        SpaceGraph.window(new ReplChip((cmd, receive) -> {
//            try {
//                n.input(cmd);
//            } catch (Narsese.NarseseException e) {
//                receive.accept(e.toString());
//            }
//        }), 800, 200);
    }

    public static void gui(NAR n) {
        SpaceGraph.window(NARui.top(n), 1000, 800);//.window.eventClosed.on(n::reset);
    }


    //    static void wall(NAR nar) {
//        GraphEdit w = SpaceGraph.wall(800, 600);
//        w.frame(new ServicesTable(nar.services), 5, 4);
//        w.frame(new OmniBox(new LuceneQueryModel()), 6, 1);
//        w.frame(NARui.top(nar), 4, 4);
//    }

}
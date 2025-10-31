package nars.gui;

import nars.NAR;
import nars.Premise;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.text.BitmapLabel;

public class PremiseSurface extends Bordering {

    public PremiseSurface(Premise p, NAR n) {
        super();

        north(new BitmapLabel(p.toString()));

//        final TermGraph2D<Premise> g = new TermGraph2D<>(n) {
//            @Override
//            protected void _set(Iterable<Premise> links, int n) {
//                super._set(Streams.stream(links).flatMap(Premise::premiseStream)::iterator, n);
//            }
//        };
//        set(g);
//        g.setAll(List.of(p));
    }
}
package nars.gui.concept;

import nars.NAR;
import nars.term.Termed;
import spacegraph.space2d.container.unit.MutableUnitContainer;

public abstract class ConceptView extends MutableUnitContainer {

    public final Termed term;

    //TODO
//    private transient Concept _concept = null;

    protected ConceptView(Termed t) {
        super();

        this.term = t;
    }

    protected abstract void update(NAR nar);

//    protected @Nullable Concept concept(NAR nar) {
//        if (_concept == null || _concept.isDeleted()) {
//            _concept = nar.conceptualizeDynamic(term);
//        }
//        return _concept;
//    }


}

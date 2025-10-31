package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.Term;
import nars.Truth;
import nars.TruthFunctions;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.truth.DynTaskify;

import static nars.Op.DIFF;

public class DynDiff extends DynTruth {

    public static final DynDiff the = new DynDiff();

    private DynDiff() {
        super();
    }

    public boolean ditherComponentOcc() { return false; }

    @Override
    public Truth truth(DynTaskify d) {
        return pair(d, DynDiff::_diffCommutative, d.get(0), d.get(1));
    }

    public static float _diffCommutative(float a, float b) {
        return (float) TruthFunctions.diffCommutative(a, b);
    }

    @Override
    public boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
        Subterms ab = superterm.subtermsDirect();
        return each.accept(ab.sub(0), start, end) && each.accept(ab.sub(1), start, end);
    }

    @Override
    public Term recompose(Compound superterm, DynTaskify d) {
        return DIFF.the(d.term(0), d.term(1));
    }

    @Override
    public int componentsEstimate() {
        return 2;
    }

}
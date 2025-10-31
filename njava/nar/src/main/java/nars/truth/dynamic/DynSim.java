package nars.truth.dynamic;

import jcog.Fuzzy;
import jcog.util.ObjectLongLongPredicate;
import nars.Term;
import nars.Truth;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.truth.DynTaskify;

import static nars.Op.INH;
import static nars.Op.SIM;

public class DynSim extends DynTruth {

    public static final DynSim the = new DynSim();

    private DynSim() {
        super();
    }

    public boolean ditherComponentOcc() { return false; }

    @Override
    public Truth truth(DynTaskify d) {
        return pair(d, Fuzzy::and, d.get(0), d.get(1));
    }

    @Override
    public boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
        Subterms ab = superterm.subtermsDirect();
        Term a = ab.sub(0), b = ab.sub(1);
        return each.accept(INH.the(a,b), start, end) && each.accept(INH.the(b, a), start, end);
    }

    @Override
    public Term recompose(Compound superterm, DynTaskify d) {
        return SIM.the(d.term(0), d.term(1));
    }

    @Override
    public int componentsEstimate() {
        return 2;
    }
}

package nars.term.builder;

import nars.$;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.compound.LightCompound;
import nars.term.compound.LightDTCompound;
import nars.term.compound.LightUnitCompound;

import static nars.Op.DTERNAL;

public class LightHeapTermBuilder extends HeapTermBuilder {

    public static final TermBuilder the = new LightHeapTermBuilder();

    private LightHeapTermBuilder() { }

    @Override
    public Term compound1New(Op o, Term x) {
        return new LightUnitCompound(o, x);
        //return new CachedUnitCompound(o, x);
        //return compoundNNew(o, DTERNAL, new Term[] { x}, null);
    }

    @Override
    public Term compoundNew(Op o, int dt, Subterms subs) {
        //HACK TODO LightDTCompound again
        return dt == DTERNAL ?
            new LightCompound(o, subs) :
            new LightDTCompound(o.id,subs, dt);
    }


    @Override
    public Subterms subtermsNew(Term... t) {
        return $.vFast(t);
    }

}
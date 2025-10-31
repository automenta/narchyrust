package nars.term.anon;

import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.util.map.ByteAnonMap;

public class AnonCompound extends Compound {
    public final ByteAnonMap index;
    public final Compound base;

    public static Compound anon(Compound x) {
        Anon a = new Anon();
        a.keepIntrin = false;
        Term base = a.put(x);
        return base == x ? x : new AnonCompound((Compound) base, a.map);
    }

    public AnonCompound(Compound base, ByteAnonMap index) {
        super(base.opID());
        this.index = index;
        this.base = base;
    }

    @Override
    public Term sub(int i) {
        return new Anon(index).get(base.sub(i));
    }

    @Override
    public int complexity() {
        return base.complexity();
    }

    @Override
    public Subterms subterms() {
        return base.subterms().transformSubs(new Anon(index)::get);
    }

    @Override
    public int dt() {
        return base.dt();
    }

    @Override
    public int subs() {
        return base.subs();
    }
}

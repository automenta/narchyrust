package nars.unify.unification;

import nars.Term;
import nars.term.var.Variable;
import nars.unify.Unify;

public class OneTermUnification extends DeterministicUnification {

    public final Term tx, ty;

    public OneTermUnification(Term tx, Term ty) {
        super();
        this.tx = tx;
        this.ty = ty;
    }

    @Override
    protected boolean equals(DeterministicUnification obj) {
        if (obj instanceof OneTermUnification u) {
            return tx.equals(u.tx) && ty.equals(u.ty);
        }
        return false;
    }

    @Override
    public Term xy(Variable t) {
        return tx.equals(t) ? ty : null;
    }

    @Override
    public boolean apply(Unify u) {
        return u.put((Variable/*HACK*/) tx, ty);
    }
}
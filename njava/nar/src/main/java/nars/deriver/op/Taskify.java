package nars.deriver.op;

import jcog.Util;
import nars.Deriver;
import nars.Term;
import nars.deriver.util.DerivationFailure;
import nars.term.atom.Bool;
import nars.term.util.transform.VariableTransform;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;


public final class Taskify implements Function<Deriver, Term> {
    private final Term pattern;
    public final Function<Deriver, Term> conc;

    public Taskify(Term pattern, @Nullable Function<Deriver, Term> conc) {
        this.pattern = pattern;
        this.conc = conc!=null ? conc : this;
    }

    public final @Nullable Term termify(Deriver d) {
        var y = conc.apply(d);

        if (y instanceof Bool) {
            DerivationFailure.Null.record(d);
            return null;
        }
        if (d.invalidVol(y.unneg())) {
            DerivationFailure.VolMax.record(d);
            return null;
        }

        return y;
    }

    public static Term questionSalvage(Term x) {
//        return x;
        //convert orphaned indep vars to query/dep variables
        return //ValidIndepBalance.valid(x, true) ?
                VariableTransform.indepToQueryVar.apply(x);// : x;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Taskify) obj;
        return Objects.equals(this.pattern, that.pattern) &&
                Objects.equals(this.conc, that.conc);
    }

    @Override
    public int hashCode() {
        return Util.hashCombine(pattern, conc);
    }

    @Override
    public String toString() {
        return "Taskify[" +
                "pattern=" + pattern + ", " +
                "conc=" + conc + ']';
    }

    /** the default conclusion transformation implementation, don't call directly. */
    @Override public final Term apply(Deriver d) {
        return d.unify.transformDerived.apply(pattern);
    }


}
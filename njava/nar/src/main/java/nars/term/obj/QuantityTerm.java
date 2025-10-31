package nars.term.obj;

import nars.$;
import nars.Term;
import nars.term.Compound;
import nars.term.ProxyCompound;
import org.jetbrains.annotations.Nullable;
import tec.uom.se.AbstractQuantity;

import javax.measure.Quantity;

public class QuantityTerm extends ProxyCompound {

    public final Quantity<?> quant;

    public QuantityTerm(Quantity<?> q) {
        super( (Compound)$.p( $.atomic(q.getUnit().toString()), $.the( q.getValue() ) ) );
        this.quant = q;
    }

    public static @Nullable QuantityTerm the(String toParse) throws IllegalArgumentException {
        Quantity<?> q = AbstractQuantity.parse(toParse);
        return q == null ? null : new QuantityTerm(q);
    }

    /** interpret a product of the form (unit, time) */
    public static @Nullable QuantityTerm the(Term qTerm) throws IllegalArgumentException {
        if (qTerm.PROD() && qTerm.subs()==2) {
            Term unit = qTerm.sub(0);
            double value = $.doubleValue(qTerm.sub(1));
            return the(value + " " + unit);
        }
        return null;
    }

}
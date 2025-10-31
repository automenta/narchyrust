package nars.premise;

import com.google.common.base.Objects;
import jcog.Util;
import nars.$;
import nars.NALTask;
import nars.Premise;
import nars.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

public abstract class SubPremise<T extends Termed> extends AbstractPremise {

    public final T id;

    protected SubPremise(T id, @Nullable Premise superPremise) {
        super(superPremise!=null ? Util.hashCombine(superPremise, id) : id.hashCode());
        this.id = id;
        this.setParent(superPremise);
    }

    @Override
    public final boolean equals(Object _x) {
        if (this == _x) return true;

        SubPremise x;
        return getClass() == _x.getClass() &&
               hash==(x=(SubPremise)_x).hash &&
               Objects.equal(parent, x.parent) &&
               id.equals(x.id);
    }

    @Override
    @Deprecated public final Term term() {
        return $.pFast(id.term(), parent.term());
    }

    @Override
    public final String toString() {
        return id + "(" + parent + ")";
    }

    @Override @Deprecated public final NALTask task() { return parent.task(); }

    @Override @Deprecated public final NALTask belief() { return parent.belief(); }

    @Override @Deprecated public final Term from() {
        return parent.from();
    }

    @Override @Deprecated public final Term to() {
        return parent.to();
    }
}
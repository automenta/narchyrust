package nars.term.util.transform;

import nars.Term;
import nars.term.Compound;
import nars.term.builder.TermBuilder;
import org.jetbrains.annotations.Nullable;

public abstract sealed class MapSubstWithStructFilter extends Subst permits Replace.MapSubst2, Replace.MapSubstN {

    private final int structure;

    MapSubstWithStructFilter(int structure, TermBuilder B) {
        super(B);
        this.structure = structure;
        assert(structure!=0);
    }

    @Override
    public @Nullable Term applyCompound(Compound x) {
        return x.hasAny(structure) ? super.applyCompound(x) : x;
    }

}
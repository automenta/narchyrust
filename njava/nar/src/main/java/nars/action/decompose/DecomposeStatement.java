package nars.action.decompose;

import nars.Deriver;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;

public class DecomposeStatement extends DecomposeTerm {

    public DecomposeStatement(Op... ops) {
       isAny(PremiseTask, ops);
    }

    @Override public @Nullable Term decompose(Compound root, Deriver d) {
        //assert(root.STATEMENT());
        return Decompose1.decomposer.subterm(root, d.rng);
    }

    @Override protected boolean structural() {
        return true;
    }

}
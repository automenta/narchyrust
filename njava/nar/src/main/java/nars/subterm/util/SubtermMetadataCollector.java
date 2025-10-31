package nars.subterm.util;

import jcog.Util;
import nars.Term;
import nars.term.atom.Atomic;
import nars.term.var.Variable;

import static nars.Op.*;

public final class SubtermMetadataCollector {
    public int structure;
    public int hash = 1;
    public int complexity = 1;
    public int varPattern;
    public int varQuery;
    public int varDep;
    public int varIndep;

    public SubtermMetadataCollector() {

    }

    public SubtermMetadataCollector(Term[] terms) {
        for (Term x : terms)
            add(x);
    }

    private void addAtomic(int op, int termHash) {
        this.complexity++;
        this.structure |= (1<<op);
        this.hash = Util.hashCombine(this.hash, termHash);
    }

    static {
        final int vp = VAR_PATTERN.id;
        assert(vp == 9 && VAR_QUERY.id == vp+1 && VAR_INDEP.id == vp+2 && VAR_DEP.id == vp+3);
    }

    private void addVar(int type) {
		switch (type) {
			case 9  -> varPattern++;
			case 10 -> varQuery++;
			case 11 -> varIndep++;
			case 12 -> varDep++;
			default -> throw new UnsupportedOperationException();
		}
    }

    public void add(Term x) {
        int hash = x.hashCode();
        if (x instanceof Atomic)
            addAtomic(x, hash);
        else
            addCompound(x, hash);
    }

    private void addCompound(Term x, int termHash) {
        this.hash = Util.hashCombine(this.hash, termHash);

        this.complexity += x.complexity();

        int xs = x.struct();
        this.structure |= xs;

        if ((xs & VAR_PATTERN.bit) != 0)
            this.varPattern += x.varPattern();
        if ((xs & VAR_DEP.bit) != 0)
            this.varDep += x.varDep();
        if ((xs & VAR_INDEP.bit) != 0)
            this.varIndep += x.varIndep();
        if ((xs & VAR_QUERY.bit) != 0)
            this.varQuery += x.varQuery();
    }

    private void addAtomic(Term x, int termHash) {
        int xo = x.opID();

        addAtomic(xo, termHash);

        if (x instanceof Variable)
            addVar(xo);
    }

}
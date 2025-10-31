package nars.subterm.util;


import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Termlike;
import nars.term.anon.Intrin;
import nars.term.var.NormalizedVariable;
import nars.term.var.Variable;

import java.util.function.Predicate;

/**
 * cached values for target/subterm metadata
 */
public abstract class TermMetadata implements Termlike {

    /**
     * bitvector of subterm types, indexed by Op.id and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    protected final int structure;
    /**
     * normal high-entropy "content" hash of the terms
     */
    public final int hash;
    /**
     * stored as volume+1 as if this termvector were already wrapped in its compound
     */
    private final int complexity;
    /**
     * stored as complexity+1 as if this termvector were already wrapped in its compound
     */
    private final int complexityConst;
    private final int varPattern;
    private final int varDep;
    private final int varQuery;
    private final int varIndep;

    protected TermMetadata(Term... terms) {
        this(new SubtermMetadataCollector(terms));
    }

    protected TermMetadata(SubtermMetadataCollector s) {
        int varTot =
                (this.varPattern =  s.varPattern) +
                (this.varQuery =  s.varQuery) +
                (this.varDep =  s.varDep) +
                (this.varIndep =  s.varIndep);

        this.complexityConst = ((this.complexity = s.complexity) - varTot);

        this.hash = s.hash;
        this.structure = s.structure;

        if (varTot == 0 && this instanceof Subterms ss)
            ss.setNormalized();
    }


    /**
     * for AnonVector
     */
    protected static boolean normalized(short[] subterms) {
        /* checks for monotonically increasing variable numbers starting from 1,
         which will indicate that the subterms is normalized
         */

        int minID = 0;
        int typeToMatch = -1;
        for (short x: subterms) {
            boolean neg = x < 0;
            if (neg)
                x = (short) -x;

            int varID = Intrin.varID(x);
            if (varID == -1) {
                /*..*/
            } else if (varID == minID) {
                //same order, ok
                int type = Intrin.group(x);
                if (typeToMatch == -1)
                    typeToMatch = type;
                else if (typeToMatch!=type)
                    return false; //same id different type, needs normalized
            } else if (varID == minID + 1) {
                //increase the order, ok, set new type
                typeToMatch = Intrin.group(x);
                minID++;
            } else if (varID > minID + 1) {
                return false; //cant be sure
            }
        }
        return true;
    }

    @Override public final int vars() {
        return complexity - complexityConst; //varDep + varIndep + varQuery + varPattern;
    }

    @Override public final boolean hasVars() {
        return vars() > 0;
    }

    @Override public final int varQuery() {
        return varQuery;
    }

    @Override public final int varDep() {
        return varDep;
    }

    @Override public final int varIndep() {
        return varIndep;
    }

    @Override public final int varPattern() {
        return varPattern;
    }

    @Override
    public final boolean hasVarQuery() {
        return varQuery > 0;
    }

    @Override
    public final boolean hasVarIndep() {
        return varIndep > 0;
    }

    @Override
    public final boolean hasVarDep() {
        return varDep > 0;
    }

    @Override
    public final boolean hasVarPattern() {
        return varPattern > 0;
    }

    @Override public final int struct() {
        return structure;
    }

    @Override
    public final int structSubs() {
        return structure;
    }

    @Override public final int complexity() {
        return complexity;
    }

    @Override public final int complexityConstants() {
        return complexityConst;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public abstract boolean equals(Object obj);

    public static final class VarPreNormalization implements Predicate<Term> {

        private int n;
        private final byte[] types;
        //final ByteArrayList types = new ByteArrayList(2);

        public VarPreNormalization(int vars) {
            this.types = new byte[vars];
        }

        @Override
        public boolean test(Term v) {
            if (v instanceof NormalizedVariable nv) {
                byte varID = nv.id();
//                int nTypes = types.size();
                if (varID <= n) {
                    return types[varID - 1] == nv.opID();
                } else if (varID == 1 + n) {
//                    if (types.length <= nTypes + 1) types = new byte[nTypes * 2]; //grow
                    types[n++] = nv.opID();
                    return true;
                } else
                    return false;
            }

            return !(v instanceof Variable);
        }

        public static boolean descend(Term t) {
            return t instanceof Variable || (t instanceof Compound && t.hasVars());
        }
    }
}
package nars.term.builder;

import jcog.Util;
import jcog.data.bit.IntBitSet;
import jcog.data.bit.MetalBitSet;
import jcog.memoize.Memoizers;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TmpTermList;
import nars.term.Compound;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import static nars.Op.CONJ;
import static nars.Op.NEG;

/** memoizes certain target operations in addition to interning */
public class MemoizingTermBuilder extends InterningTermBuilder {

    protected final Function<Intermed.InternedTermArray, Subterms> subs;

    protected final Function<Intermed.InternedCompoundByComponents, Term>[] terms = new Function[Op.count];

    private final Function<MemoizedUnaryTermFunction, Term> memo;

    /** used for quickly determining if op type is internable */
    protected final int termsInterned;

    private final UnaryOperator<Term> ROOT, NORMALIZE;

    static final class MemoizedUnaryTermFunction  {

        private final Term term;
        private final int hash;
        @Deprecated private final UnaryOperator<Term> f;

        private MemoizedUnaryTermFunction(UnaryOperator<Term> f, Term x) {
            super();
            this.f = f;
            this.term = x;
            this.hash = Util.hashCombine(term, f);
        }
        @Override public int hashCode() { return hash; }

        @Override
        public boolean equals(Object o) {
//            if (this == o) return true;
            //if (!(o instanceof MemoizedUnaryTermFunction)) return false;
            MemoizedUnaryTermFunction that = (MemoizedUnaryTermFunction) o;
            return /*hash == that.hash && */f==that.f && term.equals(that.term);
        }
    }

    public MemoizingTermBuilder(TermBuilder builder) {
        this(atomCapacityDefault, compoundCapacityDefault, NAL.term.interningComplexityMax, builder);
    }

    private MemoizingTermBuilder(int atomCap, int compoundCapacity, int volInternedMax, TermBuilder builder) {
        super(builder, atomCap, compoundCapacity);

        subs = memoizer("subterms", z -> _subtermsNew(z.subs), compoundCapacity * 2);

//        intrinSubs = memoizer("intrinSubterms", x -> new IntrinSubterms(x.subs), compoundCapacity);

        Function<Intermed.InternedCompoundByComponents, Term> statements = memoizer("statement", x ->
                termBuilder.statementNew(Op.op(x.op), x.dt, x.sub(0), x.sub(1)), compoundCapacity * 3);

        Function<Intermed.InternedCompoundByComponents, Term> compounds = x ->
                super.compoundNew(Op.op(x.op), x.dt, x.terms());

        for (int i = 0; i < Op.count; i++) {
            Op o = Op.op(i);
            if (o.atomic || (/*!internNegs && */o == NEG)) continue;

            //TODO use multiple PROD slices to decrease contention

            Function<Intermed.InternedCompoundByComponents, Term> c;
            if (o == CONJ)
                c = memoizer("conj", x -> _conjNew(x.dt, new TmpTermList(x.terms())), compoundCapacity);
            else if (o.statement)
                c = statements;
            else
                c = memoizer(o.str, compounds, compoundCapacity);

            terms[i] = c;
        }

        IntBitSet termsInterned = (IntBitSet) MetalBitSet.bits(terms.length);
        for (int i = 0, termsLength = terms.length; i < termsLength; i++)
            if (terms[i] != null) termsInterned.set(i);
        this.termsInterned = termsInterned.x;

        this.ROOT = x -> super.root((Compound) x);
        this.NORMALIZE = x -> super.normalize((Compound) x, (byte)0);

        memo = Memoizers.the.memoize("memo", j -> j.f.apply(j.term), compoundCapacity);
    }

    @Override
    public Term compoundNew(Op o, int dt, Subterms t) {
        boolean internable = internable(o) && internableTerms(t);
        return internable ?
                terms[o.id].apply(new Intermed.InternedCompoundByComponentsArray(o, dt, Subterms.array(t) /*o.sortedIfNecessary(dt, t)*/)) :
                termBuilder.compoundNew(o, dt, _subterms(t));
    }

    protected boolean internable(Op op) {
        return internable(op.id);
    }

    protected boolean internable(int opID) {
        return (termsInterned & (1 << opID)) != 0;
    }

    @Override
    public Term compound1New(Op o, Term x) {
        return termBuilder.compound1New(o, x);
    }

    @Override protected Term conjIntern(int dt, Term[] u) {
        return terms[CONJ.id].apply(new Intermed.InternedCompoundByComponentsArray(CONJ, dt, u));
    }

    @Override protected Term statementInterned(Op o, int dt, Term S, Term P) {
        return terms[o.id].apply(new Intermed.InternedCompoundByComponentsArray(o, dt, S, P));
    }


    @Override
    protected Subterms subtermsInterned(Term[] t) {
        return subs.apply(new Intermed.InternedTermArray(t));
    }

    @Override
    public Compound normalize(Compound x, byte varOffset) {
		return varOffset == 0 && internable(x) ?
            (Compound) memo.apply(new MemoizedUnaryTermFunction(NORMALIZE, x)) :
            super.normalize(x, varOffset);
    }

    @Override
    public Term root(Compound x) {
		return internable(x) ?
            memo.apply(new MemoizedUnaryTermFunction(ROOT, x)) :
            super.root(x);
    }

    private boolean internable(Compound x) {
        return x.complexity() < compoundVolMax && x.internable();
    }


}
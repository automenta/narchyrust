package nars.term.builder;

import jcog.The;
import jcog.memoize.ByteKeyExternal;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoizers;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TmpTermList;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;

import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;

public abstract class InterningTermBuilder extends TermBuilder {

    private final Function<String, Atomic> atomBuilder;
    private final Function<Term[], Subterms> subtermBuilder;
    final TermBuilder termBuilder;
    final int compoundVolMax;



    private static final int atomLengthMax = 16;
    static final int compoundCapacityDefault = Memoizers.DEFAULT_MEMOIZE_CAPACITY;
    static final int atomCapacityDefault = compoundCapacityDefault/2;
    public static final boolean internTemporal = false;

    protected InterningTermBuilder(TermBuilder raw, int atomCapacity, int compoundVolMax) {
        this(raw,
            new HijackMemoize<>(raw::atomNew, atomCapacity, 3),
            null, compoundVolMax);
    }

    private InterningTermBuilder(TermBuilder raw, Function<String, Atomic> atomBuilder, Function<Term[], Subterms> subtermBuilder, int compoundVolMax) {
        this.termBuilder = raw;
        this.compoundVolMax = compoundVolMax;

        if (subtermBuilder==null)
            subtermBuilder = termBuilder::subterms;

        this.subtermBuilder = subtermBuilder;
        this.atomBuilder = atomBuilder;
    }

    @Override
    public final Subterms subtermsNew(Term[] t) {
        return internableTerms(t) ? subtermsInterned(t) : _subtermsNew(t);
    }

    protected abstract Subterms subtermsInterned(Term[] t);

    final Subterms _subtermsNew(Term[] t) {
        return subtermBuilder.apply(t);
    }

    @Override
    public final Atomic atomNew(String id) {
        int l = id.length();
        return (l > 1 && l < atomLengthMax) ?
            atomBuilder.apply(id) : termBuilder.atomNew(id);
    }

    @Override
    public final Term conjN(int dt, TmpTermList xx) {
        return !dtSpecial(dt) && internableTerms(xx) ?
            conjIntern(dt, Subterms.array(xx)) :
            _conjNew(dt, xx);
    }

    protected final Term _conjNew(int dt, TmpTermList u) {
        return super.conjN(dt, u);
    }

    protected final Subterms _subterms(Subterms t) {
        if (t instanceof TmpTermList tt)
            return subterms(tt.arrayTake());
        else if (t instanceof TermList tl)
            return tl.the(this);
        else
            return t;
    }

    protected abstract Term conjIntern(int dt, Term[] u);

    @Override
    protected Term negNew(Term u) {
        return termBuilder.negNew(u);
    }

    public final boolean internable(Term x) {
        return x.complexity() < compoundVolMax && x.internable();
    }

    protected final boolean internableTerms(Term... subterms) {
        int volRemain = compoundVolMax;
        for (Term x : subterms) {
            if (x == Null || !x.internable() || (volRemain-=x.complexity()) < 0)
                return false;
        }
        return true;
    }

    final boolean internableTerms(Subterms subs) {
        return subs.internable(compoundVolMax);
    }

    @Override
    final Term statementNew(Op o, int dt, Term S, Term P) {
        return statementInternable(S, P, dt, o == IMPL) ?
            _statementNew(o, dt, S, P) :
            super.statementNew(o, dt, S, P);
    }

    private Term _statementNew(Op o, int dt, Term S, Term P) {
        boolean negate;
        if (o==IMPL) {
            negate = (P instanceof Neg);
            if (negate) P = P.unneg();
        } else {
            negate = false;
            if (S instanceof Neg) { S = S.unneg(); negate = !negate; }
            if (P instanceof Neg) { P = P.unneg(); negate = !negate; }

            if (o == SIM || o == DIFF) {
                //commutive order: pre-sort by swapping to avoid saving redundant mappings
                if (S.compareTo(P) > 0) {
                    Term x = P;
                    P = S;
                    S = x;
                }
            }
        }

        return statementInterned(o, dt, S, P).negIf(negate);
    }

    <I extends ByteKeyExternal, Y> Function<I, Y> memoizer(String name, Function<I, Y> f, int capacity) {
        return Memoizers.the.memoizeByte(getClass().getSimpleName() + '.' + name,
                f, capacity);
    }


    private boolean statementInternable(Term S, Term P, int dt, boolean impl) {
        return (S instanceof The && !(S instanceof Bool)) &&
                (P instanceof The && !(P instanceof Bool))
                && (S.complexity() + P.complexity() < compoundVolMax)
                && internable(S) && internable(P)
                && ((impl && dt != 0) || !S.equals(P));
    }

    protected abstract Term statementInterned(Op o, int dt, Term S, Term P);


}
package nars.subterm;

import jcog.The;
import jcog.Util;
import jcog.util.SingletonIterator;
import nars.Op;
import nars.Term;
import nars.term.Compound;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/** minimal light-weight wrapper of a single target as a Subterms impl */
public final class UnitSubterm implements The, Subterms {

    private final Term sub;

    public UnitSubterm(Term sub) {
        this.sub = sub;
    }

    public Term sub() {
        return sub;
    }

    @Override
    public final String toString() {
        return Subterms.toString(sub());
    }

    @Override
    public final Term[] arrayClone() {
        return new Term[]{sub()};
    }

    @Override
    public final int complexity() {
        return sub().complexity() + 1;
    }

    @Override
    public final int complexityConstants() {
        return sub().complexityConstants() + 1;
    }

    @Override
    public boolean BOOL(Predicate<? super Term> t, boolean andOrOr) {
        return t.test(sub());
    }

    @Override
    public final <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        return ANDwith(p, param);
    }

    @Override
    public final <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        return p.test(sub(), param);
    }

    @Override
    public final <X> boolean ANDwithOrdered(BiPredicate<Term, X> p, X param) {
        return ANDwith(p, param);
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj ||
           (obj instanceof Subterms s && s.subs() == 1 && sub().equals(s.sub(0)));
    }

    @Override
    public final int hashCode() {
        return Util.hashCombine1(sub());
    }

    @Override
    public final int hashCodeSubterms() {
        return Subterms.hash(sub());
    }

    @Override
    public final Term sub(int i) {
        if (i != 0) throw new ArrayIndexOutOfBoundsException();
        return sub();
    }

    @Override public final Term[] removing(int i) {
        if (i != 0) throw new ArrayIndexOutOfBoundsException();
        return Op.EmptyTermArray;
    }

    @Override
    public final int subs() {
        return 1;
    }

    @Override
    public void setNormalized() {
        var t = sub();
        if (t instanceof Compound c)
            c.setNormalized();
    }

    @Override
    public final void forEach(Consumer<? super Term> c) {
        c.accept(sub());
    }

    @Override
    public final Stream<Term> subStream() {
        return Stream.of(sub());
    }

    @Override
    public final Iterator<Term> iterator() {
        return new SingletonIterator<>(sub());
    }

    @Override
    public final int sum(ToIntFunction<Term> value) {
        return value.applyAsInt(sub());
    }
}
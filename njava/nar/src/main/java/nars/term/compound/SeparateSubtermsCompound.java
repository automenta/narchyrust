package nars.term.compound;

import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.XTERNAL;

/** delegates certain methods to a specific impl */
public abstract class SeparateSubtermsCompound extends Compound {

    private final Subterms sub;

    protected SeparateSubtermsCompound(int op, Subterms sub) {
        super(op);
        this.sub = sub;
    }

    @Override
    public final Subterms subterms() {
        return sub;
    }

    @Override
    public int complexity() {
        return sub.complexity();
    }

    @Override
    public final int structSubs() {
        return sub.structSubs();
    }

    @Override
    public final boolean NORMALIZED() {
        return sub.NORMALIZED();
    }

    @Override
    public final void setNormalized() {
        sub.setNormalized();
    }

    @Override
    public final int height() {
        return sub.height();
    }

    @Override
    public int structSurface() {
        return sub.structSurface();
    }

    @Override
    public int structSurfaceUnneg() {
        return sub.structSurfaceUnneg();
    }


    @Override
    public Term subUnneg(int i) {
        return sub.subUnneg(i);
    }

    @Override
    public boolean subEquals(int i, Term x) {
        return sub.subEquals(i, x);
    }

    @Override
    public Subterms negated() {
        return sub.negated();
    }

    @Override
    public Subterms reverse() {
        return sub.reverse();
    }

    @Override
    public final boolean equalTerms(Subterms x) {
        return sub.equalTerms(x instanceof SeparateSubtermsCompound s ? s.sub : x);
    }

    @Override
    public boolean containsInstance(Term t) {
        return sub.containsInstance(t);
    }

    @Override
    public boolean contains(Term x) {
        return sub.contains(x);
    }

    @Override
    public Predicate<Term> contains(boolean pn, boolean preFilter) {
        return sub.contains(pn, preFilter);
    }

    @Override
    public final boolean containsNeg(Term x) {
        return sub.containsNeg(x);
    }

    @Override
    public boolean containsPN(Term x) {
        return sub.containsPN(x);
    }

    @Override
    public int indexOf(Term t, int after) {
        return sub.indexOf(t, after);
    }

    @Override
    public final int hashCodeSubterms() {
        return sub.hashCodeSubterms();
    }

    @Override
    public final boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return !inSuperCompound.test(this) ||
            whileTrue.test(this) &&
            sub.recurseTermsOrdered(inSuperCompound, whileTrue, this);
    }

    @Override
    public final Term[] arrayClone() {
        return sub.arrayClone();
    }

    @Override
    public final Term[] arrayShared() {
        return sub.arrayShared();
    }

    @Override
    public final Term[] arrayClone(Term[] x, int from, int to) {
        return sub.arrayClone(x, from, to);
    }

    @Override
    public final TermList toList() {
        return sub.toList();
    }

    @Override
    public final int subs() {
        return sub.subs();
    }

    @Override
    public final Term sub(int i) {
        return sub.sub(i);
    }

    @Override
    public final Term sub(int i, Term ifOutOfBounds) {
        return sub.sub(i, ifOutOfBounds);
    }

    @Override
    public final boolean subIs(int i, Op o) {
        return sub.subIs(i, o);
    }

    @Override
    public Iterator<Term> iterator() {
        return sub.iterator();
    }

    @Override
    public final Stream<Term> subStream() {
        return sub.subStream();
    }

    @Override
    public final int count(Op matchingOp) {
        return sub.count(matchingOp);
    }

    @Override
    public final int count(Predicate<? super Term> match) {
        return sub.count(match);
    }

    @Override
    public final void forEach(/*@NotNull*/ Consumer<? super Term> c) {
        sub.forEach(c);
    }

    @Override
    public final void forEach(/*@NotNull*/ Consumer<? super Term> action, int start, int stop) {
        sub.forEach(action, start, stop);
    }

    @Override
    public int addAllTo(Term[] t, int offset) {
        return sub.addAllTo(t, offset);
    }

    @Override
    public void addAllTo(Collection<Term> target) {
        sub.addAllTo(target);
    }

    @Override
    public boolean TEMPORAL_VAR() {
        return dt()==XTERNAL || sub.TEMPORAL_VAR();
    }

    @Override
    public final int complexityConstants() {
        return sub.complexityConstants();
    }

    @Override
    public final int varQuery() {
        return sub.varQuery();
    }

    @Override
    public final int varPattern() {
        return sub.varPattern();
    }

    @Override
    public final int varDep() {
        return sub.varDep();
    }

    @Override
    public final int varIndep() {
        return sub.varIndep();
    }

    @Override
    public final int vars() {
        return sub.vars();
    }

    @Override
    public final int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        return sub.intifyShallow(v, reduce);
    }

    @Override
    public boolean BOOL(Predicate<? super Term> t, boolean andOrOr) {
        return sub.BOOL(t, andOrOr);
    }

    @Override
    public <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        return sub.ORwith(p, param);
    }

    @Override
    public <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        return sub.ANDwith(p, param);
    }

    @Override
    public <X> boolean ANDwithOrdered(BiPredicate<Term, X> p, X param) {
        return sub.ANDwithOrdered(p, param);
    }

// this version is only helpful if struct isn't cached like CachedCompound and CachedUnitCompound
//    @Override public final boolean hasAll(int x) {
//        int ta = structSubs();
//        if ((ta | x) == ta)
//            return true; //short-circuit
//        else {
//            int tab = ta | structOp();
//            return ((tab | x) == tab);
//        }
//    }
//    @Override
//    public final boolean hasAny(int struct) {
//        return Op.hasAny(structSubs(), struct) | Op.hasAny(structOp(), struct);
//    }

}
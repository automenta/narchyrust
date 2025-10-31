package nars.subterm;

import jcog.data.list.Lst;
import nars.Op;
import nars.Term;
import nars.term.Neg;
import nars.term.atom.Bool;
import nars.term.builder.TermBuilder;
import nars.term.util.conj.ConjBundle;
import nars.term.util.conj.ConjList;
import nars.term.util.transform.TermTransform;

import java.util.Collection;
import java.util.function.UnaryOperator;

import static nars.Op.ETERNAL;
import static nars.Op.EmptySubterms;
import static nars.term.atom.Bool.True;
import static nars.term.util.Image.imageNormalize;

/** mutable subterms, used in intermediate operations */
public class TermList extends Lst<Term> implements Subterms {

    public static final TermList[] EmptyTermListArray = new TermList[0];

    public TermList(int initialCapacity) {
        super(0, initialCapacity==0 ? Op.EmptyTermArray : new Term[initialCapacity]);
    }

    public TermList() {
        this(0);
    }

    public TermList(Term... direct) {
        super(direct);
    }

    public TermList(int startingSize, Term[] direct) {
        super(startingSize, direct);
    }

    public TermList(Collection<Term> copied) {
        this(copied.size());
        copied.forEach(this::addFast);
    }

    public TermList(TermList copied) {
        this((Subterms)copied);
    }

    public TermList(Subterms copied) {
        this(copied.subs());
        copied.forEach(this::addFast);
    }

    public static boolean containsNeg(Lst<Term> l, Term x) {
        if (x instanceof Neg)
            return l.contains(x.unneg());
        else {
            int n = l.size();
            for (Term t : l) {
                if (t instanceof Neg) {
                    if (x.equals(t.unneg()))
                        return true;
                }
            }
            return false;
        }
    }

    public final void addAll(Subterms x) {
        ensureCapacityForAdditional(x.subs(), true);
        for (Term xx : x)
            add(xx);
    }


    public final Subterms replaceAll(TermTransform f) {
        replaceAll((UnaryOperator<Term>) f);
        return this;
    }

    public boolean replaceAllUnlessNull(UnaryOperator<Term> operator) {
        int s = size;
        Term[] ii = this.items;
        for (int i = 0; i < s; i++) {
            Term x = ii[i] = operator.apply(ii[i]);
            if (x == null || x == Bool.Null)
                return false;
        }
        return true;
    }

    /** clone immutable instance */
    public Subterms the(TermBuilder B) {
        return B.subterms(arrayClone());
    }

//	public final void addAllNegated(Iterable<Term> x) {
//        for (Term term : x)
//            addNegated(term);
//    }

//    public final void addNegated(Term x) {
//        add(x.neg());
//    }

    @Override
    public int hashCode() {
        return Subterms.hash(items, size);
    }

    /** dont intern */
    @Override public final boolean internables() {
        return false;
    }


    @Override
    public TermList toList() {
        return new TermList(arrayClone());
    }

    @Override
    public final Term sub(int i) {
        return items[i];
    }

    @Override
    public Term[] arrayClone() {
        return isEmpty() ? Op.EmptyTermArray : toArray();
    }

    @Override
    public final int subs() {
        return size;
    }

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
//        return this == obj ||
//            ((obj instanceof TermList) ? nonNullEquals(((TermList) obj)) : equalTerms((Subterms) obj));
        return obj instanceof Subterms && Subterms.super.equalTerms((Subterms)obj);
    }

//
//    public void addAll(Subterms x, int xStart, int xEnd) {
//        ensureCapacity(xEnd-xStart);
//        for (int i = xStart; i < xEnd; i++) {
//            addWithoutResizeCheck(x.sub(i));
//        }
//    }



//    @Override
//    public final boolean containsInstance(Term term) {
//        return super.containsInstance(term);
//    }

    public final void addNeg(Term x) {
        add(x.neg());
    }
    protected final void addFastNeg(Term x) {
        addFast(x.neg());
    }

//    public void copyFrom(TermList x) {
//        if (this == x) return;
//        items = x.items;
//        size = x.size;
//    }


    @Deprecated public final boolean inhExplode(boolean commute) {
        int e = _inhExplode(commute, Op.terms);
        return e == +1;
    }

    /**
     *
     * @return
     *  -1 false
     *   0 no change
     *  +1 change, OK
     */
    public final int _inhExplode(boolean commute, TermBuilder B) {
        ConjList add = null;
        int s = size;
        if (s == 0)
            return 0;

        Term[] items = this.items;
        for (int i = 0; i < s; i++) {
            Term x = items[i];
            if (x.INH() && ConjBundle.bundled(x)) {
                items[i] = True;
                if (add == null) add = new ConjList(s - i);
                if (!add.addEvent(ETERNAL, x))
                    return -1;
            }
        }

        if (add == null)
            return 0;

        removeInstances(True);
        int e = add.inhExplode(B);
        if (e == -1)
            return -1;

        addAll(add);
        add.delete();
        if (commute) commuteThis();
        return +1;
    }


    private boolean commuteThis() {
        if (size < 2) return false;
//        if (isSorted())
//            return false;

        Subterms c = commuted();
        if (c!=this) {
        //Term[] c = Terms.commute(arrayShared());
        //if (c.length != size || !equalTerms(c)) {
            clear();
            c.forEach(this::addFast);
            return true;
        }
        return false;
    }

    public final void addImageNormalized(Term x, TermBuilder B) {
        add(imageNormalize(x, B));
    }

    public final void addAllNeg(Collection<Term> i) {
        ensureCapacityForAdditional(i.size(), false);
        i.forEach(this::addFastNeg);
        //for (var t : i) addFastNeg(t);
    }

    /** special fill for missing elements */
    public Subterms fill(Subterms x) {
        int ys = this.size;
        if (ys == 0) return EmptySubterms;

        //replace prefix nulls with the input's values
        //any other modifications specific to the superOp type
        Term[] ii = items;
        int nulled = 0;
        for (; nulled < ys; nulled++) {
            if (ii[nulled] != null)
                break; //finished at first non-null subterm
        }
        if (nulled > 0)
            x.copyTo(ii, 0, nulled);
        return this;
    }

    public final void addRoot(Term x) {
        add(x.root());
    }
}
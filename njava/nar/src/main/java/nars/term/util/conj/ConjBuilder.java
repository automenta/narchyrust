package nars.term.util.conj;

import jcog.WTF;
import nars.Term;
import nars.term.Compound;
import nars.term.builder.TermBuilder;
import org.eclipse.collections.api.iterator.LongIterator;

import static nars.Op.*;
import static nars.term.atom.Bool.True;

public interface ConjBuilder  {

    /**
     * different semantics than .add() -- returns true even if existing present.  returns false on conflict
     * returns false only if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .target() should be considered final
     */
    default boolean add(long at, Term x) {
        if (at == DTERNAL || at == XTERNAL)//HACK TEMPORARY
            throw new WTF("probably meant ETERNAL or TIMELESS");
        if (at == TIMELESS)
            throw new WTF("invalid time");

        if (x == True)
            return true; //ignore

        if (decomposeConj(at, x) || ConjBundle.bundled(x))
            return addCondEvents(at, true, (Compound) x);

        return _addEvent(at, x);
    }

    private boolean _addEventNeg(long at, Term x) {
        return _addEvent(at, x.neg());
    }

    private boolean _addEvent(long at, Term x) {
        //x = Image.imageNormalize(x); //incl. negated INH

        if (ConjBundle.bundled(x))
            return ConjBundle.eventsAND(x, xx -> addEvent(at, xx));

        return addEvent(at, x);
    }

    private static boolean decomposeConj(long at, Term x) {
        if (!x.CONJ()) return false;
        int dt = x.dt();
        //TODO simplify?
        return dt != XTERNAL &&
               (dt == DTERNAL || dt == 0 || at != ETERNAL) &&
               (at != ETERNAL || !x.SEQ())
               ;
    }

//    default boolean add(LongObjectPair<Term> whenWhat) {
//        return add(whenWhat.getOne(), whenWhat.getTwo());
//    }

//    default boolean addAll(long w, Iterable<Term> tt) {
//        for (Term t : tt) {
//            if (!add(w, t))
//                return false;
//        }
//        return true;
//    }
//    default boolean addAllNeg(long w, Iterable<Term> tt) {
//        for (Term t : tt) {
//            if (!add(w, t.neg()))
//                return false;
//        }
//        return true;
//    }

    /**
     * for internal use only
     */
    boolean addEvent(long at, Term x);

//    default boolean addEventNeg(long at, Term x) {
//        return addEvent(at, x.neg());
//    }

    int eventOccurrences();

//    default boolean remove(LongObjectPair<Term> e) {
//        return remove(e.getOne(), e.getTwo());
//    }

//    boolean remove(long at, Term t);

    boolean removeAll(Term term);

    int eventCount(long when);

    void negateConds();

    default Term term() {
        return term(
            terms
            //SimpleHeapTermBuilder.the
        );
    }

    Term term(TermBuilder b);

    LongIterator eventOccIterator();

    private boolean addCondEvents(long at, boolean posOrNeg, Compound x) {
        return x.condsAND(
            posOrNeg ? this::_addEvent : this::_addEventNeg
        , at, true, false, true);
    }

    default long shiftOrZero() {
        long s = shift();
        if (s == ETERNAL)
            return 0;
        else {
            assert (s != TIMELESS);
            return s;
        }
    }

    default long shift() {
        long min = TIMELESS;
        LongIterator ii = eventOccIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != ETERNAL && t < min)
                min = t;
        }
        return min == TIMELESS ? ETERNAL : min;
    }

    void clear();

//    default void addAll(ConjList j) {
//        j.forEachEvent(this::add);
//    }

    void close();
}
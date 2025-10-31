package nars.subterm;

import nars.Op;
import nars.Term;
import nars.term.builder.TermBuilder;
import nars.term.util.Terms;

import java.util.Arrays;
import java.util.function.Predicate;

/** indicates that instances of this class are ephemeral, temporary, etc.
 *  so that callees can determine if their value can be extracted safely for zero-copy */
public final class TmpTermList extends TermList {

    public TmpTermList(Subterms s) {
        super(s);
    }
    
    public TmpTermList(Subterms s, Predicate<Term> filter) {
        super(0);
        for (Term z : s) {
            if (filter.test(z))
                add(z);
        }
    }

    public TmpTermList(int initialCap) {
        super(initialCap);
    }

    public TmpTermList(int initialCap, int initialSize) {
        this(initialCap);
        setSize(initialSize);
    }

    public TmpTermList(Term... direct) {
        super(direct);
    }
    public TmpTermList() {
        super();
    }


    /** use this only if sure this instance is done being modified in a temporary application
     * */
    public final Term[] arrayTake() {
        int s = size;
        if (s == 0) return Op.EmptyTermArray;
        Term[] x = items;
        Term[] z = x.length == s ? x : Arrays.copyOf(x, s);

        //soft-delete, but dont modify the returned array if zero copy
        this.items = null;
        this.size = -1;

        return z;
    }
    public Term[] sortedArrayTake() {
        int s = size;
        if (s > 1)
            Arrays.sort(this.items, 0, s);

        return arrayTake();
    }
    public Subterms the(TermBuilder B) {
        return B.subterms(arrayTake());
    }
    public Term[] sortAndDedupArrayTake() {


        //TODO if size > threshld: use original insertion sort method:
//        SortedList<Term> sl = new SortedList<>(t, new Term[t.length]);
//        return sl.orderChangedOrDeduplicated ?
//                sl.toArrayRecycled(Term[]::new) : t;


        Term[] ii = this.items;

        int s = size;
        if (s > 1) {
            //TODO pre-sort by volume since this is the first condition for comparison.  if there are N unique volumes then it will be sorted without needing compareTo

            Arrays.sort(ii, 0, s);

            Term prev = ii[0];
            boolean dedup = false;
            for (int i = 1; i < s; i++) {
                Term next = ii[i];
                if (prev.equals(next)) {
                    ii[i] = null;
                    dedup = true;
                } else
                    prev = next;
            }
            if (dedup)
                removeNulls();
        }

        return arrayTake();
    }

    public void commuteThis() {
        int s = this.size;
        if (s <= 1) return;
        items = Terms.commute(arrayTake());
        //items = Terms.commuteMutable(items, s); //TODO
        size = items.length;
    }


}
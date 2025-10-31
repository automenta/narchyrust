package nars.link;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import nars.Premise;
import nars.Term;
import nars.term.Compound;

import java.util.Iterator;

/**
 * caches an array of tasklinks tangent to an atom
 * thread-safe but used in localized contexts, like per-concept etc
 */
public final class TermLinkBag extends PriReferenceArrayBag<Term, PriReference<Term>> {

    public static final PriMerge Merge =
        PriMerge.plus;

    public TermLinkBag(int capacity) {
        super(Merge);
        setCapacity(capacity);
    }

    @Deprecated private static final boolean componentFilter =
        //true;
        false;

    /**
     * skim TermLinkBag instance in the Concept's meta table, attached by a SoftReference
     */
    @Deprecated public void commit(Term x, Iterator<? extends Premise> iter, int tries, float pri, float forgetRate) {
        var xh = x.hashShort();
        var xEq = x.equals();
        int insertions = 0, rejections = 0, merges = 0;
        while (iter.hasNext() && tries-- > 0) {
            var z = iter.next();
            var y = z.other(xh, xEq);  // == t.other(x);
            if (y != null)
                if (accept(x, y)) {
                    var a = new PLink<>(y, pri);
                    var b = put(a);
                    if (b == null)
                        rejections++;
                    else {
                        if (b == a)
                            insertions++;
                        else
                            merges++;
//                    if (insertions + merges > cap) break; //activity exceeds capacity
                    }
                }
        }

        if (insertions > 0 || merges > 0 || rejections > 0)
            commit(forget(forgetRate));
    }
    @Deprecated private static boolean accept(Term x, Term y) {
        if (componentFilter) {
            return (!(x instanceof Compound X) || !X.contains(y))
                    &&
                    (!(y instanceof Compound Y) || !Y.contains(x));
        } else {
            return true;
        }

        //return !x.containsRecursively(y) && !y.containsRecursively(x);
    }



}


//    /**
//     * enforce non-structural total ordering
//     */
//    private static final boolean ENFORCE_NON_STRUCTURAL =
//            false;
//            //true;

//    private static boolean invalid(Term y, int volMin) {
//		return  y == null
//				||
//				//!y.TASKABLE()
//				//||
//				(ENFORCE_NON_STRUCTURAL && y.volume() <= volMin);
//    }
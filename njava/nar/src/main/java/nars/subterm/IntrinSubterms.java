package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.Term;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.Compound;
import nars.term.CondAtomic;
import nars.term.Neg;
import nars.term.anon.Intrin;
import nars.term.atom.IntrinAtomic;

import java.util.Iterator;
import java.util.function.Predicate;

import static nars.Op.NEG;
import static nars.io.TermIO.outNegByte;
import static nars.term.anon.Intrin._term;
import static nars.term.anon.Intrin.term;

/**
 * a subterm impl consisting only of intrin subterms
 */
public final class IntrinSubterms extends CachedSubterms implements CondAtomic /*implements Subterms.SubtermsBytesCached*/ {

    /*@Stable*/
    public final short[] subterms;

    public IntrinSubterms(short[] s) {
        super(meta(s));
        this.subterms = s;
        internableKnown = internables = true;
    }

    /**
     * assumes the array contains only Intrin-able terms
     */
    public IntrinSubterms(Term... s) {
        super(s);
        //assert(s.length>0);

        var hasNeg = hasNeg();

        var t = subterms = new short[s.length];
        for (int i = 0, sLength = s.length; i < sLength; i++) {
            var ss = s[i];
            var neg = hasNeg && ss instanceof Neg;
            if (neg)
                ss = ss.unneg();
            var tt = Intrin.id(ss); //assert(tt!=0);
            t[i] = neg ? ((short)-tt) : tt;
        }

        this.normalized = preNormalize();
    }

    @Override
    public void write(ByteArrayDataOutput out) {
        var ss = this.subterms;
        out.writeByte(ss.length);
        for (var s : ss) {
            if (s < 0) {
                outNegByte(out);
                s = (short) -s;
            }
            _term(s).write(out);
        }
    }

    private static SubtermMetadataCollector meta(short[] s) {
        assert(s.length > 0);
        var c = new SubtermMetadataCollector();
        for (var x : s)
            c.add(term(x));
        return c;
    }

    private boolean preNormalize() {
        return vars() == 0 || normalized(subterms);
    }


//        @Override
//    public @Nullable Subterms transformSubs(TermTransform f) {
//        @Nullable Subterms s = super.transformSubs(f);
//        if (s!=this && equals(s))
//            return this; //HACK
//        else
//            return s;
//    }


    /** since IntrinSubterms is flat, the structureSurface is equal to the structure if no NEG present */
    @Override public final int structSurface() {
        if ((structure & NEG.bit) == 0)
            return structure;
        else {
            var x = 0;
            for (var t : subterms)
                x |= (t < 0) ? NEG.bit : (1<<Intrin.op(t));
            return x;
        }
    }

    @Override
    public int height() {
        return hasNeg() ? 3 : 2;
    }

//    public Subterms replaceSub(Term from, Term to, Op superOp) {
//
//
//        short fid = Intrin.id(from);
//        if (fid == 0)
//            return this; //no change
//
//        boolean found = false;
//        if (fid > 0) {
//            //find positive or negative subterm
//            for (short x: subterms) {
//                if (Math.abs(x) == fid) {
//                    found = true;
//                    break;
//                }
//            }
//        } else {
//            //find exact negative only
//            for (short x: subterms) {
//                if (x == fid) {
//                    found = true;
//                    break;
//                }
//            }
//        }
//        if (!found)
//            return this;
//
//
//        short tid = Intrin.id(to);
//        if (tid != 0) {
//            assert (from != to);
//            short[] a = this.subterms.clone();
//            if (fid > 0) {
//                for (int i = 0, aLength = a.length; i < aLength; i++) { //replace positive or negative, with original polarity
//                    short x = a[i];
//                    if (x == fid) a[i] = tid;
//                    else if (-x == fid) a[i] = (short) -tid;
//                }
//            } else {
//                for (int i = 0, aLength = a.length; i < aLength; i++) //replace negative only
//                    if (a[i] == fid) a[i] = tid;
//            }
//
//            IntrinSubterms v = new IntrinSubterms(a);
//            v.normalized = preNormalize();
//            return v;
//
//        } else {
//            int n = subs();
//            Term[] tt = new Term[n];
//            short[] a = this.subterms;
//            if (fid > 0) {
//                for (int i = 0; i < n; i++) { //replace positive or negative, with original polarity
//                    short x = a[i];
//                    Term y;
//                    if (x == fid) y = (to);
//                    else if (-x == fid) y = (to.neg());
//                    else y = (term(x));
//                    tt[i] = (y);
//                }
//            } else {
//                for (int i = 0; i < n; i++) //replace negative only
//                    tt[i] = (a[i] == fid ? to : term(a[i]));
//
//            }
//            return new TermList(tt);
//        }
//    }


    @Override
    public final Term sub(int i) {
        return term(subterms[i]);
    }

    @Override
    public final Term subUnneg(int i) {
        return _term((short)Math.abs(subterms[i]));
    }

    @Override
    public int count(Op matchingOp) {
        return switch (matchingOp) {
            case NEG -> subsNeg();
            case ATOM -> countByGroup(Intrin.ANOMs) + countByGroup(Intrin.CHARs);
            case VAR_PATTERN -> countByGroup(Intrin.VARPATTERNs);
            case VAR_QUERY -> countByGroup(Intrin.VARQUERYs);
            case VAR_DEP -> countByGroup(Intrin.VARDEPs);
            case VAR_INDEP -> countByGroup(Intrin.VARINDEPs);
            case IMG -> countByGroup(Intrin.IMGs);
            case INT -> countByGroup(Intrin.INT_POSs) + countByGroup(Intrin.INT_NEGs);
            default -> 0;
        };

    }

    private int countByGroup(short group) {
        var count = 0;
        for (var s: subterms) {
            if (s > 0 && Intrin.group(s) == group)
                count++;
        }
        return count;
    }

    private int subsNeg() {
        var count = 0;
        for (var s: subterms)
            if (s < 0)
                count++;
        return count;
    }

    @Override
    public final int subs() {
        return subterms.length;
    }

    private int indexOf(short id) {
        return indexOf(id, -1);
    }
    private int indexOf(short id, int after) {
        return ArrayUtil.indexOf(subterms, id, after+1);
    }

    public int indexOf(IntrinAtomic t) {
        return indexOf(t.intrin());
    }


    @Override
    public int indexOf(Term t, int after) {
        var tid = Intrin.id(t);
        return tid != 0 ? indexOf(tid,after) : -1;
    }

    private int indexOfNeg(Term x) {
        var tid = Intrin.id(x);
        return tid != 0 ? indexOf((short) -tid) : -1;
    }

    @Override
    public boolean containsNeg(Term x) {
        return indexOfNeg(x) != -1;
    }

    @Override
    public boolean containsRecursively(Term x, Predicate<Compound> inSubtermsOf) {
        if (x instanceof Neg) {
            if (hasNeg()) {
                var ttu = Intrin.id(x.unneg());
                if (ttu!=0)
                    return indexOf((short) -ttu) != -1;
            }
        } else {
            var aid = Intrin.id(x);
            if (aid!=0) {
                var hasNegX = false;
                for (var xx : this.subterms) {
                    if (xx == aid)
                        return true; //found positive
                    else if (xx == -aid)
                        hasNegX = true; //found negative, but keep searching for a positive first
                }
                if (hasNegX)
                    return (inSubtermsOf==null || inSubtermsOf.test((Compound)(x.neg())));
            }
        }
        return false;
    }

    private boolean hasNeg() {
        return (structure & NEG.bit) != 0;
    }

    @Override
    public Iterator<Term> iterator() {
        return new AnonArrayIterator(subterms);
    }

    @Override
    public boolean equals(Object x) {
        return
            (this == x)
            ||
            (x instanceof IntrinSubterms is ?
                //Arrays.equals(subterms, is.subterms)
                ArrayUtil.equals(subterms, is.subterms)
                :
                (x instanceof Subterms ss && equalTerms(ss))
            );
    }

    @Override
    public boolean subEquals(int i, Term x) {
        var xx = Intrin.id(x);
        return xx!=0 && xx == subterms[i];
//        return xx!=0 ? subterms[i]==xx : sub(i).equals(x);
    }

    private static class AnonArrayIterator implements Iterator<Term> {

        private int current;
        private final short[] values;

        AnonArrayIterator(short[] subterms) {
            this.values = subterms;
        }

        @Override
        public boolean hasNext() {
            return current < values.length;
        }

        @Override
        public Term next() {
            return term(values[current++]);
        }

    }
}
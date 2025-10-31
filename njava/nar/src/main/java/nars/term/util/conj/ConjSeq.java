package nars.term.util.conj;

import nars.Term;
import nars.subterm.TmpTermList;
import nars.term.builder.TermBuilder;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.occToDT;

/**
 * utilities for working with conjunction sequences (raw sequences, and factored sequences)
 */
public enum ConjSeq {
    ;

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * assumes that all of the event terms have distinct occurrence times
     *
     * @param l  start index, inclusive
     * @param r end index, exclusive
     */
    static Term seqBalanced(TermBuilder B, ConjList c, int l, int r) {
        Term left, right;
        int rEvent;
        switch (r - l) {
            case 0:
                throw new IllegalArgumentException("empty events list");
            case 1:
                return c.get(l);
            case 2: {
                left =  c.get(l);
                right = c.get(rEvent = r-1);
                break;
            }
            default: {
                rEvent = 1 +
                    ConjList.centerByIndex(l, r);
                    //events.centerByVolume(start, end);
                left = seqBalanced(B, c, l, rEvent);
                right = seqBalanced(B, c, rEvent, r);
                break;
            }
        }
        return conjSeqSimple(left, right,
        occToDT(c.when(rEvent) - c.when(l)) - left.seqDur(),
           B);
    }

    private static Term conjSeqSimple(Term left, Term right, int dt, TermBuilder B) {
        if (left == Null || right == Null) return Null;
        if (left == False || right == False) return False;
        if (left == True) return right;
        if (right == True) return left;

        return dt == 0 || dt == DTERNAL ?
                B.conj(left, right) //parallel
                :
                conjSeqSimple(B, left, dt, right);
    }

    private static Term conjSeqSimple(TermBuilder B, Term a, int _dt, Term b) {
        assert (_dt != XTERNAL);

        var dt = _dt;
        //assert(x.length==2);
        var c = a.compareTo(b);
        if (c > 0) {
            dt = -dt;
            //swap order
            var t = b;
            b = a;
            a = t;
        } else if (c == 0) {
            //equal.  sequence of repeating terms; share identity
            b = a;
            if (dt < 0)
                dt = -dt;
        }

        return B.compoundNew(CONJ, dt, a, b);
    }


    public static Term conjAppend(Term x, int dt, Term y, TermBuilder B) {
        return conj(x, dt, y, true, B);
    }

    /**
     * attaches two events together with dt separation
     */
    public static Term conj(Term x, int dt, Term y, boolean append, TermBuilder B) {

        if (x == Null  || y == Null) return Null;
        if (x == False || y == False) return False;
        if (x == True) return y;
        if (y == True) return x;

        if (dt == XTERNAL || dt == DTERNAL)
            return B.conj(dt, x, y);

        int xRange = x.seqDur(), yRange = y.seqDur();

        int xToY;
        if (append) {
            long bStart;
            if (dt >= 0) {
                bStart = +dt + xRange;
            } else {
                bStart = -dt + yRange;
                var ab = x;
                x = y;
                y = ab; //swap
            }
            xToY = occToDT(bStart);
        } else {
            xToY = dt;
        }

        var simple =
            (xRange==0 && yRange==0 && !x.unneg().CONDS() && !y.unneg().CONDS());
            //(!x.unneg().CONDS() && !y.unneg().CONDS());
            //dt!=0
            //(dt!=0 || !xu.equals(yu))
            //: (aRange==0 && bRange==0);
            //(dt!=0 && aRange==0 && bRange==0);
            //(aRange==0 && bRange==0);

        return simple ?
            conjSeqSimple(x, y, xToY, B) :
            conjSeqComplex(xToY, x, y, B);
    }

    private static Term conjSeqComplex(int xToY, Term x, Term y, TermBuilder B) {
        try (var c = new ConjList(2)) {
            if (c.add(0L, x))
                c.add((long) xToY, y);
            return c.term(B);
        }
//        try (ConjTree c = new ConjTree()) {
//            if (c.add(0, x))
//                c.add(xToY, y);
//            return c.term(B);
//        }
    }

    static Term conjSeqComplex(int n, Term[] items, long[] when, TermBuilder B) {
        try (var seqs = new ConjTree()) {
            TmpTermList pars = null;
            for (var i = 0; i < n; i++) {
                var wi = when[i];
                var ii = items[i];
                if (wi == ETERNAL) {
                    ((pars == null) ? pars = new TmpTermList(1) : pars).add(ii);
                } else {
                    if (!seqs.add(wi, ii))
                        break;
                }
            }

            return conjSeqComplex(seqs, pars, B);
        }
    }

    private static Term conjSeqComplex(ConjTree seqs, TmpTermList pars, TermBuilder B) {
        var par = pars !=null ? B.conj(pars) : null;
        if (par == Null) return Null;

        var seq = seqs.term(B);
        if (seq == Null) return Null;

        if (par == False || seq == False) return False;

        return par != null ? B.conj(par, seq) : seq;
    }



}
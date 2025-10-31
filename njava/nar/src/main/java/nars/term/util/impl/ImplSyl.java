package nars.term.util.impl;

import jcog.WTF;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.atom.CharAtom;
import nars.term.atom.Int;
import nars.time.Tense;

import static nars.Op.*;

/** utilities for Implication syllogism rules */
public enum ImplSyl {;

    public static Term implSyl(Compound taskTerm, Compound beliefTerm, Subterms args, int ditherDT) {
        int aa = args.subs();
        if (aa < 5)
            throw new WTF();

        boolean bNeg, dNeg;
        if (aa > 5) {
            bNeg = CharAtom.the(args.sub(5)) == 'n';
            dNeg = CharAtom.the(args.sub(6)) == 'n';
        } else {
            bNeg = dNeg = false;
        }

        int ab = taskTerm.dt(), cd = beliefTerm.dt();

        //HACK convert XTERNAL to DTERNAL
        if (ab == XTERNAL && cd == DTERNAL) ab = DTERNAL;
        if (cd == XTERNAL && ab == DTERNAL) cd = DTERNAL;

        int dt = ab == XTERNAL || cd == XTERNAL ? XTERNAL : Tense.dither(
                implSylDT(taskTerm, beliefTerm,
                        ab, Int.i(args.sub(2)),
                        cd, Int.i(args.sub(3)),
                        CharAtom.the(args.sub(4)), bNeg, dNeg)
                , ditherDT);

        Term s = args.sub(0), p = args.sub(1);
        return IMPL.the(s, dt, p);
    }

    @Deprecated public static int implSylDT(Compound AB, Compound CD, int ab, int tm, int cd, int bm, char mode) {
        return implSylDT(AB, CD, ab, tm, cd, bm, mode, false, false);

    }

    /**
     * (A ==> +-B), (C ==> +-D)
     */
    public static int implSylDT(Compound AB, Compound CD, int ab, int tm, int cd, int bm, char mode, boolean bNeg, boolean dNeg) {
        Term a = AB.sub(0), b = AB.sub(1).negIf(bNeg), c = CD.sub(0), d = CD.sub(1).negIf(dNeg);

        if (ab == DTERNAL) ab = 0; //HACK
        if (cd == DTERNAL) cd = 0; //HACK

        if (tm == 1 && bm == 1) {
            //DEDUCTION
            //outer
            return ab + cd + (mode == 'i' ? implSylDTevent(b, c, false, false) : implSylDTevent(d, a, false, false));
            
        } else if (tm == -1 && bm == -1) {
            //EXEMPLIFICATION
            //inner
            return -ab - cd - (mode == 'i' ? implSylDTevent(b, c, false, false) : implSylDTevent(d, a, false, false)); //outer
        } else if (tm == +1 && bm == -1) {
            //INDUCTION/ABDUCTION
            return mode == 'p' ?
                    ab - cd - implSylDTevent(d, b, true, true) - c.seqDur() : //pred
                    ab - cd + (a.seqDur() - implSylDTevent(a, c, false, false)) - d.seqDur();

        } else /*if (tm == -1 && bm == +1)*/ {
            //INDUCTION/ABDUCTION
            return mode == 'p' ?
                    -ab + cd + implSylDTevent(b, d, true, true) - a.seqDur() : //pred
                    -ab + cd - (a.seqDur() - implSylDTevent(a, c, false, false)) - b.seqDur();
        }
    }

//    private static int implSylDT_o11(int ab, int cd, Term a, Term d) {
//        //TODO
////                if ((a instanceof Compound || d instanceof Compound) && !a.equals(d)) {
////                    int aInD = d instanceof Compound ? ((Compound)d).condTime(a, true) : XTERNAL;
////                    if (aInD!=XTERNAL)
////                        z += aInD;
////                    else {
////                        //TODO
////                    }
////                }
//
//        //outer
//        return ab + a.seqDur() + cd;
//    }

    private static int implSylDTevent(Term x, Term y, boolean reverseMode, boolean equalMode) {

        if (x.equalsPN(y)) //HACK PN?  for eqPN in impl.syl.nal rules
            return equalMode ? 0 : x.seqDur();


        if (x instanceof Compound || y instanceof Compound) {
            int cInB = x instanceof Compound xc? xc.when(y, true) : XTERNAL;
            if (cInB == DTERNAL) cInB = 0;
            if (cInB != XTERNAL) {
                return cInB;
            } else {
                int bInC = y instanceof Compound yc ? yc.when(x, false) : XTERNAL;
                if (bInC == DTERNAL) bInC = 0;
                if (bInC != XTERNAL)
                    return reverseMode ? -bInC : y.seqDur() - bInC;
//                else {
//                    Util.nop(); //HACK TODO ?? shouldnt happen
//                }
            }
        }

        return 0; //??
    }

}
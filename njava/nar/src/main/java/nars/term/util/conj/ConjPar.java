package nars.term.util.conj;

import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import nars.NAL;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static jcog.data.bit.MetalBitSet.bits;
import static nars.Op.*;
import static nars.term.atom.Bool.*;

/**
 * utilities for working with commutive conjunctions (DTERNAL, parallel, and XTERNAL)
 */
public enum ConjPar {;

    /** how many sequences to allow in a parallel.  0 prevents any */
    static private final int SEQ_MAX =
        1;
        //0;

    /** whether to allow negated sequences in a parallel */
    static private final boolean NEG_SEQ_ALLOW =
                false;

    public static Term parallel(int dt, TmpTermList t, boolean sort, TermBuilder B) {
        //assert(dt!=XTERNAL);
        var n = t.size();
        return switch (n) {
            case 0 -> True;
            case 1 -> t.getFirst();
            default -> parallelN(dt, t, sort, B, n);
        };
    }

    private static Term parallelN(int dt, TmpTermList t, boolean sort, TermBuilder B, int n) {
        if (dt == 0)
            dt = DTERNAL; //HACK

        //if (t.containsInstance(Null)) return Null;
        //if (t.containsInstance(False)) return False;
        var seqCount = 0;
        //int implCount = 0;
        var fals = false; //deferred, for the opportunity to detect Null's first
        for (var tt : t) {
            if (tt == Null) return Null;
            else if (tt == False) fals = true;

            if (tt.unneg().SEQ()) {
                if (!NEG_SEQ_ALLOW && tt instanceof Neg)
                    return Null;
                seqCount++;
            }
        }
        if (fals) return False;
        if (seqCount > SEQ_MAX) return Null;

        return NAL.CONJ_BOOL ? conjBool(dt, t, sort, B, n) : conjRaw(dt, t, sort, B);
    }

    public static Term conjRaw(int dt, TmpTermList t, boolean preCommute, TermBuilder B) {
        return B.compound(CONJ, DTERNAL, preCommute, t.arrayTake());
    }

    private static Term conjBool(int dt, TmpTermList t, boolean preCommute, TermBuilder B, int n) {
        if (NAL.term.INH_BUNDLE) {
            var e = t._inhExplode(true, B);
            if (e == -1)
                return False;
            else if (e == 1)
                n = t.size();
        }

        if (n == 2 && dt == DTERNAL && !t.hasAny(CONJ)) {
            var p2 = parallel2Simple(t, preCommute, B); //fast 2-ary tests
            if (p2 != null) return p2;
        }

        var d = disjunctive(t, dt, B);
        if (d != null) {
            t.clear();
            t.addFast(d); //HACK
        }

        var xt = xternalDistribute(dt, t, B);
        if (xt != null) return xt;

        return parallel(t, dt, B);
    }

    @Nullable private static Term parallel2Simple(TmpTermList t, boolean sort, TermBuilder B) {
        Term a = t.get(0), b = t.get(1);
        if (a == True) return b;
        if (b == True) return a;

        if (a instanceof Neg != b instanceof Neg) {
            if (a.equalsNeg(b)) return False;
        } else {
            if (a.equals(b)) return a;
        }

        return simple(a) && simple(b) ?
                conjRaw(DTERNAL, t, sort, B)
                :
                null;
    }

    private static boolean simple(Term a) {
        return a instanceof Atomic || !a.hasAny(Temporals);
    }

    private static @Nullable Term xternalDistribute(int dt, TmpTermList xx, TermBuilder builder) {
        //HACK complex XTERNAL disj pre-filter
        //TODO multiarity comparison
        var n = xx.size();

        for (var a = 0; a < n; a++) {
            var A = xx.get(a);
            if (A instanceof Neg) {
                var ux = A.unneg();
                if (ux.CONJ() && ux.dt() == XTERNAL) { //TODO bitset
                    for (var b = a + 1; b < n; b++) {
                        var B = xx.get(b);
                        var uux = ux.subterms();
                        if (uux.hasAll(B.unneg().struct())) {
                            Term xn = null;
                            for (var v : uux) {
                                if (v instanceof Neg) {
                                    var vu = v.unneg();
                                    if (vu.CONJ()) {
                                        if (xn == null) xn = B.neg();
                                        if (vu.containsRecursively(xn)) {
                                            //TODO test the xternal's components as if they werent xternal to see if becomes False, if so then False
                                            var r = builder.conj(/*dt,?*/ v, B);
                                            if (r.equals(B)) {
                                                return False; //because it will be negated and --X && X == False
                                            }/* else if (r.equalsNeg(xx.get(1))) {
                                        return True;
                                    }*/
                                        }
                                    }
                                }

                                if (v.equals(B)) {
                                    //eliminate
                                    var aa = A
                                        .replace(v.neg(), False)
                                        .replace(v, True);
                                    if (aa == False || aa == Null)
                                        return aa;
                                    xx.setFast(a, aa);
                                    return builder.conj(dt, xx);
                                }

                            }
                        }
                    }
                }
            }
        }

        var xternalCount = 0;
        var lastXternal = -1;
        for (var i = 0; i < n; i++) {
            var t = xx.get(i);
            if (t.CONJ() && t.dt() == XTERNAL) {
                lastXternal = i;
                xternalCount++;
            }
        }

        return xternalCount == 1 ?
                distribToXternal(dt, xx, builder, lastXternal) : null;
    }

    @Nullable
    private static Term distribToXternal(int dt, TmpTermList xx, TermBuilder B, int lastXternal) {
        //distribute to xternal components
        var x = xx.remove(lastXternal);
        //if (xx.isEmpty()) return x;

        // y = ArrayUtil.remove(xx, lastXternal);
        Term Y;
        if (xx.subs()==1)
            Y = xx.sub(0);
        else {
            if (dt!=DTERNAL) {
                xx.add(lastXternal, x);
                return null; //HACK?
            }
            Y = parallel(DTERNAL, new TmpTermList(xx) /* clone HACK */, true, B);
        }
        if (Y.unneg().TEMPORAL_VAR()) {
            xx.add(lastXternal, x);
            return null; //internal xternal
        }

        if (Y == Null) return Null;
        if (Y == False) return False;
        if (Y == True) return x;
        var xs = x.subterms();
        var ys = Util.map(xxx -> B.conj(dt, xxx, Y),
                new Term[xs.subs()], xs.arrayShared());

        //TODO factor XTERNAL like DTERNAL
//        if (xx.subs()>0 && ys.length==2) {
//            Term ys0 = ys[0], ys1 = ys[1];
//            if (ys0.CONDS() && ys1.CONDS()) {
//                if ((((Compound) ys0).condOf(xs.sub(0)) && ((Compound) ys0).condOf(Y))) {
//                    if ((((Compound) ys1).condOf(xs.sub(1)) && ((Compound) ys1).condOf(Y))) {
//                        //leave distributed, as nothing was reduced
//                        xx.add(x); //HACK restore
//                        return null;
//                    }
//                }
//            }
//        }

        return B.conj(XTERNAL, ys);
    }

    @Deprecated public static @Nullable Term disjunctive(TmpTermList X, int dt, TermBuilder B) {
        var xxn = X.size();
        if (xxn < 2) return null;

        @Deprecated MetalBitSet disjPar = null, disjSeq = null;
        for (var i = 0; i < xxn; i++) {
            var x = X.get(i);
            if (x instanceof Neg) {
                var xu = x.unneg();
                if (xu.CONDS()) {
                    var xdt = xu.dt();
                    if (xdt == DTERNAL || xdt == 0)
                        ((disjPar == null) ? (disjPar = bits(xxn)) : disjPar).set(i);
                    else if (xdt != XTERNAL)
                        ((disjSeq == null) ? (disjSeq = bits(xxn)) : disjSeq).set(i);
                }
            }
        }

        return disjPar == null && disjSeq == null ? null :
            _disjunctive(X, dt, disjPar, disjSeq, B);

    }

    /**
     * BAD
     * TODO simplify
     */
    @Nullable private static Term _disjunctive(TmpTermList X, int dt, MetalBitSet disjPar, MetalBitSet disjSeq, TermBuilder B) {
        var x = X.array();
        var xxn = X.size();

        var changedOuter = false;
        //foreach disjunction:
        outer: for (var j = xxn - 1; j >= 0; j--) {
            if ((disjSeq == null || !disjSeq.test(j)) && (disjPar == null || !disjPar.test(j)))
                continue;

            var ND = x[j]; assert(ND instanceof Neg);
            var D = (Compound) ND.unneg(); assert(D.CONDS());

            //compare sub-events with sibling events
            ConjList dd = null;
            var changedInner = false;
            var ds = D.struct();
            for (var i = xxn - 1; i >= 0; i--) {
                if (i == j) continue;

                var xi = x[i];
                if (hasAll(ds, xi.unneg().struct() & ~CONJ.bit)) { //HACK bundled events will fail impossibleSubterm
                //if (!D.impossibleSubTerm(xi)) {
                    if (dd == null) {
                        dd = ConjList.conds(D, true, false);
                        dd.inhExploded(B);
                    }

                    //par -> seq contradiction
                    //TODO is this the right place to test for this?
                    if (xi.CONDS()) {
                        if (((Compound) xi).condsOR(dd::contains, true, true))
                            return False;
                    }


                    if (dd.containsNeg(xi)) {
                        x[j] = True; //eliminated
                        changedOuter = true;
                        continue outer;
                    }

                    changedInner |= dd.removeAll(xi);
                }
            }
            if (changedInner) {

                var E = (D.dt() == XTERNAL ? B.conj(XTERNAL, dd.toArrayRecycled()) : dd.term(B)).neg();
                if (E == Null) return Null;
                if (E == False) return False;

                changedOuter = true;
                x[j] = E;
            }
            if (dd!=null)
                dd.close();
        }

        return __disjunctive1(X, dt, disjPar, disjSeq, B, changedOuter, xxn, x);
    }

    private static @Nullable Term __disjunctive1(TmpTermList X, int dt, MetalBitSet disjPar, MetalBitSet disjSeq, TermBuilder B, boolean changedOuter, int xxn, Term[] x) {
        if (changedOuter) {
            X.removeInstances(True);
            return switch (X.subs()) {
                case 0 -> True;
                case 1 -> X.sub(0);
                default -> parallel(dt, new TmpTermList(X) /*X*/, true, B); //changed, restart
            };
        }

        if (disjPar == null) {
            int seqs;
            if (disjSeq != null) {
                seqs = disjSeq.cardinality();

                if (seqs > 1) {
                    //TODO if seqs < n and seqs > 1
                    //TODO n>2
                    //see if common prefix or suffix exists
                    if (xxn == 2 && seqs == xxn) {
                        var ss = new ConjList[seqs];

                        //TODO optimization: if all have 2 events, they need to have equal eventRange otherwise it wont work

                        var minEvents = Integer.MAX_VALUE;
                        var sj = 0;
                        for (var i = 0; i < xxn; i++) {
                            if (disjSeq.test(i))
                                minEvents = Math.min((ss[sj++] = ConjList.conds(x[i].unneg(),  false, false)).size(), minEvents);
                        }
                        //boolean fwd = true; /*fwd only*/
                        //for (boolean fwd : new boolean[]{true, false}) {
                        var shared = 0;
                        matching:
                        while (shared < minEvents) {
                            var w = TIMELESS;
                            Predicate<Term> z = null;
                            for (var i = 0; i < sj; i++) {
                                var ssi = ss[i];
                                var index = //fwd ?
                                        shared;
                                        //: (ssi.size() - 1) - shared;
                                if (i == 0) {
                                    w = ssi.when(index);
                                    z = ssi.get(index).equals();
                                } else {
                                    if (ssi.when(index) != w) {
                                        shared--;
                                        break matching; //interval has now changed
                                    }
                                    if (!z.test(ssi.get(index)))
                                        break matching;
                                }
                            }
                            shared++;
                        }
                        if (shared > 0) {
                            var s0s = ss[0].size();
                            if (shared >= s0s) {
                                if (NAL.DEBUG)
                                    throw new TODO(); //?? what does it mean
                                return Null;
                            }
                            var common = (//fwd ?
                                    ss[0].subEvents(0, shared)
                                    //: ss[0].subEvents(s0s - shared, s0s)
                            ).term(B);
                            var st = new Term[sj];
                            var offset = Tense.occToDT(ss[0].when(shared));
                            for (var i = 0; i < sj; i++) {
                                var ssi = ss[i];
                                /*if (fwd)*/ ssi.removeBelow(shared);
                                //else ssi.removeAbove(shared);
                                st[i] = ssi.term(B);
                            }

                            for (int i = 0, ssLength = ss.length; i < ssLength; i++) {
                                ss[i].delete(); ss[i] = null;
                            }

                            var orred = DISJ(st);
//                                if (!fwd && orred.unneg().seqDur() != 0)
//                                    Util.nop(); //TEMPORARY
                            return (//fwd ?
                                    ConjSeq.conjAppend(common, offset - common.seqDur(), orred, B)
                                    //:
                                    //ConjSeq.conjAppend(orred, offset - orred.seqDur(), common, B)
                            ).neg();
                            //TODO combine with other non-seq terms
                        }
                    }
                }
            }
            return null;
        }

        var d = disjPar.cardinality();
        return d <= 1 ? null : __disjunctive2(X, dt, disjPar, B, d, xxn, x);

    }

    private static @Nullable Term __disjunctive2(TmpTermList X, int dt, MetalBitSet disjPar, TermBuilder B, int d, int xxn, Term[] x) {
        var dxx = new Term[d];
        var od = 0;
        TmpTermList cxx = null;//d != n ? new Term[n - d + 1 /* for extra slot */] : null;  int oc = 0; //conjunctive component
        ObjectByteHashMap<Term> i = null;
        var anyFull = false;
        for (var j = 0; j < xxn; j++) {
            var J = x[j];
            if (disjPar.test(j)) {
                var dc = dxx[od++] = J;
                for (var ct : ConjList.conds(dc.unneg())) {
                    Term ctn;
                    if (i != null && i.containsKey(ctn = ct.neg())) {
                        //disqualify both permanently since factoring them would cancel each other out
                        i.put(ct, Byte.MIN_VALUE);
                        i.put(ctn, Byte.MIN_VALUE);
                    } else {
                        if (i == null) i = new ObjectByteHashMap<>(d);
                        anyFull |= (d == i.updateValue(ct, (byte) 0, v -> (v >= 0) ? (byte) (v + 1) : v));
                    }
                }
            } else {
                if (cxx == null) cxx = new TmpTermList(xxn - j);
                cxx.addFast(J);
            }
        }
        if (!anyFull)
            return null;

        i.values().removeIf(b -> b < d);
        if (!i.isEmpty()) {
            var common = i.keySet();
            var factor = common.size() > 1 ? B.conj(common.toArray(EmptyTermArray)) : common.iterator().next();
            if (factor == Null) return Null;
            if (factor == False) return False;

            if (cxx != null) {
                if (cxx.contains(factor))
                    factor = True;
                else if (cxx.containsNeg(factor))
                    return _conj(cxx, dt, B);
            }

            var commonMissing = ((Predicate<Term>) common::contains).negate();

            var mm = dxx.length;
            for (var m = 0; m < mm; m++) {

                var xxj = dxx[m].unneg().subterms().subs(commonMissing);

                if (xxj.length != 0) {
                    if ((dxx[m] = (xxj.length == 1 ? xxj[0] : B.conj(xxj)).neg()) == False)
                        break; //eliminated
                } else {
                    dxx[m] = False; //eliminated
                    break;
                }
            }
            if (X.equalTerms(dxx))
                return null;
            var disj = B.conj(dxx).neg();
            if (disj == Null)
                return Null;

            var yd = B.conj(dt, factor, disj).neg();
            if (yd != Null && cxx != null) {
                cxx.add(yd);
                return _conj(cxx, dt, B);
            }
            return yd;
        }
        return null;
    }

    private static Term _conj(TmpTermList c, int dt, TermBuilder B) {
        return c.subs() == 1 ? c.getFirst() : B.conj(dt, c);
    }

    public static Term parallel(TmpTermList pn, int dt, TermBuilder B) {
        return switch (pn.size()) {
            case 0 -> True;
            case 1 -> pn.getFirst();
            default -> complexPar(pn) ?
                    parallelComplex(pn, dt, B) :
                    parallelSimple(pn, B);
        };
    }

    private static Term parallelComplex(TmpTermList pn, int dt, TermBuilder B) {
        try (var c = new ConjTree()) {
            return c.parallelComplex(pn, dt, B);
        }
    }

    static Term parallelSimple(TmpTermList pn, TermBuilder B) {
        var n = pn.subs();

        if (n > 1) {
            pn.sortThis();

            if (NAL.term.INH_BUNDLE) {
                Term y = null;
                boolean bundled;
                if (bundled = ConjBundle.inhBundle(pn.array(), null, n, B)) {
                    if (pn.containsInstance(Null)) {
                        y = Null;
                    } else if (pn.containsInstance(False)) {
                        y = False;
                    } else {
                        pn.removeInstances(True);
                    }
                    n = pn.subs();
                }
                if (y == null && n > 1 && complexPar(pn)) {
                    _flatten(pn);
                    y = parallel(DTERNAL, pn,
                            bundled /* re-sort if bundled changed anything */,
                            /*prevents cycle*/ B);
                    n = pn.subs();
                }
                if (y != null)
                    return y;
            }
        }

        return switch (n) {
            case 0 -> True;
            case 1 -> pn.getFirst();
            default -> B.compoundNew(CONJ, DTERNAL, pn.arrayTake());
        };
    }

    /** HACK flatten any inner parallels */
    @Deprecated private static void _flatten(TmpTermList pn) {
        var pnExtra = new TmpTermList();
        pn.removeIf(z -> {
            if (z.CONJ() && z.dt()==DTERNAL && !z.SEQ()) {
                pnExtra.addAll(z.subtermsDirect());
                return true;
            }
            return false;
        });
        if (!pnExtra.isEmpty()) {
            pn.addAll((Subterms)pnExtra);
            pnExtra.delete();
        }
    }

    static boolean complexPar(TmpTermList PN) {
        boolean hasP = false, hasN = false;
        for (int i = 0, n = PN.size(); i < n; i++) {
            var x = PN.sub(i);
            if (x instanceof Neg) hasN = true;
            else hasP = true;

            if ((hasN && hasP) || x.unneg().CONJ())
                return true;
        }
        return false;
    }
}
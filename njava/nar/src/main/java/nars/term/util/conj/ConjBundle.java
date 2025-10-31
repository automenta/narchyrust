package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.Lst;
import jcog.data.map.ObjShortHashMap;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TmpTermList;
import nars.term.Neg;
import nars.term.builder.TermBuilder;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.api.tuple.primitive.ObjectShortPair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.subterm.Subterms.array;
import static nars.term.atom.Bool.*;

/** inh-conj NAL3..NAL6 integration */
public enum ConjBundle { ;

    /** @param xx possibly modified */
    @Nullable
    private static Term inhBundle(TmpTermList xx, TermBuilder B) {
        int n = xx.size(); assert n > 1;

        return n == 2 ? inhBundle2(xx.get(0), xx.get(1), B) : inhBundleN(xx, B, n);
    }

    private static final Comparator<ObjectIntPair<Term>> inhBundleSort =
            Comparator.comparingInt((ObjectIntPair<Term> i) -> -Math.abs(i.getTwo())).thenComparing(ObjectIntPair::getOne).thenComparing(ObjectIntPair::getTwo);

    @Nullable
    private static Term inhBundleN(TmpTermList x, TermBuilder b, int n) {

        Lst<ObjectIntPair<Term>> sp = members(x, n);
        if (sp == null) return null;

        TmpTermList all = new TmpTermList(4);

        MutableSet<Term> components = null;
        for (ObjectIntPair<Term> j : sp) {
            int xxxn = x.size();
            if (xxxn < 2)
                break; //done
            if (components != null)
                components.clear();
            Term jj = j.getOne();  assert !(jj instanceof Neg);
            int subjOrPred =
                    j.getTwo() > 0 ? 0 : 1;
                    //jj instanceof Neg ? 1 : 0;
            //jj = jj.unneg();
            Predicate<Term> jjEq = jj.equals();
            for (Term xxxi : x) {
                Subterms xxi = xxxi.unneg().subterms();
                if (jjEq.test(xxi.sub(subjOrPred))) {
                    if (components != null) {
                        if (components.anySatisfy(xxxi::equalsNeg))
                            return False; //contradiction detected
                    } else {
                        components = new UnifiedSet<>(xxxn);
                    }
                    components.add(xxxi);
                }
            }
            if (components == null || components.size() <= 1)
                continue;

            Term bc2 = bc2(x, components, subjOrPred, b);

            Term cc = subjOrPred == 0 ?
                    b.inh(jj, bc2) :
                    b.inh(bc2, jj)
                    //: INH.the(B, Op.DISJ(B, c2.toArray(Op.EmptyTermArray)), jj)
                    ;
            if (cc == False || cc == Null)
                return cc;
            if (cc != True)
                all.add(cc);
        }
        all.addAll((Subterms)x);
        all.commuteThis();

        if (all.size()==1)
            return all.getFirst();
        else
            return b.compoundNew(CONJ, DTERNAL, array(all));
            //ConjPar.parallel(DTERNAL, all, true, false, b);
            //b.conj(all);
    }

    @Nullable
    private static Lst<ObjectIntPair<Term>> members(TmpTermList xx, int n) {
        var y = counts(xx, n);
        if (y == null) return null;
        int sps = y.size();
        if (sps == 0)  return null;
        if (sps > 1) y.sort(inhBundleSort);
        return y;
    }

    @Nullable
    private static Lst<ObjectIntPair<Term>> counts(TmpTermList xx, int n) {
        ObjShortHashMap<Term> counts = new ObjShortHashMap<>(n);
        for (int i = 0; i < n; i++) {
            Term xi = xx.get(i).unneg(); //assert xi.INH();
            Subterms xis = xi.subtermsDirect();
            populate(counts, xis, 0);
            populate(counts, xis, 1);
        }

        Lst<ObjectIntPair<Term>> y = null;
        for (ObjectShortPair<Term> e : counts.keyValuesView()) {
            Term k = e.getOne();
            int c01 = e.getTwo();
            int c0 =  c01       & 0xff; if (c0 > 1) y = populate(k, +c0, y);
            int c1 = (c01 >> 8) & 0xff; if (c1 > 1) y = populate(k, -c1, y);
        }
        //counts.clear();
        return y;
    }

    private static Lst<ObjectIntPair<Term>> populate(Term k, int c0, Lst<ObjectIntPair<Term>> sp) {
        if (sp == null) sp = new Lst<>(2); /* TODO estimate better */
        sp.add(PrimitiveTuples.pair(k, c0));
        return sp;
    }

    private static void populate(ObjShortHashMap<Term> counts, Subterms sp, int sub) {
        Term x = sp.sub(sub);
        short c = counts.getIfAbsent(x, (short)0);
        counts.put(x, (short) (sub == 0 ?
            (c & 0xff00 | (c & 0xff) + 1) :
            (c & 0x00ff | ((c >> 8) + 1) << 8)));
    }

    /** simple case */
    @Nullable private static Term inhBundle2(Term aa, Term bb, TermBuilder B) {
        Subterms a = aa.unneg().subterms(), b = bb.unneg().subterms();

        Term as = a.sub(0);
        Term ap = a.sub(1);
        Term bs = b.sub(0);
        Term bp = b.sub(1);
        if (ap.equals(bp))
            return B.inh(B.conj(as.negIf(aa instanceof Neg), bs.negIf(bb instanceof Neg)), ap);

        if (as.equals(bs))
            return B.inh(as, B.conj(ap.negIf(aa instanceof Neg), bp.negIf(bb instanceof Neg)));

        return null;
    }

    private static Term bc2(TermList t, MutableSet<Term> components, int subjOrPred, TermBuilder b) {
        TmpTermList c = new TmpTermList(components.size());
        for (Term z : components)  {
            t.removeInstances(z);
            c.addFast(z.unneg().sub(1 - subjOrPred).negIf(z instanceof Neg));
        }
        return b.conj(c);
    }


    /** @param items possibly modified
     *  @param when if null, assumes they occurr at the same time (parallel)
     */
    static boolean inhBundle(Term[] items, @Nullable long[] when, int s, TermBuilder B) {
        if (s < 2) return false;

        MetalBitSet ii = null;
        for (int i = 0; i < s; i++) {
            if (items[i].unneg().INH())
                (ii==null ? ii = MetalBitSet.bits(s) : ii).set(i);
        }
        if (ii == null || ii.cardinality() < 2) return false;

        long t = when!=null ? when[0] : TIMELESS;
        int tt = 0;
        boolean changed = false;
        for (int i = 1; i <= s; i++) {
            long wi;
            if (when!=null) {
                wi = i < s ? when[i] : TIMELESS;
                if (wi == t)
                    continue; //keep accumulating until time changes
            } else {
                if (i != s) continue;
                wi = TIMELESS;
            }

            if (i - tt >= 1) {

                int inh = ii.cardinality(tt, i);
                if (inh > 1) {
                    TmpTermList k = new TmpTermList(inh);
                    for (int j = tt; j < i; j++) if (ii.test(j)) k.add(items[j]);
                    Term z = inhBundle(k, B);

                    //substitute result back into the event list at the first subsumed event,
                    // and replace other subsumed events with True
                    if (z!=null /*&& !(z instanceof Bool)*/) {
                        for (int j = tt; j < i; j++) {
                            if (ii.test(j)) {
                                items[j] = z;
                                z = True; //replace remaining with True
                                changed = true;
                            }
                        }
                    }
                }
            }
            t = wi;
            tt = i;
        }
        return changed;
    }

//    public static void assertReduced(Term y) {
//        //TEMPORARY
//        if (y instanceof Compound) {
//            y.ORrecurse(z -> true, (a,s)->{
//                if (a.CONJ() && a.dt()==DTERNAL && a.subterms().hasAny(INH)) {
//                    Term[] i = a.subterms().arrayClone();
//                    if (i.length > 1) {
//                        Term b = SimpleHeapTermBuilder.the.conj(i);
//                        if (!b.equals(a))
//                            throw new WTF();
//                    }
//                }
//                return true;
//            }, null);
//        }
//    }

    @Deprecated public static boolean bundled(Term x) {
//        if (!NAL.term.INH_BUNDLE)
//            return false;
        Subterms xx;
        return x.INH() && (
           (xx = (x.subtermsDirect()))
                .subOpID(0)==CONJ.id
           || xx.subOpID(1)==CONJ.id
       );
    }

    public static boolean events(Term x, Consumer<Term> each) {
        return eventsAND(x, xx->{
            each.accept(xx);
            return true;
        });
    }

    public static boolean eventsOR(Term x, Predicate<Term> each) {
        return events(x, each, false);
    }
    public static boolean eventsAND(Term x, Predicate<Term> each) {
        return events(x, each, true);
    }

    private static boolean events(Term x, Predicate<Term> each, boolean andOrOR) {
        assert(bundled(x/*.unneg()*/));
//        if (!bundled(x/*.unneg()*/))
//            return each.test(x);

        //HACK TODO without constructing ConjList
        try (ConjList l = new ConjList(1 + eventsEstimate(x))) {
            l.addEvent(0, x);
            l.inhExplode();
            return andOrOR ? l.AND(each) : l.OR(each);
        }
    }

    private static int eventsEstimate(Term x) {
        return Math.max(inhSubEvents(x.sub(0)), inhSubEvents(x.sub(1)));
    }

    private static int inhSubEvents(Term a) {
        return a.CONJ() ? a.subs() : 0;
    }

    public static TermList events(Term y) {
        TermList e = new TermList(2 /* TODO better estimate */);
        e.add(y);
        e.inhExplode(false);
        return e;
    }

    public static boolean containsBundledPossibly(Term e) {
        return e.hasAny(INH) && hasAny(e.structSubs(), CONJ);
    }
}
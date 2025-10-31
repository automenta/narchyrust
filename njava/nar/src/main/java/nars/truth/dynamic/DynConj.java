package nars.truth.dynamic;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.util.ObjectLongLongPredicate;
import nars.NALTask;
import nars.Term;
import nars.TruthFunctions;
import nars.term.Compound;
import nars.term.atom.Bool;
import nars.term.util.conj.ConjList;
import nars.term.util.conj.ConjTree;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.truth.DynTaskify;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static nars.Op.*;

public class DynConj extends DynSect {

    private static final boolean decomposeSeqEte =
        false;
        //true;

    public static final DynConj Conj = new DynConj();

    public static final DynConj Disj = new DynConj() {
        @Override protected TruthFunctions truthFn() {
            return TruthFunctions.Union;
        }
    };

    private DynConj() {}

    @Override
    public boolean projectComponents(Term template) {
        return false;
    }

    private static void occ(DynTaskify d) {
        var s = d.earliestStart();
        d.occ(s, s + d.rangeMin());
    }

    @Override
    public final Term recompose(Compound superterm, DynTaskify d) {
        occ(d);

        var ditherDT = d.timeRes;
        var negComponents = false;

        //var c = new ConjTree();
        var c = d.size() > 2 ? new ConjTree() : new ConjList();
        for (var t : d)
            if (!c.add(when(t, ditherDT), t.term().negIf(negComponents))) //TODO c.addEventNeg
                return null;
        return c.term().negIf(false);
    }

    private static long when(NALTask x, int ditherDT) {
        var is = x.start();
        return ditherDT>1 ? Tense.dither(is, ditherDT) : is;

//        if (!x.term().unneg().SEQ() && (is <= ds && dither(
//                x.end()
//                //is0 + ix.seqDur()
//                , ditherDT) >= de))
//            return ETERNAL;  //condition spans entire conj

        //return is == ETERNAL  ? 0 /* HACK */ : is;
    }


    @Override
    public boolean decompose(Compound c, long start, long end, ObjectLongLongPredicate<Term> each) {

        try (var cc = new ConjList()) {

            var dt = c.dt();
            var dtx = dt == XTERNAL;
            var ete = !c.SEQ();

            var ss = start==ETERNAL ? 0 : start;
            if (dtx || ete)
                cc.addAllDirect(ss, c);
            else
                decomposeSeqN(c, ss, cc);

            if (c.hasAny(VAR_DEP) && !pairVars(cc))
                return false; //give up

            if (cc.size() < 2) {
                assert (false);
                return false;
            }

            return dtx || ete ?
                decomposePar(cc, start, end, each) :
                decomposeSeq(cc, end - start, each);
        }
    }

    private static boolean decomposePar(ConjList c, long s, long e, ObjectLongLongPredicate<Term> each) {
        var x = c.array();
        var n = c.size();
        for (var i = 0; i < n; i++) {
            if (!each.accept(x[i], s, e))
                return false;
        }
        return true;
    }

    /**
     * n-ary sequence decompose
     */
    private static void decomposeSeqN(Compound conj, long start, ConjList c) {
        c.addConds(start, conj, decomposeSeqEte, false, false, null);
    }

    private static boolean decomposeSeq(ConjList c, long range, ObjectLongLongPredicate<Term> each) {
        var cw = c.when;
        var ct = c.array();
        var n = c.size();
        for (var i = 0; i < n; i++) {
            var w = cw[i];
            long s, e;
            if (w != ETERNAL) {
                s = w;
                e = w + range;
            } else {
                s = c.whenEarliest();
                e = s == ETERNAL ? ETERNAL : c.whenLatest() + range;
            }
            if (!each.accept(ct[i], s, e))
                return false;
        }
        return true;
    }

//    private static boolean eventsXternal(ConjList cc, Subterms c, long start, long end) {
//        if (c.subs() != 2 /*|| !c.sub(0).equalsPN(c.sub(1))*/)
//            return false;
//
//        //random temporal order for fairness
//        //				Random rng =
//        //					ThreadLocalRandom.current();
//        //					//d.random()
//        //				int z = rng.nextInt(2);
//        int z =
//                ThreadLocalRandom.current().nextInt(2);
//        //Math.abs(Util.hashCombine(System.identityHashCode(c), System.identityHashCode(conj))) & 1; //HACK GOOD
//        //Util.hashCombine(System.identityHashCode(c), System.identityHashCode(conj)) & 1; //HACK GOOD
//
//        cc.ensureCapacity(2);
//        cc.addDirect(start, c.sub(z));
//        cc.addDirect(end, c.sub(1 - z));
//        return true;
//    }


//		@Nullable
//		private Compound evalEternalComponents(Compound conj, long start, long end, ObjectLongLongPredicate<Term> each) {
//			Compound seq = null;
//			int range = end == Op.ETERNAL ? 0 : conj.eventRange();
//			for (Term x : conj) {
//				if (x.SEQUENCE()) {
//					assert (seq == null) : "only one temporal in factored seq";
//					seq = (Compound) x;
//				} else {
//					if (!each.accept(x, start, end + range))
//						return null;
//				}
//			}
//			return seq;
//		}


    /**
     * (special case)
     * variable (#) events that need paired with other non-variable events before evaluating
     * TODO improve this
     */
    private static boolean pairVars(ConjList c) {
        var n = c.size();
        if (n < 2) return false;
        var vars = pairVarsBits(c, n);
        if (vars == null)
            return true; //no variable events

        Random rng = null;
        for (var x = 0; x < n; x++) {
            if (!vars.test(x))
                continue;
            var X = c.get(x);
            var wX = c.when(x);
            if (wX == ETERNAL)
                return false; //TODO attach to all non-eternal events.


            //choose random other event to pair with
            //TODO prefer smaller non-conj, non-disj event

            var removed = false;
            if (rng == null) rng = ThreadLocalRandom.current();
            var w = c.when;
            for (var r = 0; r < n * 2; r++) { //max tries
                var y = rng.nextInt(n);
                if (y == x || vars.test(y)) continue;
                var Y = c.get(y);
                if (Y != null) {
                    var wY = w[y];
                    if (wY == ETERNAL)
                        continue;
                    var dt = Tense.occToDT(wX - wY);
                    if (dt==0) dt = DTERNAL; //HACK
//						if (dt == XTERNAL)
//							throw new WTF();
                    var xy = CONJ.the(dt, Y, X);
                    if (!(xy instanceof Bool)) {
                        //vars.clear(x);
                        c.setNull(x);
                        removed = true;
                        c.setFast(y, xy);
                        w[y] += (dt >= 0) ?
                                /* var after */  0 :
                                /* var before */ -dt;
                        break;
                    }
                }
            }
            if (!removed)
                return false; //failed to pair
        }
        c.removeNulls();
        return true;
    }

    private static @Nullable MetalBitSet pairVarsBits(ConjList c, int n) {
        MetalBitSet vars = null;
        for (var i = 0; i < n; i++) {
            if (c.get(i).unneg().VAR_DEP()) {
                if (vars == null) vars = MetalBitSet.bits(n);
                vars.set(i);
            }
        }
        return vars;
    }

    public static boolean condDecomposeable(Term c, boolean event) {
        return condDecomposeable(c, event, Integer.MAX_VALUE);
    }

    public static boolean condDecomposeable(Term c, boolean event, int maxEvents) {
        return c instanceof Compound C
            && c.CONDS()
            && !c.hasAny(VAR_INDEP)
            // && !(c.CONJ() && C.ORunneg(Term::EQ)) //HACK dont try to decompose when sub-event is =
            && _condDecomposeable(C, event, maxEvents);
    }

    /**
     * TODO test for variable independence among the events, not just that there are <=1 variables total.
     */
    @Deprecated
    private static boolean _condDecomposeable(Compound c, boolean event, int maxEvents) {
        return Util.inIncl(new CondDecomposeability(c, event).events, 2, maxEvents);
    }

    private static final class CondDecomposeability implements LongObjectPredicate<Term>, Predicate<Term> {

        private final boolean event;
        int events, vars;

        /** assumes cond.CONDS() */
        CondDecomposeability(Compound cond, boolean event) {
            //assert(cond.CONDS());
            this.event = event;
            var cdt = cond.dt();
            var y = cond.condsAND(this, 0, cdt == DTERNAL, cdt == XTERNAL, true);
            if (!y) events = -1;
        }

        @Override
        public boolean test(Term x) {
            return accept(0, x);
        }

        @Override
        public boolean accept(long when, Term x) {
            x = x.unneg();

            var var = (x instanceof Variable);
            if (!var) {
                if (event && !x.TASKABLE())
                    return false;
            }

            if (var || x.hasVars())
                if (++vars > 1) //depvar
                    return false; //too many vars

            if (!var)
                events++;
            return true;
        }
    }


}
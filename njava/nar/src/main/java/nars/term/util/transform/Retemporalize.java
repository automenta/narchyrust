package nars.term.util.transform;

import nars.Term;
import nars.term.Compound;

import static nars.Op.XTERNAL;


public abstract class Retemporalize extends RecursiveTermTransform {

    public static Term patternify(Term x) {
        return x instanceof Compound ?
            retemporalizeAllToXTERNAL.applyCompound((Compound) x) : x;
    }

    @Override
    public final Term applyCompound(Compound x) {
        return switch (x.op()) {
            case INH, SIM -> x;
            //case INH, SIM -> applyInhSim(x);
            case NEG      -> applyNeg(x);
            case CONJ     -> applyConj(x);
            case IMPL     -> applyImpl(x);
            default       -> x.TEMPORALABLE() ? super.applyCompound(x) : x;
        };
    }

//    /** HACK for keeping inh/sim && subterms as DTERNAL.  otherwise the default behavior would make them XTERNAL */
//    private Term applyInhSim(Compound x) {
//        Term s = x.sub(0), p = x.sub(1);
//        Term ss = s, pp = p;
//        if (!s.CONJ() && s.TEMPORALABLE()) ss = applyCompound((Compound) s);
//        if (!p.CONJ() && p.TEMPORALABLE()) pp = applyCompound((Compound) p);
//        return s != ss || p != pp ? x.op().the(ss, pp) : x;
//    }

//    protected Term applyInhSim(Compound x) {
//        //HACK: skip one layer
//        Term s = x.sub(0), p = x.sub(1);
//        Term S = applyInhSimComponent(s), P = applyInhSimComponent(p);
//        if (s!=S || p!=P)
//            return builder().compound(x.op(), S, P);
//        else
//            return x; //unchanged
//    }
//
//    protected Term applyInhSimComponent(Term s) {
//        return s;
////        return s instanceof Compound ?
////                Retemporalize.retemporalizeAllToDTERNAL.applyCompound((Compound) s)
////                : s;
//    }

    protected abstract Term applyImpl(Compound x);

    protected abstract Term applyConj(Compound x);


    private static final class RetemporalizeAll extends Retemporalize {

        private final int targetDT;

        private RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

//        @Override
//        protected Term applyInhSim(Compound x) {
//            if (targetDT == XTERNAL && x.EVENTCONTAINER()) {
//                //explode and combine as &&+-
//                TermList t = new TermList(1 /* estimate better */);
//                t.addFast(x);
//                t.inhExplode(false);
//                if (t.size() > 1) //HACK why would it remain==1?
//                    t.replaceAll(this);
//                return CONJ.the(XTERNAL, (Subterms)t);
//            }
//
//            return super.applyInhSim(x); //TODO check
//        }

//        @Override
//        protected Term applyInhSimComponent(Term s) {
//            if (targetDT==XTERNAL && s.EVENTCONTAINER())
//                return s;//super.applyCompound((Compound)s); //HACK
//
//            return super.applyInhSimComponent(s);
//        }

        @Override protected Term applyImpl(Compound x) {
            return applyCompound(x, targetDT);
        }

        @Override protected Term applyConj(Compound x) { return applyCompound(x, targetDT); }

    }



    public static final class RetemporalizeFromTo extends Retemporalize {

        private final int from;
        private final int to;

        public RetemporalizeFromTo(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        protected Term applyImpl(Compound x) {
            return applyTemporalCompound(x);
        }

        @Override
        protected Term applyConj(Compound x) {
            return applyTemporalCompound(x);
        }

        private Term applyTemporalCompound(Compound x) {
            int dt = x.dt();
            return applyCompound(x, dt==from ? to : dt);
        }
    }


    //public static final Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    ////    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    public static final Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
//    public static final Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);

}
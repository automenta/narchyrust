package nars.term.compound;

import nars.subterm.Subterms;
import nars.term.Compound;


/**
 * flyweight Compound implementation for non-DTERNAL dt values.
 * wraps a referenced base Compound and caches only the adjusted hash value,
 * referring to the base for all other details.
 * TODO a CachedCompound version of this
 */
public final class LightDTCompound extends SeparateSubtermsCompound {

    /**
     * numeric (target or "dt" temporal relation)
     */
    private final int dt;

    //private final Compound ref;

    public LightDTCompound(Compound base, int dt) {
        this(base.opID(), base.subtermsDirect(), dt);
    }

    public LightDTCompound(int o, Subterms s, int dt) {
		super(o, s); //dt == DTERNAL ? base.hashCode() : Util.hashCombine(base.hashCode(), dt));

        //this.ref = base;

//        if (!(dt == XTERNAL || Math.abs(dt) < Param.DT_ABS_LIMIT))
//            throw new TermException(base.op(), dt, s, "exceeded DT limit");

//        Op op = op();
//        if (NAL.test.DEBUG_EXTRA) {
//
//            assert (dt != DTERNAL);
//
//            int size = s.subs();
//
//            if (op.temporal && (op != CONJ && size != 2))
//                throw new TermException("Invalid dt value for operator", op, dt, s.arrayShared());
//
//            if (dt != XTERNAL && op.commutative && size == 2) {
//                if (sub(0).compareTo(sub(1)) > 0)
//                    throw new RuntimeException("invalid ordering");
//            }
//
//        }

//
//        if (dt != DTERNAL && dt < 0 && op == CONJ && s.subs() == 2) {
//
//            if (s.sub(0).equals(s.sub(1)))
//                dt = -dt;
//        }


        //assert dt == DTERNAL || dt == XTERNAL;// || (Math.abs(dt) < Param.DT_ABS_LIMIT) : "abs(dt) limit reached: " + dt;

        this.dt = dt;

    }

//    @Override
//    public Term the() {
//        throw new TODO();
//    }

//    @Override
//    public boolean the() {
//        //throw new TODO();
//        //return op().the(dt(), arrayShared());
//        return false;
//    }

    @Override
    public int dt() {
        return dt;
    }

}
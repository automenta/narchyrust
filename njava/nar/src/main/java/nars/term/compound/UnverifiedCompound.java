//package nars.term.compound;
//
//import nars.subterm.Subterms;
//import nars.term.Unverified;
//
//import static nars.Op.NEG;
//
//public class UnverifiedCompound extends SeparateSubtermsCompound implements Unverified {
//    final int dt;
//
//    public UnverifiedCompound(byte op, int dt, Subterms subs) {
//        super(op, subs);
//        assert(op!=NEG.id);
//        this.dt = dt;
//    }
//
//    @Override
//    public int dt() {
//        return dt;
//    }
//}

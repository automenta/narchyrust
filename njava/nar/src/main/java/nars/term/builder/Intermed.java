package nars.term.builder;

import jcog.memoize.ByteKeyExternal;
import nars.Op;
import nars.Term;
import nars.io.TermIO;
import nars.subterm.Subterms;

/** interned terms and subterms implementations */
public enum Intermed  { ;


    public abstract static class InternedCompoundByComponents extends ByteKeyExternal {
        public final byte op;
        public final int dt;

        InternedCompoundByComponents(byte o, int dt) {
            super();
            TermIO.writeCompoundPrefix(this.op = o, this.dt = dt, key);
        }

        public abstract Term[] terms();

        public abstract Term sub(int i);
    }

    public static final class InternedCompoundByComponentsArray extends InternedCompoundByComponents {
        final transient Term[] subs;

        public InternedCompoundByComponentsArray(Op o, int dt, Term... subs) {
            super(o.id, dt);
            TermIO.writeSubterms(key, this.subs = subs);
            commit();
        }

        @Override
        public Term sub(int i) {
            return subs[i];
        }

        @Override
        public Term[] terms() {
            return subs;
        }
    }

    public static final class InternedTermArray extends ByteKeyExternal {

        final transient Term[] subs;

        public InternedTermArray(Term[] s) {
            super();
            TermIO.writeSubterms(key, this.subs = s);
            commit();
        }
    }

    public static final class SubtermsKey extends ByteKeyExternal {
        public final Subterms subs;

        public SubtermsKey(Subterms s) {
            super();
            (this.subs = s).write(key);
            commit();
        }
    }

    //    public static final class InternedCompoundByComponentPair extends InternedCompoundByComponents {
//        public final transient Term x;
//        public final transient Term y;
//
//        public InternedCompoundByComponentPair(Op o, int dt, Term x, Term y) {
//            super(o, dt);
//            this.x = x; this.y = y;
//            TermIO.the.writeSubterms(key, x, y);
//            commit();
//        }
//
//        @Override
//        public Term[] subs() {
//            return new Term[] { x, y };
//        }
//    }

//    public static final class InternedCompoundByComponentsSubs extends InternedCompoundByComponents {
//
//        private final Subterms s;
//
//        public InternedCompoundByComponentsSubs(Compound s) {
//            super((byte) s.opID(), s.dt());
//            (this.s = s.subtermsDirect()).write(key);
//            commit();
//        }
//
//        @Override
//        public Term sub(int i) {
//            return s.sub(i);
//        }
//
//        @Override public Term[] terms() {
//            return s.arrayShared();
//        }
//    }

}
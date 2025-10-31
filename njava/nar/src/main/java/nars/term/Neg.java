package nars.term;

import jcog.Is;
import jcog.The;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.term.anon.Intrin;
import nars.term.atom.IntrinAtomic;
import nars.term.compound.LightUnitCompound;
import nars.term.compound.UnitCompound;

import static nars.Op.NEG;

@Is("Negativity_bias") public interface Neg extends Subterms {

    @Override
    default Subterms negated() {
        return new UnitSubterm(sub(0));
    }

    final class NegLight extends LightUnitCompound implements Neg, The {

        public NegLight(Term negated) {
            super(NEG.id, negated);
        }

        @Override
        public Term unneg() { return sub(); }

        @Override
        public Term neg() {
            return sub();
        }

    }

    /** TODO refine */
    final class NegIntrin extends UnitCompound implements The, Neg, CondAtomic {

        public final short sub;

        public NegIntrin(IntrinAtomic i) {
            this(i.intrin());
            this.root = true; /* but not concept */
        }

        public NegIntrin(short sub) {
            super(NEG.id);
            this.sub = sub;
        }

        @Override
        public int complexity() {
            return 2;
        }

        @Override
        public boolean internable() {
            return true;
        }

        @Override
        protected Term sub() {
            return Intrin.term(sub);
        }

        @Override
        public Term unneg() {
            return sub();
        }

        @Override
        public Term neg() {
            return sub();
        }

    }

//    final class NegCached extends SemiCachedUnitCompound implements Neg {
//
//        public NegCached(Term negated) {
//            super(NEG.id, negated);
//        }
//
//    }

}
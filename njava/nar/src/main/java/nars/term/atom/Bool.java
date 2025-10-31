package nars.term.atom;

import jcog.Is;
import jcog.The;
import nars.Op;
import nars.Term;

import static nars.Op.BOOL;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - absolutely nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual target.  not to be confused with Task-level Truth
 *
 *  Implements "Unknown-state logic" (https://en.wikipedia.org/wiki/Ternary_computer)
 */
@Is("Ternary_computer")
public abstract sealed class Bool extends SpecialAtomic {

    @Override
    public final int hashCodeNeg() {
        return neg().hashCode();
    }

    /**
     * absolutely nonsense
     */
    public static final Bool Null = new _Null();
    /**
     * tautological absolute false
     */
    public static final Bool False = new _False();
    /**
     * tautological absolute true
     */
    public static final Bool True = new _True();

//    public static final Term[] False_Array = { False };

    private Bool(String label, byte code) {
        super(label, BOOL.id, code);
    }

    private static non-sealed class _True extends Bool implements The  {

        private _True() {
            super("true", (byte) 1);
        }

        @Override
        public Term neg() {
            return False;
        }

    }

    private static non-sealed class _False extends Bool implements The {

        private _False() {
            super("false", (byte) 0);
        }

        @Override
        public Term unneg() {
            return True;
        }

        @Override
        public Term neg() {
            return True;
        }

    }

    private static non-sealed class _Null extends Bool {

        private _Null() {
            super(String.valueOf(Op.NullSym), ((byte) -1));
        }

        @Override
        public Term neg() {
            return this;
        }

    }
}
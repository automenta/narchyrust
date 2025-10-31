package nars.term.util;

import jcog.signal.meter.SafeAutoCloseable;
import nars.Term;
import nars.term.anon.Anon;

public interface TermCodec<X extends Term, Y extends Term> extends SafeAutoCloseable {

    //Supplier<TermCodec<?,?>> DIRECT = ()->TermCodec.Direct;
    TermCodec<?,?> Direct = new TermCodec<>() {

        @Override
        public Term encode(Term x) {
            return x;
        }

        @Override
        public Term decode(Term y) {
            return y;
        }

        @Override
        public void close() {

        }
    };

    Y encode(X x);
    X decode(Y x);

    default Y encodeSafe(X x) {
        Y y = encode(x);
        if (y.opID() != x.opID())
            throw new TermTransformException(this.toString(), x, y);
        return y;
    }

    class AnonTermCodec extends Anon implements TermCodec<Term,Term> {

        public AnonTermCodec(boolean keepIntrin, boolean keepVar) {
            super();
            this.keepIntrin = keepIntrin;
            this.keepVariables = keepVar;
        }

        @Override
        public Term encode(Term x) {
            return this.put(x);
        }

        @Override
        public Term decode(Term x) {
            return this.get(x);
        }

        @Override
        public void close() {
            this.clear();
        }
    }
}
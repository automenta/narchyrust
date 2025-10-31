package nars.action.transform;


import jcog.WTF;
import nars.Deriver;
import nars.Op;
import nars.Term;
import nars.action.TaskTermTransformAction;
import nars.io.IO;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.atom.BytesAtom;
import nars.term.util.Image;
import nars.term.util.transform.RecursiveTermTransform;
import nars.unify.constraint.StructMatcher;
import nars.unify.constraint.TermMatch;
import org.jetbrains.annotations.Nullable;

/**
 * compound<->dynamic atom abbreviation.
 *
 * TODO support multiple codecs, in use simultaneously
 */
public enum Abbreviate { ;

    private static Atomic abbreviateTerm(Compound x) {
        return x.hasAny(Op.Variables | Op.Temporals) ? null : BytesAtom.atomBytes(x);
    }

    public static @Nullable Term unabbreviateTerm(Term x) {
        return x instanceof BytesAtom ? IO.bytesToTerm(((BytesAtom) x).bytes(), 3 /* op and 2 length bytes */) : null;
    }

    protected abstract static class AbstractAbbreviate extends TaskTermTransformAction {
        final int volMin;

        AbstractAbbreviate(int volMin, int volMax) {
            this.volMin = volMin;

            single();

            iffNot(PremiseTask, new StructMatcher.HasAny(Op.Variables));

            vol(PremiseTask, volMin, volMax);
        }

        @Override
        public Term apply(Term t, Deriver d) {
            Term x = Image.imageNormalize(t.term());
            if (!(x instanceof Compound))
                throw new WTF(); //HACK


            return abbreviate((Compound) x);
        }

        protected abstract Term abbreviate(Compound x);
    }

    protected static class AbbreviateRoot extends AbstractAbbreviate {

        public AbbreviateRoot(/*String prefix,*/ int volMin, int volMax) {
            super(volMin, volMax);
        }

        @Override
        protected @Nullable Term abbreviate(Compound x) {
            return abbreviateTerm(x);
        }
    }

    public static class AbbreviateRecursive extends AbstractAbbreviate  {

        final RecursiveTermTransform transform =new RecursiveTermTransform() {
            @Override
            public Term applyCompound(Compound x) {
                if (x instanceof Neg) return applyNeg(x);

                int v = x.complexity();
                if (v >= volMin) {
                    if (v <= subVolMax) {
                        Term y = abbreviateTerm(x); //terminal
                        if (y != null)
                            return y;
                    }

                    if (v >= volMin + 1)
                        return super.applyCompound(x);
                }

                return x;
            }
        };

        private final int subVolMax;

        public AbbreviateRecursive(/*String prefix,*/ int volMin, int volMax) {
            super(volMin, Integer.MAX_VALUE);
            this.subVolMax = volMax;
        }

        @Override
        protected Term abbreviate(Compound x) {
            return transform.apply(x);
//            Term y = transform.apply(x);
//            return y.equals(x) ? null : y;
        }

    }

    /** unabbreviates abbreviated root terms (not recursively contained) */
    protected static class UnabbreviateRoot extends TaskTermTransformAction {

        public UnabbreviateRoot(/*String prefix,*/) {
            single();
            iff(PremiseTask, TermMatch.Is.is(Op.ATOM));
            //TODO match prefix and/or other features inside the ATOM
        }

        @Override
        public Term apply(Term t, Deriver d) {
            Term xx = t.term();
            return unabbreviateTerm(xx);
        }

    }

    /** unabbreviates abbreviated root terms (not recursively contained) */
    public static class UnabbreviateRecursive extends TaskTermTransformAction {

        static final RecursiveTermTransform transform =new RecursiveTermTransform() {
            @Override
            public Term applyAtomic(Atomic a) {
                Term b = unabbreviateTerm(a);
                return b==null ?
                        a
                        :
                        b;
            }
        };

        public UnabbreviateRecursive(/*String prefix,*/) {
            single();
            iff(PremiseTask, new StructMatcher.HasAny(Op.ATOM));
            //TODO match prefix and/or other features inside the ATOM
            //TODO more specific conditions
        }

        @Override
        public Term apply(Term x, Deriver d) {
            return transform.apply(x);
        }

    }


}
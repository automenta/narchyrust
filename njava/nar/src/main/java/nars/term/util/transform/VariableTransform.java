package nars.term.util.transform;

import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.var.SpecialOpVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;

import static nars.Op.*;


public abstract class VariableTransform extends RecursiveTermTransform {

    @Override
    public final Term applyCompound(Compound x) {
        return x instanceof Neg ? applyNeg(x) : preFilter(x) ? super.applyCompound(x) : x;
    }

    @Override
    public Term applyAtomic(Atomic a) {
        return variable(a) ? applyVariable((Variable)a) : a;
    }

    protected boolean variable(Atomic a) {
        return a instanceof Variable;
    }

    protected abstract Term applyVariable(Variable v);

    public boolean preFilter(Compound x) {
        return x.hasVars();
    }


    private static VariableTransform variableTransformN(Op from, Op to) {
        return new SimpleVariableTransform(from, to);
    }

    /** transforms variables from one type to another */
    private static final class SimpleVariableTransform extends FilteredVariableTransform {

        private final Op to;

        private SimpleVariableTransform(Op from, Op to) {
            this(from.bit, to);
        }

        private SimpleVariableTransform(int fromStructure, Op to) {
            super(fromStructure);
            this.to = to;
        }

        @Override
        protected Term applyVariable(Variable v) {
            return unnormalizedShadow(v, to);
        }

        private static UnnormalizedVariable unnormalizedShadow(Variable v, Op to) {
            //return new UnnormalizedVariable(to, atomic.toString());
            return new SpecialOpVariable(v, to);
        }

    }

    //    private static TermTransform variableTransform1(Op from, Op to) {
//
//        return new OneTypeOfVariableTransform(from, to) {
//            @Override
//            public Term applyAtomic(Atomic atomic) {
//                if (!(atomic instanceof nars.term.var.Variable) || atomic.op() != from)
//                    return atomic;
//                else {
//                    if (atomic instanceof NormalizedVariable) {
//                        //just re-use the ID since the input term is expected to have none of the target type
//                        return NormalizedVariable.the(to, ((NormalizedVariable) atomic).id());
//                    } else {
//                        //unnormalized, just compute the complete unnormalized form
//                        return unnormalizedShadow((Variable)atomic, to);
//                    }
//
//                }
//            }
//        };
//    }

    /**
     * change all query variables to dep vars by use of Op.imdex
     */
    public static final VariableTransform queryToDepVar = variableTransformN(VAR_QUERY, VAR_DEP);
    //    public static final SimpleVariableTransform indepToDepVar = variableTransformN(VAR_INDEP, VAR_DEP);
    public static final VariableTransform indepToQueryVar = variableTransformN(VAR_INDEP, VAR_QUERY);

}
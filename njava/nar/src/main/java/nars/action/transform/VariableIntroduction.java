package nars.action.transform;

import nars.$;
import nars.Deriver;
import nars.Op;
import nars.Term;
import nars.action.TaskTermTransformAction;
import nars.deriver.reaction.Reaction;
import nars.premise.NALPremise;
import nars.premise.SubPremise;
import nars.term.Compound;
import nars.term.control.PREDICATE;
import nars.term.util.var.DepIndepVarIntroduction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.set.MutableSet;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.True;

/**
 * selects via non-self premises, with belief term selecting a potentially repeat subterm to replace */
public class VariableIntroduction extends TaskTermTransformAction {

    private static final boolean
        filterAbstract = true,
        decompose =
            true
            //false
            ;


    /** two stage introduction: an intermediate decomposing NALPremise is created */
    private static final boolean decomposeSubPremise =
            true;
            //false;

    public VariableIntroduction() {
        super();
        volMin/*complexityMin*/(PremiseTask, 3);
        in(PremiseTask, PremiseBelief);
        isNotAny(PremiseBelief, Op.Variables);

        pre(VariableIntroductionPreFilter);
    }

    private static final PREDICATE<Deriver> VariableIntroductionPreFilter = new PREDICATE<>($.p(VariableIntroduction.class.getSimpleName(), "preFilter")) {
        @Override public boolean test(Deriver d) {
            var p = d.premise;
            return componentRepeats(p.from(), p.to());
        }

        @Override
        public float cost() {
            return 0.95f;
        }
    };

    private static boolean possiblyGt1Subterms(Term x, Term y) {
        return x.complexity() > y.complexity() * 2;
    }

    @Override
    protected boolean self() {
        return false;
    }

    @Nullable
    @Override
    public Term apply(Term x, Deriver d) {
        var victim = componentRepeats(x, d.premise.to(), d);
        if (victim == null) return null;

        var y = DepIndepVarIntroduction.the.apply((Compound) x, victim);

        return y == null ? null :
            (filterAbstract && !y.hasAny(Op.AtomicConstant) ? null : y);
    }

    @Nullable private static boolean componentRepeats(Term x, Term y) {
        return repeats(x, y) || (y instanceof Compound Y && decompose && anyRepeats(x, Y));
    }

    @Nullable private Term componentRepeats(Term x, Term y, Deriver d) {
        if (repeats(x, y))
            return y;

        if (decompose && y instanceof Compound Y) {
            var yy = subRepeats(x, Y, d);
            if (yy!=null) {
                if (decomposeSubPremise) {
                    subPremise(d, yy);
                    return null;
                }
                return yy;
            }
        }
        return null;
    }

    private void subPremise(Deriver d, Term yy) {
        var parentPremise = (SubPremise) d.premise;

        var virtualPremise = NALPremise.the(parentPremise.task(), yy, false);
        virtualPremise.parent = parentPremise;

        d.premise = virtualPremise; //HACK
        d.add(new SubSubPremise(parentPremise, virtualPremise));
        d.premise = parentPremise; //restore
    }

    private static boolean anyRepeats(Term x, Compound y) {
        return y.subtermsDirect().recurseSubtermsToSet(~0 & ~Op.Variables)
                .anySatisfy(z -> repeats(x, z));
    }

    private static boolean repeats(Term x, Term y) {
        return possiblyGt1Subterms(x, y) && !x.recurseTermsOrdered(new NoRepeats(y));
    }

    private static @Nullable Term subRepeats(Term x, Compound y, @Nullable Deriver d) {
        var yy = y.subtermsDirect().recurseSubtermsToSet(~0 & ~Op.Variables);
        Predicate<Term> valid = z -> repeats(x, z);
        if (d == null) {
            return yy.anySatisfy(valid) ? True : null;
        } else {
            yy.removeIf(valid.negate());
            return victimChoose(x, yy, d);
        }
    }

    private static Term victimChoose(Term x, MutableSet<Term> victimSubs, Deriver d) {
        return switch (victimSubs.size()) {
            case 0 -> throw new IllegalStateException(); //no introduction possible
            case 1 -> victimSubs.getFirst();
            default -> DepIndepVarIntroduction.the.choose(x, victimSubs.toArray(Op.EmptyTermArray), d.rng);
        };
    }

    /** true if there are no repeats, false if there are (and exits early) */
    private static final class NoRepeats implements java.util.function.Predicate<Term> {
        final java.util.function.Predicate<Term> eqY;
        boolean sawOne;

        public NoRepeats(Term y) {
            eqY = y.equals();
            sawOne = false;
        }

        @Override
        public boolean test(Term s) {
            if (eqY.test(s)) {
                if (sawOne)
                    return false; //sawTwo, cut

                sawOne = true;
            }
            return true;
        }
    }

    private final class SubSubPremise extends SubPremise<Term> {
        private final SubPremise parentPremise;

        private SubSubPremise(SubPremise parentPremise, NALPremise virtualPremise) {
            super(((Term) parentPremise.id).sub(0), virtualPremise);
            this.parentPremise = parentPremise;
        }

        @Override
        public void run(Deriver d) {
            VariableIntroduction.this.run(d);
        }

        @Override
        public @Nullable Reaction reaction() {
            return parentPremise.reaction();
        }
    }
}
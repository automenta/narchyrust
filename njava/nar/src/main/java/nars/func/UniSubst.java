package nars.func;

import nars.$;
import nars.Deriver;
import nars.NAL;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Atom;
import nars.term.util.TermException;
import nars.term.util.transform.InlineFunctor;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;

/**
 * substituteIfUnifies....(target, varFrom, varTo)
 * <p>
 * <patham9_> for A ==> B, D it is valid to unify both dependent and independent ones ( unify(A,D) )
 * <patham9_> same for B ==> A, D     ( unify(A,D) )
 * <patham9_> same for <=> and all temporal variations
 * <patham9_> is this the general solution you are searching for?
 * <patham9_> for (&&,...) there are only dep-vars anyway so there its easy
 * <sseehh> i found a solution for one which would be to make it do both dep and indep var
 * <sseehh> like you said
 * <sseehh> which im working now, making it accept either
 * <patham9_> ah I see
 * <sseehh> i dont know if the others are solved by this or not
 * <patham9_> we also allow both in 2.0.x here
 * <sseehh> in all cases?
 * <sseehh> no this isnt the general solution i imagined would be necessary. it may need just special cases always, i dunno
 * <sseehh> my dep/indep introducer is general because it isnt built from any specific rule but operates on any target
 * <patham9_> the cases I mentioned above, are there cases that are not captured here?
 * <sseehh> i dont know i have to look at them all
 * <sseehh> i jus tknow that currently each one is either dep or indep
 * <sseehh> and im making the first one which is both
 * <sseehh> and if this works then ill see if the others benefit from it
 * <patham9_> yes it should allow both here anyway
 * <sseehh> i hope its the case that they all can be either
 * <patham9_> unify("$") also allows unify("#") but not vice versa
 * <patham9_> thats what we also had in 1.7.0
 * <sseehh> so you're syaing anywhree i have substituteIfUnifiesDep i can not make both, but anywhere that is substituteIfUnifiesIndep i can?
 * <sseehh> or that they both can
 * <patham9_> yes thats what I'm saying
 * <sseehh> k
 * <patham9_> substituteIfUnifiesIndep  is always used on conditional rules like the ones above, this is why unifying dep here is also fine here
 * <patham9_> for substituteIfUnifiesDep there has to be a dependent variable that was unified, else the rule application leads to a redundant and weaker result
 * <patham9_> imagine this case: (&&,<tim --> cat>,<#1 --> animal>).   <tim --> cat>.   would lead to <#1 --> animal>  Truth:AnonymousAnalogy altough no anonymous analogy was attempted here
 * <patham9_> which itself is weaker than:  <#1 --> animal>  as it would have come from deduction rule alone here already
 * <sseehh> i think this is why i tried something like subtituteOnlyIfUnifiesDep but it probably needed this condition instead
 * <sseehh> but i had since removed that
 * <patham9_> I see
 * <patham9_> yes dep-var unification needs a dep-var that was unified. while the cases where ind-var unification is used, it doesnt matter if there is a variable at all
 * <sseehh> ok that clarifies it ill add your notes here as comments
 * <sseehh> coding this now, carefly
 * <sseehh> carefuly
 * <patham9_> also i can't think of a case where dep-var unification would need the ability to also unify ind-vars, if you find such a case i don't see an issue with allowing it, as long as it requires one dep-var to be unified it should work
 * <patham9_> hm not it would be wrong to allow ind-var-unification for dep-var unification, reason: (&&,<$1 --> #1> ==> <$1 --> blub>,<cat --> #1>) could derive <cat --> #1> from a more specific case such as <tim --> #1> ==> <tim --> blub>>
 * <patham9_> *no
 * <patham9_> so its really this:
 * <patham9_> allow dep-var unify on ind-var unify, but not vice versa.
 * <patham9_> and require at least one dep-var to be unified in dep-var unification.
 * <patham9_> in principle the restriction to have at least one dep-var unified could be skipped, but the additional weaker result doesn't add any value to the system
 */
public class UniSubst extends Functor implements InlineFunctor<Evaluation> {


    /** must involve a variable substitution, deriving a new term */
    public static final Term NOVEL = atomic("novel");

    public static final Term INDEP_VAR = $.quote("$");
    public static final Term DEP_VAR = $.quote("#");
    public static final Atom UNISUBST = (Atom) atomic("unisubst");


    private final Deriver deriver;

    public UniSubst(Deriver deriver) {
        super(UNISUBST);
        this.deriver = deriver;
    }

    @Override
    public Term apply(Evaluation e, Subterms a) {

        int pp = a.subs();
        if (pp < 3)
            throw new TermException(UniSubst.class.getSimpleName() + " argument underflow", a);

        //TODO cache these in compiled unisubst instances
        boolean strict = false;
        int var = VAR_DEP.bit | VAR_INDEP.bit | VAR_QUERY.bit;
        if (a.hasAny(ATOM)) {
            for (int p = 3; p < pp; p++) {
                Term ap = a.sub(p);
                if (ap instanceof Atom) {
                    if (ap.equals(NOVEL))
                        strict = true;
                    else if (ap.equals(INDEP_VAR))
                        var = VAR_INDEP.bit;
                    else if (ap.equals(DEP_VAR))
                        var = VAR_DEP.bit;
                    else
                        throw new UnsupportedOperationException("unrecognized parameter: " + ap);
                }
            }
        }

        /* target being transformed if x unifies with y */

        return apply(a, var, strict);
    }

    private Term apply(Subterms a, int var, boolean strict) {

        Term in = a.sub(0), x = a.sub(1), y = a.sub(2);

        //pre-filter
        int volMax = deriver.complexMax;
        if (in.complexity() > volMax || x.complexity() > volMax || y.complexity() > volMax)
            return Null;

        if (x.equals(y))
            return strict ? Null : in;

        if (!(x.hasAny(var) || y.hasAny(var)) && (strict || !(x.TEMPORAL_VAR() || y.TEMPORAL_VAR())))
            return Null;

        boolean xv = x instanceof Variable, yv = y instanceof Variable;

        if (xv && yv) {
            //obey variable subsumption ordering
            if (x.opID() < y.opID()) yv = false;
            else xv = false;
        }

        Term out;
        if (NAL.unify.UNISUBST_RECURSION_FILTER && xv /*&& !yv*/ && y instanceof Compound && !y.containsRecursively(x)) {
            out = substDirect(strict, in, x, y);
        } else if (NAL.unify.UNISUBST_RECURSION_FILTER && yv     && x instanceof Compound && !x.containsRecursively(y)) {
            out = substDirect(strict, in, y, x);
        } else {
            out = substUnify(var, strict, in, x, y);
        }

        return out == null ? Null : out;

    }

    @Nullable private Term substUnify(int var, boolean strict, Term in, Term x, Term y) {
        try (Deriver.MyUnifyTransform u = deriver.unifyTransform(NAL.derive.TTL_UNISUBST)) {
            u.vars = var;
            u.novel = strict;

            Term out = result(in, u.unifySubst(x, y, in), strict);
            if (out != null)
                u.postUnified();

            return out;
        }
    }

    @Nullable
    private Term substDirect(boolean strict, Term in, Term x, Term y) {
        Term out;
        out = result(in, in.replace(x, y), strict); //result determined by substitution
        if (out != null) {
            //deriver.unify.retransform.put(x, y);
            deriver.unify.retransform.put(x, y);
        }
        return out;
    }

    @Nullable
    private static Term result(Term in, Term out, boolean strict) {
        return out == null || (strict && in.equals(out)) ? null : out;
    }


}
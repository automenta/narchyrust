package nars.subterm;

import jcog.Is;
import nars.Term;
import nars.term.Compound;
import nars.term.builder.TermBuilder;
import nars.term.compound.SeparateSubtermsCompound;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** assumes that each items in the base subterm are utilized exactly once in the structure, containment, etc.
 * a relaxed variation of this can be created without this assumption (vars() etc will need calculated, not ref.___ shortcut)
 * TODO separate into 2 abstract subclasses: Direct and Negating
 * ShuffledSubterms will inherit from Direct, otherwise generally use Negating
 * */
@Is("Arithmetic_coding") public abstract sealed class RemappedSubterms<S extends Subterms> extends MappedSubterms<S> permits RemappedPNSubterms, ReversedSubterms, ShuffledSubterms {

    final byte subs;

    protected RemappedSubterms(S ref) {
        super(ref);
        this.subs = (byte)this.ref.subs(); //TODO safe numeric cast
    }

    @Override
    public int complexity() {
        return ref.complexity();
    }

    @Override
    public int struct() {
        return ref.struct();
    }

    static <S extends Subterms> S unwrap(S ref, TermBuilder B) {
        if (ref instanceof SeparateSubtermsCompound ssc)
            ref = (S)ssc.subterms();

        return ref instanceof TermList tl ?
                //TermList is mutable so it would be dangerous to ref to it without cloning it
                (S)tl.the(B)
                :
                ref;
    }

    /** whether any of the subterms are wrapped in virtual negations */
    protected boolean wrapsNeg() { return false; }

    protected int negs() { return 0; }

    @Override
    public final int subs() {
        return subs;
    }


    @Override
    public int structSubs() {
        return ref.structSubs();
    }

    @Override
    public int structSurface() {
        return wrapsNeg() ?
                super.structSurface() /* exhaustive */ :
                ref.structSurface();
    }


    @Override
    public boolean contains(Term x) {
        return wrapsNeg() ?
                super.contains(x) /* exhaustive  TODO elide exhaustive in some neg cases */ :
                ref.contains(x);
    }
    @Override
    public boolean containsNeg(Term x) {
        return wrapsNeg() ?
                super.containsNeg(x) /* exhaustive   TODO elide exhaustive in some neg cases */ :
                ref.containsNeg(x);
    }

    @Override
    public Predicate<Term> contains(boolean pn, boolean preFilter) {
        return wrapsNeg() ?
                super.contains(pn, preFilter) :
                ref.contains(pn, preFilter);
    }

    @Override
    public boolean containsInstance(Term x) {
        return wrapsNeg() ?
                super.containsInstance(x) /* exhaustive TODO elide exhaustive in some neg cases */ :
                ref.containsInstance(x);
    }

    @Override
    public boolean containsRecursively(Term x, @Nullable Predicate<Compound> subTermOf) {
        return wrapsNeg() ?
                super.containsRecursively(x, subTermOf) :
                ref.containsRecursively(x, subTermOf);
    }


//    @Override
//    public boolean boolRecurse(Predicate<Compound> inSuperCompound, BiPredicate<Term, Compound> whileTrue, Compound parent, boolean andOrOr) {
//        return wrapsNeg() ?
//                super.boolRecurse(inSuperCompound, whileTrue, parent, andOrOr) :
//                ref.boolRecurse(inSuperCompound, whileTrue, parent, andOrOr);
//    }


}
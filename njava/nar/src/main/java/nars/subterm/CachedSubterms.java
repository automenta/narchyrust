package nars.subterm;

import jcog.Hashed;
import jcog.The;
import nars.Term;
import nars.subterm.util.SubtermMetadataCollector;
import nars.subterm.util.TermMetadata;

import java.util.function.Predicate;

/**
 * what differentiates TermVector from TermContainer is that
 * a TermVector specifically for subterms.  while both
 * can be
 */
public abstract sealed class CachedSubterms extends TermMetadata implements Subterms, The, Hashed permits ArraySubterms, BiSubterm, IntrinSubterms, ShortSubterms {

    transient boolean normalized, normalizedKnown;
    transient boolean internables, internableKnown;

    CachedSubterms(SubtermMetadataCollector intrinMetadata) {
        super(intrinMetadata);
    }

    private static final Predicate<Term> these = Term::internable;

    protected CachedSubterms(Term... terms) {
        super(terms);
    }

    @Override
    public boolean equals(/*@NotNull*/ Object obj) {
        return obj instanceof Subterms s && equalTerms(s);
    }

    @Override
    public int indexOf(Term t, int after) {
        return impossibleSubTerm(t) ? -1 : Subterms.super.indexOf(t, after);
    }

    @Override
    public final int hashCodeSubterms() {
        //assert(hash == Subterms.super.hashCodeSubterms());
        return hash;
    }

    @Override
    public final boolean internables() {
        if (!internableKnown) {
            internables = AND(these);
            internableKnown = true;
        }

        return internables;
    }

    @Override
    public final int seqDur(boolean xternalSensitive) {
        return TEMPORALABLE() ? Subterms.super.seqDur(xternalSensitive) : 0;
    }

    @Override
    @Deprecated public void setNormalized() {
        normalized = normalizedKnown = true;
    }

    @Override
    public boolean NORMALIZED() {
        if (!normalizedKnown) {
            normalized = Subterms.super.NORMALIZED();
            normalizedKnown = true;
        }
        return normalized;
    }


    @Override
    public abstract Term sub(int i);

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

}
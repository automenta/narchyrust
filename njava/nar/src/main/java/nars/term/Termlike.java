package nars.term;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * something which is like a target but isnt quite,
 * like a subterm container
 * <p>
 * Features exhibited by, and which can classify terms
 * and termlike productions
 */
public interface Termlike {

    /** whether contains any potentially temporal (&&,==>) terms */
    default boolean TEMPORALABLE() {
        return this instanceof Subterms &&
               !(this instanceof CondAtomic) &&
               hasAny(Temporals);
    }

    /**
     * subterm access
     */
    default Term sub(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    default Term subUnneg(int i) {
        return sub(i).unneg();
    }



    /* final */

    /**
     * tries to get the ith subterm (if this is a TermContainer),
     * or of is out of bounds or not a container,
     * returns the provided ifOutOfBounds
     */
    default Term sub(int i, Term ifOutOfBounds) {
        return i >= subs() ? ifOutOfBounds : sub(i);
    }

    default Term subSafe(int i) {
        return i >= subs() ? null : sub(i);
    }

    /**
     * number of subterms. if atomic, size=0
     * TODO move to Subterms
     */
    int subs();

    /**
     * recursion height; atomic=1, compound>1
     */
    int height();

    /**
     * syntactic complexity = 1 + total complexity of terms = complexity of subterms - # variable instances
     */
    int complexity();

    /**
     * syntactic complexity 1 + total complexity number of leaf terms, excluding variables which have a complexity of zero
     */
    default int complexityConstants() {
        return sum(Term::complexityConstants) + 1;
    }

    /**
     * only 1-layer (shallow, non-recursive)
     */
    default int sum(ToIntFunction<Term> value) {
//        int x = 0;
//        int s = subs();
//        for (int i = 0; i < s; i++)
//            x += value.applyAsInt(sub(i));
//        return x;
        return intifyShallow(0, (x, t) -> x + value.applyAsInt(t));
    }

    /**
     * only 1-layer (shallow, non-recursive)
     */
    default int max(ToIntFunction<Term> value) {
        return intifyShallow(Integer.MIN_VALUE,
            (x, t) -> Math.max(value.applyAsInt(t), x));
    }


    /**
     * non-recursive, visits only 1 layer deep, and not the current if compound
     */
    default int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = reduce.intValueOf(v, sub(i));
        return v;
    }


    /**
     * structure hash bitvector
     */
    int struct();


    /** only the ops in the immediate subterms of compounds, 0 if atomic */
    int structSubs();

    /**
     * structure of the first layer (surface) only
     */
    int structSurface();


    /** if the term is or contains variably-temporal subterms  (ie. XTERNAL) */
    boolean TEMPORAL_VAR();

    /**
     * parent compounds must pass the descent filter before ts subterms are visited;
     * but if descent filter isnt passed, it will continue to the next sibling:
     * whileTrue must remain true after vistiing each subterm otherwise the entire
     * iteration terminates
     *
     * implementations are not obligated to visit in any particular order, or to repeat visit a duplicate subterm
     * for that, use recurseTermsOrdered(..)
     */
    boolean boolRecurse(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent, boolean andOrOr);

    default /* final */ boolean ANDrecurse(Predicate<Compound> inSuper, BiPredicate<Term, Compound> whileTrue, @Nullable Compound parent) {
        return boolRecurse(inSuper, whileTrue, parent, true);
    }
    default /* final */ boolean ORrecurse( Predicate<Compound> inSuper, BiPredicate<Term, Compound> whileTrue, @Nullable Compound parent) {
        return boolRecurse(inSuper, whileTrue, parent, false);
    }

    default boolean impossibleSubTerm(Termlike x) {
        return
            impossibleSubComplexity(x.complexity()) ||
            impossibleSubStructure(x.struct());
//        return impossibleSubTerm().test(x);
    }

    default /* final */ boolean impossibleSubStructure(int structure) {
        return this instanceof Atomic || !has(structSubs(), structure, true);
    }

    default boolean impossibleSubComplexity(int otherTermComplexity) {
        return this instanceof Atomic || otherTermComplexity > complexity() - subs();
    }

    default boolean hasAll(int struct) {
        return Op.hasAll(struct(), struct);
    }

    default boolean hasAny(int struct) {
        return Op.hasAny(struct(), struct);
    }

    default /* final */ boolean hasAny(/*@NotNull*/ Op op) {
        return hasAny(op.bit);
    }

    default boolean hasVarIndep() {
        return hasAny(VAR_INDEP);
    }

    default boolean hasVarDep() {
        return hasAny(VAR_DEP);
    }

    default boolean hasVarQuery() {
        return hasAny(VAR_QUERY);
    }

    default boolean hasVarPattern() {
        return hasAny(VAR_PATTERN);
    }

    default boolean hasVars() {
        return false;
    }

    int vars();


    default /* final */ boolean isConstant() { return !hasVars(); }

    int varIndep();
    int varQuery();
    int varPattern();
    int varDep();


    default void recurseTermsOrdered(Predicate<Term> inSuperCompound, Consumer<Term> each) {
        recurseTermsOrdered(inSuperCompound, (y)->{
            each.accept(y);
            return true;
        });
    }

    default void recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue) {
        recurseTermsOrdered(inSuperCompound, whileTrue, null);
    }

    boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    default /* final */ boolean recurseTermsOrdered(Predicate<Term> whileTrue) {
        return recurseTermsOrdered(x -> true, whileTrue, null);
    }

    void write(ByteArrayDataOutput out);

    default int addAllTo(Term[] t, int offset) {
        int s = subs();
        for (int i = 0; i < s; )
            t[offset++] = sub(i++);
        return s;
    }

    default boolean containsRecursively(Term x) {
        return false;
    }

    default Predicate<Termlike> impossibleSubTerm() {
        return this instanceof Atomic ?
            x -> true
            :
            new ImpossibleSubterm(this);
    }

    default Predicate<Termlike> impossibleSubTermOf() {
        return new ImpossibleSubtermOf(this);
    }


    final class ImpossibleSubterm implements Predicate<Termlike> {
        private final int xCmplSub, xStructureSubs;

        private ImpossibleSubterm(Termlike x) {
            xCmplSub = x.complexity() - x.subs();
            xStructureSubs = x.structSubs();
        }

        @Override
        public boolean test(Termlike y) {
            return
                xCmplSub < y.complexity() ||
                !has(xStructureSubs, y.struct(), true);
        }

    }
    final class ImpossibleSubtermOf implements Predicate<Termlike> {
        private final int yComplexity, yStruct;

        private ImpossibleSubtermOf(Termlike y) {
            yComplexity = y.complexity() - y.subs();
            yStruct = y.struct();
        }

        @Override
        public boolean test(Termlike x) {
            return
                yComplexity >= x.complexity() ||
                !has(x.structSubs(), yStruct,true);
        }

    }


}
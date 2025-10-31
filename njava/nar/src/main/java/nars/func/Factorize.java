package nars.func;

import jcog.Research;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.memoize.Memoizers;
import nars.$;
import nars.Deriver;
import nars.Term;
import nars.action.transform.CondIntroduction;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Bool;
import nars.term.atom.Img;
import nars.term.builder.Intermed;
import nars.term.var.Variable;
import nars.unify.constraint.TermMatch;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.ListIterator;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * trivial case:
 * (f(a) && f(b))   |-   (f(#1) && member(#1,{a,b}))
 * trivial induction case:
 * ((f(#1) && member(#1,{a,b})) && f(c))
 * (&&, f(c), f(#1),  member(#1,{a,b}))
 * (&&, f(#2), member(#2, {#1,c}),  member(#1,{a,b}))   |-   (f(#1) && member(#1,{a,b,c}))
 * <p>
 * optimizations:
 * compute as a byte trie, where each shadow is represented as a byte[] only
 * commutive subterms handled in a special way, pulling the target subterm's variable replacement to the 0th index regardless where it was originally
 * the remaining terms are sorted in their natural order
 * final target does not need to ompute shadows except for paths which already exist; so order largest target last? (especially if only 2)
 * <p>
 * TODO
 * allow modification of inner conjunctions, not only at top level
 */
@Research
public enum Factorize {
    ;

    @Nullable private static Subterms _factorize(Subterms xx) {
        Term[] y = distribute(xx);
        return y == null ? EmptyProduct :
            Util.ifEqualThen(applyConjNonNull(y,
                    xx.hasVarDep() ? f : fIfNoVar
                    //xx.hasVarIndep() ? g : gIfNoIndep
            ), xx, EmptyProduct);
    }

    /**
     * returns null if detects no reason to re-process
     */
    private static @Nullable Term[] distribute(Subterms xx) {
        TmpTermList x = xx.toTmpList();

        //TODO track what subterms (if any) are totally un-involved and exclude them from processing in subsequent stages
        //TODO sort the subterms for optimal processing order
        boolean stable;
        restart:
        do {
            stable = true;

            for (int i = 0, xLength = x.size(); i < xLength; i++) {
                Term s = x.get(i);
                Term var;

                //TODO optimization step: distribute any equal(#x,constant) terms

                if (Member.member.equals(Functor.func(s)) && (var = s.sub((byte) 0, (byte) 0)) instanceof Variable) {
                    Term r = s.sub((byte) 0, (byte) 1);
                    if (r != null && r.SET()) {
                        if (xLength == 2) {
                            //special case: there is no reason to process this because it consists of one member and one non-member
                            return null;
                        }


                        //erase even if un-used, it would have no effect
                        x.removeFast(i);

                        Subterms rs = null;

                        ListIterator<Term> jj = x.listIterator();
                        while (jj.hasNext()) {
                            Term xj = jj.next();
                            if (xj instanceof Compound && xj.containsRecursively(var)) {
                                jj.remove();
                                if (rs == null)
                                    rs = r.subterms();
                                for (Term rr : rs) {
                                    jj.add(xj.replace(var, rr));
                                }
                            }
                        }

                        stable = false;

                        continue restart;
                    }
                }

            }
        } while (!stable);

        x.inhExplode(true);

        return x.arrayTake();
    }

    //TODO static class ShadowCompound extends Compound(Compound base, ByteList masked)

    /**
     * returns the subterms, as a sorted target array setAt, for the new conjunction.  or null if there was nothing factorable
     */
    @Nullable public static Subterms applyConj(Term[] x, Variable f) {
        return new ShadowTargets(f).run(x);
    }
    private static Subterms applyConjNonNull(Term[] x, Variable f) {
        Subterms y = applyConj(x, f);
        return y == null ? EmptyProduct : y; //HACK
    }

    private static boolean validMember(Term g) {
        return !(g instanceof Variable) && !(g instanceof Bool) && !(g instanceof Img);
    }


    public static class FactorIntroduction extends CondIntroduction {
        {
            //TODO nal3 inh/conj
            is(PremiseTask, CONJ);

            ifNot(PremiseTask, TermMatch.SEQ); //HACK for unfactored CONJ
        }

        @Override
        protected Term apply(Term x, int volMax, Deriver d) {

            Subterms xx = x.subterms();
            Subterms xy = _factorize(xx);
            if (xy == EmptyProduct || xy.equals(xx))
                return Null;

            Term factoring = xy.sub(0);
            Term factored = CONJ.the(x.dt(), xy.sub(1).subterms());

            return CONJ.the(factoring, factored);
            //return $.func(Member.member, ArrayUtil.add(Functor.argsArray(factoring), factored));
            //return IMPL.the(factoring, factored);
        }

    }

    /** shadow target -> replacements */
    private static final class ShadowTargets implements BiPredicate<ByteList, Term> {

        private final Variable f;

        transient byte i;
        transient int pathMin;

        transient UnifiedSetMultimap<Term, ObjectBytePair<Term>> p;
        private transient Term s;

        ShadowTargets(Variable f) {
            this.f = f;
        }

        @Nullable TermList run(Term[] x) {
            byte n = (byte)x.length;
            for (i = 0; i < n; i++) {
                s = x[i];
                if (s instanceof Compound) {
                    pathMin = s.subs() == 1 ? 2 : 1; //dont factor the direct subterm of 1-arity compound (ex: negation, {x}, (x) )
                    s.pathsTo(Factorize::validMember, v -> true, this);
                }
            }

            s = null;

            if (p == null)
                return null;

            MutableList<Pair<Term, RichIterable<ObjectBytePair<Term>>>> r = p.keyMultiValuePairsView().select((pathwhat) ->
                    pathwhat.getTwo().size() > 1)
                    .toSortedList(c);
            p = null;
            if (r.isEmpty())
                return null;


            Pair<Term, RichIterable<ObjectBytePair<Term>>> rr = r.getFirst();
            RichIterable<ObjectBytePair<Term>> rrr = rr.getTwo();

            //flatten
            Term mm = SETe.the(rrr.collect((ob) -> {
                Term y1 = ob.getOne();
                return y1.CONJ() && y1.dt() == DTERNAL ? SETe.the(y1.subterms()) : y1; //flatten
            }).toSet());
            if (!mm.SETe() || mm.subs() < 2)
                return null;

            Term m = $.func(Member.member, f, mm);
            if (!m.INH())
                return null;

            MetalBitSet mask = MetalBitSet.bits(n);
            rrr.collectByte(ObjectBytePair::getTwo).forEach(mask::set);
            java.util.Set<Term> y = new UnifiedSet<>(n - mask.cardinality() + 1);
            for (byte i = 0; i < n; i++)
                if (!mask.test(i))
                    y.add(x[i]);

            y.add(rr.getOne() /* shadow */);

            return new TermList(m, $.pFast(y));
        }

        @Override
        public boolean test(ByteList path, Term what) {

            //if (what.unneg().volume() > 1) { //dont remap any atomics
            if (!(what instanceof Variable)) { //dont remap variables
                if (path.size() >= pathMin) {
                    if (p == null) p = UnifiedSetMultimap.newMultimap();
                    p.put(
                        s.replaceAt(path, f),
                        pair(what, i)
                    );
                }
            }
            return true;
        }
    }

    private static final Variable f = $.varDep("_f"), fIfNoVar = $.varDep(1);
    //private static final Variable f = $.varIndep("_f"), fIfNoVar = $.varIndep(1);

    private static final Comparator<Pair<Term, RichIterable<ObjectBytePair<Term>>>> c = Comparator
        .comparingInt((Pair<Term, RichIterable<ObjectBytePair<Term>>> p) -> -p.getTwo().size()) //more unique subterms involved
        .thenComparingInt(p -> -p.getOne().complexity()) //longest common path
        .thenComparingInt(p -> -(int) (p.getTwo().sumOfInt(z -> z.getOne().complexity())));  //larger subterms involved

    private static class CachedFactorize  {
        /**
         * subterm = 2-arity ( member(...), (&&,...) )
         */
        private static final Function<Subterms, Subterms> factorize = Memoizers.the.memoizeByte(
                Factorize.class.getSimpleName() + "_factorize",
                Intermed.SubtermsKey::new,
                CachedFactorize::_factorize, 48 * 1024);

        private static Subterms _factorize(Intermed.SubtermsKey x) {
            return Factorize._factorize(x.subs);
        }
    }


}
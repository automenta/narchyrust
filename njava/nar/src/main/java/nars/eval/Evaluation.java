package nars.eval;

import com.google.common.collect.Iterables;
import jcog.Is;
import jcog.Util;
import jcog.data.iterator.CartesianIterator;
import jcog.data.list.Lst;
import jcog.random.RandomBits;
import jcog.util.ArrayUtil;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.NAL;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Neg;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.term.var.Variable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static nars.term.atom.Bool.*;

/**
 * see: https://www.swi-prolog.org/pldoc/man?section=preddesc
 * //TODO extends Unify
 */
@Is({"Open-world_assumption", "Meta-circular_evaluator"})
public class Evaluation {

    private final Evaluator e;
    private final Predicate<Term> each;
    private VersionMap<Term, Term> subs;

    private Lst<Iterable<Predicate<Evaluation>>> termutes;
    private Versioning<Term> v;

    @Deprecated private transient Set<Term> out;

    public Evaluation(Evaluator e, @Nullable Predicate<Term> each) {
        this.e = e;
        this.each = each != null ? each : (Predicate<Term>) this;
    }

    /**
     * 2-ary AND
     */
    public static Predicate<Evaluation> assign(Term x, Term xx, Term y, Term yy) {
        if (x.equals(xx))
            cyclic(x);
        if (y.equals(yy))
            cyclic(y);

        return m -> m.is(x, xx) && m.is(y, yy);
    }


    private static Predicate<Evaluation> assign(Term x, Term y) {
        if (x.equals(y))
            cyclic(x);
        return m -> m.is(x, y);
    }

    private static void cyclic(Term x) {
        throw new TermException("assign cycle", x);
    }

    protected RandomGenerator random() {
        return new RandomBits(ThreadLocalRandom.current());
    }

    private boolean termute1(Term y, int start) {
        for (var t : termutes.removeFirst()) {
            if (t.test(this) && !eval(transform(y)))
                return false;
            if (!revert(start))
                return false;
        }
        return true;
    }

    private boolean revert(int start) {
        if (v.use(NAL.derive.TTL_COST_TRY)) {
            v.revert(start);
            return true;
        } else {
            return false; //CUT
        }
    }

    private boolean termuteN(Term y, int start) {

        Iterable<Predicate<Evaluation>>[] tt = termutes.toArray(Iterable[]::new);
        termutes.clear();

        //for (int i = 0, ttLength = tt.length; i < ttLength; i++) tt[i] = tt[i];

        if (tt.length > 1 && NAL.unify.SHUFFLE_TERMUTES)
            ArrayUtil.shuffle(tt, random());

//		QuickSort.sort(tt, 0, tt.length, (int x)-> Iterables.size(tt[x]));

        var pp = new CartesianIterator<Predicate<Evaluation>>(Predicate[]::new, tt);
        while (pp.hasNext()) {
            var appliedAll = true;
            for (var p : pp.next()) {
                if (!p.test(this)) {
                    appliedAll = false;
                    break;
                }
            }

            if (appliedAll) {
                var z = transform(y);
                if (!y.equals(z))
                    eval(z);
            }

            if (!revert(start))
                return false;
        }

        return true;
    }

    public void clear() {
        if (this.out!=null)
            out.clear();

        if (v != null) {
            if (termutes != null)
                termutes.clear();
            v.clear();
        }
    }

//    private void canBe(Predicate<Termerator> x) {
//        canBe(List.of(x));
//    }

    /**
     * assign 1 variable
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public final boolean is(Term x, Term y) {

        if (x == Null || y == Null) return false;

        if (x.equals(y)) return true;


        if (y instanceof Compound && /*x instanceof Variable && */ y.containsRecursively(x))
            return false; //infinite recursion
//        if (x instanceof Compound && /*y instanceof Variable && */ x.containsRecursively(y))
//            return false; //infinite recursion

//        if (v != null && v.size() > 0) {
//            Term xx = x.replace(subs), yy = y.replace(subs);
//            if (!xx.equals(x) || !yy.equals(y))
//                return is(xx, yy); //recurse
//
//            //replace existing subs
//            if (!subs.replace((sx, sy)-> x.equals(sx) ? sy : sy.replace(x, y)))
//                return false;
//
//        } else {
        ensureReady();
//        }

        if (subs.set(x, y))
            return true;     //assert(set); //return true;

        if (y instanceof Variable) {
            //try reverse assignment
            var xx = subs.get(x);
            return subs.set(y, xx!=null ? xx : x);
        }

        return false;
    }

    /**
     * assign 2-variables at once.
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term xx, Term y, Term yy) {
        return is(x, xx) && is(y, yy);
    }

    /**
     * OR, forked
     * TODO limit when # termutators exceed limit
     */
    private void canBe(Iterable<Predicate<Evaluation>> x) {
        if (termutes == null)
            termutes = new Lst<>(1);
        else if (!termutes.contains(x)) //HACK
            termutes.add(x);
    }

    public boolean canBe(Term x, Iterable<Term> y) {
        switch (sizeEstimate(y)) {
            case 0 -> {
                return true; //no effect
            }
            case 1 -> {
                return is(x, y.iterator().next()); //deterministic substitution
            }
            default -> {
                if (!termutable())
                    return false;
                else {
                    canBe(new ChoiceIterable(x, y));
                    return true; //HACK
                }
            }
        }
    }

    private static int sizeEstimate(Iterable<Term> y) {
        //TODO Term commute for the set of terms in y, not just the direct iteration
        if (y instanceof Subterms s)
            return s.subs();
        else if (y instanceof Collection c)
            return c.size();
        else
            return Iterables.size(y); //TODO parameterize this because some big Iterables would be expensive just to count here
    }

    protected boolean termutable() {
        return true;
    }

    private void ensureReady() {
        if (v == null) {
            v = new Versioning<>(NAL.unify.UNIFICATION_STACK_CAPACITY, NAL.EVALUATION_RECURSE_LIMIT);
            subs = new VersionMap<>(v);
        }
    }

    private boolean outContains(Term y) {
        return out != null && out.contains(y);
    }

    private int now() {
        return v == null ? 0 : v.size();
    }

    public boolean eval(Term t) {

        if (outContains(t))
            return true;

        var c = e.clauses((Compound)t);
        if (c == null)
            return true;

        Term x, y;

        int iters = NAL.EVAL_ITER_MAX; //safety

        do {

            y = eval(c, x = c.start());

        } while (y instanceof Compound Y && iters-- > 0 && !Y.equals(x) && (c = e.clauses(Y))!=null);

        return evalResult(x, y);
    }

    private Term eval(EvaluationPhase c, Term x) {
        var before = now();

        var y = c.apply(this, x);
        if (y == null) y = x;

        return !y.equals(x) || before != now() ? transform(y) : y;
    }

    private boolean evalResult(Term x, Term y) {
        //if termutators, collect all results. otherwise 'cur' is the only result to return
        int termutes;
        if (!(y instanceof Bool) && (termutes = termutators()) > 0) {
            var sizeBefore = now();
            return termutes == 1 ? termute1(y, sizeBefore) : termuteN(y, sizeBefore);
        } else {
            return out(x, y);
        }
    }

    /** Consider terminal result: x -> y */
    private boolean out(Term x, Term y) {
        if (y instanceof Bool yb)
            y = bool(x, yb);

        if (out == null)
            out = new UnifiedSet<>(2);

        return !out.add(y) || each.test(y);
    }

    private Term bool(Term x, Bool b) {
        if (b == True)
            return boolTrue(x);
        else if (b == False)
            return False; //boolFalse(x);
        else
            return Null;
    }

//	protected Term bool(Term x, Bool b) {
//		return b;
//	}

    protected Term boolTrue(Term x) {
        return x;
    }

    protected Term boolFalse(Term x) {
        return x.neg();
    }

    private int termutators() {
        return termutes != null ? termutes.size() : 0;
    }

    private Term transform(Term x) {
        return subs != null ? x.replace(subs, Evaluator.B) : x;
    }

    public Term compute(Term x, Term y) {
        if (x == y)
            return True;

        //handle negated vars by negating one or both sides like unification
        else if (x instanceof Neg && y instanceof Neg) {
            x = x.unneg();
            y = y.unneg();
            if (x == y)
                return True;
        } else if (x instanceof Neg) {
            var xu = x.unneg();
            if (xu.equals(y)) return False; //co-negated
            if (xu instanceof Variable) {
                if (y instanceof Variable)
                    return null; //do nothing; maintain negation relationship
                x = x.unneg();
                y = y.neg();
            }
        } else if (y instanceof Neg) {
            var yu = y.unneg();
            if (yu.equals(x)) return False; //co-negated
            if (yu instanceof Variable) {
                if (x instanceof Variable)
                    return null; //do nothing; maintain negation relationship
                y = y.unneg();
                x = x.neg();
            }
        }

        boolean xIsVar = x instanceof Variable, yIsVar = y instanceof Variable;
        if (xIsVar == yIsVar) {
            if (x.equals(y))
                return True;
            //if (x instanceof Atomic &&) ..
        }


        return compute(x, y, xIsVar, yIsVar);
    }

    private @Nullable Term compute(Term x, Term y, boolean xIsVar, boolean yIsVar) {
        //TODO maybe only for certain var types
        if (xIsVar && yIsVar) { //var equivalence
            //Variable common = CommonVariable.common((Variable)x, (Variable)y); //return e.is(x, common) && e.is(y, common) ? True : Null;
            //try assigning in reverse
            //noinspection ConditionalExpressionWithIdenticalBranches
            return is(y, x) || is(x, y) ? null : null;
        }


        boolean xHasVar = xIsVar || x.hasVars(), yHasVar = yIsVar || y.hasVars();
        if (!xHasVar && !yHasVar)
            return False;

        if (x instanceof Compound || y instanceof Compound) {
            var i = nars.func.Equal.reverse(this, x, xHasVar, y, yHasVar);
            if (i != null)
                return i;
        }

        return _compute(x, y, xIsVar, yIsVar);
    }

    private @Nullable Bool _compute(Term x, Term y, boolean xIsVar, boolean yIsVar) {
        if (xIsVar) return IS(x, y);
        else if (yIsVar) return IS(y, x);
        else return null;
    }

    private Bool IS(Term x, Term y) {
        return this.is(x, y) ? True : False;
    }

    @Nullable final Term call(Compound a) {
        Term x = a.sub(0), y = a.sub(1);
        return a.EQ() ?
            compute(x, y) :
            ((Functor) y).apply(this, x.subtermsDirect());
    }

    private record ChoiceIterable(Term x, Iterable<Term> y) implements Iterable<Predicate<Evaluation>> {

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof ChoiceIterable(var x1, var y1) && y.equals(y1) && x.equals(x1);
        }

        @Override
        public int hashCode() {
            return Util.hashCombine(y, x);
        }

        @Override
        public Iterator<Predicate<Evaluation>> iterator() {
            return new ChoiceIterable.ChoiceIterator(x, y);
        }

        private static final class ChoiceIterator implements Iterator<Predicate<Evaluation>> {

            final Iterator<Term> y;
            private final Term x;

            ChoiceIterator(Term x, Iterable<Term> y) {
                this(x, y.iterator());
            }

            ChoiceIterator(Term x, Iterator<Term> y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public boolean hasNext() {
                return y.hasNext();
            }

            @Override
            public Predicate<Evaluation> next() {
                return assign(x, y.next());
            }
        }
    }

    public static class First implements Predicate<Term> {
        public Term the;

        @Override
        public boolean test(Term what) {
            //TODO move this to a separate predicate
//			if (what instanceof Bool) {
//				return true; //ignore and continue try to find a non-bool solution
//			} else {
            the = what;
            return false;
//			}
        }
    }

    public static class All implements Predicate<Term> {

        private final Set<Term> the;

        public All() {
            this(new UnifiedSet<>(0, 0.5f));
        }

        All(UnifiedSet<Term> ee) {
            this.the = ee;
        }

        @Override
        public boolean test(Term t) {
            the.add(t);
            return true;
        }

        public final Set<Term> the() {
            return the.isEmpty() ? Collections.EMPTY_SET : the;
        }
    }

}
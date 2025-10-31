package nars.subterm;

import com.google.common.base.Joiner;
import com.google.common.collect.Streams;
import com.google.common.io.ByteArrayDataOutput;
import jcog.Hashed;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.decide.Roulette;
import nars.Op;
import nars.Term;
import nars.subterm.util.TermMetadata;
import nars.term.Compound;
import nars.term.CondAtomic;
import nars.term.Neg;
import nars.term.Termlike;
import nars.term.atom.Bool;
import nars.term.buffer.TermBuffer;
import nars.term.util.Terms;
import nars.term.var.NormalizedVariable;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static nars.Op.*;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface Subterms extends Termlike, Iterable<Term> {

//    IntObjectToIntFunction<Term> HASH_COMBINER = Util::hashCombine;

    static int hash(Term onlySub) {
        return Util.hashCombine1(onlySub);
    }


    static int hash(Term[] term, int n) {
        var h = 1;
        for (var i = 0; i < n; i++)
            h = Util.hashCombine(h, term[i]);
        return h;
    }

    static Term[] array(Subterms s) {
        return s instanceof TmpTermList t ?
            t.arrayTake() :
            s.arrayShared();
    }

    default int hashExhaustive() {
        //return s.intifyShallow(1, HASH_COMBINER);
        int n = subs(), v = 1;
        for (var i = 0; i < n; i++)
            v = Util.hashCombine(v, sub(i));
        return v;
    }

//    static boolean commonSubtermsRecursive(Term a, Term b, boolean excludeVariables) {
//
//        Subterms aa = a.subterms(), bb = b.subterms();
//
//        int commonStructure = aa.structure() & bb.structure();
//        if (excludeVariables)
//            commonStructure &= ~(Op.Variables) & AtomicConstant;
//
//        if (commonStructure == 0)
//            return false;
//
//        Collection<Term> scratch = new UnifiedSet<>(0);
//        aa.recurseSubtermsToSet(commonStructure, scratch, true);
//        return bb.recurseSubtermsToSet(commonStructure, scratch, false);
//    }

    static String toString(Iterable<? extends Term> subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

    static String toString(Term... subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

    static int compare(Subterms a, Subterms b) {
        if (a == b)
            return 0;

        int s;
        int diff;
        if ((diff = Integer.compare(s = a.subs(), b.subs())) != 0)
            return diff;

        if (!a.hasVars()) {
            for (var i = 0; i < s; i++) {
                var d = a.sub(i).compareTo(b.sub(i));
                if (d != 0) return d;
            }
        } else {
            //compare non-variable terms first for var-normalization purposes
            for (var j = 0; j < 2; j++)
                for (var i = 0; i < s; i++) {
                    var ai = a.sub(i);
                    if (ai.hasVars() == (j == 1)) {
                        var d = ai.compareTo(b.sub(i));
                        if (d != 0) return d;
                    }
                }
        }

        return 0;
    }

    private static boolean onlyNormalizedVariable(Variable z) {
        return z instanceof NormalizedVariable n && n.id() == 1;
    }

    default Subterms transformSubs(UnaryOperator<Term> f) {

        var s = subs();
        var out = new TmpTermList(s);

        var changed = false;
		for (var j = 0; j < s; j++) {
            var x = sub(j);
            var y = f.apply(x); //assert(k!=null);
			if (y == Bool.Null)
				return null;
			if (x != y) changed = true;

            out.addFast(y);
		}

		return changed ? out : this;
    }

    @Override
    default boolean hasVars() {
        return hasAny(Variables);
    }

    @Override
    default int vars() {
        return sum(Termlike::vars);
    }

    @Override
    default boolean TEMPORAL_VAR() {
        return !(this instanceof CondAtomic) && (hasAny(Temporals) && ORunneg(Termlike::TEMPORAL_VAR));
    }

    default boolean containsAny(Subterms ofThese) {
        return ofThese.OR(contains(false, false));
    }

    default <X> X[] array(Function<Term, X> map, IntFunction<X[]> arrayizer) {
        var s = subs();
        var xx = arrayizer.apply(s);
        for (var i = 0; i < s; i++)
            xx[i] = map.apply(sub(i));
        return xx;
    }

    default int seqDurSub(int i, boolean xternalSensitive) {
        return this instanceof CondAtomic ? 0 : subUnneg(i).seqDur(xternalSensitive);
    }

    default @Nullable Term subRoulette(FloatFunction<Term> subValue, RandomGenerator rng) {
        var s = subs();
        return switch (s) {
            case 0 -> null;
            case 1 -> sub(0);
            default -> sub(Roulette.selectRoulette(s, i -> subValue.floatValueOf(sub(i)), rng));
        };
    }

    @Override
    default int struct() {
        return structSubs();
    }

    @Override
    default int structSurface() {
        //return intifyShallow(0, (s, x) -> s | x.opBit());
        int n = subs(), s = 0;
        for (var i = 0; i < n; i++)
            s |= sub(i).structOp();
        return s;
    }

    default int structSurfaceUnneg() {
        int n = subs(), s = 0;
        for (var i = 0; i < n; i++)
            s |= subUnneg(i).structOp();
        return s;
    }

    /**
     * overridden only in Compounds
     */
    @Override
    default int structSubs() {
        //return intifyShallow(0, (s, x) -> s | x.structure());
        var n = subs();
        var s = 0;
        for (var i = 0; i < n; i++)
            s |= sub(i).struct();
        return s;
    }

    /**
     * sorted and deduplicated
     */
    default Subterms commuted() {
        if (subs() <= 1) return this;
        var x = arrayShared();
        var y = Terms.commute(x);
        return x == y ? this : terms.subterms(y);
    }

    default Subterms sorted() {
        if (subs() <= 1) return this;
        var x = arrayShared();
        var y = Terms.sort(x);
        return x == y ? this : terms.subterms(y);
    }

    default boolean subtermsSorted() {
        var s = subs();
        if (s >= 2) {
            var p = sub(0);
            for (var i = 1; i < s; i++) {
                var n = sub(i);
                if (p!=n && p.compareTo(n) > 0)
                    return false;
                p = n;
            }
        }
        return true;
    }

    /**
     * TODO constructing a Stream like this just for the Iterator is not very efficient
     */
    @Override
    default Iterator<Term> iterator() {
        return new SubtermsIterator(this);
        //return IntStream.range(0, subs()).mapToObj(this::sub).iterator();
    }

    @Deprecated default boolean conjDecompose(LongObjectPredicate<Term> each, long when, int dt, boolean dE, boolean dX) {
        return (dE && !dX && dt == DTERNAL) || (dX && !dE && dt == XTERNAL) ?
            conjDecomposePar(each, when) :
            conjDecomposeSeq(each, when, dt, dE, dX);
    }

    private boolean conjDecomposePar(LongObjectPredicate<Term> each, long t) {
        return AND(x -> each.accept(t, x));
    }

    private boolean conjDecomposeSeq(LongObjectPredicate<Term> each, long t, int dt, boolean dE, boolean dX) {
        var parallel = (dt == DTERNAL) || (dt == XTERNAL);
        var changeDT = !parallel;// && t != ETERNAL && t != TIMELESS /* motionless in time */;

        if (t == ETERNAL || t == TIMELESS)
            throw new UnsupportedOperationException();

        boolean fwd;
        if (changeDT) {
            if (!(fwd = (dt >= 0)))
                dt = -dt;
        } else
            fwd = true;

        var s = subs() - 1;
        for (var i = 0; i <= s; i++) {
            var ii = sub(fwd ? i : s - i);

            if (ii.CONJ() ? !((Compound) ii).condsAND(each, t, dE, dX, dE) : !each.accept(t, ii))
                return false;

            if (changeDT && i < s)
                t += dt + ii.seqDur();
        }

        return true;
    }

    /**
     * HACK probably best not to call directly
     * TODO move to MegaHeapTermBuilder
     */
    default Subterms negated() {
        if (0 == (structSurface() & NEG.bit))
            return new NegatedSubterms(this);
        else
            throw new TODO();
    }

    default void appendTo(TermBuffer b) {
        //forEach(b::append);
        var s = subs();
        for (var i = 0; i < s; i++)
            b.append(sub(i));
    }

    /**
     * the structure common to each subterm
     */
    default int structureIntersection() {
        return switch (subs()) {
            case 0 -> 0;
            case 1 -> sub(0).struct();
            default -> intifyShallow(~(0), (s, y) -> s != 0 ? (y.struct() & s) : 0);
        };
    }

    /**
     * the minimum volume of any subterm
     */
    default int volMin() {
        return switch (subs()) {
            case 0 -> 0;
            case 1 -> sub(0).complexity();
            default -> intifyShallow(Integer.MAX_VALUE, (v, y) -> Math.min(v, y.complexity()));
        };
    }

    default int seqDur(boolean xternalSensitive) {
        var r = 0;
        var s = subs();
        for (var i = 0; i < s; i++) {
            var di = seqDurSub(i, xternalSensitive);
            if (di == XTERNAL) {
                if (xternalSensitive)
                    return XTERNAL;
                else
                    throw new UnsupportedOperationException();
            }
            r = Math.max(r, di);
        }
        return r;
    }

    default boolean subEquals(int i, Term x) {
        return sub(i).equals(x);
    }
    default boolean subEquals(int i, int j) {
        return i==j || subEquals(i, sub(j));
    }

    default SortedSet<Term> toSetSorted() {
        SortedSet<Term> u = new TreeSet<>(); //new MetalTreeSet();
        addAllTo(u);
        return u;
    }

    /**
     * an array of the subterms, which an implementation may allow
     * direct access to its internal array which if modified will
     * lead to disaster. by default, it will call 'toArray' which
     * guarantees a clone. override with caution
     */
    default Term[] arrayShared() {
        return arrayClone();
    }

    /**
     * an array of the subterms
     * this is meant to be a clone always
     */
    default Term[] arrayClone() {
        var s = subs();
        return s == 0 ? EmptyTermArray : arrayClone(new Term[s], 0, s);
    }

    default Term[] arrayClone(Term[] target, int from, int to) {
        for (int i = from, j = 0; i < to; i++, j++)
            target[j] = sub(i);

        return target;
    }

    default TermList toList() {
        return new TermList(this);
    }

    default TmpTermList toTmpList() {
        return new TmpTermList(this);
    }

    /**
     * @return a Mutable Set, unless empty
     */
    default MutableSet<Term> toSet() {
        var s = subs();
        var u = new UnifiedSet<Term>(s);
        //if (s > 0)
        addAllTo(u);
        return u;
    }

    default @Nullable <C extends Collection<Term>> C collect(Predicate<Term> ifTrue, C c) {
        var s = subs();
        for (var i = 0; i < s; i++) {
            var x = sub(i);
            if (ifTrue.test(x))
                c.add(x);
        }

        return c;
    }

    /**
     * by default this does not need to do anything
     * but implementations can cache the normalization
     * in a boolean because it only needs done once.
     *
     * do not call.
     */
    default void setNormalized() {
        //throw new UnsupportedOperationException();
    }

    /**
     * assume its normalized if no variables are present
     */
    default boolean NORMALIZED() {
		/*
		 TODO
			int normalized(Term x, int offset)
				--continues by keeping or increasing the variable offset, or returns -1 on first inconsistency encountered.

		 */
        int v;
        return switch (v = this.vars()) {
            case 0 -> true;
            case 1 -> normalize1();
            default -> normalizeN(v);
        };
    }

    private boolean normalizeN(int v) {
        if (this.subs() == 1) {
            return switch (this.sub(0)) {
                case Compound xc -> xc.NORMALIZED();
                case Variable xv -> onlyNormalizedVariable(xv);
                case null, default -> true;
            };
        } else
            return this.recurseTermsOrdered(TermMetadata.VarPreNormalization::descend, new TermMetadata.VarPreNormalization(v), null);
    }

    /** optimized 1-ary case */
    private boolean normalize1() {
        return this.ANDrecurse(Subterms::hasVars, z ->
                        !(z instanceof Variable zv) || onlyNormalizedVariable(zv),
                null);
    }

    /**
     * gets the set of unique recursively contained terms of a specific type
     * TODO generalize to a provided lambda predicate selector
     */
    default MutableSet<Term> recurseSubtermsToSet(Op _onlyType) {
        return recurseSubtermsToSet(_onlyType.bit);
    }

    default MutableSet<Term> recurseSubtermsToSet(int structIs) {
        if (!hasAny(structIs))
            return Sets.mutable.empty();
        var s = new UnifiedSet<Term>(complexity());
        recurseSubtermsToSet(this, structIs, s);
        return s;
    }

    private static void recurseSubtermsToSet(Subterms terms, int structIs, UnifiedSet<Term> s) {
        for (var x : terms) {
            if (isAny(x, structIs))
                s.add(x);
            if (x instanceof Compound xc && Op.hasAny(xc.structSubs(), structIs))
                recurseSubtermsToSet(xc, structIs, s);
        }
    }

    /*@NotNull*/
    default boolean recurseSubtermsToSet(int inStructure, Collection<Term> t, boolean untilAddedORwhileNotRemoved) {
        var r = new boolean[]{false};


        ANDrecurse(
                inStructure == -1 ? any -> true : p -> p.hasAny(inStructure),

                s -> {
                    if (!untilAddedORwhileNotRemoved && r[0])
                        return false;
                    if (s.hasAny(inStructure))
                        r[0] |= (untilAddedORwhileNotRemoved) ? t.add(s) : t.remove(s);
                    return true;
                }, null);


        return r[0];
    }


    @Override default /* final */boolean containsRecursively(Term t) {
        return containsRecursively(t, null);
    }

    default boolean containsRecursively(Term x, @Nullable Predicate<Compound> subTermOf) {

        var xv = x.complexity();
        if (!impossibleSubComplexity(xv)) {
            var xs = x.struct();
            if (!impossibleSubStructure(xs)) {
                var s = subs();
                Term prev = null;
                for (var i = s - 1; i >= 0; i--) {
                    var y = sub(i);
                    if (prev != y) {
                        var yv = y.complexity();
                        if (yv == xv) {
                            if (x.equals(y))
                                return true;
                        }
                        if (y instanceof Compound yc && yv > xv) {
                            if (yc.containsRecursively(x, subTermOf))
                                return true;
                        }
                        prev = y;
                    }
                }
            }
        }
        return false;
    }

    default boolean equalTerms(Term[] x) {
        if (this instanceof TermList tl && tl.array() == x) return true;

        var n = x.length;
        if (subs()!= n) return false;
        for (var i = 0; i < n; i++) {
            if (!subEquals(i, x[i])) return false;
        }
        return true;
    }

    default boolean equalTerms(Subterms x) {
        return this == x || (equalHashSubterms(x) && equalExhaustive(x));
    }

    private boolean equalHashSubterms(Subterms x) {
        return !(this instanceof Hashed) || !(x instanceof Hashed) || hashCodeSubterms() == x.hashCodeSubterms();
    }

    default boolean equalExhaustive(Subterms x) {
        var s = this.subs();
        if (s != x.subs())
            return false;

        //reverse (since in commuted terms, volumes are sorted in descending order, so the comparisons at the end will likely be cheaper on average)
        for (--s; s >= 0; s--) {
            if (!subEquals(s, x.sub(s)))
                return false;
        }
        return true;
    }

    default void addAllTo(Collection<Term> target) {
        forEach(target::add);
    }

    default Term[] terms(IntObjectPredicate<Term> filter) {
        TmpTermList l = null;
        var s = subs();
        for (var i = 0; i < s; i++) {
            var t = sub(i);
            if (filter.accept(i, t)) {
                if (l == null)
                    l = new TmpTermList(s - i);
                l.addFast(t);
            }
        }
        return l == null ? EmptyTermArray : l.arrayTake();
    }

    default void forEach(Consumer<? super Term> action, int start, int stop) {
        if (start < 0 || stop > subs())
            throw new ArrayIndexOutOfBoundsException();

        for (var i = start; i < stop; i++)
            action.accept(sub(i));
    }

    @Override
    default void forEach(Consumer<? super Term> action) {
        forEach(action, 0, subs());
    }

    /**
     * return whether a subterm op at an index is an operator.
     */
    default boolean subIs(int i, Op o) {
        return subOpID(i) == o.id;
    }

    default byte subOpID(int i) {
        return sub(i).opID();
    }

    /**
     * counts subterms matching the predicate
     */
    default /* final */ int count(Predicate<? super Term> match) {
        return intifyShallow(0, counter(match));
    }

    static IntObjectToIntFunction<Term> counter(Predicate<? super Term> match) {
        return (c, sub) -> match.test(sub) ? c + 1 : c;
    }


    /**
     * counts subterms matching the supplied op
     */
    default int count(Op matchingOp) {
        int matchingOpID = matchingOp.id;
        return count(x -> x.opID() == matchingOpID);
    }

    /**
     * first index of; follows normal indexOf() semantics; -1 if not found
     */
    default /* final */ int indexOf(Term x) {
        return indexOf(x, -1);
    }

    default /* final */ int indexOfInstance(Term x) {
        var n = subs();
        for (var i = 0; i < n; i++) {
            if (sub(i)==x)
                return i;
        }
        return -1;
    }

    default int indexOf(Term t, int after) {
        return indexOf(t.equals(), after);
    }

    /**
     * stream of each subterm
     */
    default Stream<Term> subStream() {
        return Streams.stream(this);
    }

    /**
     * allows the subterms to hold a different hashcode than hashCode when comparing subterms
     */
    default int hashCodeSubterms() {
        return hashExhaustive();
    }

    /**
     * TODO write negating version of this that negates only up to subs() bits
     */
    default MetalBitSet indicesOfBits(Predicate<Term> match) {
        var n = subs();
        var m = MetalBitSet.bits(n);
        for (var i = 0; i < n; i++) {
            if (match.test(sub(i)))
                m.set(i);
        }
        return m;
    }

    @Nullable default Term[] subs(Predicate<Term> toKeep) {
        return subsIncExc(indicesOfBits(toKeep), true);
    }

    default Term[] removing(MetalBitSet toRemove) {
        return subsIncExc(toRemove, false);
    }

    default @Nullable Term[] subsIncExc(MetalBitSet s, boolean includeOrExclude) {

        var c = s.cardinality();

        if (c == 0) {
//            if (!includeOrExclude)
//                throw new UnsupportedOperationException("should not reach here");
            return includeOrExclude ? EmptyTermArray : arrayShared();
        }

        var size = subs();
        assert (c <= size) : "bitset has extra bits setAt beyond the range of subterms";

        if (includeOrExclude) {
            if (c == size) return arrayShared();
            if (c == 1) return new Term[]{sub(s.first(true))};
        } else {
            if (c == size) return EmptyTermArray;
            if (c == 1) return removing(s.first(true));
        }


        var newSize = includeOrExclude ? c : size - c;
        var t = new Term[newSize];
        var j = 0;
        for (var i = 0; i < size; i++) {
            if (s.test(i) == includeOrExclude)
                t[j++] = sub(i);
        }
        return t;
    }

    default /* final */ Term[] subRangeReplace(int from, @Nullable Term fromInst, @Nullable Term toInst) {
        return subRangeReplace(from, subs(), fromInst, toInst);
    }

    /**
     * match a range of subterms of Y.
     * if replacing, fromInst matches by instance,
     * or NEGATED instance.  negations preserved in replacement
     *
     * WARNING: provides a shared (non-cloned) copy if the entire range is selected
     *
     * @param fromInst if a subterm is instance identical to fromInst, inline replaces with toInst
     */
    @Deprecated default Term[] subRangeReplace(int from, int to, @Nullable Term fromInst, @Nullable Term toInst) {
        var n = subs();
        if (from < 0) from = 0;
        if (to > n) to = n;

        if (from == 0 && to == n && fromInst == null) {
            return arrayShared();
        } else {

            var s = to - from;
            if (s == 0)
                return EmptyTermArray;
            else {
                var y = new Term[s];
                var j = from;
                for (var i = 0; i < s; i++) {
                    var x = sub(j++);
                    if (x == fromInst) x = toInst;
                    else if (x instanceof Neg && x.unneg() == fromInst) x = toInst.neg();
                    y[i] = x;
                }
                return y;
            }
        }
    }

    default int indexOf(Predicate<Term> p) {
        return indexOf(p, -1);
    }

    default int indexOf(Predicate<Term> p, int after) {
        var s = subs();
        Term prev = null;
        for (var i = after + 1; i < s; i++) {
            var next = sub(i);
            if (prev != next) {
                if (p.test(next))
                    return i;
                prev = next;
            }
        }
        return -1;
    }

    default boolean BOOL(Predicate<? super Term> t, boolean andOrOr) {
        var s = subs();
        //Term prev = null;
        for (var i = 0; i < s; i++) {
            var next = sub(i);
            //if (prev != next) {
                if (t.test(next) != andOrOr)
                    return !andOrOr;
            //    prev = next;
            //}
        }
        return andOrOr;
    }

    default /* final */ boolean AND(Predicate<? super Term> t) {
        return BOOL(t, true);
    }

    default /* final */ boolean OR(Predicate<? super Term> t) {
        return BOOL(t, false);
    }
    default /* final */ boolean ORunneg(Predicate<? super Term> t) {
        return OR(x -> t.test(x.unneg()));
    }
    default /* final */ boolean ANDunneg(Predicate<? super Term> t) {
        return AND(x -> t.test(x.unneg()));
    }

    /**
     * warning: elides test for repeated subterm
     */
    default <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        var s = subs();
        Term prev = null;
        for (var i = 0; i < s; i++) {
            var next = sub(i);
            if (prev != next) {
                if (p.test(next, param))
                    return true;
                prev = next;
            }
        }
        return false;
    }

    /**
     * warning: elides test for repeated subterm
     */
    default <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        var s = subs();
        Term prev = null;
        for (var i = 0; i < s; i++) {
            var next = sub(i);
            if (prev != next) {
                if (!p.test(next, param))
                    return false;
                prev = next;
            }
        }
        return true;
    }

    /**
     * visits each, incl repeats
     */
    default <X> boolean ANDwithOrdered(BiPredicate<Term, X> p, X param) {
        var s = subs();
        for (var i = 0; i < s; i++) {
            if (!p.test(sub(i), param))
                return false;
        }
        return true;
    }

    /**
     * must be overriden by any Compound subclasses
     */
    @Override
    default boolean boolRecurse(Predicate<Compound> inSuperCompound, BiPredicate<Term, Compound> whileTrue, Compound parent, boolean andOrOr) {
        if (this instanceof Term t) {
            if (!inSuperCompound.test((Compound) t)) //skip
                return andOrOr;
            else if (whileTrue.test(t, parent) != andOrOr) //short-circuit condition
                return !andOrOr;
        }

        return boolRecurseSubterms(inSuperCompound, whileTrue, compoundParent(parent), andOrOr);
    }

    default boolean boolRecurseSubterms(Predicate<Compound> inSuperCompound, BiPredicate<Term, Compound> whileTrue, Compound parent, boolean andOrOr) {
        //return AND(s -> s.ANDrecurse(aSuperCompoundMust, whileTrue, parent));
        var s = this instanceof Compound c ? c.subtermsDirect() : this;
        var n = s.subs();
        for (var i = 0; i < n; i++) {
            var next = s.sub(i);
            if (next.boolRecurse(inSuperCompound, whileTrue, parent, andOrOr) != andOrOr)
                return !andOrOr;
        }
        return andOrOr;
    }

    private Compound compoundParent(Compound parent) {
        return this instanceof Compound c ? c : parent;
    }

    default /* final */ boolean ANDrecurse(Predicate<Compound> inSuperCompound, Consumer<Term> each, Compound parent) {
        return ANDrecurse(inSuperCompound, (x) -> { each.accept(x); return true; }, parent);
    }

    default /* final */ boolean ANDrecurse(Predicate<Compound> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return boolRecurse(inSuperCompound, (x, p) -> whileTrue.test(x), compoundParent(parent), true);
    }

    /**
     * incl repeats
     */
    default boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        Subterms s;
        if (this instanceof Compound c) {
            if (!inSuperCompound.test(c))
                return true;
            if (!whileTrue.test(c))
                return false;
            s = c.subtermsDirect();
        } else
            s = this;

        parent = compoundParent(parent);

        var n = s.subs();
        for (var i = 0; i < n; i++)
            if (!s.sub(i).recurseTermsOrdered(inSuperCompound, whileTrue, parent))
                return false;
        return true;
    }

    /** produces a new result */
    default Subterms reverse() {
        return ReversedSubterms.reverse(this);
    }

    /**
     * removes first occurrence only
     */
    default Term[] removing(int index) {
        var s = subs();
        var x = new Term[s - 1];
        var k = 0;
        for (var j = 0; j < s; j++) {
            if (j != index)
                x[k++] = sub(j);
        }
        return x;

        //return ArrayUtils.remove(arrayShared(), Term[]::new, i);
    }

//	default @Nullable Term[] removing(Term x) {
//		if (impossibleSubTerm(x))
//			return null;
//		MetalBitSet toRemove = indicesOfBits(x::equals);
//		return toRemove.cardinality() == 0 ? null : removing(toRemove);
//	}

    default void copyTo(Term[] y, int start, int end) {
        for (var i = start; i < end; i++)
            y[i] = sub(i);
    }


//    /**
//     * dont override
//     */
//    default Subterms replaceSub(Term from, Term to) {
//        return !from.equals(to) && !impossibleSubTerm(from) ? transformSubs(MapSubst.replace(from, to), ATOM) : this;
//    }

//	default Subterms transformSub(int which, UnaryOperator<Term> f) {
//		Term x = sub(which);
//		Term y = f.apply(x);
//		if (!differentlyTransformed(x, y))
//			return this;
//
//		Term[] yy = arrayClone();
//		yy[which] = y;
//		return Op.terms.subterms(yy);
//	}

    /**
     * whether all subterms are internable
     */
    default boolean internables() {
        //return AND(Term::internable);
        // iterate in reverse so it can potentially fail faster since smaller terms are more likely to be at the end, at least when commutive
        var n = subs();
        for (var i = n - 1; i >= 0; i--)
            if (!sub(i).internable()) return false;
        return true;
    }

    default Predicate<Term> contains(boolean pn, boolean preFilter) {
        Predicate<Term> equality = switch (subs()) {
            case 0 -> x -> false;
            case 1 -> {
                var s0 = sub(0);
                yield pn ? s0::equalsPN : s0.equals();
            }
            default -> pn ? this::containsPN : this::contains;
        };
        if (preFilter) {
            var is = structSubs();
            var r = ((Predicate<Term>) x -> Op.hasAll(is, x.struct())).and(equality);
            return pn ? r.and(x -> r.test(x.unneg())) : r;
        } else
            return equality;
    }

    /**
     * (first-level only, non-recursive)
     * if contained within; doesnt match this target (if it's a target);
     * false if target is atomic since it can contain nothing
     */
    default boolean contains(Term x) {
        return indexOf(x) != -1;
    }

    default boolean containsInstance(Term t) {
        return ORwith((i, T) -> i == T, t);
    }

    default boolean containsInstancePN(Term t) {
        return ORwith((i, T) -> i.unneg() == T, t.unneg());
    }

    default boolean containsNeg(Term x) {
        return x instanceof Neg ?
            contains(x.unneg())
            :
            _containsNeg(x);
    }

    private boolean _containsNeg(Term x) {
        return (!(this instanceof TermMetadata) || hasAny(NEG)) //!impossibleSubStructure(x.structure() | NEG.bit) && !impossibleSubVolume(x.volume() + 1)
            &&
            ORwith((z, xu) -> z instanceof Neg && xu.equals(z.unneg()), x);
    }

    /**
     * tries both polarities
     */
    default boolean containsPN(Term x) {
        //TODO optimize

        //simple impl:
        //return contains(x) || containsNeg(x);

        var xu = x.unneg();
        return !impossibleSubTerm(xu) && ORwith((y, XU) -> XU.equals(y.unneg()), xu);
    }



    default int complexity() {
        //return 1 + sum(Termlike::volume);
        int s = subs(), v = 1;
        for (var i = 0; i < s; i++)
            v += sub(i).complexity();
        return v;
    }

    default int height() {
        return subs() == 0 ? 1 : 1 + max(Termlike::height);
    }

    default int varDep() {
        return sum(Term::varDep);
    }

    default int varIndep() {
        return sum(Term::varIndep);
    }

    default int varQuery() {
        return sum(Term::varQuery);
    }

    default int varPattern() {
        return sum(Term::varPattern);
    }

    default void write(ByteArrayDataOutput out) {
        var n = subs(); //assert(n < Byte.MAX_VALUE);
        out.writeByte(n);
        for (var i = 0; i < n; i++)
            sub(i).write(out);
    }

    /** TODO protected - assumes the superterm is INH */
    default boolean condOfInh(Subterms xu, int polarity) {
        if (this instanceof CondAtomic)
            return false;
        Term c0 = sub(0), c1 = sub(1);
        boolean c0c = c0.CONJ(), c1c = c1.CONJ();
        return (c0c || c1c) && condOfInh(xu, polarity, c0, c1, c0c, c1c);
    }

    private static boolean condOfInh(Subterms xu, int polarity, Term c0, Term c1, boolean c0c, boolean c1c) {
        Term e0 = xu.sub(0), e1 = xu.sub(1);
        if (c0c && c1c) {
            return switch(polarity) {
                case +1 -> condOfInhN(c0, c1, e0, e1);
                case -1 -> condOfInhNNeg(c0, c1, e0, e1);
                case  0 -> condOfInhN(c0, c1, e0, e1) || condOfInhN(c0, c1, e0.neg(), e1.neg()) || condOfInhNNeg(c0, c1, e0, e1);
                default -> throw new UnsupportedOperationException();
            };
        } else if (c1c)
            return c0.equals(e0) && ((Compound) c1).condOf(e1, polarity);
        else if (c0c)
            return c1.equals(e1) && ((Compound) c0).condOf(e0, polarity);
        else
            return false;
    }

    private static boolean condOfInhNNeg(Term c0, Term c1, Term e0, Term e1) {
        return condOfInhN(c0, c1, e0.neg(), e1) || condOfInhN(c0, c1, e0, e1.neg());
    }

    private static boolean condOfInhN(Term c0, Term c1, Term e0, Term e1) {
        return (c0.equals(e0) || ((Compound) c0).condOf(e0, +1))
               &&
               (c1.equals(e1) || ((Compound) c1).condOf(e1, +1));
    }

    default boolean hasSeq() {
        return !(this instanceof CondAtomic) && ORunneg(Term::SEQ);
    }

    default boolean internable(int complexityMax) {
        if (complexity() <= complexityMax) {
            //if (AND(Term::internable)) return true;
            int s = subs();
            for (int i = 0; i < s; i++) {
                if (!subUnneg(i).internable()) //subUnneg here assumes NEG is internable
                    return false;
            }
            return true;
        }
        return false;
    }

//    final class EventCounter implements Consumer<Term> {
//        private int c;
//
//        EventCounter(Subterms x) {
//            if (x instanceof Compound)
//                accept((Compound)x);
//            else
//                x.forEach(this);
//        }
//
//
//        @Override
//        public void accept(Term x) {
//            if (x instanceof Neg)
//                x = x.unneg();
//
//            if (x.CONJ()) {
//                ((Compound)x).events(this, x.dt()==DTERNAL, x.dt()==XTERNAL);
//            } else
//                c++;
//        }
//    }


//    default boolean identical(Subterms x) {
//		if (this == x) return true;
//		int s = subs(); if (x.subs()!=s) return false;
//		for (int i = 0; i < s; i++) {
//			if (sub(i)!=x.sub(i)) return false;
//		}
//		return true;
//	}


    final class SubtermsIterator implements Iterator<Term> {
        private final int s;
        private final Subterms terms;
        int i;

        private SubtermsIterator(Subterms terms) {
            this.terms = terms;
            this.s = terms.subs();
        }

        @Override
        public boolean hasNext() {
            return i < s;
        }

        @Override
        public Term next() {
            return terms.sub(i++);
        }
    }

}
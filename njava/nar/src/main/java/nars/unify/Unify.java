package nars.unify;

import jcog.Is;
import jcog.WTF;
import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import jcog.util.ArrayUtil;
import jcog.version.KeyUniVersioned;
import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Img;
import nars.term.atom.IntrinAtomic;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.TermTransform;
import nars.term.var.CommonVariable;
import nars.term.var.NormalizedVariable;
import nars.term.var.Variable;
import nars.unify.mutate.Termutator;
import nars.unify.unification.MapUnification;
import nars.unify.unification.OneTermUnification;
import nars.unify.unification.Termutification;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import static nars.Op.*;
import static nars.unify.Unification.Null;
import static nars.unify.Unification.Self;
import static nars.unify.Unifier.Equal;


/* recurses a pair of compound target tree's subterms
across a hierarchy of sequential and permutative fanouts
where valid matches are discovered, backtracked,
and collected until power is depleted.



https:
https:
see this code for a clear explanation of what a prolog unifier does.
this code does some additional things but shares a general structure with the lojix code which i just found now
So it can be useful for a more easy to understand rewrite of this class TODO


*/
public abstract class Unify extends Versioning<Term> implements TermTransform {

    /**
     * variable-variable (same op) modes
     */
    //private static final int VAR_NONE = 0;
//    public static final int VAR_COMMON = 1;
//    public static final int VAR_GREEDY = 2;
    private static final int ATOMIC = 0, VAR = 1, COMPOUND = 2;

    public final VersionMap<Variable, Term> xy;
    public final RandomGenerator random;
    /**
     * accumulates the next segment of the termutation stack
     */
    private final Lst<Termutator> termutes = new Lst<>(0);
    /**
     * bits of the unifiable variables; variables not unifiable are tested for equality only
     * TODO split into 'varAssignable', 'varCommonable', etc.. vectors.  then 'int vars' will contain the union of them
     */
    public int vars;
    private final RecursiveTermTransform F = new RecursiveTermTransform() {
        @Override
        public Term applyAtomic(Atomic x) {
            return var(x) ? resolveVar((Variable) x) : x;
        }

        @Override
        public Term applyCompound(Compound x) {
            return x.hasAny(vars) /* TODO more specific about which vars are actually assigned */ ? super.applyCompound(x) : x;
        }
    };
    /**
     * dtTolerance >= 1
     */
    public int dur = 1;
//    /**
//     * variable-variable (same op) mode
//     */
//    public int varvar = VAR_COMMON;
    /**
     * match counter
     */
    private transient int matches;


    /**
     * @param type if null, unifies any variable type.  if non-null, only unifies that type
     */
    protected Unify(@Nullable Op type, RandomGenerator random, int stackMax, int initialTTL) {
        this(type, random, stackMax);
        setTTL(initialTTL);
    }

    @Deprecated
    protected Unify(@Nullable Op type, RandomGenerator random, int stackMax) {
        this(type == null ? Variables : type.bit, random, stackMax);
    }

    protected Unify(int vars, RandomGenerator random, int stackMax) {
        this(vars, random, stackMax,
                //new TermHashMap<>()
                new UnifriedMap<>(4)
                //new HashMap<>(4)
        );
    }

    protected Unify(int vars, RandomGenerator random, int stackMax, Map termMap) {
        super(stackMax);

        this.random = random;
        this.vars = vars;

        this.xy = new ConstrainedVersionMap(termMap);
    }

    @Deprecated
    public static boolean isPossible(Term xx, Term yy, int var, int dur) {
        return how(xx, yy, var, dur, false) != null;
    }

    @Nullable
    public static AbstractUnifier how(Term x, Term y, int vars, int dur, boolean serious) {
        if (x == y) return Equal;

        boolean xn = x instanceof Neg, yn = y instanceof Neg;
        if (xn && yn) {
            x = x.unneg();
            y = y.unneg();
        }

        if (x.equals(y)) return Equal;

        if (NAL.unify.mobiusConst) {
            if (xn && !yn) {
                var xu = x.unneg();
                if (varAny(xu, vars))
                    if (assignable((Variable) xu, y))
                        return Unifier.DirectVarXNeg;

            }
            if (yn && !xn) {
                var yu = y.unneg();
                if (varAny(yu, vars))
                    if (assignable((Variable) yu, x))
                        return Unifier.DirectVarYNeg;

            }
        }

        var xv = varAny(x, vars);
        var yv = varAny(y, vars);

        if (xv && !yv) return Unifier.DirectVarX;
        if (yv && !xv) return Unifier.DirectVarY;
        if (xv) {
            if (x.opID() == y.opID()) return Unifier.DirectVarX; //common
            return Unifier.Direct;
        }

        if (x instanceof Compound X && y instanceof Compound Y)
            return howCompound(X, Y, vars, dur, serious);

        return null;
    }

    private static boolean varAny(Term x, int vars) {
        return x instanceof Variable && isAny(x, vars);
    }

    /**
     * can x be assigned to y (y <= x)
     */
    private static boolean assignable(Variable x, Term y) {
        return
            (y instanceof CommonVariable Y && Y.contains(x))
            ||
            (x instanceof CommonVariable X && y instanceof Variable _Y && X.contains(_Y))
            ||
            (
                (!y. /*isAny*/ hasAny(assignableExcl(x)))
                &&
                (!(y instanceof Compound) || !y.containsRecursively(x))
            );
    }

    private static int assignableExcl(Variable x) {
        return switch (x.op()) {
            case VAR_DEP -> VAR_PATTERN.bit | VAR_QUERY.bit | VAR_INDEP.bit | VAR_DEP.bit;
            case VAR_INDEP -> VAR_PATTERN.bit | VAR_QUERY.bit | VAR_INDEP.bit;
            case VAR_QUERY -> VAR_PATTERN.bit | VAR_QUERY.bit;
            case VAR_PATTERN -> VAR_PATTERN.bit;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Deprecated
    private static Consumer<Versioned<Term>> versionedToBiConsumer(BiConsumer<Term, Term> each) {
        return v -> {
/*            if (v instanceof KeyMultiVersioned)
                each.accept(((KeyMultiVersioned<Term, Term>) v).key, v.get());
            else */
            if (v instanceof KeyUniVersioned)
                each.accept(((KeyUniVersioned<Term, Term>) v).key, v.get());
        };
    }

    private static boolean unifyDT(Compound x, Compound y, int op, int dur) {

        var xdt = x.dt();
        if (xdt == XTERNAL) return true;

        var ydt = y.dt();
        if (ydt == XTERNAL) return true;

        //dont eliminate certain sequences where the top-level DT doesnt match but the seq actually might.
        //TODO more exclusion cases
        if (op==CONJ.id && (x.SEQ() || y.SEQ()))
            return true;

        return unifyDT(xdt, ydt, dur);
    }

    public final boolean unifyDT(int xdt, int ydt) {
        return unifyDT(xdt, ydt, dur);
    }

    /**
     * xdt and ydt must both not equal either XTERNAL or DTERNAL
     */
    private static boolean unifyDT(int xdt, int ydt, int dur) {
        return xdt == ydt ||
               Math.abs(zeroDT(xdt) - zeroDT(ydt)) < dur;
    }

    private static int zeroDT(int dt) {
        return dt == DTERNAL ? 0 : dt; //HACK
    }

    /**
     * assumes x and y have been tested for Neg first
     */
    @Nullable
    private static AbstractUnifier howCompound(Compound x, Compound y, int vars, int dur, boolean serious) {
        int op = x.opID;
        if (op != y.opID)
            return null;
        if (op == CONJ.id || op == IMPL.id) {
            if (!unifyDT(x, y, op, dur))
                return null;
        }

        return Unifier.howSubterms(op, x, y, vars, dur, serious);
    }

    public boolean unifies(Term xx, Term yy, int ttl) {
        var h = how(xx, yy, vars, dur, true);
        return h != null && h.apply(xx, yy, clear(ttl));
    }

//    private boolean unifyVariableGreedy(Variable x, Variable y) {
//        //subsume the higher id'd variable of the same type with the lower id'd
//        if (x.compareTo(y) > 0) {
//            //canonical ordering
//            Variable z = x;
//            x = y;
//            y = z;
//        }
//
//        return //_putXY(y, x) // y => x
//               // ||
//                uniVarDirect(x, y); // x => y
//    }

    private boolean uniVarCommon(Variable xx, Variable yy) {

        if (xx.equals(yy))
            return true; //already linked

        if (xx instanceof CommonVariable XX && (yy instanceof NormalizedVariable) && XX._contains(yy))
            return uniVarDirect(yy, xx);
        if (yy instanceof CommonVariable YY && (xx instanceof NormalizedVariable) && YY._contains(xx))
            return uniVarDirect(xx, yy);

        var xUnassigned =
                //xx == x;
                var(xx);

        var yUnassigned =
                //yy == y;
                var(yy);

        if (xUnassigned && yUnassigned)
            return _unifyVariableCommon(xx, yy);
        else if (xUnassigned)
            return uniVarDirect(xx, yy);
        else if (yUnassigned)
            return uniVarDirect(yy, xx);
        else
            return uni(xx, yy);
    }

    private boolean _unifyVariableCommon(Variable x, Variable y) {
        var xy = CommonVariable.common(x, y);
        return uniVarDirect(x, xy) && uniVarDirect(y, xy);
    }

    /**
     * call after having tested for equality
     */
    @Is({"Prolog", "Unification_(computer_science)", "Negation", "Möbius_strip", "Total_order", "Recursion"})
    private boolean uniVar(Variable x, Term y) {
        if (y instanceof Img)
            return false; //TODO other types?

        y = y instanceof Variable Y ? resolveVar(Y) : y;

        var xx = resolveVar(x);
        if (xx == y)
            return true;

        if (!var(xx))
            return uni(xx, y);

        x = (Variable) xx;



        if (x.opID() == y.opID())
            return uniVarCommon(x, (Variable) y);
        else {
            if (assignable(x, y))
                return uniVarDirect(x, y);
            else if (y instanceof Variable Y && assignable(Y, x))
                return uniVarDirect(Y, x);
        }
        return false;
    }
//
//    private boolean uniVarSame(Variable x, Variable y) {
//        //if (varvar != VAR_NONE) {
//        return switch (varvar) {
//            case VAR_COMMON -> unifyVariableCommon(x, y);
//            case VAR_GREEDY -> unifyVariableGreedy(x, y);
//            default -> throw new UnsupportedOperationException();
//        };
////        }
////        return false;
//    }

    public final Term postUnify(Term x) {
        return x.replace(xy);
    }

    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    protected abstract boolean match();

    public final boolean match(Termutator[] chain, int next) {
        return (++next < chain.length) ?
                chain[next].apply(chain, next, this) //next link
                :
                matches(); //end of chain (leaf)
    }

    /**
     * completely dereferences a target (usually a variable)
     */
    public Term resolveVar(Variable x) {
        var s = this.assignments;
        if (s == 0) return x; //nothing assigned

        var y = x;
//        int hops = 0;
        do {
            var z = xy.get(y);
            if (z instanceof Variable v) {
//                //TEMPORARY
//                if (z == x || ++hops > NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT)
//                    throw new WTF("var cycle detected");

                y = v; //loop
            } else {
                return z == null ? y : z;
            }
        } while (true);
//		}
    }

    /**
     * default usage: invokes the match callback if successful
     * shouldnt call directly
     */
    public final boolean unify(Term x, Term y) {
        return unify(x, y, true);
    }

    private int type(int o) {
        var oBit = 1 << o;
        if (hasAny(Compounds, oBit)) return COMPOUND;
        else if (hasAny(vars, oBit)) return VAR;
        else return ATOMIC;
    }

    /**
     * component unification
     * call 'unified' afterward to verify at least one permutation matches
     */
    public final boolean uni(Term x, Term y) {
        if (x == y) return true;

        boolean xn = x instanceof Neg, yn = y instanceof Neg;

        if (xn) {
            var xu = x.unneg();
            if (yn)
                return _uni(xu, y.unneg());

            if (NAL.unify.mobiusVar) {
                if (var(xu)) {
//                if (xu.equals(y))
//                    return false; //can not equal its neg
                    if (subsumeNeg((Variable) xu, y)) return uniVar((Variable) xu, y.neg());
                }
            }
        }

        if (NAL.unify.mobiusVar && yn) {
            var yu = y.unneg();
            if (var(yu)) {
//                if (yu.equals(x))
//                    return false; //can not equal its neg
                if (subsumeNeg((Variable) yu, x)) return uniVar((Variable) yu, x.neg());
            }
        }

        return _uni(x, y);
    }

    private boolean _uni(Term x, Term y) {
        int xo = x.opID(), yo = y.opID();
        var sameOp = xo == yo;
        if (sameOp && x.equals(y)) return true;

        var xt = type(xo);
        var xtv = xt == VAR;

        if (sameOp && !xtv)
            return xt == COMPOUND && uniCompound((Compound) x, (Compound) y);

        if (xtv)
            return uniVar((Variable) x, y);

        var ytv = type(yo) == VAR;
        if (ytv)
            return uniVar((Variable) y, x);

//        if (xtv && (!ytv || /* variable subsumption order */ xo <= yo))
//            return uniVar((Variable) x, y, ytv);
//
//        if (ytv)
//            return uniVar((Variable) y, x, false);

        return false;
    }

    private boolean subsumeNeg(Variable a, Term b) {
        if (!var(b)) return true;
        int ao = a.opID(), bo = b.opID();
        if (ao > bo) return false;
        else if (ao == bo) {
            if (a.equals(b))
                return false;

            boolean an = a instanceof NormalizedVariable, bn = b instanceof NormalizedVariable;
            if (an && bn)
                return (((IntrinAtomic) a).id() < ((IntrinAtomic) b).id());
            else
                return an || !an && !bn; //ex: any CommonVariable's
        } else
            return true;
    }

    private boolean uniCompound(Compound x, Compound y) {
        if (x instanceof Neg) {
            var xu = x.unneg();
            return y instanceof Neg ?
                    uni(xu, y.unneg()) :
                    (NAL.unify.mobiusConst && var(xu) && uni(xu, y.neg()));
        } else {
            if (y instanceof Neg) return false;

            var how = howCompound(x, y, vars, dur, true);
            return how != null && how.apply(x, y, this);
        }
    }

    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     * <p>
     * setting finish=false allows matching in pieces before finishing
     * <p>
     * NOT thread safe, use from single thread only at a time
     */
    public final boolean unify(Term x, Term y, boolean finish) {
        //try {
        return uni(x, y) && (!finish || anyMatches());
//        } catch (StackOverflowError e) {
//            throw new WTF("stack overflow: " + x + " " + y + "\n" + this.xy);
//        }
    }

    public boolean unified() {
        return termutes.isEmpty() || anyMatches();
    }

    private boolean anyMatches() {
//        for (Term y : xy.map.keySet())
//            if (xy.get(y)==null)
//                return false;//HACK something unassigned

        this.matches = 0;
        matches();
        return matches > 0;
    }

    /**
     * @noinspection ArrayEquality
     */
    public Unification unification(boolean clear) {
        var xyPairs = new Lst<Term>(size * 2 /* estimate */);

        var termutes = commitTermutes();
        assert (termutes != Termutator.CUT) : "this means fail"; //?

        BiConsumer<Term, Term> eachXY = xyPairs::addAll;
        if (clear) {
            clear(versionedToBiConsumer(eachXY));
        } else {
            forEach(eachXY);
        }

        var n = xyPairs.size() / 2;
        var base = switch (n) {
            case 0 -> Self;
            case 1 -> new OneTermUnification(xyPairs.get(0), xyPairs.get(1));
            default -> new MapUnification().putIfAbsent(xyPairs);
        };

        return termutes == null ? base :
                new Termutification(this, base, termutes);
    }

    @Deprecated
    final Unification unification(Term x, Term y, int discoveryTTL) {
        Unification u;
        if (!unify(x, y, false)) {
            clear();
            u = Null;
        } else
            u = unification(true);

        if (u instanceof Termutification T)
            T.discover(this, Integer.MAX_VALUE, discoveryTTL);

        return u;
    }

    /**
     * @noinspection ArrayEquality
     */
    private boolean matches() {
        var t = commitTermutes();
        if (t == null)
            return matchNext();
        else if (t == Termutator.CUT)
            return false;
        else
            return match(t, -1); //recurse
    }

    private boolean matchNext() {
        this.matches++;
        var kont = use(NAL.derive.TTL_COST_MATCH);  //pre-pay
        return match() && kont;
    }


    /**
     * termutes will have been cleared before this function returns
     */
    @Nullable
    private Termutator[] commitTermutes() {
        var t = termutes;
        if (t.isEmpty()) return null;

        var tt = _commitTermutes(t);
        termutes.clear();
        return tt;
    }

    @Nullable
    private Termutator[] _commitTermutes(Lst<Termutator> t) {
        var s = t.size();
        var removed = false;
        Set<Termutator> seen = null;
        for (var i = 0; i < s; i++) {
            var x = t.get(i);
            Termutator y;
            if (seen != null && !seen.add(x)) {
                y = Termutator.ELIDE; //deduplicated
            } else {
                y = x.commit(this);
                if (y == null)
                    return Termutator.CUT;
                if (t.size()!=s)
                    s = t.size(); //new termutes may have been appended
            }

            if (y == Termutator.ELIDE) {
                //if (--s == 0) return null; //last one
                t.nullify(i);
                removed = true;
            } else if (x != y && !x.equals(y)) {
                t.setFast(i, y);
            }

            if (seen == null && i < s - 1)
                (seen = new UnifiedSet<>(s - i)).add(x); //first one
        }

        if (removed) t.removeNulls();

        //assert(!t.isEmpty());

        return commit(t);
    }

    private Termutator[] commit(Lst<Termutator> t) {
        var tt = t.toArray(Termutator.CUT /* 0 len array */);

        if (NAL.unify.SHUFFLE_TERMUTES && tt.length > 1)
            ArrayUtil.shuffle(tt, random);

        return tt;
    }

    @Override
    public Unify clear() {
//        xy.map.clear(); //HACK?
        clear(null);
        return this;
    }

    public Unify clear(int ttl) {
        clear();
        setTTL(ttl);
        return this;
    }

    private void clear(@Nullable Consumer<Versioned<Term>> each) {
        revert(0, each);
    }

    @Override
    public String toString() {
        return xy + "$" + this.ttl;
    }

    public final boolean var(int varID) {
        return varBit(1 << varID);
    }

    private boolean varBit(int varBit) {
        return (this.vars & varBit) != 0;
    }

    public final boolean var(Term x) {
        return x instanceof Variable X && var(X);
    }

    private boolean var(Variable x) {
        return varBit(x.structOp());
    }

    /**
     * how many matchable variables are present
     */
    public int vars(Term x) {
        return switch (x) {
            case Compound X        -> vars(X);
            case Variable variable -> (0 != (vars & x.structOp())) ? 1 : 0;
            case null, default     -> 0;
        };
    }

    private int vars(Compound x) {
        var s = x.struct();
        if (hasAny(s, vars)) {
            var c = 0;
            if (0 != (s & VAR_PATTERN.bit)) c += x.varPattern();
            if (0 != (s & VAR_QUERY.bit))   c += x.varQuery();
            if (0 != (s & VAR_DEP.bit))     c += x.varDep();
            if (0 != (s & VAR_INDEP.bit))   c += x.varIndep();
            return c;
        }
        return 0;
    }

    private void forEach(BiConsumer<Term, Term> each) {
        forEach(versionedToBiConsumer(each));
    }

    /**
     * returns true if the assignment was allowed, false otherwise
     * args should be non-null. the annotations are removed for perf reasons
     */
    @Deprecated
    public final boolean put(Variable x, Term y) {
        //assert(!x.equals(y));
        return var(x) && /* TODO assignable ? */ uniVarDirect(x, y);
    }

    private boolean uniVarDirect(Variable x, Term y) {
        return this.xy.set(x, y);
    }

    protected final void constrain(UnifyConstraint[] m) {
        for (var mm : m)
            constrain(mm);
    }

    public final void constrain(UnifyConstraint m) {
        ((ConstrainedVersionedTerm) xy.getOrCreateIfAbsent(m.x)).constrain(m, xy);
    }

    public Term resolveTerm(Term x) {
        return resolveTerm(x, false);
    }

    /**
     * full resolution of a term
     */
    private Term resolveTerm(Term i, boolean recurse) {
        if (assignments == 0) return i;

        var neg = i instanceof Neg;
        var x = neg ? i.unneg() : i;

        var xv = var(x);

        if (!xv && !x.hasAny(vars)) return i;

        var y = xv ? resolveVar((Variable) x) : x;

        if (recurse && y instanceof Compound && x != y && y.hasAny(vars))
            y = F.apply(y); //recurse (full transform)

        return x == y ?
                i
                :
                (neg ? y.neg() : y);
    }

    @Override
    public final Term apply(Term x) {
        return F.apply(x);
    }

    final @Nullable TermList resolveListIfChanged(Subterms x) {
        if (assignments == 0) return null; //no assignments

        var y = RecursiveTermTransform.transformSubs(x, this::resolveTerm);
        if (y == null)
            return new TermList(Bool.Null); //HACK
        else if (y != x)
            return y instanceof TermList yy ? yy : y.toList();
        else
            return null;
    }

    public final void termute(Termutator t) {
        termutes.add(t);
    }

    public final Unify ttl(int ttlUnisubst) {
        setTTL(ttlUnisubst);
        return this;
    }

    boolean uniVarUnneg(Term x, Term y) {
        if (x instanceof Neg) {
            assert (y instanceof Neg);
            x = x.unneg();
            y = y.unneg();
        }
        return uniVar((Variable) x, y);
    }

    static final class ConstrainedVersionedTerm extends KeyUniVersioned<Term, Term> {

        /**
         * lazily constructed
         */
        UnifyConstraint<Unify> constraint;

        private Versioned unconstrain;

        ConstrainedVersionedTerm(Term key) {
            super(key);
        }

        @Override
        protected boolean valid(Term x, Versioning<Term> context) {
            if (NAL.DEBUG) {
                if (!assignable((Variable) key, x))
                    throw new WTF("should not have been assigned"); //return false;
            }

            var c = this.constraint;
            return c == null || !c.invalid(x, (Unify) context);
        }

        void constrain(UnifyConstraint m, VersionMap<Variable, Term> xy) {
            //assert(constraint == null): "at most 1 constraint may be enabled";
            constraint = m;
            var u = this.unconstrain;
            xy.context.add(u == null ? (this.unconstrain = new VersionedConstraint()) : u);
        }

        private final class VersionedConstraint implements Versioned {

            @Override
            public Object get() {
                //assert (constraint != null);
                return constraint;
            }

            @Override
            public void pop() {
                constraint = null;
            }

        }
    }

    /**
     * extension adapter, can be used to extend a Unify
     */
    public static class ContinueUnify extends Unify {

        /**
         * if xy is null then inherits the Map<Term,Term> from u
         * otherwise, no mutable state is shared between parent and child
         */
        public ContinueUnify(Unify parent, @Nullable Map xy) {
            super(parent.vars, parent.random, parent.items.length,
                    xy != null ? xy : parent.xy);
            dur = parent.dur;
            //TODO any other flags?
        }

        @Override
        protected boolean match() {
            return true;
        }
    }

    private final class ConstrainedVersionMap extends VersionMap<Variable, Term> {
        ConstrainedVersionMap(Map<Variable, Versioned<Term>> m) {
            super(Unify.this, m);
        }

        @Override
        public int merge(Term prev, Term next) {
            return prev.equals(next) ||
                    ((!(prev instanceof Variable) || !(next instanceof Variable)) &&
                            uni(resolveIfVar(prev), resolveIfVar(next))) ? 0 : -1;

            //return uni(prev, next) ? 0 : -1; //re-entrant
            //return prev.equals(next) ? 0 : -1;
        }

        private Term resolveIfVar(Term next) {
            return var(next) ? resolveVar((Variable) next) : next;
        }

        @Override
        public Versioned<Term> apply(Variable x) {
            return new ConstrainedVersionedTerm(x);
        }
    }


}
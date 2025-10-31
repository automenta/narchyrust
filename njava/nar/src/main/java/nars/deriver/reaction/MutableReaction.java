package nars.deriver.reaction;

import jcog.Log;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import jcog.util.ArrayUtil;
import nars.*;
import nars.Deriver.PremiseUnify;
import nars.deriver.op.CeqConstraint;
import nars.deriver.op.HasBelief;
import nars.deriver.op.PredicateConstraints;
import nars.deriver.op.SubtermEquality;
import nars.deriver.util.PremiseTermAccessor;
import nars.deriver.util.PuncMap;
import nars.link.TaskLink;
import nars.premise.NALPremise;
import nars.subterm.Subterms;
import nars.subterm.util.SubtermCondition;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.NOT;
import nars.term.control.PREDICATE;
import nars.term.control.TermMatching;
import nars.term.util.Image;
import nars.term.util.TermException;
import nars.term.util.TermPaths;
import nars.term.var.Variable;
import nars.unify.UnifyConstraint;
import nars.unify.constraint.*;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static jcog.util.ArrayUtil.indexOf;
import static nars.Op.*;
import static nars.Premise.Belief;
import static nars.Premise.Task;
import static nars.subterm.util.SubtermCondition.*;
import static nars.term.atom.Bool.Null;
import static nars.term.util.transform.Retemporalize.patternify;
import static nars.unify.constraint.RelationConstraint.NotEqual;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * mutable class collecting components for reaction construction.
 */
public abstract class MutableReaction extends Reaction<Deriver> {

    private static final int separateBitsMin = 2,
            separateBitsMax =
                //2
                3
                //Integer.MAX_VALUE
    ;

    /** task=from */
    public static final Variable PremiseTask = $.varPattern(1);

    /** belief=to */
    public static final Variable PremiseBelief = $.varPattern(2);

    public static final PREDICATE<Deriver> TaskBeliefEqual = new PREDICATE<>($.func("equal", Task, Belief)) {
        @Override
        public boolean test(Deriver d) {
            var p = d.premise;
            return p.from().equals(p.to());
        }

        @Override
        public float cost() {
            return 0.05f;
        }

    };

    public static final PREDICATE<Deriver> TaskBeliefEqualRoot = new PREDICATE<>($.func("equalRoot", Task, Belief)) {

        @Override
        public boolean test(Deriver d) {
            var p = d.premise;
            return p.from().equalsRoot(p.to());
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    };

    public static final PREDICATE<Deriver> conjParallelTaskBelief = new PREDICATE<>("conjParallelTaskBelief") {

        private static boolean para(Term t) {
            return t.CONJ() && !t.SEQ();
        }

        @Override
        public boolean test(Deriver d) {
            return para(d.premise.from()) && para(d.premise.to());
        }

        @Override
        public float cost() {
            return 0.05f;
        }
    };

    private static final Atomic POS = Atomic.atomic("pos");

    public static final PREDICATE<Deriver> taskPos = new PREDICATE<>($.p(Task, POS)) {
        @Override
        public boolean test(Deriver d) {
            return taskPolarity(d);
        }

        @Override
        public float cost() {
            return 0.02f;
        }
    };

    public static final PREDICATE<Deriver> beliefPos = new PREDICATE<>($.p(Belief, POS)) {
        @Override
        public boolean test(Deriver d) {
            return beliefPolarity(d);
        }

        @Override
        public float cost() { return 0.02f; }
    };

    public static final PREDICATE<Deriver> taskEternal = new PREDICATE<>("taskEternal") {
        @Override public boolean test(Deriver d) { return d.premise.task().ETERNAL(); }
        @Override public float cost() { return 0.02f; }
    };
    public static final PREDICATE<Deriver> beliefEternal = new PREDICATE<>("beliefEternal") {
        @Override public boolean test(Deriver d) { return d.premise.task().ETERNAL(); }
        @Override public float cost() { return 0.02f; }
    };

    public static final PREDICATE<Deriver> taskInput = new PREDICATE<>("taskInput") {
        @Override
        public boolean test(Deriver d) {
            return d.premise.task().isInput();
        }

        @Override
        public float cost() {
            return 0.02f;
        }
    };
    public static final PremiseTermAccessor TaskTermAccessor = new PremiseTermAccessor(0, Premise.TaskInline) {
        @Override public Term apply(Deriver d) {
            return d.premise.from();
        }
    };
    public static final PremiseTermAccessor BeliefTermAccessor = new PremiseTermAccessor(1, Premise.BeliefInline) {
        @Override public Term apply(Deriver d) {return d.premise.to(); }
    };
    public static final PREDICATE<Deriver> premiseTaskLink = new PREDICATE<>($.inh("tasklink", "premise")) {
        @Override
        public boolean test(Deriver d) {
            return d.premise instanceof TaskLink;
        }

        @Override
        public float cost() {
            return 0.001f;
        }
    };
    public static final PREDICATE<Deriver> PremiseSelf = new PREDICATE<>("SelfPremise") {
        @Override
        public boolean test(Deriver d) {
            return d.premise.self();
        }

        @Override
        public float cost() {
            return 0.01f;
        }
    };
    protected static final PREDICATE<Deriver> NonImages = new PREDICATE<>("NonImages") {
        @Override
        public boolean test(Deriver deriver) {
            var p = deriver.premise;
            return Image.imageNormalized(p.from()) && Image.imageNormalized(p.to());
        }

        @Override
        public float cost() {
            return 0.01f;
        }
    };
    static final PREDICATE<Deriver> NoOverlap = new PREDICATE<>("no_overlap") {
        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean test(Deriver d) {
            var o = ((NALPremise) d.premise).overlapDouble();
            return !o || d.randomBoolean(d.nar.loopy.floatValue());
        }
    };


    private static final Logger logger = Log.log(MutableReaction.class);
    private static final Map<PREDICATE, PREDICATE> preds = new ConcurrentHashMap<>(1024);
    /**
     * cause tag for grouping rules under the same cause. if null, generates a unique cause
     */
    @Nullable
    public String tag;
    /**
     * optional for debugging and other purposes
     */
    public String source = getClass().getSimpleName() + "@" + System.identityHashCode(this);
    protected Term taskPattern = Null;
    Term beliefPattern = Null;
    /**
     * final constructed form, for runtime usage
     */
    transient UnifyConstraint<PremiseUnify>[] constraints;
    /**
     * arity: 0 = unknown, 1 = single, 2 = double
     */
    transient int arity;
    /**
     * constraints as input.  the active constraints in 'this.constraints' may be different and fewer once compiled.
     */
    private MutableSet<UnifyConstraint<PremiseUnify>> _constraints = new UnifiedSet<>(4);
    private MutableSet<PREDICATE<Deriver>> _pre = new UnifiedSet<>();

    private static PremiseTermAccessor TaskOrBelief(boolean taskOrBelief) {
        return taskOrBelief ? TaskTermAccessor : BeliefTermAccessor;
    }

    public final MutableReaction taskIsNot(Op... o) {
        return isNotAny(PremiseTask, o);
    }

    /**
     * TODO fix the bit separation, at least isNotAny has a bug
     */
    private static boolean separateBits(int struct) {
        int bc = Integer.bitCount(struct); return bc >= separateBitsMin && bc <= separateBitsMax;
        //return false; //TODO get working
    }

    private static <X> PREDICATE<X> intern(PREDICATE<X> y) {
        var z = preds.putIfAbsent(y, y);
        return z != null ? z : y;
    }

    private static boolean taskPolarity(Deriver d) {
        return d.premise.task().POSITIVE();
    }
    private static boolean beliefPolarity(Deriver d) {
        return d.premise.belief().POSITIVE();
    }

    protected final Reaction taskEqualsBelief() {
        pre(PremiseSelf);
        return this;
    }

    /**
     * single premise
     */
    public final MutableReaction single(boolean b, boolean g, boolean q, boolean Q) {
        taskPunc(b, g, q, Q, false);
        taskEqualsBelief();
        hasBeliefTask(false);
        return this;
    }

    public final MutableReaction single() {
        return single(true, true, true, true);
    }

//    public void neqPN(Variable x, Variable y) {
//        eqPN(x, y, false);
//    }

    public final MutableReaction singleNonSelf(boolean b, boolean g, boolean q, boolean Q) {
        taskPunc(b, g, q, Q, false);
        pre(PremiseSelf.neg());
        hasBeliefTask(false);
        return this;
    }

    /**
     * match a command, ex: tasklink
     */
    public void taskCommand() {
        taskPunc(false, false, false, false, true);
    }

    public void tasklink() {
        taskCommand();
        pre(premiseTaskLink);
    }

    public void noOverlap() {
        pre(NoOverlap);
    }

    public MutableReaction taskPattern(Term x) {

        var p = patternify(x);

        if (p instanceof Neg || !p.VAR_PATTERN() && !p.TASKABLE())
            throw new TermException("invalid task pattern", p);

        this.taskPattern = p;

        return this;
    }

    public MutableReaction beliefPattern(Term x) {

        var p = Util.maybeEqual(patternify(x), taskPattern);

        if (p instanceof Neg)
            throw new TermException("belief pattern can never be NEG", p);

        this.beliefPattern = p;

        return this;
    }

    public MutableReaction taskPunc(boolean belief, boolean goal, boolean question, boolean quest) {
        return taskPunc(belief, goal, question, quest, false);
    }

    public final MutableReaction taskPunc(byte... puncs) {
        if (puncs == null || puncs.length == 0) {
            //return; //no filtering
            throw new WTF();
        }

        return taskPunc(
                indexOf(puncs, BELIEF) != -1,
                indexOf(puncs, GOAL) != -1,
                indexOf(puncs, QUESTION) != -1,
                indexOf(puncs, QUEST) != -1
        );
    }

    public final MutableReaction taskPunc(boolean belief, boolean goal, boolean question, boolean quest, boolean command) {
        return taskPunc(new PuncMap(
                belief ? BELIEF : 0, goal ? GOAL : 0,
                question ? QUESTION : 0, quest ? QUEST : 0,
                command ? COMMAND : 0));
    }

    public final MutableReaction taskPunc(PuncMap m) {
        _pre.removeIf(x -> x instanceof PuncMap); //remove any existing
        //TODO warn

//        if (pre.anySatisfy(isPuncMap))
//            throw new UnsupportedOperationException("puncmap already set: " + this + " " + pre);

        if (!m.all())
            pre(m); //add filter to allow only the mapped types
        return this;
    }

    /**
     * conjunction condition equality
     */
    public void ceq(Term xs, Term ys, boolean trueOrFalse) {
        ceqdeq(xs, true, ys, true, trueOrFalse);
    }

    /**
     * disjunction condition equality
     */
    public void deq(Term xs, Term ys, boolean trueOrFalse) {
        ceqdeq(xs, false, ys, false, trueOrFalse);
    }

    /**
     * conj/disj condition equality
     */
    public void ceqdeq(Term xs, boolean xc, Term ys, boolean yc, boolean trueOrFalse) {
        constrain(new CeqConstraint(xs, xc, ys, yc), trueOrFalse);
        if (trueOrFalse) {
            constrain(new RelationConstraint.StructureCoContained(
                    (Variable) xs.unneg(), (Variable) ys.unneg(),
                    //(Variable) xs.unnegIf(!xc || !yc), (Variable) ys.unnegIf(!xc || !yc),
                    CONJ.bit //incase INH in INH bundled, the && wont be present in INH only
                    //0
                    //!xc || !yc ? NEG.bit : 0
                    //CONJ.bit | NEG.bit
            ));
        }
    }

    public void condConstraint(Term container, Term x, boolean exactOrUnifiable, boolean xPN) {
        if (!(container instanceof Variable C))
            throw new UnsupportedOperationException();

        var xNeg = x instanceof Neg;
        if (!(!xPN || !xNeg))
            throw new UnsupportedOperationException();

        var xu = x.unneg();
        if (!(xu instanceof Variable X))
            throw new UnsupportedOperationException();

        eventContainer(C);

        condable(X);

        if (exactOrUnifiable) {
            //EXACT
            constrain(new SubConstraint(Cond, C, X, xPN ? 0 : (xNeg ? -1 : +1)));
            bigger(C, X); //TODO valid for PN? (+-1 for Neg)
        } else {
            //if (xPN) throw new UnsupportedOperationException();
            neq/*neqRoot*/(C, X);

            //TODO test more carefully, may be excluding positive cases
            biggerIfConstant(C, X);
            //isNotAny(X, Variables); //?? doesn't this defeat the purpose of unification
        }
    }

    public final void eventContainer(Variable x) {
        iff(x, TermMatch.Conds);
    }

    public final void condOf(SubtermCondition cond, Variable x, Term y, boolean bipolar, boolean trueOrFalse) {
        condOf(cond, x, (Variable) y.unneg(), y instanceof Neg, bipolar, trueOrFalse);
    }

    /**
     * bigger is necessary if CONJ_WITHOUT_ALL sub-events that must be matched individually
     */
    private void condOf(SubtermCondition cond, Variable x, Variable y, boolean yNeg, boolean bipolar, boolean trueOrFalse) {
        constrain(new SubConstraint<>(cond, x, y, bipolar ? 0 : yNeg ? -1 : +1), trueOrFalse);

        if (trueOrFalse) {
            eventContainer(x);
            condable(y);

            /* TODO determine if true is 100% ok */
            var bigger =
                    //false;
                    true;
            if (bigger) {
                bigger(x, false, y, yNeg);
            } /*else
                neqPN(x, y);*/
        }
    }

    void neqRCom(Variable x, Variable y) {
        neq(x, y);
        constrain(new RelationConstraint.NotRecursiveSubtermOf(x, y));
    }

    public final MutableReaction in(Variable x, Term y) {
        return in(x, y, true);
    }

    /**
     * x contains recursively y
     */
    public MutableReaction in(Variable x, Term y, boolean trueOrFalse) {
        return in(x, y, trueOrFalse, Recursive, y instanceof Neg ? -1 : +1);
    }

    public MutableReaction subtermOf(Variable x, Term y, boolean trueOrFalse, int polarity) {
        return in(x, y, trueOrFalse, Subterm, polarity);
    }

    public MutableReaction in(Variable x, Term y, boolean trueOrFalse, SubtermCondition contains, int polarity) {
        var yV = (Variable) y.unneg();
        bigger(x, false, yV, y instanceof Neg); //TODO +1 margin if neg
        return constrain(new SubConstraint<>(contains, x, yV, polarity), trueOrFalse);
    }

    protected void hasBeliefTask(boolean trueOrFalse) {
        var nextArity = trueOrFalse ? 2 : 1;
        if (arity != nextArity) {
            if (arity != 0)
                throw new WTF("premise arity already set: " + arity + " is not " + nextArity);
            this.arity = nextArity;
        }
    }

    private void guard(boolean taskOrBelief) {
        guard(taskOrBelief, null, null);
    }

    private void guard(boolean taskOrBelief, @Nullable Term /* Compound */ x, @Nullable ByteArrayList p) {

        int depth;
        byte[] pp;

        var root = taskOrBelief ? taskPattern : beliefPattern;

        if (p == null) {
            if (root.VAR_PATTERN())
                return; //top-level VAR_PATTERN, nothing to test
            else {
                //start descent
                pp = ArrayUtil.EMPTY_BYTE_ARRAY;
                assert x == null;
                x = root;
                depth = 0;
            }
        } else {
            //continue descent
            pp = p.toArray();
            depth = pp.length;
        }


        var r = taskOrBelief ? TaskTermAccessor : BeliefTermAccessor;
        var accessor = depth == 0 ? r : r.path(pp);

        guardStruct(x, depth, accessor);
        guardVolMin(x, depth, accessor);

        var xx = x.subtermsDirect();
        if ((xx.struct() & ~VAR_PATTERN.bit) == 0)
            return; //just pattern vars beneath
        var n = xx.subs();

        if (x.COMMUTATIVE()) {
            guardCommutiveSubterms(xx, depth, accessor);
        } else if (n > 0) {
            if (p == null) p = new ByteArrayList(4);

            for (byte i = 0; i < n; i++) {
                if (i == 0)
                    p.add((byte) 0);
                else
                    p.set(depth, i);
                var y = xx.sub(i);
                if (!y.VAR_PATTERN())
                    guard(taskOrBelief, y, p);
            }
            p.removeAtIndex(p.size() - 1);//popByte();
        }
    }

    private void guardCommutiveSubterms(Subterms x, int depth, Function<Deriver, Term> accessor) {
        //TODO fully recursive
        //commutive term
        //proceed differently because of indeterminacy //TODO any other clues to find?
        //any properties shared by ALL elements of the commutive term can be tested as a precondition

        var structSurface = x.structSurface(); //x.sub(0).opBit(); boolean sameOp = opBit == x.sub(1).opID();
        if ((structSurface & Variables) == 0) { //if all are non-variables

            var structCommon = x.structureIntersection(); //x.sub(0).opBit(); boolean sameOp = opBit == x.sub(1).opID();
            var volMin = x.volMin();

            SortedSet<TermMatch> matchers = new TreeSet<>(TermMatch.paramComparator);

//            if (structSurface != 0)
//                matchers.add(TermMatch.Is.is(structSurface));
            if ((structCommon & Variables) == 0 && structCommon != structSurface)
                matchers.add(new TermMatch.HasAllStruct(structCommon));
            if (volMin > 1)
                matchers.add(new TermMatch.ComplexMin(volMin));

            //TODO A eqNeg B?

            if (!matchers.isEmpty()) {
                pre(new TermMatching<>(
                        new TermMatch.TermMatcherSubterms(TermMatch.AND(matchers)),
                        accessor, depth
                ));
            }
        }

    }

//    public final MutableReactionBuilder complexityMin(Term x, int min) {
//        volMin(x, min);
//        return iff(x, new TermMatcher.ComplexityMin(min));
//    }

    private void guardVolMin(Term t, int depth, Function<Deriver, Term> accessor) {
        //volume constraint
        var v = t.complexity();
        if (v > 1)
            pre(new TermMatching<>(new TermMatch.ComplexMin(v), accessor, depth));
    }

    private void guardStruct(Term t, int depth, Function<Deriver, Term> accessor) {
        //structure constraint

        var o = t.op();
        assert o != VAR_PATTERN;
        var xs = t.structSubs() & ~VAR_PATTERN.bit;

        pre(new TermMatching<>(TermMatch.Is.is(o), accessor, depth));

        if (xs != 0 && Integer.bitCount(xs) > 2)
            pre(new TermMatching<>(new TermMatch.HasAllStruct(xs), accessor, depth/*+1*/));
    }

    public void eqPN(Variable x, Variable y, boolean trueOrFalse) {
        if (trueOrFalse) {
            constrain(new EqualPosOrNeg(x, y));
        } else {
            neq(x, y);
            neqNeg(x, y);
        }
    }

    public MutableReaction eq(Variable x, Variable y) {
        constrain(new Equal(x, y));
        return this;
    }

    public MutableReaction neq(Variable x, Term y) {
        switch (y) {
            case Neg neg when y.unneg() instanceof Variable ->
                neqNeg(x, (Variable) y.unneg());
            case Variable v ->
                constrain(new NotEqual(x, v));
            case null, default ->
                iffNot(x, new TermMatch.Equals(y));
        }
        return this;
    }

    private void neqNeg(Variable x, Variable y) {
        constrain(new EqualNeg(x, y), false);
    }

    public void neqRoot(Variable x, Variable y) {
        constrain(new RelationConstraint.NotEqualRoot(x, y));
    }

    public final void bigger(Variable x, Term y) {
        y = y.unneg();
        bigger(x, (Variable) y);
    }

    public final void bigger(Variable x, Variable y) {
        bigger(x, false, y, false);
    }

    public final void bigger(Variable x, boolean xNeg, Variable y, boolean yNeg) {
        if (xNeg && yNeg)
            xNeg = yNeg = false; //either or neither, not both

        if (xNeg == yNeg)
            neq(x, y);

        constrain(new ComplexCmp(x, y, false, +1 /* X > Y */, xNeg, yNeg));
    }

    public MutableReaction biggerIfConstant(Variable x, Variable y) {
        //TODO dangerous, check before using
        return constrain(new ComplexCmp(x, y, true, +1, false, false));
    }

    public final MutableReaction pre(PREDICATE/*<Deriver>*/... x) {
        for (var xx : x)
            pre(xx);
        return this;
    }

    public final MutableReaction constrain(UnifyConstraint<PremiseUnify> c, boolean trueOrFalse) {
        return constrain((UnifyConstraint<PremiseUnify>) c.negIf(!trueOrFalse));
    }

    public final MutableReaction pre(PREDICATE<Deriver> x, boolean trueOrFalse) {
        return pre((PREDICATE<Deriver>) x.negIf(!trueOrFalse));
    }

    public final MutableReaction pre(PREDICATE<Deriver> x) {
        if (NAL.DEBUG && _pre.contains(x.neg()))
            throw new WTF();//contradiction
        if (!_pre.add(x)) {
            if (NAL.DEBUG)
                logger.warn("adding duplicate condition {}\n{}\n{}", x, tag, source);
        }
        return this;
    }

    public final MutableReaction constrain(UnifyConstraint<PremiseUnify> x) {
        //TODO test for presence of contradiction: --x
        if (!_constraints.add(x)) {
            if (NAL.DEBUG)
                logger.warn("adding duplicate constraint {}\n{}\n{}", x, tag, source);
        }
        return this;
    }


    protected MutableReaction iff(Term x,
                           BiConsumer<byte[], byte[]> preDerivationExactFilter,
                           @Nullable Supplier<UnifyConstraint> ifNotPre
    ) {

        var pt = TermPaths.pathExact(taskPattern, x);
        var pb = TermPaths.pathExact(beliefPattern, x);// : null;
        if (pt == null && pb == null) {
            if (ifNotPre != null)
                constrain(ifNotPre.get());
        } else {
            if (pt != null && pb != null) {
                //only need to test one. use shortest path
                if (pb.length < pt.length)
                    pt = null;
                else
                    pb = null;
            }

            preDerivationExactFilter.accept(pt, pb);
        }
        return this;
    }

    public final MutableReaction iff(Term x, TermMatch m) {
        return iff(x, m, true);
    }

    public final MutableReaction iffNot(Term x, TermMatch m) {
        return iff(x, m, false);
    }

    public final MutableReaction volMin(Term x, int min) {
        return iff(x, new TermMatch.ComplexMin(min));
    }

    public final MutableReaction volMax(Term x, int max) {
        if (max < Integer.MAX_VALUE)
            iff(x, new TermMatch.ComplexMax(max));
        return this;
    }

    protected final Reaction volMaxMargin(Term x, int margin) {
        pre(new VolMaxMargin(x, margin));
        return this;
    }

    public final MutableReaction taskComplex(int min, int max) {
        return vol(taskPattern, min, max);
    }

    public final MutableReaction vol(Term x, int min, int max) {
        volMin(x, min);
        volMax(x, max);
        return this;
    }

    public final MutableReaction ifNot(Term x, TermMatch m) {
        return iff(x, m, false);
    }

    public final MutableReaction iff(Term x, TermMatch m, boolean trueOrFalse) {
        return iff(x, m, trueOrFalse, true);
    }

    /**
     * if x is SETe, then applies to all its subterms
     */
    public final void iffThese(Term x, TermMatch m, boolean trueOrFalse) {
        if (x.SETe()) {
            for (var xx : (Subterms) x)
                iff(xx, m, trueOrFalse);
        } else
            iff(x, m, trueOrFalse);
    }

    public MutableReaction iff(Term x, TermMatch m, boolean trueOrFalse, boolean constraintIfNotPre) {
        return iff(x, new ReactionBuilderPathMatch(m, trueOrFalse),
            constraintIfNotPre ?
                () -> m.constraint((Variable) x, trueOrFalse) : null);
    }

    /**
     * cost-sorted array of constraint enable procedures, bundled by common term via CompoundConstraint
     */
    private UnifyConstraint<PremiseUnify>[] reduce(SortedSet<UnifyConstraint<PremiseUnify>> x) {

        var n = x.size();
        if (n == 0)
            return UnifyConstraint.EmptyUnifyConstraints;

        var cc = x.stream();

        //TODO move this to a final step, after having folded common predicates
        if (n > 1)
            cc = cc.filter(new UnifyConstraintReducer(x)); //eliminate local subsumptions

        var p = new Lst<PREDICATE<Deriver>>();

        var uc = UnifyConstraint.the(cc.filter(c -> {
            var d = PredicateConstraints.constraintPredicate(c, taskPattern, beliefPattern);
            if (d != null) {
                if (d.unneg() instanceof PredicateConstraints.RelationPremisePredicateConstraint && c instanceof RelationConstraint)
                    p.add(d); //save for filter() in next stage
                else
                    pre(d);
                return false;
            }
            return true;
        }));

        filter(p);

        p.forEach(this::pre);

        return uc;
    }

    private static void filter(Lst<PREDICATE<Deriver>> p) {
        if (!p.isEmpty()) {
            p.sortThis(); //canonical ordering
            var sn = p.size();
            for (var i = 0; i < sn - 1; i++) {
                var I = p.get(i);
                if (I == null) continue;

                for (var j = i + 1; j < sn; j++) {
                    var J = p.get(j);
                    if (J == null) continue;
                    if (I instanceof NOT != J instanceof NOT)
                        continue;
                    if (I instanceof NOT) {
                        I = I.unneg();
                        J = J.unneg();
                    }

                    if (mirrors(I, J)) {
                        p.setNull(j);
                        break;
                    }
                }

            }

            p.removeNulls();
        }
    }

    private static boolean mirrors(PREDICATE<Deriver> I, PREDICATE<Deriver> J) {
        return ((PredicateConstraints.RelationPremisePredicateConstraint) I).symmetric() && PredicateConstraints.isMirror(I, J);
    }


    public final MutableReaction isAny(Term x, int struct) {
        return iff(x, TermMatch.Is.is(struct));
    }

    private MutableReaction _isNotAny(Term x, int struct) {
        return iffNot(x, TermMatch.Is.is(struct));
    }

    public MutableReaction hasAny(Term x, Op o) {
        return hasAny(x, o, true);
    }

    public MutableReaction hasAny(Term x, int structure) {
        return hasAny(x, structure, true);
    }

    public MutableReaction hasAny(Term x, Op o, boolean trueOrFalse) {
        return hasAny(x, o.bit, trueOrFalse);
    }

    public MutableReaction hasAny(Term x, int structure, boolean trueOrFalse) {
        return hasAnyOrAll(x, structure, trueOrFalse, true);
    }

    public final MutableReaction hasNone(Term x, int structure) {
        return hasAny(x, structure, false);
    }

    public MutableReaction hasAll(Term x, int structure, boolean trueOrFalse) {
        return hasAnyOrAll(x, structure, trueOrFalse, false);
    }

    public final MutableReaction isNotAny(Term x, int struct) {
        if (struct==0) throw new IllegalArgumentException();

        if (separateBits(struct)) {
            //NOT ANY = must test all, so can be decomposed into components
            var v = Op.values().length;
            for (var i = 0; i < v; i++) {
                var mask = 1 << i;
                if ((struct & mask) != 0)
                    _isNotAny(x, mask);
            }
            return this;
        }

        return _isNotAny(x, struct);
    }


    private MutableReaction hasAnyOrAll(Term x, int struct, boolean trueOrFalse, boolean anyOrAll) {
        if (struct==0) throw new IllegalArgumentException();

        if (!anyOrAll && separateBits(struct)) {
            //NOT ANY = must test all, so can be decomposed into components
            var v = Op.values().length;
            for (var i = 0; i < v; i++) {
                var mask = 1 << i;
                if ((struct & mask) != 0)
                    _hasAnyAll(x, mask, trueOrFalse, false);
            }
            return this;
        }

        return _hasAnyAll(x, struct, trueOrFalse, anyOrAll);
    }

    MutableReaction _hasAnyAll(Term x, int struct, boolean trueOrFalse, boolean anyOrAll) {
        return iff(x, anyOrAll ? new StructMatcher.HasAny(struct) : new StructMatcher.HasAll(struct), trueOrFalse);
    }

    public final MutableReaction isUnneg(Term x, int structAny, boolean trueOrFalse) {
        return iff(x, new TermMatch.IsUnneg(structAny), trueOrFalse);
    }

    public final MutableReaction is(Term x, Op o) {
        return isAny(x, o.bit);
    }

    public final MutableReaction hasNot(Term x, Op... o) {
        return hasAny(x, or(o), false);
    }

    public final MutableReaction isNot(Term x, Op o) {
        return isNotAny(x, o.bit);
    }

    public final MutableReaction isAny(Term x, Op... ops) {
        return isAny(x, or(ops));
    }


    public final MutableReaction isNotAny(Term x, Op... o) {
        return isNotAny(x, or(o));
    }

    public final void condable(Variable X) {
        isAny(X, Condables);
    }

    public void beliefPolarity(boolean b) { pre(beliefPos, b); }

    public void taskPolarity(boolean b) { pre(taskPos, b); }

    public void taskEternal(boolean b) {
        pre(taskEternal, b);
    }
    public void beliefEternal(boolean b) { pre(beliefEternal, b); }

    protected void taskInput(boolean b) {
        pre(taskInput, b);
    }

    @Override
    protected java.util.Set<PREDICATE<Deriver>> compileConditions() {
        commit();
        HashSet<PREDICATE<Deriver>> y = new HashSet<>(_pre.size());
        for (var predicate : _pre)
            y.add(intern(predicate));
        _pre = null;
        return y;
    }

    /**
     * expand mirror's
     */
    private SortedSet<UnifyConstraint<PremiseUnify>> mirror(MutableSet<UnifyConstraint<PremiseUnify>> x) {
        SortedSet<UnifyConstraint<PremiseUnify>> y = null;
        for (var c : x) {
            if (c instanceof RelationConstraint<PremiseUnify> C) {
                if (y == null) y = new TreeSet<>();
                y.add(C.mirror());
            }
        }
        if (y == null)
            return new TreeSet<>(x);
        else {
            y.addAll(x);
            return y;
        }
    }

    protected void commit() {

        Term t = this.taskPattern, b = this.beliefPattern;
        if (t instanceof Bool || b instanceof Bool)
            throw new TermException("Bool task or belief pattern");

        guard(true);
        guard(false);
        guardEqualities(t, b);

        /* constraints must be computed BEFORE preconditions as some constraints may be transformed into preconditions */
        constraints = reduce(mirror(_constraints));
        _constraints = null; //prevent further additions

        switch (arity) {
            case 1 -> pre(HasBelief.SINGLE);
            case 2 -> pre(HasBelief.DOUBLE);
            default -> { /* either. */ }
        }
    }

    private void guardEqualities(Term t, Term b) {
        if (t.equals(b)) {
            pre(TaskBeliefEqual);
            return;
        }
        if (t instanceof Variable && b instanceof Variable)
            return; //simple case

        var subterms = new UnifriedMap<Term, List<ObjectBooleanPair<byte[]>>>();
        for (var x : new Term[] { t, b }) {
            x.pathsTo(z -> true, z -> !z.COMMUTATIVE(), (path, y) -> {
                subterms.computeIfAbsent(y, z -> new Lst<>(1)).add(
                        pair(path.toArray(), x == t));
                return true;
            });
        }
        subterms.removeIf((x,paths)->paths.size()<2);

        if (!subterms.isEmpty())
            subterms.forEach((x, paths) -> pre(new SubtermEquality(paths)));
    }

    public final String tag() {
        return tag;
    }

    @Override
    public String toString() {
        return source;
    }

    public void compile() {
        conditions();
    }

    @Deprecated
    public static final class VolMaxMargin extends PREDICATE<Deriver> {
        private final int margin;

        @SuppressWarnings({"unused", "FieldCanBeLocal"}) //TODO
        private final Term pattern;

        VolMaxMargin(Term pattern, int margin) {
            super($.func("volMaxMargin", $.the(margin)));
            assert margin >= 1;
            this.pattern = pattern;
            this.margin = margin;
        }

        @Override
        public float cost() {
            return 0.08f;
        }

        @Override
        public boolean test(Deriver d) {
            return d.premise.from().complexity() <= d.complexMax - margin;
        }

    }

    static class UnifyConstraintReducer implements Predicate<UnifyConstraint<PremiseUnify>> {

        final UnifyConstraint[] copy;
        int remain;

        UnifyConstraintReducer(Set<UnifyConstraint<PremiseUnify>> x) {
            copy = x.toArray(UnifyConstraint.EmptyUnifyConstraints);
            remain = copy.length;
        }

        @Override
        public boolean test(UnifyConstraint<PremiseUnify> c) {
            if (remain < 2 || c.remainAmong(copy))
                return true;
            else {
                copy[ArrayUtil.indexOfInstance(copy, c)] = null;
                remain--;
                return false;
            }
        }
    }

    /** permits randomly based on Task frequency */
    static final class PolarityProb extends PREDICATE<Deriver> {
        private final boolean taskOrBelief, posOrNeg;

        /** extra specificity; 1 is disabled */
        private final static float power =
                1;
                //2;

        PolarityProb(boolean taskOrBelief, boolean posOrNeg) {
            super($.p(taskOrBelief ? Task : Belief, posOrNeg ? "\"+p\"" : "\"-p\""));
            this.taskOrBelief = taskOrBelief; this.posOrNeg = posOrNeg;
        }

        @Override
        public boolean deterministic() {
            return false;
        }

        @Override
        public boolean test(Deriver d) {
            var p = d.premise;
            var t = taskOrBelief ? p.task() : p.belief();
            if (t == null || t.QUESTION_OR_QUEST())
                return false;
            var f = t.freq();
            var r = posOrNeg ? f : 1 - f;
            if (power!=1) r = (float) Math.pow(r, power);
            return d.rng.nextBooleanFast16(r);
        }

        /** TODO move this to the start of the last branch that reaches only the conclusion this guards */
        @Override @Deprecated public float cost() {
            //return 0.03f;
            return 2; //HACK force push down toward leaf
        }
    }

    class ReactionBuilderPathMatch implements BiConsumer<byte[], byte[]> {
        private final TermMatch m;
        private final boolean trueOrFalse;

        ReactionBuilderPathMatch(TermMatch m, boolean trueOrFalse) {
            this.m = m;
            this.trueOrFalse = trueOrFalse;
        }

        @Override
        public void accept(byte[] pathInTask, byte[] pathInBelief) {
            if (pathInTask != null)     iff(true, pathInTask);
            if (pathInBelief != null)   iff(false, pathInBelief);
        }

        private void iff(boolean taskOrBelief, byte[] path) {
            pre(new TermMatching<>(this.m, TaskOrBelief(taskOrBelief).path(path), path.length), trueOrFalse);
        }
    }

}
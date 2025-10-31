package nars.deriver.reaction;

import jcog.TODO;
import jcog.data.map.UnifriedMap;
import nars.*;
import nars.Narsese.NarseseException;
import nars.action.Action;
import nars.action.TruthifyAction;
import nars.control.Cause;
import nars.deriver.op.*;
import nars.deriver.util.PuncMap;
import nars.deriver.util.Unifiable;
import nars.premise.DerivingPremise;
import nars.premise.NALPremise;
import nars.subterm.Subterms;
import nars.subterm.util.SubtermCondition;
import nars.table.BeliefTables;
import nars.table.dynamic.DynTruthBeliefTable;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Neg;
import nars.term.Termlike;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.PREDICATE;
import nars.term.util.Image;
import nars.term.util.TermException;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.VariableNormalization;
import nars.term.var.Variable;
import nars.truth.MutableTruthInterval;
import nars.truth.func.TruthFunction;
import nars.truth.func.TruthModel;
import nars.unify.UnifyConstraint;
import nars.unify.constraint.*;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectBooleanHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.Premise.BeliefInline;
import static nars.Premise.TaskInline;
import static nars.subterm.util.SubtermCondition.*;
import static nars.term.atom.Bool.Null;
import static nars.term.util.TermPaths.pathExact;
import static nars.term.util.transform.Retemporalize.patternify;
import static nars.unify.constraint.TermMatch.Conds;
import static nars.unify.constraint.TermMatch.Is.Variable;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PatternReaction extends MutableReaction {


    private static final Pattern ruleImpl = Pattern.compile("\\|-");
    //private static final Atomic eteConj = $.the("eteConj");
    private static final Map<Term, Truthify> truthifies = new ConcurrentHashMap<>(512);
    @Deprecated
    private static final Term IDENTITY = Atomic.atom("Identity");
    private static final int NEW_OP = 0, REVERSED = 1, NEGATED = 2;
    private static final int REVERSE_COST = 1, NEGATE_COST = 1;
    private final TruthModel truthModel;
    /**
     * derivation conclusion pattern
     */
    @Deprecated
    protected ByteToByteFunction concPunc;
    private Term id;
    private Term idCondition;
    public Term idConclusion;

    private Term beliefTruth, goalTruth;
    /**
     * TODO just use one punc->punc transfer function, and its return of 0 to deny
     */
    private BytePredicate taskPunc;
    public Truthify truthify;

    public Term pattern;
    @Deprecated
    private transient Term _pattern;

    public Taskify taskify;
    public Termify termify;
    public DerivedOccurrence time;

    public PatternReaction(TruthModel truthModel, String ruleSrc, String tag) throws NarseseException {
        this.truthModel = truthModel;
        this.tag = tag;

        var rule = _parse(this.source = ruleSrc);
        if (rule == Null)
            throw new NarseseException("pattern error:\n" + ruleSrc);

        var post = rule.sub(1).subterms();
        concPattern(post.sub(0));

        var pre = rule.sub(0).subterms();
        taskPattern(pre.sub(0));
        beliefPattern(pre.sub(1));
        double pp = pre.subs();
        for (var i = 2; i < pp; i++)
            cond(pre.sub(i));

        time = null;
        ((Compound) post.sub(1)).forEach(this::addModifier);
    }

    private static Term _parse(String ruleSrc) throws NarseseException {
        return new FunctorLinkingVariableNormalization().apply(
            new UppercaseAtomsToPatternVariables().apply(
                    parseRuleComponents(ruleSrc)
            )
        );
    }

    /**
     * default time mode
     */
    public static DerivedOccurrence time(BytePredicate punc) {
//        if (punc.accept(BELIEF) && !punc.accept(GOAL) && !punc.accept(QUESTION) && !punc.accept(QUEST)) {
//            //HACK
//            return OccurrenceSolver.Mid;
//        } else {
        return DerivedOccurrence.Task;
//        }
    }

    private static Truthify intern(Truthify x) {
        var y = truthifies.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    @Deprecated
    public static Stream<Reaction<Deriver>> parse(String... rules) {
        return parse(Stream.of(rules), TruthFunctions.the);
    }

    public static Stream<Reaction<Deriver>> parse(Stream<String> lines, TruthModel truthModel) {
        String[] currentTag = {null};
        return lines
            .map(String::trim)
            .filter(x -> !x.isEmpty())
            .map(line -> preprocess(line, currentTag))
            .filter(Objects::nonNull)
            //.distinct();
            .map(line -> {
                try {
                    var x = new PatternReaction(truthModel, line, currentTag[0]);
                    x.compile();
                    return x;
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage() + ": " + line, e);
                }
            });
    }

    private static @Nullable String preprocess(String line, String[] currentTag) {
        if (!line.contains("|-")) {
            var ll = line.length();
            if (line.charAt(ll - 1) == '{') {
                //start tag
                assert currentTag[0] == null;
                currentTag[0] = line.substring(0, ll - 1).trim();
                assert !currentTag[0].isEmpty();
                return null;
            } else if (line.charAt(ll - 1) == '}') {
                //close tag
                if (currentTag[0] == null)
                    throw new TermException("unclosed }: " + line);
                currentTag[0] = null;
                return null;
            }
        }
        return line;
    }

    private static Term parseRuleComponents(String src) throws NarseseException {
        var ab = ruleImpl.split(src);
        if (ab.length != 2)
            throw new NarseseException("Rule component must have arity=2, separated by \"|-\": " + src);

        return $.pFast(
                $.$$$('(' + ab[0].trim() + ')'),
                $.$$$('(' + ab[1].trim() + ')'));
    }

//    private static Term restoreEteConj(Term c, List<Term> subbedConj, ArrayHashSet<Term> savedConj) {
//        for (int i = 0, subbedConjSize = subbedConj.size(); i < subbedConjSize; i++) {
//            c = c.replace(subbedConj.get(i), savedConj.get(i));
//        }
//        return c;
//    }

//    @Override public String toString() { return source!=null ? source : super.toString(); }
//
//    /**
//     * HACK preserve any && occurring in --> by substituting them then replacing them
//     */
//    @Deprecated
//    private static Term saveEteConj(Term c, Collection<Term> subbedConj, ArrayHashSet<Term> savedConj) {
//
//        //TODO use a TermTransform
//        c.ANDrecurse(x -> x.hasAll(INH.bit | CONJ.bit), t -> {
//            if (t.INH()) {
//                Term s = t.sub(0);
//                Term su = s.unneg();
//                if (su.CONJ() && su.dt() == DTERNAL)
//                    savedConj.add(patternify(s, false));
//                Term p = t.sub(1);
//                Term pu = p.unneg();
//                if (pu.CONJ() && pu.dt() == DTERNAL)
//                    savedConj.add(patternify(p, false));
//            }
//        });
//        if (!savedConj.isEmpty()) {
//            int i = 0;
//            for (Term x : savedConj) {
//                Term y = $.p(eteConj, $.the(i));
//                subbedConj.add(y);
//                c = c.replace(x, y);
//                i++;
//            }
//        }
//        return c;
//    }

    private static boolean found(Term x, Term e) {
        return e.equals(x) ||
                e instanceof Compound && e.containsRecursively(x);
    }

    private static Function<Term, Term> extractSubterm(byte[] p) {
        if (p.length == 0)
            return x -> x; //as-is
        else if (p.length == 1) {
            var p0 = p[0];
            return x -> x.sub(p0);
        } else
            return x -> x.sub(p);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof PatternReaction && id.equals(((PatternReaction) o).id);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * adds a condition opcode
     * interpreted from the Term according to Meta NAL semantics
     */
    private void cond(Term a) throws NarseseException {

        var negated = a instanceof Neg;
        if (negated)
            a = a.unneg();

        var name = Functor.func(a);
        if (name == null)
            throw new NarseseException("invalid cond: " + a);

        var args = Functor.args(a);
        var an = args.subs();
        var X = an > 0 ? args.sub(0) : null;
        var Y = an > 1 ? args.sub(1) : null;


        var pred = name.toString();

        var negAppl = new boolean[1];
        cond(negated, negAppl, pred, X, Y);
        if (negAppl[0] != negated) throw new NarseseException("unhandled negation: " + a);

        switch (pred) {
            case "belief" -> {
                var xs = X.toString();
                switch (xs) {
                    case "\"+\"" -> beliefPolarity(true);
                    case "\"-\"" -> beliefPolarity(false);
                    case "\"+p\"" -> pre(new PolarityProb(false, true));
                    case "\"-p\"" -> pre(new PolarityProb(false, false));
                    default -> throw new NarseseException("Unknown belief modifier: " + xs);
                }
            }
            case "task" -> {
                var xs = X.toString();
                //assert xs.equals("\"+\"") || xs.equals("\"-\"") || taskPunc == null;
                switch (xs) {
                    case "\"+\"" -> taskPolarity(true);
                    case "\"-\"" -> taskPolarity(false);
                    case "\"+p\"" -> pre(new PolarityProb(true, true));
                    case "\"-p\"" -> pre(new PolarityProb(true, false));
                    case "\"?\"" -> taskPunc = t -> t == QUESTION;
                    case "\"@\"" -> taskPunc = t -> t == QUEST;
                    case "\".\"" -> taskPunc = t -> t == BELIEF;
                    case "\"!\"" -> taskPunc = t -> t == GOAL;
                    case "\".?\"" -> taskPunc = t -> t == BELIEF || t == QUESTION;
                    case "\"?@\"" -> {
                        assert concPunc == null;
                        taskPunc = t -> t == QUESTION || t == QUEST;
                        concPunc = c -> c;
                    }
                    case "\".!\"" -> {
                        assert concPunc == null;
                        taskPunc = t -> t == BELIEF || t == GOAL;
                        concPunc = c -> c;
                    }
                    case "all" -> taskPunc = t -> true;
                    default -> throw new NarseseException("Unknown task modifier: " + xs);
                }
            }
        }
    }

    private void cond(boolean neg, boolean[] _negationApplied, String pred, Term x, Term y) throws NarseseException {
        nars.term.var.Variable Xv = x instanceof Variable xv ? xv : null, Yv = y instanceof Variable yv ? yv : null;

        Term xs, ys;
        if (y == null) {
            xs = x;
            ys = null;
        } else {
            if (x.compareTo(y) <= 0) {
                xs = x;
                ys = y;
            } else {
                xs = y;
                ys = x;
            }
        }
        var Xvs = xs instanceof Variable xsv ? xsv : null;
        var Yvs = ys instanceof Variable ysv ? ysv : null;

        @Deprecated var negd = _negationApplied[0]; //safety check to make sure semantic of negation was applied by the handler

        switch (pred) {
            case "taskEternal" -> {
                taskEternal(!neg);
                negd = neg;
            }
            case "beliefEternal" -> {
                beliefEternal(!neg);
                negd = neg;
            }
            case "neq" -> {
                if (Yvs != null)
                    neq(Xvs, Yvs);
                else
                    neq(Xv, y); //???
            }
            case "deq" -> {
                deq(xs, ys, !neg);
                negd = neg;
            }
            case "ceq" -> {
                ceq(xs, ys, !neg);
                negd = neg;
            }
            case "ceqdeq" -> {
                ceqdeq(x, true, y, false, !neg);
                negd = neg;
            }
            case "ceqPN" -> {
                ceqPN(Xvs, Yvs, neg);
                negd = neg;
            }
            case "eqPN" -> {
                eqPN(Xvs, Yvs, !neg);
                negd = neg;
            }
            case "eqNeg" -> {
                //TODO special predicate: either (but not both) is Neg
                if (!neg)
                    neq(Xvs, Yvs);
                constrain(new EqualNeg(Xvs, Yvs), !neg);
                negd = neg;
            }
            case "neqRCom" -> neqRCom(Xvs, Yvs);


            //TODO rename: condIntersect
            case "eventIntersect", "eventIntersectContainer" -> {
                eventCommon(Xvs, Yvs, 1, !neg);
                if (!neg && pred.endsWith("Container")) {
                    eventContainer(Xvs);
                    eventContainer(Yvs);
                }
                negd = neg;
            }
            case "eventIntersectPN" -> {
                eventCommon(Xvs, Yvs, 0, !neg);
                negd = neg;
            }

//            case "mutexable":
//                neq(Xv, Yv);
//                is(x, CONJ);
//                is(y, CONJ);
//                constrain(new CommonSubEventConstraint.EventCoNeg(Xv, Yv));
//                break;

            case "opEq" -> constrain(new RelationConstraint.OpEqual(Xvs, Yvs));
            case "setsIntersect" -> constrain(new RelationConstraint.SetsIntersect(Xvs, Yvs));

//				constraints.add(new RelationConstraint.StructureIntersect(Xvs, Yvs));
            case "notSetsOrDifferentSets" -> constrain(new RelationConstraint.NotSetsOrDifferentSets(Xvs, Yvs));
            case "sub", "subPN" -> {

                SubtermCondition mode;

//                    if (pred.startsWith("sub"))

                if (y instanceof Neg) {
                    Yv = (Variable) (y = y.unneg());
                    mode = SubtermNeg;
                } else
                    mode = Subterm;

                if (!neg) bigger(Xv, false, Yv, mode == SubtermNeg);


                int polarity;
                if (pred.endsWith("PN")) {
                    polarity = 0;
                    assert !(y instanceof Neg);
                } else {
                    polarity = 1; //negation already handled
                }

                if (y instanceof Variable yv) {
                    constrain(new SubConstraint(mode, Xv, yv, polarity), !neg);
                } else {
                    if (polarity == 0)
                        throw new TODO(); //TODO contains(Y) || contains(--Y)
                    iff(Xv, new TermMatch.Contains(y), !neg);
                }

                negd = neg;
            }
            case "in" -> in(Xv, y, !(negd = neg));
            case "hasBelief" -> hasBeliefTask(!(negd = neg));
            case "taskBeliefEq" -> pre(TaskBeliefEqual.negIf(negd = neg));
            case "conjParallelTaskBelief" -> pre(conjParallelTaskBelief, TaskBeliefEqual.neg());

            case "seq" -> {
                if (!neg) is(x, CONJ);
                iff(x, TermMatch.SEQ, !(negd = neg));
            }
            case "dtUnify" -> pre(dtUnify);
            case "cond", "condPN" -> {
                if (!neg) bigger(Xv, y);
                condOf(Cond, Xv, y, pred.endsWith("PN"), !neg);
                negd = neg;
            }
            case "condStart", "condEnd" -> {
                if (!neg) bigger(Xv, y);
                condOf(pred.equals("condStart") ? CondStart : CondEnd, Xv, y, false, !neg);
                negd = neg;
            }
            case "condStartPN" -> {
                if (!neg) bigger(Xv, y);
                condOf(CondStart, Xv, y, true, !neg);
                negd = neg;
            }
            case "condFirst", "condLast", "condFirstPN", "condLastPN" -> {
                if (!neg) bigger(Xv, y);
                condOf(condFirstOrLast(pred),
                        Xv, y, pred.contains("PN"), !neg);
                negd = neg;
            }
            case "subsMin" -> iff(x, new TermMatch.SubsMin((short) $.intValue(y)));
            case "is" -> {
                var oy = structOR(y);
                if (neg) {
                    isNotAny(x, oy); negd = neg;
                } else
                    isAny(x, oy);
            }
            case "var" -> {
                iffThese(x, Variable, !neg);
                negd = neg;
            }
            case "hasEvents" -> {
                iff(x, Conds, !neg);
                negd = neg;
            }
            case "hasVar" -> {
                _hasAnyAll(x, Variables, !neg, true);
                //if (!neg)
                //    iff(x, new TermMatch.ComplexMin(2));
                negd = neg;
            }
            case "hasVars" -> constrain(new HasVars(Xvs, Yvs)); //HACK hasVars(X,Y) == hasVar(X)||hasVar(Y)
            case "isUnneg" -> { isUnneg(x, op($.unquote(y)).bit, !neg); negd = neg; }
            case "has" -> { hasAll(x, structOR(y), !neg); negd = neg; }
            case "hasAny" -> { hasAny(x, structOR(y), !neg); negd = neg; }
            case "overlap" -> {
                if (!neg)
                    throw new NarseseException("only --overlap() supported");
                pre(NoOverlap);  negd = true;
            }
            case "task", "belief" -> { /* ignore; handled in elsewhere */ }
            default -> throw new NarseseException("Unknown condition " + pred + " in:\n\t" + source);
        }

        _negationApplied[0] = negd;
    }

    private static SubtermCondition condFirstOrLast(String pred) {
        return pred.contains("First") ? CondFirst : CondLast;
    }

    private void ceqPN(Variable x, Variable y, boolean neg) {
        constrain(new CeqPNConstraint(x, y), !neg);

        if (!neg)
            constrain(new RelationConstraint.StructureCoContained(x, y, NEG.bit));
    }

    public void eventCommon(Variable x, Variable y, int polarity, boolean isTrue) {
        if (polarity < 0) throw new UnsupportedOperationException();

//        if (!isTrue) {
//            if (polarity==1) {
//                neq(x,y);
//            } else if (polarity==0) {
//                eqPN(x,y,false);
//            }
//        }

        constrain(new CommonSubEventConstraint(x, y, polarity), isTrue);

        if (isTrue) {
            if (polarity == 1)
                constrain(new RelationConstraint.StructureIntersect(x, y)); //TODO structure intersect, with except for polarity!=1
            //constrain(new RelationConstraint.StructureCoContained(x, y, polarity!=1 ? Op.NEG.bit : 0));
        }
    }

    private Reaction concPattern(Term c) {
        var cu = c.unneg();
        if (!(cu instanceof Variable || cu.TASKABLE()))//concTerm instanceof Bool)
            throw new TermException("conclusion pattern is bool", c);

        _pattern = c;
        return this;
    }

    public final Reaction time(String taskEvent) {
        time = DerivedOccurrence.solvers.get(Atomic.atom(taskEvent));
        return this;
    }


    private void addModifier(Term m) {

        if (!m.INH()) throw new TermException("Unknown postcondition format", m);

        Term which = m.sub(0), type = m.sub(1);

        switch (type.toString()) {
            case "Punctuation" -> {
                /** belief -> question, goal -> quest */
                /** belief -> question, goal -> quest */
                /** re-ask a new question/quest in response to question/quest */
                /** belief,question -> question, goal,quest -> quest */
                switch (which.toString()) {
                    case "Belief" -> concPunc = x -> BELIEF;
                    case "Goal" -> concPunc = x -> GOAL;
                    case "Question" -> concPunc = x -> QUESTION;
                    case "Quest" -> concPunc = x -> QUEST;

                    case "Answer" -> {
                        assertTaskPuncAndConcPuncNull();
                        assert beliefTruth != null && goalTruth != null;
                        taskPunc = i -> i == QUESTION || i == QUEST;
                        concPunc = o -> switch (o) {
                            case QUESTION -> BELIEF;
                            case QUEST -> GOAL;
                            default -> (byte) 0;
                        };
                    }
                    case "Ask" -> {
                        assertTaskPuncAndConcPuncNull();
                        taskPunc = i -> i == BELIEF || i == GOAL;
                        concPunc = o -> switch (o) {
                            case BELIEF -> QUESTION;
                            case GOAL -> QUEST;
                            default -> (byte) 0;
                        };
                    }
                    case "AskAsk" -> {
                        assertTaskPuncAndConcPuncNull();
                        taskPunc = i -> i == QUESTION || i == QUEST;
                        concPunc = o -> switch (o) {
                            case QUESTION -> QUESTION;
                            case QUEST -> QUEST;
                            default -> (byte) 0;
                        };
                    }

                    case "AskHowAsk" -> {
                        /* like AskAsk, but also adds Goal->Quest (how) */
                        assertTaskPuncAndConcPuncNull();
                        taskPunc = i -> i == QUESTION || i == QUEST || i == GOAL;
                        concPunc = o -> switch (o) {
                            case QUESTION -> QUESTION;
                            case QUEST, GOAL -> QUEST;
                            default -> (byte) 0;
                        };
                    }
                    case "AskAll" -> {
                        assertTaskPuncAndConcPuncNull();
                        taskPunc = i -> true;
                        concPunc = o -> switch (o) {
                            case BELIEF, QUESTION -> QUESTION;
                            case GOAL, QUEST -> QUEST;
                            default -> throw new UnsupportedOperationException();
                        };
                    }

                    case "Identity" -> {
                        assert beliefTruth == null && goalTruth == null;
                        taskPunc = i -> true;
                        concPunc = o -> o;
                        beliefTruth = goalTruth = IDENTITY;
                    }

                    default -> throw new RuntimeException("unknown punctuation: " + which);
                }
            }
            case "Time" -> {
                time = DerivedOccurrence.solvers.get(which);
                if (time == null)
                    throw new RuntimeException("unknown Time modifier:" + which);
            }
            case "Belief" -> beliefTruth = which;
            case "Goal" -> goalTruth = which;
            default -> throw new RuntimeException("Unhandled postcondition: " + type + ':' + which);
        }
    }

    private void assertTaskPuncAndConcPuncNull() {
        assert taskPunc == null && concPunc == null;
    }

    /**
     * @param c assumed to be unneg
     */
    private Term conclusion(final Term c) {

        var cu = c.unneg();

        //verify that all pattern variables in c are present in either taskTerm or beliefTerm
        //assertConclusionVariablesPresent(cu);

        if (cu instanceof Compound) {

//            Term c00;
//            Pair<List<Term>, ArrayHashSet<Term>> conjs = null;
//            if (c.hasAll(INH.bit | CONJ.bit)) {
//                List<Term> subbedConj = new Lst<>(0);
//                ArrayHashSet<Term> savedConj = new ArrayHashSet<>(0);
//                c00 = saveEteConj(c, subbedConj, savedConj);
//                if (!subbedConj.isEmpty())
//                    conjs = Tuples.pair(subbedConj, savedConj);
//            } else {
//                c00 = c;
//            }

            var c1 = patternify(cu);

            var c2 = concOptInline(c1); //auto-add taskInline,beliefInline - but may elide constraint

            c2.ANDrecurse(x -> x.hasAny(INH.bit | SIM.bit), x -> {
                conclusionInhSim(x);
                return true;
            });

            //return conjs != null ? restoreEteConj(c2, conjs.getOne(), conjs.getTwo()) : c2;
            return c2.negIf(c instanceof Neg);
        } else {
            assert cu instanceof Variable;
            isNotAny(cu, ~Taskables); //must be taskable

//            if (!c.equals(taskPattern) && taskPattern instanceof Compound && (((Compound)taskPattern).containsRecursively(c) || ((Compound)beliefPattern).containsRecursively(c))) {
//
//                //isNotAny(c, s); ///any other ops?
//                iff(c, TermMatcher.Is.is(Op.Variables | Op.IMG.bit), false, false); //only in precondition, not constraint
//            }

            return c;
        }

    }

    /** TODO this needs to be more exhaustive and test the predicates */
    private void assertConclusionVariablesPresent(Term c) {
        var tb = taskPattern.equals(beliefPattern);
        c.ANDrecurse(Termlike::hasVarPattern, z -> {
            if (z.VAR_PATTERN()) {
                if (!(taskPattern.equals(z) || taskPattern instanceof Compound && taskPattern.containsRecursively(z)) &&
                        (tb || !(beliefPattern.equals(z) || beliefPattern instanceof Compound && beliefPattern.containsRecursively(z))))
                    throw new TermException("conclusion " + c + " does not contain task or belief pattern", z);
            }
        });
    }

    private void conclusionInhSim(Term x) {
        if (x.isAny(INH.bit | SIM.bit)) {//auto-constraints for construction of a valid inh/sim
            var xx = x.subtermsDirect();
            Term xa = xx.sub(0), xb = xx.sub(1);
            if (xa instanceof Variable xav && xb instanceof Variable xbv) {

                if (knownValid(x))
                    return;
                if (knownValid((x.INH() ? SIM : INH).the(xx))) //try the opposite, for which also holds
                    return;
//                //try the reverse inh
//                if (knownValid(INH.the(xx.reversed())))
//                    return;

                neqRCom(xav, xbv);
            }
        }

    }

    /**
     * tests if an example of the INH has already been successfully constructed so automatically valid
     */
    private boolean knownValid(Term x) {
        return found(x, taskPattern)
                ||
                taskPattern !=/*!.equals(*/beliefPattern/*)*/ && found(x, beliefPattern);
    }

    private Term concOptInline(Term x) {

        if (x.TEMPORAL_VAR())
            return x; //dont modify, because the result could be different but same root

        //decide when inline "paste" (macro substitution) of the premise task or belief terms is allowed.
        var tInline = !(taskPattern instanceof Variable) && taskPattern.dt() != XTERNAL;
        var bInline = !(beliefPattern instanceof Variable) && !taskPattern.equals(beliefPattern) && beliefPattern.dt() != XTERNAL;

        Term T, B;
        /* one is missing or b < t */
        if (!tInline || !bInline || beliefPattern.complexity() <= taskPattern.complexity()) {
            //subst task first
            T = tInline ? x.replace(taskPattern, TaskInline) : x;
            B = bInline ? T.replace(beliefPattern, BeliefInline) : T;
            return B;
        } else {
            //reverse order for max coverage
            B = x.replace(beliefPattern, BeliefInline);
            T = B.replace(taskPattern, TaskInline);
            return T;
        }


    }

    @Override
    protected void commit() {

        var beliefTruthOp = truthModel.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null)
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);

        var goalTruthOp = truthModel.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null)
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);

        /* default arity: force one or the other */
        if (arity == 0)
            arity = beliefTruthOp != null && !beliefTruthOp.single() || goalTruthOp != null && !goalTruthOp.single() ? 2 : 1;


        /* infer missing conclusion punctuation */
        if (concPunc == null) {
            if (beliefTruth != null && goalTruth != null) {
                if (taskPunc == null) taskPunc = x -> x == BELIEF || x == GOAL;
                concPunc = x -> x;
            } else if (beliefTruth != null) {
                if (taskPunc == null) taskPunc = x -> x == BELIEF;
                concPunc = x -> BELIEF;
            } else if (goalTruth != null) {
                if (taskPunc == null) taskPunc = x -> x == GOAL;
                concPunc = x -> GOAL;
            }

        }

        if (concPunc == null)
            throw new UnsupportedOperationException("no concPunc specified");

        /* infer necessary task punctuation */
        if (taskPunc == null) {
            //auto
            if (beliefTruth != null && goalTruth != null) {
                taskPunc = p -> p == BELIEF || p == GOAL; //accept belief and goal and map to those
            } else if (beliefTruth != null) {
                taskPunc = p -> p == BELIEF; //accept only belief -> belief
            } else if (goalTruth != null) {
                taskPunc = p -> p == GOAL; //accept only goal -> goal
            } else
                throw new UnsupportedOperationException();
        }

        var p = new PuncMap(
            PuncMap.p(taskPunc, concPunc, BELIEF),
            PuncMap.p(taskPunc, concPunc, GOAL),
            PuncMap.p(taskPunc, concPunc, QUESTION),
            PuncMap.p(taskPunc, concPunc, QUEST),
            (byte) 0 //COMMAND
        );
        taskPunc(p);


        this.truthify = intern(new Truthify(p, beliefTruthOp, goalTruthOp));
        if (!p.can(QUESTION) && !p.can(QUEST) &&
                (truthify.beliefMode == -1 || truthify.beliefMode == 0 && !truthify.beliefOverlap) &&
                (truthify.goalMode == -1 || truthify.goalMode == 0 && !truthify.goalOverlap)
        ) {
            //TODO decide if all cases are covered by this
            noOverlap();
        }

        if (time == null) time = time(taskPunc);

        this.pattern = _pattern.transformPN(this::conclusion);

        if (pattern.hasAll(FuncBits))
            pattern.ANDrecurse(x -> x.hasAll(FuncBits),
                    Unifiable.eventFuncConstraints(this));

        super.commit();

        this.taskify = taskify(pattern);
    }

    @Override
    public synchronized void compile() {
        if (termify == null) {
            conditions(); //compute FIRST

            this.termify = new Termify(taskPattern, beliefPattern, constraints);

            this.idCondition = SETe.the(PREDICATE.ids((Collection)pre));

            this.idConclusion = $.p(
                termify.ref,
                pattern,
                truthify, time.term
            );

            this.id = $.p(idCondition, idConclusion);
        }
    }

    @Override
    public final Term term() {
        return id;
    }

    @Override
    public Action action(Cause<Reaction<Deriver>> why) {
        return new TruthifyAction(this, why);
    }

    private Taskify taskify(Term p) {

        //1. direct
        Function<Deriver, Term> c = null;

        //noinspection ConstantConditions
        if (c == null && (p == TaskInline || p.equals(taskPattern)))
            c = concludePremise(true);
        if (c == null && (p.equalsNeg(TaskInline) || p.equalsNeg(taskPattern)))
            c = neg(concludePremise(true));

        if (c == null && (p == BeliefInline || p.equals(beliefPattern)))
            c = concludePremise(false);
        if (c == null && (p.equalsNeg(BeliefInline) || p.equalsNeg(beliefPattern)))
            c = neg(concludePremise(false));

        if (c == null)
            c = conclusionExtractPremiseSubterm(p);

//        if (c == null) {
//            c = neg(conclusionExtractPremiseSubterm(p.neg()));
//            if (c!=null)
//                Util.nop(); //TEMPORARY
//        }

        if (c == null)
            c = conclusionExtractPremiseSubtermTransformed(p); //TODO constraints
//        if (c == null) {
//            c = neg(conclusionExtractPremiseSubtermTransformed(p.neg()));
//            if (c!=null)
//                Util.nop(); //TEMPORARY
//        }

        return new Taskify(p, c);
    }

    @Nullable
    private static Function<Deriver, Term> neg(@Nullable Function<Deriver, Term> f) {
        return f == null ? null : f.andThen(Term::neg);
    }

    @Nullable
    private Function<Deriver, Term> concludePremise(boolean taskOrBelief) {
        return constrain(taskOrBelief(taskOrBelief), taskOrBelief ? taskPattern : beliefPattern);
    }

    @Nullable
    private Function<Deriver, Term> conclusionExtractPremiseSubtermTransformed(Term pattern) {

        if (pattern instanceof Compound && !pattern.CONJ()) { //TODO prefilter by check structure intrsecton of pattern with task/belief

            var patternSubs = pattern.subtermsDirect();

            var N = patternSubs.subs();
            var pv = patternSubs.complexity();
            var ps = patternSubs.structSubs();

            var patternSubsReversed = N > 1 ? patternSubs.reverse() : null;

            var patternCommute = N > 1 && pattern.op().commutative;
            var negCount = patternSubs.count(t -> t instanceof Neg);
            var patternSubsNegated = negCount == 0 /*|| negCount == patternSubs.subs()*/ ? //HACK
                    patternSubs.negated() : null;

            byte[] bestPath = null;
            var bestRoot = false;
            var bestMode = -1;

            var exacts = new ObjectBooleanHashMap<Term>(0);
            ObjectBooleanHashMap<Term> reversed = patternSubsReversed != null ? new ObjectBooleanHashMap<>(0) : null;
            ObjectBooleanHashMap<Term> negated = patternSubsNegated != null ? new ObjectBooleanHashMap<>(0) : null;

            Predicate<Compound> possibleContainer =
                    x -> x.complexity() >= pv && Op.hasAll(x.structSubs(), ps);

            var sources = taskPattern == beliefPattern ? new boolean[]{true} : new boolean[]{true, false};
            for (var root : sources) {

                Consumer<Term> processContent = z -> {
                    if (z instanceof Atomic)
                        return;

                    var zz = z.subterms();
                    var zs = zz.subs();
                    if (zs != N) return;

                    var zCommute = z.COMMUTATIVE();
                    if (!patternCommute && zCommute)
                        return; //avoid sourcing from commutive terms which may be unified in alternate permutations

                    if (patternSubs.equals(zz))
                        exacts.put(z, root);
                    else if (patternSubsReversed != null && !(patternCommute && zCommute) && patternSubsReversed.equals(zz))
                        reversed.put(z, root);
                    else if (patternSubsNegated != null && patternSubsNegated.equals(zz))
                        negated.put(z, root);
                    //TODO add reversed's, negated, etc
                };
                (root ? taskPattern : beliefPattern).ANDrecurse(possibleContainer, processContent);
            }


            for (var m : exacts.keyValuesView()) {
                var root = m.getTwo();
                var p = pathExact(root ? taskPattern : beliefPattern, m.getOne());
                if (p != null && (bestPath == null || bestPath.length > p.length)) {
                    bestRoot = root;
                    bestPath = p;
                    bestMode = NEW_OP;
                }
            }

            if (reversed != null) {
                for (var m : reversed.keyValuesView()) {
                    var root = m.getTwo();
                    var p = pathExact(root ? taskPattern : beliefPattern, m.getOne());
                    if (p != null && (bestPath == null || bestPath.length > p.length + REVERSE_COST)) {
                        bestRoot = root;
                        bestPath = p;
                        bestMode = REVERSED;
                    }
                }
            }


            if (negated != null) {
                for (var m : negated.keyValuesView()) {
                    var root = m.getTwo();
                    var p = pathExact(root ? taskPattern : beliefPattern, m.getOne());
                    if (p != null && (bestPath == null || bestPath.length > p.length + NEGATE_COST)) {
                        bestRoot = root;
                        bestPath = p;
                        bestMode = NEGATED;
                    }
                }
            }


            if (bestMode != -1) {
                var patternDT = pattern.dt();
                var o = pattern.op();
                return taskOrBelief(bestRoot)
                        .andThen(extractSubterm(bestPath))
                        .andThen(Term::subtermsDirect)
                        .andThen(switch (bestMode) {
                            case 0 -> s -> s;
                            case 1 -> Subterms::reverse;
                            case 2 -> Subterms::negated;
                            default -> throw new TODO();
                        }).andThen(s -> o.the(patternDT, s));
            }
        }

        return null;

    }

    private Function<Deriver, Term> conclusionExtractPremiseSubterm(Term p) {
        var concInTask = pathExact(taskPattern, p);
        var concInBelief = beliefPattern == taskPattern ? null : pathExact(beliefPattern, p);
        var ct = concInTask != null;
        var cb = concInBelief != null;
        if (ct && cb) {
            if (concInTask.length > concInBelief.length) {
                concInTask = null;
                ct = false;
            } else
                cb = false;
        }

        if (!ct && !cb) return null;

        var sub = extractSubterm(ct ? concInTask : concInBelief);
        var patternSub = sub.apply(ct ? taskPattern : beliefPattern);

        return constrain(taskOrBelief(ct).andThen(sub), patternSub);
    }

    /**
     * wraps resolver in a guard that tests for constraints
     */
    @Nullable
    private Function<Deriver, Term> constrain(@Nullable Function<Deriver, Term> resolver, Term patternComponent) {
        if (resolver == null)
            return null;

        if (constraints.length == 0) return resolver;

        var pcEq = patternComponent.equals();
        UnifyConstraint<Deriver.PremiseUnify>[] constraints = Stream.of(this.constraints)
            .filter(c -> pcEq.test(c.x) ||
                    patternComponent instanceof Compound && patternComponent.containsRecursively(c.x))
            .toArray(UnifyConstraint[]::new);

        if (constraints.length == 0)
            return resolver;
        else {
            return d -> {
                var y = resolver.apply(d);
                if (y != null && y != Null) {
                    d.unify.clear(constraints);
                    if (!d.unify.unify(patternComponent, y))
                        return null;
//                    for (UnifyConstraint u : constraints) {
//                        if (u.invalid(y, null))
//                            return null; //constraint violated
//                    }
                }
                return y;
            };
        }
    }

    private static Function<Deriver, Term> taskOrBelief(boolean taskOrBelief) {
        return taskOrBelief ?
                d -> d.premise.from() :
                d -> d.premise.to();
    }

    @Override
    public Class<? extends Reaction<Deriver>> type() {
        return PatternReaction.class;
    }

    private static class UppercaseAtomsToPatternVariables extends RecursiveTermTransform {

        static final java.util.Set<Atomic> reservedMetaInfoCategories = java.util.Set.of(
                Atomic.atomic("Belief"),
                Atomic.atomic("Goal"),
                Atomic.atomic("Punctuation"),
                Atomic.atomic("Time")
        );

        private final Map<String, Term> map = new UnifriedMap<>(8);

        @Override
        public Term applyAtomic(Atomic a) {
            if (a instanceof Atom && !reservedMetaInfoCategories.contains(a)) {
                var name = a.toString();
                if (name.length() == 1 && Character.isUpperCase(name.charAt(0)))
                    return map.computeIfAbsent(name, this::var);
            }
            return a;
        }

        private Variable var(String n) {
            return $.varPattern(1 + map.size());
        }
    }

    private static class FunctorLinkingVariableNormalization extends VariableNormalization {
        @Override
        public Term applyAtomic(Atomic x) {
            if (x instanceof Atom) {
                var f = NARS.Functors.functor(x);
                return f != null ? f : x;
            } else
                return super.applyAtomic(x);
        }

    }

//    public static class EqualSub extends UnifyConstraint<Deriver.PremiseUnify> {
//        private final Function<Deriver, Term> y;
//        private final byte[] path;
//
//        static public EqualSub the(byte[] ct, byte[] cb, Variable c) {
//            boolean taskOrBelief = cb == null || ct != null && ct.length <= cb.length;
//            byte[] path = taskOrBelief ? ct : cb;
//            return new EqualSub(c, taskOrBelief, path);
//        }
//
//        private EqualSub(Variable c, boolean taskOrBelief, byte[] path) {
//            super(c, "equalSub", taskOrBelief ? Atomic.atom("task") : Atomic.atom("belief"), $.p(path));
//            y = extractTaskOrBelief(taskOrBelief);
//            this.path = path;
//        }
//
//        @Override
//        public float cost() {
//            return 0.1f;
//        }
//
//        @Override
//        public boolean invalid(Term x, Deriver.PremiseUnify f) {
//            Term Y = y.apply(f.deriver());
//            return !x.equals(Y.sub(path));
//        }
//    }

    private static class HasVars extends RelationConstraint<Deriver.PremiseUnify> {

        HasVars(Variable x, Variable y) {
            super(HasVars.class, x, y);
        }

        @Override
        public boolean invalid(Term x, Term y, Deriver.PremiseUnify u) {
            return !x.hasVars() && !y.hasVars();
        }

        @Override
        protected HasVars newMirror(Variable newX, Variable newY) {
            return new HasVars(newX, newY);
        }

        @Override
        public float cost() {
            return 0.04f;
        }
    }

    public static final TermMatch ImageAlignable = new TermMatch() {

        @Override
        public boolean test(Term term) {
            return !Image.imageNormalize(term).equals(term);
            //return Image.imageNormalize(term).CONDS();
        }

        @Override
        public float cost() {
            return 0.01f;
        }
    };

    private static final PREDICATE<Deriver> dtUnify = new PREDICATE<>("dtUnify") {
        @Override
        public boolean test(Deriver d) {
            var p = d.premise;
            return d.unify.unifyDT(p.from().dt(), p.to().dt());
        }

        @Override
        public float cost() {
            return 0.05f;
        }
    };

    private static int structOR(Term y) {
        if (!y.SETe()) {
            return bit(y);
        } else {
            var struct = 0;
            for (var yy : y.subterms())
                struct |= bit(yy);
            return struct;
        }
    }

    private static int bit(Term yy) {
        var o = op($.unquote(yy));
        if (o == null)
            throw new UnsupportedOperationException("not an operator: " + yy);
        return o.bit;
    }

    @Nullable public final DerivingPremise premise(Deriver d) {
        var p = (NALPremise) d.premise;
        var punc = truthify.punc.get(p.task().punc());
        var t = time.when(p, punc, d);
        if (t != null) {
            var f = truthify.function(p, punc, d);
            if (f == null || truth(f, t, punc, d))
                return new DerivingPremise(this, p, punc, t);
        }
        return null;
    }

    private boolean truth(TruthFunction f, MutableTruthInterval t, byte punc, Deriver d) {
        return f == TruthFunctions.Dynamic ? truthDynamic(t, punc, d) : d.truth(f, t);
    }

    private boolean truthDynamic(MutableTruthInterval t, byte punc, Deriver d) {
        //TODO implement better:
        // does not support inline evaluations
        // does not support ...
        //assert punc ==BELIEF || punc ==GOAL;

        var u = d.unify;
        var termify = this.termify;
        u.clear(termify.constraints);

        var p = d.premise; //HACK
        if (u.unify(termify.taskPattern, p.from(), false)) {
            if (u.unify(termify.beliefPattern, p.to(), true)) {
                var c = u.apply(pattern);
                if (c instanceof Neg)
                    throw new TODO(); //does this happen?

                return truthDynamic(t, punc, d, c);
            }
        }
        return false;
    }

    private static boolean truthDynamic(MutableTruthInterval t, byte punc, Deriver d, Term c) {
        var n = d.nar;
        var table = dynTable(c, punc, n);
        if (table != null) {
            var tr = table.truth(t, c, d.dur(), n);
            if (tr != null) {
                t.set(tr);
                return true;
            }
        }
        return false;
    }

    private static @Nullable DynTruthBeliefTable dynTable(Term c, byte punc, NAR n) {
        var b = n.table(c, punc == BELIEF, true);

        if (b instanceof BeliefTables tbb) {
            for (var x : tbb.tables)
                if (x instanceof DynTruthBeliefTable dx)
                    return dx;
        } else if (b instanceof DynTruthBeliefTable dx)
            return dx;

        return null;
    }
}
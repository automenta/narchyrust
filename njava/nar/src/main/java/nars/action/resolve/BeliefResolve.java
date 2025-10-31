package nars.action.resolve;

import jcog.signal.FloatRange;
import nars.*;
import nars.focus.time.TaskWhen;
import nars.premise.NALPremise;
import nars.task.proxy.SpecialTermTask;
import nars.term.Neg;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.control.PREDICATE;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.*;

/**
 * matches a belief task for a premise's beliefTerm, converting a single-premise to a double-premise
 */
public class BeliefResolve extends Answerer {

    public final FloatRange
            unifyQuery = FloatRange.unit(1/2f),
            unifyIndep = FloatRange.unit(1/2f),
            unifyDep   = FloatRange.unit(1/2f)
            //unifyQuery = FloatRange.unit(1),
            //unifyIndep = FloatRange.unit(1),
            //unifyDep   = FloatRange.unit(0)
    ;

    private static final boolean allowRefinedSingle = false;

    public BeliefResolve(boolean belief, boolean goal, boolean question, boolean quest, TaskWhen timing, TaskResolver resolver) {
        super(timing, resolver);


        pre(
            TaskBeliefNotEqualIfTaskBelief
            //TaskBeliefEqual.neg()
            //TaskBeliefEqualRoot.neg()
        );
        taskPunc(belief, goal, question, quest);
        hasBeliefTask(false);


        isAny(PremiseBelief, Taskables);
        pre(Structural.the.neg());

//        hasAny(PremiseBelief, Op.AtomicConstant); //dont bother with variable-only terms
    }


    @Override
    protected void run(Deriver d) {
        var y = resolveBelief(NALPremise.nalPremise(d.premise), d);
        if (y!=null)
            d.add(y);
    }

    public @Nullable Premise resolveBelief(NALPremise x, Deriver d) {
        final var task = x.task();
        final Term tPrev = task.term(), bPrev = x.to();

        Term tNext = tPrev, bNext = bPrev;

        var unifyVars = unifyVars(d);

        if (unifyVars != 0) {
            var taskMutable =
                task.QUESTION_OR_QUEST()
                &&
                tNext.hasAny(unifyVars);

            var beliefMutable =
                bNext.hasAny(unifyVars);

            if (taskMutable || beliefMutable) {
                if (tNext.unifiable(unifyVars, 0).test(bNext)) {
                    //TODO if cant unify at top-level, maybe unify with a subterm, recursively
                    try (var u = d.unifyTransform(NAL.derive.TTL_UNISUBST)) {
                        u.vars = unifyVars;

                        var c = u.unifySubst(tNext, bNext, taskMutable ? tNext : bNext);
                        if (c != null) {
                            c = c.unneg().normalize();

                            var tUnif = taskMutable ? c /* u.transform(taskTerm) */ : null;
                            if (tUnif != null && valid(tNext, tUnif, d) && tUnif.TASKABLE())
                                tNext = tUnif;

                            var bUnif = beliefMutable ? (taskMutable ? u.transform(bNext) : c) : null;
                            if (bUnif!=null) {
                                if (bUnif instanceof Neg)
                                    bUnif = bUnif.unneg();//HACK
                                if (bUnif!=bPrev && bUnif!=c)
                                    bUnif = bUnif.normalize();
                                if (valid(bNext, bUnif, d))// && !btUnified.equalsRoot(tt))
                                    bNext = bUnif;
                            }

                        }

                    }
                }
            }
        }

        Termed B = null;
        if (bNext.CONCEPTUALIZABLE() /*&& (!taskBelief || !beliefTerm.equals(_t))*/ && !bNext.hasAny(VAR_QUERY)) {
            B = resolve(bNext, task, d);
            if (B != null)
                bNext = B.term();
        }

        boolean varShift;
        if (B != null) {
            //belief task resolved
            varShift = true;
        } else {
            if (!allowRefinedSingle && (bNext.equals(bPrev) && tNext.equals(tPrev)))
                return null;

            //different belief term?
            varShift = false;
            B = bNext;
        }

        var T = reterm(task, tPrev, tNext);
        return T == null ? null : NALPremise.the(T, B, varShift);
    }

    /** wraps Task if term has changed */
    private static @Nullable NALTask reterm(NALTask task, Term tPrev, Term tNext) {
        if (tPrev.equals(tNext)) return task;

        var p = SpecialTermTask.proxy(task, tNext, true);
        return p == null ? null : p.copyMeta(task);
    }

    @Nullable private NALTask resolve(Term x, NALTask src, Deriver d) {
        var y = resolver.resolveTask(x, BELIEF,
            timing.whenRelative(src, d)
            , d,
            filter(x, src)
        );
        return valid(src, d, y) ? y : null;
    }

    private static boolean valid(NALTask src, Deriver d, NALTask y) {
        return y != null && valid(null, y.term(), d) && valid(y, src);
    }

    private static @Nullable Predicate<NALTask> filter(Term x, NALTask src) {
        return EQUALITY_FILTER && src.BELIEF() && src.term().equalsRoot(x) ?
                NALTask.notEqualTo(src) : null;
    }

    private int unifyVars(Deriver d) {
        return
            unifyVarBit(VAR_DEP,   unifyDep,   d) |
            unifyVarBit(VAR_QUERY, unifyQuery, d) |
            unifyVarBit(VAR_INDEP, unifyIndep, d) ;
    }

    private static int unifyVarBit(Op varDep, FloatRange unifyRate, Deriver d) {
        return d.randomBoolean(unifyRate.floatValue()) ? varDep.bit : 0;
    }

    private static boolean valid(@Nullable Term x, Term y, Deriver d) {
        return !(y instanceof Bool) &&
                y.complexity() <= d.complexMax
                //&& (x==null || !y.equalsRoot(x))
                ;
    }

    /**
     * only allow unstamped tasks to apply with stamped beliefs.
     * otherwise stampless tasks could loop forever in single premise or in interaction with another stampless task
     */
    private static boolean valid(NALTask belief, NALTask task) {
        return !task.equals(belief)
               &&
               !(task.stampLength() == 0 && belief.stampLength() == 0) //prevent infinite recursion in cases where stampLength==0
       ;
    }


    private static final boolean EQUALITY_FILTER = false;

    public static final PREDICATE<Deriver> TaskBeliefNotEqualIfTaskBelief = new PREDICATE<>("TaskBeliefNotEqualIfTaskBelief") {
        @Override
        public boolean test(Deriver d) {
            var p = d.premise;
            var t = p.task();
            return !t.BELIEF() || !t.term().equals(p.to());
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    };

    /** for use with Decomposers and the 'structural' field it may set */
    private static class Structural extends PREDICATE<Deriver> {

        static final Structural the = new Structural();

        private Structural() {
            super("structural");
        }

        @Override
        public boolean test(Deriver d) {
            return d.premise instanceof NALPremise.NonSeedTaskPremise pp && pp.structural;
        }

        @Override
        public float cost() {
            return 0.01f;
        }
    }
}
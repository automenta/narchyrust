package nars.func.prolog;

import alice.tuprolog.*;
import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.signal.MutableFloat;
import jcog.util.Range;
import nars.*;
import nars.Term;
import nars.memory.Memory;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;
import nars.util.TaskChannel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static nars.Op.*;

/**
 * Prolog mental coprocessor for accelerating reasoning
 * WARNING - introduces cognitive distortion
 * <p>
 * Causes a NARProlog to mirror certain activity of a NAR.  It generates
 * prolog terms from NARS beliefs, and answers NARS questions with the results
 * of a prolog solution (converted to NARS terms), which are input to NARS memory
 * with the hope that this is sooner than NARS can solve it by itself.
 */
public class PrologCore extends Prolog implements Consumer<NALTask> {

    static final Logger logger = LoggerFactory.getLogger(PrologCore.class);

    //    public static final String AxiomTheory;
    public static final alice.tuprolog.Term ONE = new NumberTerm.Int(1);
    public static final alice.tuprolog.Term ZERO = new NumberTerm.Int(0);
//
//    static {
//        String a;
//        try {
//            a = Util.inputToString(
//                    PrologCore.class.getClassLoader()
//                            .getResourceAsStream("nars/prolog/default.prolog")
//            );
//        } catch (Throwable e) {
//            logger.error("default.prolog {}", e.getMessage());
//            a = "";
//        }
//        AxiomTheory = a;
//    }

    private final NAR nar;

    final Map<nars.Term, alice.tuprolog.Term> beliefs = new ConcurrentHashMap();

    /**
     * beliefs above this expectation will be asserted as prolog beliefs
     */
    @Range(min = 0.5, max = 1.0)
    public final Number trueFreqThreshold = new MutableFloat(0.9f);

    /**
     * beliefs below this expectation will be asserted as negated prolog beliefs
     */
    @Range(min = 0, max = 0.5)
    public final Number falseFreqThreshold = new MutableFloat(0.1f);

    /**
     * beliefs above this expectation will be asserted as prolog beliefs
     */
    @Range(min = 0, max = 1.0)
    public final Number confThreshold = new MutableFloat(0.75f);


    @Range(min = 0, max = 1.0)
    public final Number answerConf = new MutableFloat(confThreshold.floatValue() * 0.9f);


    private final TaskChannel in;
    private final Focus what;

    /** TODO adapt NAR memory as ClauseIndex */
    public static class MyClauseIndex extends ConcurrentHashClauseIndex {


        public MyClauseIndex(Memory t) {

        }


    }

    public PrologCore(NAR n) {
        super(new MyClauseIndex(n.memory), new ConcurrentHashClauseIndex());

        if (NAL.DEBUG)
            setSpy(true);

        this.in = new TaskChannel(n.causes.newCause(this));
        this.nar = n;
        this.what = nar.main(); //TODO be parameter


//        /*@Deprecated*/ n.eventTask.on(this);
    }

    @Override
    @Deprecated public void accept(NALTask task) {

        if (task.BELIEF()) {

            if (task.ETERNAL()) {
                int dt = task.term().dt();
                if (dt == 0 || dt == DTERNAL) {
					float c = (float) task.conf();
                    if (c >= confThreshold.floatValue()) {
                        float f = task.freq();
                        float t = trueFreqThreshold.floatValue();
                        if (f > t)
                            believe(task, true);
                        else if (f < 1f - t)
                            believe(task, false);
                    }
                }
                /* else: UNSURE */
            }
        } else if (task.QUESTION()) {
            if (task.ETERNAL() || task.start() == nar.time()) {
                question(task);
            }
        }


    }

    protected void believe(Task t, boolean truth) {


        Term ct = t.term();

        if (!ct.hasAny(Op.AtomicConstant))
            return;

        beliefs.computeIfAbsent(ct, (pp/*=ct?*/) -> {

            Struct next = (Struct) pterm(t.term());

            if (!truth) {
                if (t.term().IMPL()) {
                    next = new Struct(":-", negate(next.subResolve(1)), next.subResolve(0));
                } else {
                    next = negate(next);
                }
            }

            Solution s = solve(assertion(next));
            if (s.isSuccess())
                logger.info("believe {}", next);
            else
                logger.warn("believe {} failed", next);

            return next;
        });


    }


    protected void question(Task question) {
        Term tt = question.term();
        /*if (t.op() == Op.NEGATE) {
            
            tt = ((Compound)tt).target(0);
            truth = !truth;
        }*/

        alice.tuprolog.Term questionTerm = pterm(tt);


        logger.info("solve {}", questionTerm);

        long timeoutMS = 50;
        solveWhile(questionTerm, (answer) -> {


            switch (answer.result()) {
                case PrologRun.TRUE:
                case PrologRun.TRUE_CP:


                    answer(question, answer);

                    break;
                case PrologRun.FALSE:


                default:

                    break;
            }
            return true;
        }, timeoutMS);

    }

    private void answer(Task question, Solution answer) {
        try {
            Term yt = nterm(answer.goal);

            NALTask y = NALTask.task(yt, BELIEF, $.t(1f, answerConf.floatValue()), ETERNAL, ETERNAL, nar.evidence());
            y.pri(nar);

            if (y != null) {
                logger.info("answer {}\t{}", question, y);
                in.accept(y, what);
            }

        } catch (Exception e) {
            logger.error("answer {} {} {}", question, answer, e);
        }
    }

    private static nars.Term nterm(Struct s, int subterm) {
        return nterm(s.sub(subterm));
    }

    private static Term[] nterms(alice.tuprolog.Term[] t) {
        return Util.map(PrologCore::nterm, new Term[t.length], t);
    }

    private static @Nullable nars.Term[] nterms(Struct s) {
        int len = s.subs();
        nars.Term[] n = new nars.Term[len];
        for (int ni = 0; ni < len; ni++) {
            if ((n[ni] = nterm(s.subResolve(ni))) == null)
                return null;
        }
        return n;
    }


    private static nars.Term nterm(alice.tuprolog.Term t) {
        nars.Term result;
        if (t instanceof Var) {
            result = $.varDep(((Var) t).name());
        } else {
            Struct s = (Struct) t;
            if (s.subs() > 0) {
                result = switch (s.name()) {
                    case "-->" -> theTwoArity(Op.INH, s);
                    case "<->" -> theTwoArity(Op.SIM, s);
                    case "==>" -> theTwoArity(Op.IMPL, s);
                    case "[" -> SETi.the((nterms(s)));
                    case "{" -> SETe.the((nterms(s)));
                    case "*" -> PROD.the((nterms(s)));
                    case "&&" -> CONJ.the((nterms(s)));
                    case "||" -> DISJ(nterms(s));
                    case "not" -> (nterm(s, 0).neg());
                    default -> $.func(unwrapAtom(s.name()), nterms(s.subArrayShared()));
                };
            } else {
                String n = s.name();
                if (n.startsWith("'#")) {

                    result = $.varDep(n.substring(2, n.length() - 1));
                } else {
                    result = $.atomic(unwrapAtom(n));
                }

            }


        }
//        else {
//            throw new RuntimeException(t + " untranslated");
//        }
        return result;
    }


    private static String unwrapAtom(String n) {
        if (n.charAt(0) == '_')
            n = n.substring(1);
        return n;
    }

    private static String wrapAtom(String n) {
        return '_' + n;
    }

    private static Term theTwoArity(Op inherit, Struct s) {
        return inherit.the(nterm(s, 0), nterm(s, 1));
    }


    public static alice.tuprolog.Term assertion(alice.tuprolog.Term p) {
        return new Struct("assertz", p);
    }

    public static alice.tuprolog.Term retraction(alice.tuprolog.Term p) {
        return new Struct("retract", p);
    }

    public static Struct negate(alice.tuprolog.Term p) {
        return new Struct("--", p);
    }

    public static alice.tuprolog.Term[] psubterms(Subterms s) {
        return s.array(PrologCore::pterm, alice.tuprolog.Term[]::new);
    }

//    public static alice.tuprolog.Term tterm(String punc, final alice.tuprolog.Term nalTerm, boolean isTrue) {
//        return new Struct(punc, nalTerm, isTrue ? ONE : ZERO);
//    }


    public static alice.tuprolog.Term pterm(nars.Term term) {
        switch (term) {
            case Compound compound -> {
                Op op = term.op();
                alice.tuprolog.Term[] st = psubterms(term.subterms());
                switch (op) {
                    case IMPL:
                        return new Struct(":-", st[1], st[0] /* reversed */);
                    case CONJ: {
                        return new Struct(",", st); //TODO may need special depvar handling
//                    int s = target.subs();
//                    alice.tuprolog.Term t = pterm(target.sub(s - 1));
//                    for (int i = s-2; i >= 0; i--) {
//                        t = new Struct(",", t, pterm(target.sub(i)));
//                    }
//                    return t;
                    }
                    case NEG:
                        //TODO detect disj
                        return new Struct(/*"\\="*/"not", st);
                    case PROD:
                        return new Struct(st);
                    case INH:
                        Term pred = term.sub(1);
                        if (pred.ATOM()) {
                            Term subj = term.sub(0);
                            if (subj.PROD()) {
                                alice.tuprolog.Term args = st[0];
                                return new Struct(wrapAtom(pred.toString()),
                                        args != null ?
                                                Iterators.toArray(((Struct) st[0]).listIterator(), alice.tuprolog.Term.class) :
                                                new alice.tuprolog.Term[]{args});
                            }
                        }
                        break;

                }

                return new Struct(op.str, st);
            }
            case NormalizedVariable variable -> {
                switch (term.op()) {
                    case VAR_QUERY:
                    case VAR_PATTERN:
                    case VAR_DEP:
                    case VAR_INDEP:
                        return new Var("_" + (((NormalizedVariable) term).id()));
                }
            }
            case Atomic atomic -> {
                return new Struct(wrapAtom(term.toString()));
            }
            case null, default -> {
            }
        }

        throw new UnsupportedOperationException();


    }


}
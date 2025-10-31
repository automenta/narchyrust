package nars;


import jcog.Config;
import jcog.Is;
import jcog.pri.op.PriMerge;
import jcog.signal.DoubleRange;
import jcog.signal.FloatRange;
import jcog.signal.FloatRangeDivisible;
import jcog.signal.IntRange;
import jcog.thing.Thing;
import jcog.util.Range;
import nars.focus.PriNode;
import nars.focus.PriSource;
import nars.task.util.StampPairOverlap;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.util.transform.Conceptualization;
import nars.term.util.transform.Retemporalize;
import nars.time.Tense;
import nars.time.Time;
import nars.truth.evi.EviProjector;
import nars.truth.proj.MutableTruthProjection;
import nars.truth.util.ConfRange;
import nars.util.Timed;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Random;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

import static jcog.Config.IS;
import static jcog.Util.curve;
import static nars.Op.*;

/**
 * NAR Parameters
 */
public abstract class NAL<W extends NAL<?>> extends Thing<W, Term> implements Timed {



    private static final boolean EXPERIMENTAL =
        false;
        //true;

    /** batching should improve performance but reordering affects attention dynamics in realtime cases */
    public static final boolean REMEMBER_BATCHED = true;

    @Deprecated public static final float CURIOSITY_RATE_ON =
            //1/100f;
            1/150f;
            //1/200f;
            //1/50f;
            //1/40f;
            //1/60f;
            //1/20f;

    @Deprecated public static final float CURIOSITY_RATE_OFF = 1/4f;

//    /** base curiosity period, in game durs.
//     *  larger value means less curiosity / procedural noise */
//    @Deprecated public static final float CURIOSITY_DURS =
//        //150;
//        225;
//        //300;
//        //192;
//        //200;
//        //96;
//        //48;
//        //128;
//        //256;
//        //32;
//        //16;
//        //24;
//        //64;
//
//    @Deprecated public static final float CURIOSITY_DUTY_CYCLE =
//        (1/16f + 1/2f)/2;
//        //1/64f;
//        //1/96f;
//        //1/128f;
//        //1/16f;
//        //1/4f;

    /** keep this to false so that multithreaded NAR shutdowns arent interfered with */
    @Deprecated public static final boolean NAR_PARTS_NULLIFY_NAR_ON_DELETE = false;

    /** whether to link task even if not 'novel' */
    @Deprecated public static final boolean TASK_ACTIVATE_ALWAYS =
        //true;
        false;


    public static final boolean HAPPINESS_GEOMETRIC = false;

    /** boolean-like conjunction construction
     *  if false, enables recursive and contradictory conjunction arguments, ex: (x && --x) which can have some fuzzy meaning,
     *  even if the boolean expression equals false
     *
     *  a complete implementation of this switch will involve significant changes to the conjunction construction process, including
     *  assumptions elsewhere about the impossibility or inevitability of contradictory conj subterms.
     * TODO experimental */
    public static final boolean CONJ_BOOL = true;

    /** values > 1: leak (negative pressure) apply extra forgetting */
    public static final float FORGETTING =
        1
        //0.5f
        //0
        //0.9f;
        //1/2f;
        //1.01f;
        //Util.PHI_min_1f
    ;
    public static final int EVAL_ITER_MAX = 8;

    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
    public static boolean DEBUG_DERIVED_NALTASKS;

    public static final PriMerge taskPriMerge = PriMerge.max, taskBagMerge = taskPriMerge;

    /** true: separate tasklink bags by punctuation; false: use one bag for all */
    public static boolean TASKBAG_PUNC_OR_SHARED;

    public final double eviMin() {
        return confMin.evi();
    }

    /** memory search depth.
     *  determines belief projection accuracy vs speed
     *  0..100%
     *  */
    public final FloatRange answerDepthBeliefGoal = FloatRange.unit(
        1
        //3/4f
        //1/2f
        //1/3f
        //1/4f
        //1/6f
        //1/8f
    );

    public enum answer { ;


        public static final float ANSWER_DEPTH_QUESTION =
            0;

        /** determines answer capacity in proportion to STAMP_CAPACITY.
         *  determines the rate of evidence accumulation via projection, dynamic truth, etc
         *
         *  this is effectively a 'truth precision strength' parameter.
         *  proportional to how much 'justice' a belief can potentially get in a judgment.
         *
         *  TODO should be 'density': capacity/dur
         */
        public static final int REVISION_CAPACITY =
            2;
            //3;
            //4;
            //5;
            //8;
            //16;

        public static final int ANSWER_CAPACITY =
            REVISION_CAPACITY;
            //REVISION_CAPACITY + 1;
            //REVISION_CAPACITY + 2;
            //REVISION_CAPACITY + 3;
            //REVISION_CAPACITY * 2;
            //REVISION_CAPACITY * 3;

//        /** experimental */
//        public static final boolean DYN_DEPTH_DROPOUT =
//            //true;
//            false;

        public static final boolean RANK_BY_EVI_CURVE = true;


        /** TODO make this a 'default' so Actions can configure their own mode */
        public static final int ACTION_ANSWER_CAPACITY =
            //2;   //revise strongest 2 only
            //1; //strongest only
            ANSWER_CAPACITY;
            //REVISION_CAPACITY;
            //ANSWER_CAPACITY+1;
            //ANSWER_CAPACITY*2;
            //ANSWER_CAPACITY*4;
            //8;
            //6;
            //12;
            //6;


        /** max revision and induction clustering iterations */
        public static final double CLUSTER_REVISE_PRECISION =
            2;
            //3;
            //4;
            //8;
            //1;
            //16;
            //12;
            //8;
            //5;
            //3;
            //4;

        /** in ranking answer tasks (when Answer.rangeSpecific is enabled):
         *    proportion of evidence to sacrifice for tasks with range exceeding the query range.
         *    any non-zero value should theoretically prefer the closest-matching ranged task, all else being equal on the target interval.
         *
         *  it is a benefit-of-the-doubt that a weaker but more temporally relevant task
         *  should have an increased chance against a stronger but less temporally relevant.
         * */
        public static final float RANGE_SPECIFIC =
            0; //DISABLED
            //0.05f;
            //0.1f;
            //0.01f; //"tie-breaker"
            //0.9f;
            //1;
            //0.5f;
            //0.1f;
            //0.25f;
            //0.75f;
            //0.1f;

        /** answer specificity of intermpolation alignment
         *  balance occurrence relevance with internal temporal structure relevance
         *  probably should be greater than RANGE_SPECIFIC (pressuring merge with similar DT more than similar range)
         *
         *  partitions tasks with differring time-scales within the same concept from intermpolation during revision
         * */
        public static final float DT_SPECIFIC =
            1;
            //1/2f;
            //2;
            //1.5f;
            //1;
            //4;
            //0.75f;
            //0.5f;
            //0.1f;
            //0.95f;
            //0.75f;
            //0.9f;
            //1;
            //1.5f;
            //0.5f;
            //0.5f;
            //3;
            //4;
            //1 + 1/3f;

        /** TODO decide if this actually matters with regard to all DynTruth implementation's overrides */
        @Deprecated public static final boolean DYN_DITHER_COMPONENT_OCC_DEFAULT = true;

        /** depth multiplication factor for each DynTruthTaskify recursion */
        public static final float ANSWER_DEPTH_DECAY =
            //1 //no decay
            3/4f;
            //0.5f
            //Util.PHI_min_1f
        ;

        /** experimental */
        public static final boolean DYN_UNDERLAP =
            true;
            //false;

//        /** weakens sub-Answer search strength,
//         *  >=0, 0 to disable */
//        public static final float dynRecursionIgnorance =
//            //0.1f;
//            //0.5f;
//            0; //DISABLE

//        public static final boolean dynRecursionIgnoranceComponents = false;

//        /** completely different modes.. not sure which is better */
//        public static final boolean DERIVE_PROJECTION_ABSOLUTE_OR_RELATIVE = true;
//        public static final boolean DYN_PROJECTION_ABSOLUTE_OR_RELATIVE = DERIVE_PROJECTION_ABSOLUTE_OR_RELATIVE;
//        public static final boolean ANSWER_PROJECTION_ABSOLUTE_OR_RELATIVE = true;
//        public static final boolean TEMPORAL_REVISION_ABSOLUTE_OR_RELATIVE = true;

    }

    public enum concept { ;
        public static final ToIntFunction<Termed> termComplexity = c->c.term().complexity();

        /* shared eternal belief and goal capacity curve */
        public static final ToIntFunction<Termed> beliefEternalCapacity = curve(termComplexity,
                1, 8,
                        16, 6,
                        32, 4
        );

        /* shared temporal belief and goal capacity curve */
//        ToIntFunction<Concept> bgTemporal = c -> 128; //flat
        public static final ToIntFunction<Termed> beliefTemporalCapacity = curve(termComplexity,

//                1, 256,
//                8, 96,
//                16, 64,
//                32, 32

//                1, 64,
//                8, 24,
//                16, 16,
//                32, 8

                1, 64,
                8, 32,
                16, 16

//                1, 32,
//                8, 16,
//                16, 8


//                1, 256,
//                8, 192,
//                16, 128,
//                32, 64
        );

        /* shared question and quest capacity curve */
        public static final ToIntFunction<Termed> questionCapacity = curve(termComplexity,
                1, 8,
                64, 6
        );
    }

    /** Term -> Concept mapping */
    public static final Retemporalize conceptualization =
        Conceptualization.Hybrid; //stable
        //Conceptualization.Xternal; //almost works
        //Conceptualization.Flatten;  //probably doesnt
        //Conceptualization.Sequence; //probably doesnt


    /**
     * TODO needs tested whether recursive Unification inherits TTL
     */
    public static final int EVALUATION_RECURSE_LIMIT = 8;

    public static final boolean ABSTRACT_TASKS_ALLOWED = true;

    @Deprecated public static final boolean CAREFUL_CONCEPTUALIZATION = false;


    /**
     * Evidential Horizon, the amount of future evidence to be considered
     */
    public static final double HORIZON = Config.FLOAT("horizon", 1);

    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     * TODO IntRange
     */
    public static final int STAMP_CAPACITY = 16;

//    /** when stamp hits capacity, prevents creating many revisions with
//     * varying stamps by seeding an RNG to determine what stamp elements to remove.
//     * however this lacks generality as many permutations of stamps will not be tried. */
//    public static final boolean STAMP_HASH_SEED = false;

    /** TODO auto-size according to Game vocabulary size */
    @Deprecated public static final int DEFAULT_GAME_CAPACITY =
        1024;
        //2048;
        //512;


    /**
     * enables higher-order (recursive) implication statements
     *
     * if true: allows recursive implication constructions
     * if false: reduce to Null as an invalid statement
     *      ex: (((a==>b) && x) ==> y)
     */
    public static final boolean IMPLICATION_CAN_CONTAIN_IMPLICATION = false;


    /** various post-processing of contained variables prior to use in Task content */
    public static final boolean POST_NORMALIZE = true;

    /** probability of unifying subterms randomly (not using any heuristics as usual) */
    public static final float SUBTERM_UNIFY_ORDER_RANDOM_PROBABILITY = 0;

//    /** seems safe and will reduce equivalent permutations cluttering tasklink bags */
//    public static final boolean TASKLINK_NORMALIZE_IMAGES = false;


    /** 1.0 = perfectly sorted each duration (commit); values closer to zero are less accurate but faster */
    public static final float tasklinkSortedness = 1;


//    public static double valueBeliefOrGoal(NALTask t, Timed n) {
//        return
//            1
//            //t.conf()
//            //(Evidence.eviRelative(t, n.time(), n.dur()))
//            //((t.conf() * (t.isEternal() ? n.dur() : t.range())) / t.volume())
//            ;
//    }
//    public static float valueQuestion(Task t) {
//        return
//            1
//            //t.volume()
//            //t.priElseZero()
//            //1f / t.volume()
//            //t.originality()
//            //1 / t.term().volume()
//        ;
//    }


    /**
     * SignalTask's
     */
    public enum signal {
        ;

        /** sensor data structure mode */
        public static final boolean SENSOR_BELIEF_TABLES_CLASSIC = false;// !EXPERIMENTAL;
        public static final boolean VECTOR_BELIEF_TABLES_CLASSIC = SENSOR_BELIEF_TABLES_CLASSIC;

        public static final boolean SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS_ON_SIGNAL = true, SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS_ON_REMEMBER = true;

        public static final int SIGNAL_BELIEF_TABLE_SERIES_CAPACITY = 1024;

        /**
         * sensor temporal resolution parameter
         * maximum time (in durations) that a signal task can stretch the same value
         * until a new task (with new evidence) is created (seamlessly continuing it afterward)
         * 
         * this can probably, safely, be arbitrarily large
         *
         * NOTE: see 'signal stretching priority momentum' for a reason why
         * it may be helpful to periodically create new signal tasks.
         *
         * may have secondary effects, ex: SerialTask usage in CompoundClustering etc.
         */
        public static final float SERIAL_TASK_STRETCH_DURS_MAX =
            //8;
            //16;
            //32;
            //64;
            //128;
            //256;
            //512;
            //1024;
            2048;
            //4096;

        /** (reducing) conf factor for the eternal belief truth in the EternalDefaultTable when freqUsually is set */
        public static final double REWARD_USUALLY_CONF_FACTOR = 1/4f;

        @Deprecated public static final boolean STAMP_SHARING = true;


        /**
         * maximum time between signal updates to stretch an equivalently-truthed
         * data point across.
         * ie. stretches perception across amounts of lag
         *
         * values greater than 1 and less than 2 are probably best, since
         * it is unrealistic to expect it will be perfectly on time (1.0)
         * and increasing latching will further distort the recorded signal.
         */
        public static final float SIGNAL_LATCH_LIMIT_DURS =
            1.5f;

        /** whether to compute happiness using dur = 0 (exact), or dur = game's dur */
        public static final boolean HAPPINESS_DUR_ZERO = true;

        public static final float PERCEPTION_RATE_DEFAULT =
            1/2f;
            //1/10f;
            //1/8f;
            //1/3f;
            //1/4f;
    }

    /** max tolerance time difference (in durations) for unification of temporal terms */
    public final FloatRange unifyDurs = new FloatRange(0.5f, 0, 2 );

    @Deprecated public final FloatRange questionForgetting =
            new FloatRange(FORGETTING, 0, 2);

    /**
     * hard upper-bound limit on Compound target complexity;
     * if this is exceeded it may indicate a recursively
     * malformed target due to a bug or unconstrained combinatorial explosion
     */
    public final IntRange complexMax = new IntRange(64, 0, 384 /*term.COMPOUND_VOLUME_MAX*/);
    public final int complexMax() { return complexMax.intValue(); }

    /**
     * truth confidence threshold necessary to form tasks
     */
    public final ConfRange confMin = new ConfRange();

    /**
     * global truth frequency resolution by which reasoning is dithered
     */
    public final FloatRange freqRes = new FloatRangeDivisible(truth.FREQ_RES_DEFAULT, truth.FREQ_EPSILON, 1,
            1, truth.FREQS);
    /**
     * global truth confidence resolution by which reasoning is dithered
     */
    public final DoubleRange confRes = new DoubleRange(truth.CONF_RES_DEFAULT,
            0, 0.5f);

    /**
     * time resolution:
     * how many cycles above which to dither dt and occurrence time
     * TODO move this to Time class and cache the cycle value rather than dynamically computing it
     */
    public final IntRange timeRes = new IntRange(1, 1, 1024);


    public final ConfRange beliefConfDefault = new ConfRange(0.9f);
    public final ConfRange goalConfDefault = new ConfRange(0.9f);

    /** HACK use PriNode.amp(..) to set these.  will figure this out.  pri wont work right, as this is the actual value vs. the advised (user provided) */
    public final PriSource beliefPriDefault = new PriSource("pri.", 0.5f);
    public final PriSource goalPriDefault = new PriSource("pri!", 0.5f);
    public final PriSource questionPriDefault = new PriSource("pri?", 0.5f);
    public final PriSource questPriDefault = new PriSource("pri@", 0.5f);

    public final Time time;

    private final Supplier<Random> random;

    protected NAL(Time time, Supplier<Random> rng) {
        super();
        this.random = rng;
        (this.time = time).reset();
    }

    @Override
    public final float dur() {
        return time.dur();
    }
    @Override
    public final long time() {
        return time.now();
    }

    @Deprecated public long time(Tense tense) {
        return time.relativeOccurrence(tense);
    }

    /**
     * creates a new evidence stamp
     */
    public final long[] evidence() {
        return new long[]{time.nextStamp()};
    }

    static Atom randomSelf() {
        return $.uuid(/*"I_"*/);
    }

    public final int timeRes() {
        return this.timeRes.intValue();
    }

    /** TODO double */
    public final float confDefault(byte punctuation) {
        return _confDefault(punctuation).floatValue();
    }

    public final double eviDefault(byte punctuation) {
        return _confDefault(punctuation).evi();
    }

    private ConfRange _confDefault(byte punctuation) {
        return switch (punctuation) {
            case BELIEF -> beliefConfDefault;
            case GOAL -> goalConfDefault;
            default -> throw new RuntimeException("Invalid punctuation " + punctuation + " for a TruthValue");
        };
    }

    public final float priDefault(byte punctuation) {
        PriNode p;
        switch (punctuation) {
            case BELIEF: p = this.beliefPriDefault; break;
            case GOAL: p = this.goalPriDefault; break;
            case QUESTION: p = this.questionPriDefault; break;
            case QUEST: p = this.questPriDefault; break;
            case COMMAND: return 1;
            default: throw new RuntimeException("Unknown punctuation: " + punctuation);
        }
        return p.pri();
    }


    /** temporal momentum. time dilation factor.
     *  allows memorable evidence to accumulate faster than it can be forgotten */
    public FloatFunction<NALTask> eternalization = x -> 0;

    /**
     * provides a Random number generator
     * @return
     */
    @Override
    public final RandomGenerator random() { return _random(); }

    public final Random _random() {
        return random.get();
    }

    public enum truth { ;
        /** num distinct freqs and confs */
        public static final int
            FREQS = (1<<10),
            CONFS = (1<<22);

        /** use new TruthCurve for revisions, see SENSOR_BELIEF_TABLES_CLASSIC */
        public static final boolean REVISE_CURVE = true;

        public static final int REVISE_CURVE_CAPACITY = 8;

        public static final boolean ALIGNMENT_ABSOLUTE =
            true;
            //false;

        public static final MutableTruthProjection.EviMerge eviMerge =
            IS("DOUBT_VARIANCE", false) ?
                MutableTruthProjection.EviMerge.SumDoubtVariance :
                MutableTruthProjection.EviMerge.Sum;

        public static int hash(float freq, double conf) {
            return (freqHash(freq) << 22) | confHash(conf);
        }

        private static int confHash(double conf) {
            return (int) (conf * (CONFS - 1));
        }

        private static int freqHash(float freq) {
            return (int) (freq * (FREQS - 1));
        }

        /**
         * internal granularity which truth components are rounded to
         * minimum resolution for freq and conf components
         */
        public static final float
            FREQ_EPSILON = 1f/FREQS,
            FREQ_EPSILON_half = FREQ_EPSILON/2;

        /**
         * determines degree of evidence sensitivity.
         *
         * since conf will be lower than evi approaching zero,
         * define the evidence min in terms of a confidence min
         * that respects 32-bit Float precision limits.
         *
         * expect ~16 digits of reliable 32-bit float precision:
         *    https://en.wikipedia.org/wiki/Floating-point_arithmetic#Internal_representation
         *
         * it's important for precision to tolerate low-confidence internal truth calculations
         * that may eventually accumulate to threshold levels.
         */
        public static final double
            CONF_MIN = 1.0/CONFS,
            CONF_MAX = 1 - CONF_MIN,
            EVI_MIN = TruthFunctions.c2e(CONF_MIN),
            EVI_MAX = TruthFunctions.c2e(CONF_MAX);

        /** discrete confidence step for fine-grain 23-bit taskregion conf */
        @Deprecated public static final double TASK_REGION_CONF_EPSILON = CONF_MIN;//1.0/(1<<23);


        public static final boolean CONF_DITHER_ROUND_UP = false;

        public static final float FREQ_RES_DEFAULT = 0.01f;
        public static final float CONF_RES_DEFAULT = FREQ_RES_DEFAULT;
    }

    public enum truthFn { ;
        public static final boolean MIX_EVI_OR_CONF = true;
//        public static final boolean COMPOSE_CONF_CLASSIC = true;
        public static final PriMerge CONF_COMPOSITION =
            PriMerge.and;
            //PriMerge.mean;
            //PriMerge.min;

        /** structural reduction confidence discount rate;
         *  (working name until something better)
         *  OpenNARS calls this: 'reliance'
         *  */
        public static final double GULLIBILITY = 0.9f;

        /** experimental */
        @Deprecated public static final boolean OPEN_WORLD = false;

//        /** experimental */
//        public static final boolean STRONG_COMPOSITION = IS("STRONG_COMPOSITION");

        /** 0 to disable; ~8 ensures ~1 output @ f:{<=1/4 | >=3/4}*/
        public static final double SUPERCONDUCT_SHARPNESS =
            //0;
            8;

        public static final boolean CONF_COMPOSE_LINEAR =
            false;
            //true;
    }

    /** revision and projection (revection) */
    public enum revision { ;

        /** may significantly distort results if false */
        public static final boolean INTERMPOLATE_DISCOUNT_EVIDENCE = true;

        /** strict: templateMatch will fail in cases where a CONJ template resolves to a bundled INH, which is valid */
        public static final boolean TEMPLATE_MATCH_STRICT =
            //true;
            false;

        /** false will increase temporal accuracy at the expense of
         * creating a potentially larger variety of tasks
         */
        public static final boolean DYNTASKIFY_OCC_DITHER = true;
        public static final boolean DYN_DT_DITHER = DYNTASKIFY_OCC_DITHER;
        public static final boolean PROJECT_REL_OCC_SHIFT = false;
        public static final boolean PROJECT_REL_OCC_RANGE_DIFF = false;

        /** concentrate the temporal region to minimize evidence dilution that a naive temporal union might result in
         * TODO currently has a bug, so disabled.
         */
        @Is("Center_of_mass") public static final boolean CONCENTRATE_PROJECTION = false;
        /** isosceles is less strict */
        public static final boolean RELATIVE_PROJECTION_MODE_CLASSIC_OR_ISOSCELES = true;
        /**
         * PRE is potentially lossy, especially since projection time may change after filtering.
         * TODO analyze benefits/risk, considering concentration */
        //public static final boolean TRUTH_PROJECTION_FILTER_WEAK_PRE = false;
        public static final boolean TRUTH_PROJECTION_FILTER_WEAK_POST = false;

        public static final boolean INPUT_MERGE_IMMEDIATE = true;
    }

    public enum dyn {
        ;

        /** dynamic truth tables main enable */
        public static boolean ENABLE =
            true;
            //false;

        /** TODO test */
        public static final boolean DYN_SIM = false;

        /** TODO test */
        public static final boolean DYN_DIFF = false;

        public static final boolean DYN_DELTA = true;

        public static final boolean DYN_CONJ = true;

        /** dynamic impl (induction): potentially noisy */
        public static final boolean DYN_IMPL = true;
        public static final boolean DYN_IMPL_filter = true;

        public static final boolean DYN_IMPL_LIMITING = true;

        public static final boolean DYN_IMPL_CONJ = true;
        public static final boolean DYN_IMPL_PRED_CONJ = DYN_IMPL_CONJ;
        public static final boolean DYN_IMPL_SUBJ_DISJ_MIX = DYN_IMPL_CONJ;
        @Deprecated public static final boolean DYN_IMPL_SUBJ_CONJ = false;

        public static final boolean DYN_IMPL_CONTRAPOSITION =
            true;
            //false;

        /** TODO needs a iteration limiter (<=1), otherwise infinite recursion */
        public static final boolean DYN_IMPL_CONVERSION = false /*DYNAMIC_IMPL*/;


//        public static final boolean DYN_INH_SECT = term.INH_BUNDLE;

    }

    public enum evi { ;


        private static final double eviSustain = Config.FLOAT("eviTime",
            1 //NORMAL
            //2
            //3
            //4
            //8
        );

        public static final EviProjector project =
            new EviProjector.InverseSquare(eviSustain);
            //EviProjector.InverseSqrt;
            //new EviProjector.InverseLinear(eviSustain);
            //new EviProjector.PowerLaw(eviSustain/8);
            //new EviProjector.InverseExponential(eviSustain*11);


//        public static final float DUR_SCALING =
//                0 //DISABLED
//                //0.1f
//                ;

    }

    public enum premise {
        ;

        /** TODO move elsewhere */
        public static final int TERMLINK_CAP = 64;

        /**
         * heterarchical assocatiativity rate/density factor
         * completeness factor determining maximum number of termlinks to test in a term linking procedure.
         *  % of the bag searched per refresh */
        public static final float TERMLINK_DENSITY =
            //0.2f;
            //0.25f;
            0.1f;
            //0.5f;
            //0.05f;
            //1;

        public static final boolean OCC_TRIM = true;

        /** whether to avoid double-premise stamp zipping when the premise's
         * task is a question, and the conclusion punc is not a question
         * TODO make this consider hasBelief()
         * */
        @Deprecated public static final boolean QUESTION_TO_BELIEF_ZIP = true;

        public static final StampPairOverlap OVERLAP_DOUBLE_MODE =
            StampPairOverlap.SemiStrict;
            //StampPairOverlap.Loose;
            //StampPairOverlap.Strict;
    }

    public enum temporal {
        ;

//        public static final boolean TABLE_FILTER_CONF_INPUTS = false;

        public static final boolean SCAN_START_RANDOM_OR_MID = true;

        /** TODO 'both', not just 'either' (random) */
        @Is("Arrow_of_time")
        public static final boolean TEMPORAL_INDUCTION_IMPL_BIDI =
            true;
            //false;

        /** both pos and neg subject cases (true), or stochastic (false) */
        @Deprecated public static final boolean TEMPORAL_INDUCTION_IMPL_SUBJ_PN =
            false;

        public static final boolean
            TEMPORAL_INDUCTION_POLARITY_STOCHASTIC_CONJ = true,
            TEMPORAL_INDUCTION_CONJ_NEG_SEQ = false;

        public static final boolean TEMPORAL_INDUCTION_POLARITY_STOCHASTIC_IMPL_SUBJ = true;

        /** whether to also generate the disj after conj
         *  TODO does this still break Temporal Stability tests?
         */
        public static final boolean TEMPORAL_INDUCTION_DISJ =
            false;
            //true;

        /** whether to form ==>'s in addition to &&'s in Compound Clustering
         *  TODO parameter for probability balance of the choices? */
        public static final boolean TEMPORAL_INDUCTION_COMPOUND_CLUSTER_IMPL = true;

        /** whether to allow adjacent temporal merges with non-equal, but contained-by stamp membership */
        public static final boolean ADJACENT_MERGE_STAMP_CONTAINS = true;

        /** time offset, in durs ([-1..+1]),
         * action feedback beliefs start in a Game frame.
         *    0.5: 180 degrees out of phase with inputs & rewards.
         *      0: disable
         * non-zero might clarify causality
         */
        public static final float GAME_ACTION_SHIFT_DURS =
            0; //now (current frame)
            //-1/2f;

        /** relative time offset, in game durs, where reward beliefs occurr.
         *  retroactive, at the same time as the actions which produced it
         *  non-zero might clarify causality
         */
        public static final float GAME_REWARD_SHIFT_DURS =
            //0;  //now (current frame)
            -1/2f; //between previous and now
            //-1;   //previous

        /**
         * how many tasks to compress per compression iteration.
         * in % of capacity
         * larger means potentially higher throughput,
         * at the cost of less accuracy.
         */
        public static final float TEMPORAL_BELIEF_TABLE_COMPRESSION_RATE =
            //1/7f;
            //1/20F;
            //1/10f;
            //1/4f;
            //1/5f;
            1/3f;

        /** not fully implemented */
        public static final boolean CONJ_INDUCT_NEG_SEQUENCES = false;

        /** whether to allow negated sequences in IMPL subjects */
        public static final boolean IMPL_SUBJ_NEG_SEQ = true;
    }

    public enum belief {
        ;

        /**
         * true will filter sub-confMin revision results.  false will not, allowing sub-confMin
         * results to reside in the belief table (perhaps combinable with something else that would
         * eventually raise to above-confMin).  generally, false should be more accurate with a tradeoff
         * for overhead due to increased belief table churn.
         */
        public static final boolean REVISION_MIN_EVI_FILTER = false;

        /**
         * memory reconsolidation period - time period for a memory to be refreshed as new
         *
         * memory momentum.
         *
         * useful as a novelty threshold:
         * >=0, higher values decrease the rate at which repeated tasks can be reactivated
         */
        public static final double NOVEL_DURS =
            1
            //0.5f; //high-precision
            //Util.PHI_min_1f
            //2 //low-precision
            //8
        ;



        /**
         * maximum span of a Task, in cycles.
         * beyond a certain length, evidence integration precision suffers accuracy diminishes and may become infinite
         */
        public static final long TASK_RANGE_LIMIT = 1L << 61 /* estimate */;


        //false;

        /** TODO test */
        public static final boolean QUESTION_MERGE_AGGRESSIVE = false;


        /** Array or NavigableMap temporal belief table */
        @Deprecated public static final boolean TEMPORAL_TABLE_ARRAY =
            //false;
            true;

    }


    public enum term {
        ;

        public static final boolean INH_PRINT_COMPACT = IS("INH_PRINT_COMPACT", false);

        /**
         * whether INT atoms can name a concept directly
         */
        public static final boolean INT_CONCEPTUALIZABLE = false;


        /**
         * absolute limit for constructing terms in any context in which a NAR is not known, which could provide a limit.
         * typically a NAR instance's 'compoundVolumeMax' parameter will be lower than this
         *
         * it helps if this is as low as possible for precision in measuring aggregate statistics involving term complexity
         */
        public static final int COMPOUND_VOLUME_MAX =
            4096;
            //Short.MAX_VALUE;

        /**
         * limited because some subterm paths are stored as byte[]. to be safe, use 7-bits
         */
        public static final int SUBTERMS_MAX = Byte.MAX_VALUE;
        public static final byte MAX_INTERNED_VARS = Byte.MAX_VALUE;

        /**
         * how many INT terms are canonically interned/cached. [0..n)
         */
        public static final int ANON_INT_MAX = Byte.MAX_VALUE;

        public static final int TERM_BUFFER_MIN_REPLACE_AHEAD_SPAN = 2;
//        public static final int TERM_BUFFER_MIN_INTERN_VOL = 2;


        /** prevent variable introduction from erasing negative compounds,
         *  though content within negatives can be var introduced as normal. */
        public static final boolean VAR_INTRODUCTION_NEG_FILTER = true;

        /** minimum product subterms for image structural transformations */
        public static final int imageTransformSubMin = 2;

        /** false to prevent NAR from attempting conceptualization of terms exceeding its max volume */
        public static final boolean CONCEPTUALIZE_OVER_VOLUME = false;

        /** HOL - allow implications inside conjunctions.
         * TODO what benefit does this provide? */
        @Is("Higher-order_logic")
        public static final boolean IMPL_IN_CONJ = IS("IMPL_IN_CONJ", false);


        public static final boolean CONJ_FACTOR =
            false;
            //true;

        /** master INH bundle enable */
        public static final boolean INH_BUNDLE = false;

        /** if false, helps to separate the set of HOL (impl) concepts, from event-based concepts.
         *  consider if a CONJ goal with an IMPL sub-event decomposes an IMPL goal
         *  which may (and probably is set to) be invalid.
         * */
        public static final boolean CONJ_INDUCT_IMPL = IMPL_IN_CONJ;


        public static final boolean NEG_INTRIN_CACHE = true;

        /** false requires less re-instantiation of negated subterms,
         *  at the cost of more stored permutations */
        public static final boolean SUBTERMS_UNPERMUTE =
            //false;
            true;

        public static final boolean NORMALIZE_IMPL_SUBJ_NEG_VAR = true;

        /** 0 disables */
        public static final int interningComplexityMax =
            18;
            //0; //DISABLED

//        public static final boolean CONJ_FILTER_NEG_SEQ = false;

//        /** experimental */
//        public static final boolean CONJ_RANGE_ECHO = false;

//        /** experimental TODO fix */
//        public static final boolean CONJ_FACTOR = false;
    }

    public enum test {
        ;

        /**
         * for NALTest's: extends the time all unit tests are allowed to run for.
         * normally be kept to 1 but for debugging this may be increased to find what tests need more time
         */
        public static final float TIME_MULTIPLIER = 1f;

        /**
         * how precise unit test Truth results must match expected values to pass
         */
        public static final float TEST_EPSILON =
                1/100f; //standard NAL test precision

        public static final boolean DEBUG_EXTRA = false;
        public static final boolean DEBUG_ENSURE_DITHERED_TRUTH = false;
        public static final boolean DEBUG_ENSURE_DITHERED_OCCURRENCE= false;
        public static final boolean DEBUG_ENSURE_DITHERED_DT = false;
    }

    public enum derive {;

        public static final boolean FILTER_PRIORITY_UNDERFLOW = false;

//        /**
//         * may cause unwanted "sticky" event conflation. may only be safe when the punctuation of the task in which the event contained is the same
//         */
//        public static final boolean TIMEGRAPH_ABSORB_CONTAINED_EVENT= IS("TIMEGRAPH_ABSORB_CONTAINED_EVENT");
//        /**
//         * if false, keeps intersecting timegraph events separate.
//         * if true, it merges them to one event. may cause unwanted "sticky" event conflation
//         * may only be safe when the punctuation of the task in which the event contained is the same
//         */
//        public static final boolean TIMEGRAPH_MERGE_INTERSECTING_EVENTS= IS("TIMEGRAPH_MERGE_INTERSECTING_EVENTS");


        /** TODO break into individual ones as this is used in different places */
        public static final int TTL_UNISUBST = 64;

        public static final int TTL_CONJ_MATCH = Math.max(1, TTL_UNISUBST/4);


        /**
         * cost of executing a termute match (leaf of the tree)
         */
        public static final int TTL_COST_MATCH = Math.max(1, TTL_UNISUBST/2);

        /**
         * cost of attempting to grow a branch
         */
        public static final int TTL_COST_TRY = 1;




//        /**
//         * attempt to create a question/quest task from an invalid belief/goal (probably due to missing or unsolved temporal information
//         * in some cases, forming the question may be answered by a dynamic truth calculation later
//         */
//        public static final boolean DERIVE_QUESTION_FROM_AMBIGUOUS_BELIEF_OR_GOAL = false;


//        /** override to allow all evidence overlap */
//        public static final boolean OVERLAP_GLOBAL_ALLOW = false;


        /** derivation explosion rate; >=1 TODO allow float */
        public static final int PREMISE_UNIFICATION_TASKIFY_LIMIT = 1;

        public static final boolean QUESTION_SALVAGE = true;

        /** TODO decide if this is any faster */
//        public static final boolean ACTION_METHODHANDLE = false;
//        public static final boolean TESTER_METHODHANDLE = false;
//        public static final boolean COMPILE_TO_LAMBDA = false;

        /** if false, unification and taskification are two separate premise steps */
        public static final boolean TASKIFY_INLINE = true;

        public static final boolean VARIABLE_INTRODUCE_GOALS = false;
        public static final boolean VARIABLE_INTRODUCE_QUESTIONS = false;
        public static final boolean VARIABLE_INTRODUCE_QUESTS = VARIABLE_INTRODUCE_QUESTIONS;

        public static final boolean CAUSE_PUNC = false;

        /** TODO FloatRange */
        public static final float TERMLINK_and_CLUSTERING_FORGET_RATE =
            FORGETTING;
            //1;
            //0.75f;
            //0.5f;

        public static final boolean DIVIDE_OR_DECOMPOSE =
            true;
            //false;

        public static final boolean AND_COMPILE = true;


        public static final boolean PROFILE = false;
    }

    /** premise loopiness probability */
    public final FloatRange loopy = FloatRange.unit(0);

    public enum occSolver {
        ;

        /**
         * whether timegraph should not return solutions with volume significantly less than the input's.
         * set 0 to disable the filter
         */
        public static final float TIMEGRAPH_DEGENERATE_SOLUTION_THRESHOLD_FACTOR = 0.75f;

        /** for dt's and absolute events for solution events */
        public static final boolean TIMEGRAPH_DITHER_EVENTS_EXTERNALLY =
            true;

        /**
         * whether to dither events as they are represented internally.  output events are dithered for the NAR regardless.
         */
        public static final boolean TIMEGRAPH_DITHER_EVENTS_INTERNALLY = true;

        @Range(min = 1, max = 8)
        public static final int TIMEGRAPH_ITERATIONS = 1;
    }

    public enum unify {
        ;

        /**
         * max variable unification recursion depth as a naive cyclic filter
         * includes indirections through common variables so should be at least 3.
         */
        public static final int UNIFY_VAR_RECURSION_DEPTH_LIMIT = 16;
        public static final int UNIFY_COMMON_VAR_MAX = UNIFY_VAR_RECURSION_DEPTH_LIMIT;
        public static final int UNIFICATION_STACK_CAPACITY = 64;

        public static final boolean SHUFFLE_TERMUTES = IS("SHUFFLE_TERMUTES", true);

        public static final boolean UNISUBST_RECURSION_FILTER = false;
        /** allow negated variable to be assigned to negated target
         *  TODO allow mobius if assigning variables to variables?
         * */
        @Deprecated public static final boolean mobiusVar = true, mobiusConst = false;
    }

    public static final boolean NORMALIZE_STRICT = true;

//    /** conjunction factoring, experimental */
//    @Deprecated public static final boolean CONJ_FACTOR = false;

    /** 0.5 = hairtrigger hysterical, ~1.0 = most careful */
    public static final float BUTTON_THRESHOLD_DEFAULT =
        //1/2f + truth.FREQ_EPSILON;
        1/2f;
        //2/3f;
        //3/4f;

    /** form of the resulting: a link (in the TaskLinkBag), or a premise */
    public static final boolean Decompose_LinkOrPremise =
        true;
        //false;

}

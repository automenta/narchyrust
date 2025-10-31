package nars.control;

import jcog.Fuzzy;
import jcog.Is;
import jcog.math.normalize.Percentilizer;
import jcog.pri.op.PriMerge;
import jcog.signal.FloatRange;
import nars.Deriver;
import nars.Focus;
import nars.NALTask;
import nars.Premise;
import nars.task.util.OpPri;
import nars.task.util.PuncBag;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.*;
import static nars.TruthFunctions.e2c;

/**
 * SeH's default budget impl
 */
@SuppressWarnings("WeakerAccess")
public class DefaultBudget extends Budget {

    /**
     * Taskify derived  task priority factor, by conclusion punctuation
     */
    public final PuncBag puncDerived = new PuncBag(
            //0.9f
            PHI_min_1f
            //sqr(PHI_min_1f)
            //sqr(sqr(PHI_min_1f))
            //PHI_min_1f, sqr(PHI_min_1f), PHI_min_1f, sqr(PHI_min_1f)
            //sqr(PHI_min_1f), sqr(sqr(PHI_min_1f)), sqr(PHI_min_1f), sqr(sqr(PHI_min_1f))
            //sqr(sqr(PHI_min_1f)), sqr(sqr(sqr(PHI_min_1f))), sqr(sqr(PHI_min_1f)), sqr(sqr(sqr(PHI_min_1f)))
            //0.9f, PHI_min_1f, 0.9f, PHI_min_1f
            //1, PHI_min_1f, 1, PHI_min_1f
            //1
            //0.5f
            //0.95f, PHI_min_1f, 0.95f, PHI_min_1f
            //0.1f
            , 0, 1);

    /**
     * biases choice of seed premise's punctuation in TaskBagDeriver
     */
    public final PuncBag puncSeed = new PuncBag(1, 0, 1);

    //public final PuncBag puncLink = new PuncBag(1, 0, 1)
    //.set(1, 1/2f) //b and g > q's
    //.set(1, 1/4f) //b and g >> q's

    /**
     * Taskify premise selection probability, by conclusion punctuation
     */
    //public final PuncBag puncNALPremise = new PuncBag(1);
    //public final PuncBag puncTaskifyPremise = new PuncBag();

    //public final OpPri opDerived = new OpPri();
    public final OpPri opPri = new OpPri();


    /**
     * complexity activation cost - occam's razor - demand for simplicity
     * complexity discount: power factor for penalizing incremental complexity increase
     */
    @Is("AIXI")
    public final FloatRange simple = new FloatRange(0.5f, 0, 8);

    private static final boolean simpleDerivedOrInput = false;


    /**
     * how important is it to conserve evidence.
     * leniency towards uncertain derivations
     * pressure to conserve confidence.
     * <p>
     * "keep an open mind, but not too open that brains fall out."
     * <p>
     * TODO maybe make separate belief and goal certainty
     */
    public final FloatRange certain = new FloatRange(
        1
        //2
        //1/3f
        //1/4f
        //1/2f
        //1/10f
        //1/5f
        //3/4f
        //3
        , 0, 8);


    private static final boolean simpleAbsOrRel = true, certaintyAbsOrRel = false;

    public final FloatRange inputActivation = new FloatRange(
            1
            //1/2f
            //1/4f
            //1/3f
            //1/5f
            //1/8f
            //1/10f
            , 0, 2);


//    /** link spiral out factor (ex: termlinking) */
//    @Deprecated @Is("Connectivity_(graph_theory)") private final FloatRange out =
//        new FloatRange(_out, 0, 2);

    /**
     * frequency polarity importance
     */
    public final FloatRange polarized = new FloatRange(0, 0, 1);

    /**
     * double premise derivative priority mode. TODO check TaskList.fund() consistent with this
     */
    public final PriMerge derivePri =
            //PriMerge.meanGeo
            //PriMerge.or //eager, ambitious
            PriMerge.mean //stable
            //PriMerge.meanAndMean //stable-ish
            //PriMerge.and //parent tasks remain dominant
            //PriMerge.plus //derivation-dominant
            //PriMerge.max
            //PriMerge.min
            ;

    double premiseAmp =
            1
            //1.1
            //Util.PHI;
            //1.3
            //1.2
            //1.1
            //0.1
            //0.99
            //0.1
            ;


//    /** enable complexity discount task links */
//    public final AtomicBoolean simpleTaskLink = new AtomicBoolean(false);

//    /** enable complexity discount derived links  */
//    public final AtomicBoolean simpleDerivedLink = new AtomicBoolean(false);

//    /** enable complexity discount derived tasks  */
//    public final AtomicBoolean simpleDerivedTask = new AtomicBoolean(true);


//    public final FloatRange taskActivation = FloatRange.unit(1);


    private double simpleIn(NALTask t, Focus f) {
        if (t.isInput()) return 1; //don't penalize input tasks

        return simple(
                t.term().complexity()
                //t.term().complexityConstants() //doesn't penalize variables
                , f);
    }

    @Override
    public float priDerived(NALTask xy, NALTask x, @Nullable NALTask y, Object d) {
        var p =
                (x != null ? priTaskPremise(x, y) : 1)

                        * (xy.BELIEF_OR_GOAL() ? (
                        (x == null ? 1 :
                                certain(xy, x, y, d)) *
                                polarized(xy)
                ) : 1)

                        * puncDerived.apply(xy);

        //* opDerived.apply(xy.term())

        if (simpleDerivedOrInput) {
            p *= simple(
                    xy.complexity()
                    //xy.term().complexityConstants() //doesn't penalize variables
                    , d);
        }

        //* range(xy, x, y);

        return (float) p;
    }

    public final double priTaskPremise(Premise p) {
        return priTaskPremise(p.task(), p.belief());
    }

    public double priTaskPremise(NALTask x, @Nullable NALTask y) {
        return priTaskPremise(x, y, derivePri);
    }


//    private static double range(NALTask xy, NALTask x, @Nullable NALTask y) {
//        if (x.ETERNAL()) return 1;
//        if (y!=null && y.ETERNAL()) return 1;
//
//        return min(1,
//            ((double)xy.range()) / Math.max(x.range(), y!=null ? y.range() : 0)
//        );
//    }

//    public final FloatRange volIdeal  = new FloatRange(1, 1, 64);
//    /** how insensitive volume is to the discount */
//    public final FloatRange volTolerance  = new FloatRange(8, 1, 64);
//    private static float volIdeal(float vol, float volIdeal, float volTolerance) {
//        float delta = Math.abs(vol - volIdeal);
//        return 1 / (1 + delta/volTolerance);
//    }


    public static double priTaskPremise(NALTask t, @Nullable NALTask b, PriMerge merge) {
        double tPri = t.priElseZero();
        return b != null && b != t ? merge.valueOf(tPri, b.priElseZero()) : tPri;
    }

    private double polarized(NALTask x) {
        var p = polarized.floatValue();
        if (p==0) return 1;
        else {
            float freq = x.freq();
            return lerpSafe(p, 1, Fuzzy.polarity(freq));
        }
    }

    /**
     * conservation of evidence
     */
    private double certain(NALTask xy, NALTask x, @Nullable NALTask y, Object d) {

        double eParent;
        if (certaintyAbsOrRel) {
            eParent = ((Deriver) d).nar.eviDefault(xy.punc());
        } else {
            eParent = eviParents(
                    x.truth(),
                    y == null ? null : y.truth());
            if (eParent < Double.MIN_NORMAL)
                return 1;
        }

        double eDerived = xy.evi();

        if (eDerived >= eParent) return 1;

        double c =
//            d instanceof Deriver ?
//                certaintyPercentile(xy, eParent, eDerived, (Deriver) d) :
                certaintyPct(eParent, eDerived);


        c = unitizeSafe(c);

        float certain = this.certain.floatValue();
        return Math.pow(c, certain);
    }

    protected double simple(float complexity, Object d) {
        double s = simpleAbsOrRel ?
            simpleAbsolute(complexity, d) :
            simpleRelative(complexity, (Deriver) d);

        return Math.pow(s, this.simple.floatValue());
    }

    protected static double simpleAbsolute(float complexity, Object d) {
        return 1 - complexity / (complexMax(d) + 1);
    }

    protected static double simpleRelative(float complexity, Deriver d) {
        float parentComplexity = d.premise.complexityMean();
        return parentComplexity / (complexity + parentComplexity);
    }

    //    /** priority post-processing */
//    private double p(double p, Deriver d) {
//        return dropNotPrioritize ? dropOrDontAffectPriotiy(p, d) : p;
//    }

//    private float dropRate = 0.25f;
//    private double dropOrDontAffectPriotiy(double p, Deriver d) {
//        return drop(p, d) ? 1 : Double.NaN;
//    }
//    private boolean drop(double p, Deriver d) {
//        return d.randomBoolean((float) Math.pow(p, dropRate));
//    }

    final Percentilizer
            certaintyBelief = new Percentilizer(256, true),
            certaintyGoal = new Percentilizer(256, true);

    private double certaintyPercentile(NALTask xy, double eParent, double eDerived, Deriver d) {
        double v =
                eDerived / eParent //RELATIVE
                //e2c(eDerived) //ABSOLUTE
                ;

        return (xy.BELIEF() ? certaintyBelief : certaintyGoal).valueOf(Math.min((float) 1, (float) v));
    }

    private static double certaintyPct(double eParent, double eDerived) {
        //assert(y==null || y.BELIEF());
        return confRetention.apply(eParent, eDerived);
        //System.out.println(/*n4(eParent) + "  ->" + n4(eDerived) + " = " +*/ r);

        //return lerpSafe(certain.floatValue(), 1, unitizeSafe(r));
        //return Math.pow(unitizeSafe(r), certain.floatValue());
    }

    private static final ConfidenceRetention confRetention =
        ConfidenceRetention.EviLinear;

    //ConfidenceRetention.ConfLinear;
    //ConfidenceRetention.EviSqrt;
    //ConfidenceRetention.EviLog;


    @Override public float priIn(NALTask t, Focus f) {
        float p = inputActivation.floatValue() * t.priElseZero();
        if (!simpleDerivedOrInput && !t.isInput())
            p *= simpleIn(t, f);
        return p;
    }



    interface Simplicity {
        /**
         * value in 0..1 representing simple (0) to complex (1)
         */
        float simple(float volume, int volMax);
    }

//    private final Simplicity simplicity =
//        new LinearSimplicity();
//        //new HistogramSimplicity();

    private static class LinearSimplicity implements Simplicity {
        @Override
        public float simple(float volume, int volMax) {
            return 1 - Math.min(Math.max(volume - 1, (float) 0) / volMax, (float) 1);
        }
    }

    /**
     * TODO use Percentilizer ?
     */
    private static class HistogramSimplicity implements Simplicity {

        private final Percentilizer p = new Percentilizer(128, true);

        /**
         * maximum influence, <1
         */
        private static final float strength = 0.95f;


        @Override
        public float simple(float volume, int volMax) {
            return 1 - strength * this.p.valueOf(volume / volMax);
        }
    }

    @Deprecated
    private static int complexMax(Object c) {
        return c instanceof Focus f ? f.complexMax() : ((Deriver) c).complexMax;
    }

    /** confidence loss functions.  returns a multiplier proportional to
     *  the amount of evidence retained from a parent in a derivation. */
    public enum ConfidenceRetention {
        EviLinear {
            @Override
            public double apply(double eParent, double eDerived) {
                return eDerived / eParent;
            }
        },
        ConfLinear {
            @Override
            public double apply(double eParent, double eDerived) {
                return e2c(eDerived) / e2c(eParent);
            }
        },
        EviSqrt {
            @Override
            public double apply(double eParent, double eDerived) {
                return Math.sqrt(eDerived / eParent);
            }
        },
        /** more lenient than linear */
        EviLog {
            @Override
            public double apply(double eParent, double eDerived) {
                return log1p(eDerived) / log1p(eParent);
            }
        };

        abstract public double apply(double eParent, double eDerived);
    }
}
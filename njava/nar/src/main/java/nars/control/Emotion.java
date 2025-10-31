package nars.control;


import jcog.signal.meter.*;
import nars.NAR;

import java.util.function.Consumer;

import static jcog.Str.n4;


/**
 * emotion - internal mental state
 * manages non-logical/meta states of the system
 * and collects/provides information about them to the system
 * variables used to record emotional values
 * <p>
 * TODO cycleCounter, durCounter etc
 */
public class Emotion implements Consumer<NAR> {

    static final Metered.FieldMetricsBuilder EmotionFields;
    static {
        EmotionFields = Metered.fieldsOf(Emotion.class);

    }

    /**
     * TODO
     */
    public final FastCounter conceptNew = new FastCounter("concept create");
//    public final Counter conceptCreateFail = new FastCounter("concept create fail");
    public final FastCounter conceptDelete = new FastCounter("concept delete");
    /**
     * perception attempted
     */
//    public final FastCounter perceivedTaskStart = new FastCounter("perceived task start");
    /**
     * perception complete
     */

    /**
     * increment of cycles that a dur loop lags in its scheduling.
     * an indicator of general system lag, especially in real-time operation
     */
    public final FasterCounter durLoopLag = new FasterCounter("Dur loop lag sum (cycles)");
//    public final FasterCounter durLoopSlow = new FasterCounter("Dur loop slow sum (cycles)");
    public final FastCounter narLoopLag = new FastCounter("NAR loop lag (ns)");

//    public final FastCounter derivedTask = new FastCounter("derive task");
//    public final FastCounter derivedPremise = new FastCounter("derive premise");

//    public final FastCounter derivedTaskLink = new FastCounter("derive tasklink");

    //    public final FastCounter deriveUnified = new FastCounter("derive unified");

    public final ExplainedCounter deriveFailTemporal = new ExplainedCounter("derive fail temporal");
    public final ExplainedCounter deriveFail = new ExplainedCounter("derive fail eval");
    public final FastCounter deriveFailVolLimit = new FastCounter("derive fail vol limit");
//    public final FastCounter deriveResolveVolLimit = new FastCounter("resolve fail vol limit");

    public final FastCounter deriveFailNullTerm = new FastCounter("derive fail null term");
    public final FastCounter deriveFailTaskTerm = new FastCounter("derive fail task term");
//    public final FastCounter deriveFailTaskify = new FastCounter("derive fail taskify");
    public final FastCounter deriveFailTruthUnderflow = new FastCounter("derive fail taskify truth underflow");

//    public final FastCounter deriveFailPrioritize = new FastCounter("derive fail prioritize");
    public final FastCounter deriveFailParentDuplicate = new FastCounter("derive fail parent duplicate");

    public final FastCounter derivedPremiseInvalid = new FastCounter("derived premise invalid");
//    public final FastCounter derivedTaskInvalid = new FastCounter("derived task invalid");


    public final Use time_derive_taskify =      new Use("derive taskify");
    public final Use time_derive_occurrify =      new Use("derive occurrify");
    //public final Use derive_time_Input_Derivation =     new Use("derive F");


    /** sum of volumes of input task terms */
    public final FasterCounter busyVol = new FasterCounter("busy volume");
    //public final BufferedDouble busyVol = new BufferedDouble("busy volume");
    //public final FloatAveragedWindow busyVolPriWeighted = new FloatAveragedWindow(history, 0.75f, 0);


    public final Metered fields;


    public Emotion() {
        fields = EmotionFields.get("emotion", this);
    }


    /**
     * new frame started
     */
    @Override
    public void accept(NAR nar) {
        narLoopLag.set(nar.loop.lagNS());
        durLoopLag.commit();
//        durLoopSlow.commit();
        busyVol.commit();
    }


    public String summary() {
        return "busy=" + n4(busyVol.getAsDouble());
    }


    public void durLoop(Object context, long lag, long slow, int dur) {

//        double lagPct = ((double)lag)/dur;
//        System.out.println(context + " " + lagPct);

        durLoopLag.add(lag);
        //durLoopSlow.add(slow);
    }
}
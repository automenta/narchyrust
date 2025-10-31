package nars.game.util;

import jcog.Is;
import jcog.Research;
import jcog.Util;
import jcog.pri.PlainLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.FloatRange;
import nars.Focus;
import nars.NAL;
import nars.game.Game;
import nars.game.sensor.SignalConcept;
import nars.table.dynamic.SerialBeliefTable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * perceptual signal mixer
 */
@Research
@Is({"Weber–Fechner_law", "Figure–ground_(perception)"})
public final class Perception implements Consumer<PriReference<SignalConcept>> {

    private static final float CHANGE_FACTOR = 1;

    private final Focus focus;

    /** default SerialUpdater for current time */
    public SerialBeliefTable.SerialUpdater updater;

    /**
     * set automatically
     */
    private float durUpdateMax = 1;

//    private final PLinkArrayBag<SignalConcept> queue = new PLinkArrayBag<>(PriMerge.replace,
//        new ArrayBag.PlainListArrayBagModel<>()
//    );
    private final PLinkArrayBag<SignalConcept> queue = new PLinkArrayBag<>(PriMerge.replace,
        new ArrayBag.PlainListArrayBagModel<>()
    );

    /**
     * perceptual valve: how much of attention (% of the Focus's taskbag capacity) to fill percepts
     */
    public final FloatRange perceive = FloatRange.unit(NAL.signal.PERCEPTION_RATE_DEFAULT); //TODO tune

    private float temperature;

    final Random rng = new XoRoShiRo128PlusRandom();
    private long now;

    /**
     * TODO use enum or abstract
     */
    @Deprecated
    private final boolean sampleOrGreedy = true;

    /**
     * sample or sampleUnique; used if in random mode
     */
    @Deprecated
    private final boolean sampleIndividuallyOrBatch = false;

    public Perception(Game g) {
        var f = g.focus();
        this.focus = f;
        this.updater = new SerialBeliefTable.SerialUpdater(g.time, f.nar);
    }

    public PriReference<SignalConcept> put(SignalConcept s, float pri, float changeAccumulated, float durSince) {
        return queue.put(new PlainLink<>(s, pri(pri, changeAccumulated, durSince)));
    }

    /**
     * TODO changeAccumulated, more accurately integral (mult. by time since last update)
     */
    private float pri(float pri, float changeAccumulated, float durSince) {
        var changeFactor = Math.min((float) 1, CHANGE_FACTOR * changeAccumulated);
        var timeFactor = Math.min((float) 1, durSince / durUpdateMax); //durs pending

        var p = pri *
                Math.min(changeFactor + timeFactor, (float) 1);
            //Fuzzy.mean(changeFactor, timeFactor);

        return Util.lerpSafe(p, temperature, 1);
    }

    public void commit() {
        queue.commit(null);

        var total = queue.size();
        if (total == 0) return;

        now = focus.time();

        int toActivate = activations(total);
        if (total <= toActivate)
            commitAll();
        else
            commitSome(toActivate);
    }

    private int activations(int s) {
        return Util.clampSafe(
                (int) (perceive.asFloat() * focus.attn.capacity())
                , 1, s);
    }

    private void commitSome(int toActivate) {
        //SOME
        int s = queue.size();
        durUpdateMax = 1 + ((float)s) / toActivate; //AUTO, for next cycle TODO check this math
        if (sampleOrGreedy) {
            if (sampleIndividuallyOrBatch)
                sampleIndividually(toActivate); //individual
            else
                accept(toActivate, queue.sampleUnique(rng)); //batch
        } else
            accept(toActivate, queue.iterator()); //greedy

//            System.out.println(focus.id + " attention\t"
//                    + queue.priMin() + ".." + queue.priMax());
    }

    private void commitAll() {
        queue.forEach(this);
        durUpdateMax = 1 + 1;
    }

    private final Set<PriReference<SignalConcept>> toActivate = new HashSet<>();
    private void sampleIndividually(int n) {
        //TODO may need to sample for more than nToInput due to repeats.
        for (int i = 0; i < n; i++)
            toActivate.add(queue.sample(rng));
        toActivate.forEach(this);
        toActivate.clear();
    }

    private void accept(int nToInput, Iterator<PriReference<SignalConcept>> ii) {
        for (int i = 0; i < nToInput && ii.hasNext(); i++)
            accept(ii.next());
    }

    @Override
    public void accept(PriReference<SignalConcept> s) {
        s.get().remember(focus, now);
    }

    public void capacity(int capacity) {
        queue.capacity(capacity);
        temperature = Math.max(
                1f/capacity,
                //1f/Util.sqrt(capacity)
            Prioritized.EPSILON);
    }

    public final NAL nar() {
        return focus.nar;
    }

    public void remove(SignalConcept s) {
        queue.remove(s);
    }
}

package nars.func;

import jcog.random.XoRoShiRo128PlusRandom;
import jcog.sort.QuickSort;
import jcog.tensor.ClassicAutoencoder;
import nars.*;
import nars.game.Game;
import nars.game.sensor.SignalComponent;
import nars.game.sensor.VectorSensor;
import nars.task.SerialTask;
import nars.term.Neg;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static nars.Op.*;

/**
 * decompiles a continuously trained autoencoding of an input concept vector
 * TODO make DurService
 * TODO extend VectorSensor
 */
public class AutoConceptualizer extends VectorSensor {

    public final ClassicAutoencoder ae;

    private final List<SignalComponent> concepts;

    private final boolean beliefOrGoal;
    private final float[] x;
    float learningRate = 0.05f;

    public AutoConceptualizer(Term id, List<SignalComponent> concepts, boolean beliefOrGoal, int features, NAR n) {
        super(id, concepts.size());
        this.concepts = concepts;
        this.beliefOrGoal = beliefOrGoal;
        this.ae = new ClassicAutoencoder(concepts.size(), features, ()->0.01f, new XoRoShiRo128PlusRandom(n.random().nextLong()));
        this.x = new float[concepts.size()];
        ae.noise.set(0.0002f);
    }

    @Override
    public void accept(Game g) {

        NAR n = g.nar;
        byte punc = beliefOrGoal ? BELIEF : GOAL;
        long now = n.time();
        float[] x = this.x;
        int inputs = concepts.size();
        for (int i = 0, inSize = inputs; i < inSize; i++) {
            BeliefTable beliefTable = ((BeliefTable) concepts.get(i)
                    .table(punc));
            Truth t = beliefTable.truth(now, now, n);
            x[i] = t == null ? 0.5f : t.freq();
        }

        //ae.noise.set(noiseLevel);
        ae.put(x);
        
        int outputs = ae.outputs();
        double[] b = new double[outputs];

        float thresh = n.freqRes.floatValue();

        Focus w = g.focus();

        float dur = w.dur();
        long start = Math.round(now - dur / 2), end = Math.round(now + dur / 2);
        int[] order = new int[inputs];
        Truth truth = $.t(1, g.nar.confDefault(punc));
        for (int i = 0; i < outputs; i++) {
            b[i] = 1; 

            double[] a = ae.decode(b);
            
            Term feature = conj(order, a /* threshold, etc */, 3 /*a.length/2*/,
                    thresh);
            if (feature != null)
                w.remember(onFeature(feature, truth, start, end,
                        n.evidence()));

            b[i] = 0; 
        }
    }

    protected static SerialTask onFeature(Term feature, Truth truth, long start, long end, long[] evi) {
        if (feature instanceof Neg) {
            feature = feature.unneg();
            truth = truth.neg();
        }
        return new SerialTask(feature, BELIEF, truth, start, end, evi);
    }

    private Term conj(int[] order, double[] a, int maxArity, float threshold) {

        
        int n = a.length;
        for (int i = 0; i < n; i++)
            order[i] = i;

        float finalMean = 0.5f; 
        QuickSort.sort(order, i -> (float) Math.abs(finalMean - a[i]));

        Set<Term> x = new UnifiedSet<>(maxArity);
        int j = 0;
        for (int i = 0; i < order.length && j < maxArity; i++) {
            int oi = order[i];
            double aa = a[oi];
            if (Math.abs(aa - 0.5f) < threshold)
                break; 

            x.add(concepts.get(oi).term().negIf(aa < finalMean));
            j++;
        }

        return x.isEmpty() ? null : CONJ.the(0, x);
    }

    @Override
    public Iterator<SignalComponent> iterator() {
        return concepts.iterator();
    }
}
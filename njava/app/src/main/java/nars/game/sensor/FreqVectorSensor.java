package nars.game.sensor;

import jcog.data.iterator.ArrayIterator;
import jcog.event.Off;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.wave1d.SignalInput;
import jcog.signal.wave1d.SlidingDFTTensor;
import nars.NAR;
import nars.Term;
import nars.game.Game;

import java.util.Iterator;
import java.util.function.IntFunction;

/** frequency domain representation of a waveform
 * TODO option to include phase data as additional vector inputs
 * */
public class FreqVectorSensor extends VectorSensor {


    /**TODO abstract */
    final ArrayTensor transform;

    private final FreqVectorComponentConcept[] component;

    abstract class FreqVectorComponentConcept extends SignalComponent {
        int start, end;
        private float v, vNorm;

        FreqVectorComponentConcept(Term componentID, NAR nar) {
            super(componentID, nar);
        }


        protected void mean(int start, int end) {
            double m = 0;
            float[] data = transform.data;
            for (int i = start; i < end; i++)
                m += data[i];
            v = (float) m / (end-start);
        }

        abstract void commit();

        public void normalize(float max) {
            vNorm = v/max;
        }

        @Override
        public float value(Game g) {
            return vNorm;
        }

    }

    private final class FreqComponentSignal extends FreqVectorComponentConcept {

        FreqComponentSignal(Term componentID, NAR nar) {
            super(componentID, nar);
        }

        @Override void commit() {
            mean(start, end);
        }

    }
    private final class PhaseComponentSignal extends FreqVectorComponentConcept {

        PhaseComponentSignal(Term componentID, NAR nar) {
            super(componentID, nar);
        }

        @Override
        void commit() {
            final int o = transform.data.length / 2;
            mean(o + start, o + end);
        }
    }

//    public final FloatRange noiseRemoval = new FloatRange(0.5f, 0, 1);
//    final Ewma[] history;
//    private final int freqMax;

//    float[] freqValue;
    //private float[] inBuf = null;

//    public final FloatRange center, bandwidth;

    public FreqVectorSensor(int bands, int fftSize, int fMin, int fMax, IntFunction<Term> termizer, NAR n) {
        //super(termizer.apply(fftSize/2+1) /*n+1*/, n);
        super(termizer.apply(-1), bands * 2);

//        this.buf = buf;

//        freqMax = fftSize/2;
        //        center = new FloatRange(freqMax/2f, 0, freqMax);
//        bandwidth = new FloatRange(freqMax, 0, freqMax);

        transform =
            new SlidingDFTTensor(fftSize);
            //new HaarWaveletTensor(new ArrayTensor(inBuf), sampleWindow);

        component = new FreqVectorComponentConcept[size];

        for (int c= 0; c < bands; c++) {
            component[c] = new FreqComponentSignal(termizer.apply(c), n);
            component[c+bands] = new PhaseComponentSignal(termizer.apply(c+bands), n);
        }

        fMin = Math.max(fMin, 2);

        if (fMax > fftSize)
            throw new UnsupportedOperationException();

        //TODO abstract different band patterns
        //simple linear:
        float bandwidth = fMax-fMin; //?? /2;
        final float componentsPerBand = bandwidth/bands;
        if (componentsPerBand<2)
            throw new UnsupportedOperationException();
        for (int c = 0; c < bands; c++) {
            int s = (int)(c * componentsPerBand) + fMin;
            int e = Math.min((int)(s + componentsPerBand - 1), fftSize);
            component[c].start = s;
            component[c].end = e;
            component[c+bands].start = s;
            component[c+bands].end = e;
        }

    }

    /** asynchronous sample input */
    public void input(float[] buf) {

        SlidingDFTTensor t = (SlidingDFTTensor) this.transform;
        t.update(new ArrayTensor(buf));

        int bands = component.length/2;
        float magMax = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < component.length; i++) {
            FreqVectorComponentConcept c = component[i];
            c.commit();
            if (i < bands)
                magMax = Math.max(magMax, c.v);
        }

        for (int i = 0; i < bands; i++) {
            component[i].normalize(magMax);
            component[i+bands].normalize(1); //no normalization, just commit
        }

    }

//    @Override
//    public void accept(Game g) {
//
////
////
//////        float center = this.center.floatValue();
//////        float bw = this.bandwidth.floatValue();
//////        float fMin = Math.max(0, center - bw/2);
//////        float fMax = Math.min(freqMax, center + bw/2);
//////        int n = componentValue.length;
//////        float fDelta = (fMax - fMin) / n;
//////        float fRad = fDelta/2;
//////        for (int i = 0; i < n; i++) {
//////            float c = Util.lerp((i + 0.5f)/n, fMin, fMax);
//////            float a = c - fRad;
//////            float b = c + fRad;
//////            double f = Util.interpSum(freqValue, a, b)/(b-a);
//////            componentValue[i] = (float) f;
//////        }
//        float[] componentValue = transform.data;
//        float intensity;
//        Util.normalize(componentValue, 0, intensity = Util.max(componentValue));
////
////        super.accept(g);
//    }

    @Override
    public Iterator<SignalComponent> iterator() {
        return ArrayIterator.iterate(component);
    }

    public float[] freq() {
        return transform.data;
    }

    public Off on(SignalInput in) {
        return in.wave.on((a)->{
//                int av = a.volume();
//                if (hearBuf.available() < av)
//                    hearBuf.freeHead(av);
//                hearBuf.write(a.data);
            input(((ArrayTensor)a).data);
        });
    }
}
package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Term;
import nars.func.Factorize;
import nars.game.Game;
import nars.subterm.Subterms;
import nars.term.atom.Atomic;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.var.Variable;
import nars.truth.Truther;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static nars.Op.SETe;

/**
 * base class for a group of concepts representing a sensor 'vector'
 */
public abstract class VectorSensor extends AbstractSensor implements Iterable<SignalComponent> {

    public final int size;

    protected VectorSensor(Term... id) {
        this(sensorID(id), id.length);
    }

    protected VectorSensor(Term id, int size) {
        if (size <= 1) throw new UnsupportedOperationException();
        super(id.hasVars() ? MASK.apply(id) : id);
        this.size = size;
    }


    @Override
    public void start(Game game) {
        super.start(game);
        game.nar.causes.newCause(term()).term();
    }

    public final int size() {
        return size;
    }

    //    public final Sensor model(VectorSensorAttention model) {
//        this.model = model;
//        return this;
//    }

    private static Term sensorID(Term[] states) {
        Subterms f = Factorize.applyConj(states, $.varDep(1));
        if (f!=null)
            return f.sub(1).sub(0 /* HACK unwrap PROD? */); //CONJ.the(f); //pattern found

        //fail-safe
        return SETe.the(states);
    }


//    /**
//     * best to override
//     */
//    @Override public int size() { return Iterables.size(this); }

//    /** surPRIse */
//    public double surprise() {
//        double s = 0;
//        for (Signal c : this)
//            s += ((SensorBeliefTables)c.beliefs()).surprise();
//        return s;
//    }

    @Override
    public void accept(Game g) {
        input(truther(g), resolution(), g);
    }

    private void input(Truther truther, float freqRes, Game g) {
        var priEach = priComponent();
        var p = g.perception;
        for (var s : this)
            s.input(truther.truth(s.updateValue(g), freqRes), priEach, null, p);
    }

    protected SignalComponent component(Term id, FloatSupplier f, NAR nar) {
        return new SignalComponent.LambdaSignalComponent(id, f, nar);
    }

    @Override
    public final Iterable<SignalComponent> components() {
        return this;
    }

    /**
     * consider the fairness of prioritization of each component, wrt to other sensors
     * TODO move to abstract sensor budgeting interface */
    public float priComponent() {
        return sensing.pri() / size(); //normalized
        //return sensing.pri() /  Util.sqrt(size()); //balanced
        //return sensing.pri(); //equality
        //TODO other modes
    }

    @Deprecated private static final AtomicInteger unknowns = new AtomicInteger();
    @Deprecated private static final String unknownPrefix = "⁉";
    @Deprecated private static synchronized Term nextUnknown() {
        return Atomic.atom(unknownPrefix + Character.toString(unknowns.getAndIncrement() + 'a'));
    }

    /** default: input everything , but subclasses can override to return sub-ranges */
    public Iterator<SignalComponent> inputIterator() {
        return iterator();
    }

    public double[] values() {
        double[] v = new double[size];
        int i = 0;
        for (var x : components())
            v[i++] = x.value;
        return v;
    }

    private static final RecursiveTermTransform MASK = new RecursiveTermTransform() {
        @Override
        public Term applyAtomic(Atomic x) {
            return x instanceof Variable ? nextUnknown() : x;
        }
    };


//    protected float priComponent(float pri) {
//        //post-commit phase
//        return pri / Math.max(1,
//            1
//            //active
//            //size()
//        );
//    }
}
//package nars.focus;
//
//import jcog.pri.Forgetting;
//import jcog.pri.Prioritizable;
//import jcog.pri.bag.Bag;
//import jcog.pri.op.PriAdd;
//import jcog.signal.FloatRange;
//import nars.Premise;
//
//import java.util.function.Consumer;
//
///** warning use one instance per bag */
//public abstract class AbstractBagSustain extends BagSustain implements Consumer<Premise> {
//
//    protected transient float multiplier;
//    protected transient float _factor;
//    public final FloatRange factor = new FloatRange(1, 0, 1);
//
//    @Override
//    public Consumer<Prioritizable> apply(Bag b) {
//        return Forgetting.forget(b, 1 - sustain.floatValue(), this::update);
//    }
//
//    protected Consumer<Premise> update(float multiplier) {
//        this.multiplier = multiplier;
//        this._factor = factor.floatValue();
//        return this;
//    }
//
//}
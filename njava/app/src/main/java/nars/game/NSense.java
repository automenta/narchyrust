package nars.game;

import jcog.Fuzzy;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.math.Digitize;
import jcog.math.FloatCached;
import jcog.math.FloatDifference;
import jcog.math.FloatSupplier;
import jcog.math.normalize.FloatNormalized;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.NAR;
import nars.Op;
import nars.Term;
import nars.func.AutoEncodedBitmap;
import nars.game.sensor.*;
import nars.sensor.BitmapSensor;
import nars.sensor.PixelBag;
import nars.term.Compound;
import nars.term.atom.Atom;
import nars.term.atom.Int;
import nars.term.util.TermException;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.hipparchus.util.MathUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.*;

import static jcog.math.Digitize.FuzzyNeedle;
import static nars.$.*;

/**
 * agent sensor builder
 */
public interface NSense {


    Int LOW = Int.NEG_ONE; //Atomic.the("low");
    Int MID = Int.ZERO; //Atomic.the("mid");
    Int HIH = Int.ONE; //Atomic.the("hih");


//    /**
//     * interpret an int as a selector between enumerated values
//     TODO move to a SelectorSensor constuctor */
//    default <E extends Enum> void senseSwitch(String term, Supplier<E> value) throws Narsese.NarseseException {
//        E[] values = ((Class<? extends E>) value.get().getClass()).getEnumConstants();
//        for (E e : values) {
//            sense(switchTerm(term, e.toString()), () -> value.get() == e);
//        }
//    }
//


//    static class EnumSignal extends AbstractSensor {
//
//        @Override
//        public void update(long last, long now, long next, NAR nar) {
//
//        }
//
//        @Override
//        public Iterable<Termed> components() {
//            return null;
//        }
//    }

    NAR nar();

    @SuppressWarnings("LambdaUnfriendlyMethodOverload")
    default AbstractSensor sense(Term term, BooleanSupplier value) {
        return sense(term, () -> value.getAsBoolean() ? 1 : 0);
    }

    /*
    default void senseFields(String id, Object o) {
        Field[] ff = o.getClass().getDeclaredFields();
        for (Field f : ff) {
            if (Modifier.isPublic(f.getModifiers())) {
                sense(id, o, f.getName());
            }
        }
    }



    default void sense(String id, Object o, String exp) {

        try {
            //Object x = Ognl.parseExpression(exp);
            Object initialValue = Ognl.getValue(exp, o);


            String classString = initialValue.getClass().toString().substring(6);
            switch (classString) {
                case "java.lang.Double":
                case "java.lang.Float":
                case "java.lang.Long":
                case "java.lang.Integer":
                case "java.lang.Short":
                case "java.lang.Byte":
                case "java.lang.Boolean":
                    senseNumber(id, o, exp);
                    break;

                //TODO String

                default:
                    throw new RuntimeException("not handled: " + classString);
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }*/

    <S extends Sensor> S addSensor(S s);

    /**
     * interpret an int as a selector between (enumerated) integer values
     */
    default SelectorSensor senseSwitch(Term id, Term[] values, IntSupplier value) {
        return addSensor(new SelectorSensor(id, values, value, nar()));
    }

    /**
     * interpret an int as a selector between (enumerated) object values
     */
    default void senseSwitch(String term, Supplier value, Object... values) {
        assert (values.length > 1);
        for (Object e : values)
            sense(inh(term, '"' + e.toString() + '"'), () -> value.get().equals(e));
    }

    default List<AbstractSensor> sense(int from, int to, IntFunction<String> id, IntFunction<FloatSupplier> v) {

        List<AbstractSensor> l = new Lst<>(to - from);
        for (int i = from; i < to; i++)
            l.add(sense(id.apply(i), v.apply(i)));

        return l;
    }

    /**
     * normalized
     */
    default AbstractSensor senseDiff(Term id, FloatSupplier v) {
        return sense(id, diff(v));
    }
    default AbstractSensor senseDiff(Term id, float clampRange, FloatSupplier v) {
        return sense(id, diff(v, clampRange));
    }

    default AbstractSensor senseDiffBi(Term id, FloatSupplier v) {
        return senseNumberBi(id, diff(v));
    }

    default DemultiplexedScalarSensor senseDiffBi(Term id, float clampRange, FloatSupplier v) {
        return senseNumberBi(id, diff(v, clampRange));
    }

    default AbstractSensor senseDiffBi(float clampRange, FloatSupplier v, Term... states) {
        return senseNumber(diff(v, clampRange), states);
    }
    default AbstractSensor senseDiffBi(FloatSupplier v, Term... states) {
        return senseNumber(diff(v), states);
    }

    default DemultiplexedScalarSensor senseDiffTri(Term id, float clampRange, FloatSupplier v) {
        return senseNumberTri(id, diff(v, clampRange));
    }

    default DemultiplexedScalarSensor senseDiffTri(Term id, FloatSupplier v) {
        return senseNumberTri(id, diff(v));
    }

    default FloatNormalized diff(FloatSupplier v) {
        return new FloatNormalized(
                new FloatDifference(v, narTimeDiff())
                //.nanIfZero()
        ).polar();
    }


    /** TODO test */
    default FloatNormalized diff(FloatSupplier v, float clampRange) {
        FloatDifference delta = new FloatDifference(v, narTimeDiff()) {
            @Override
            public float asFloat() {
                float x = super.asFloat();
                if (x == x)
                    return Util.clamp(x, -clampRange, clampRange);
                return x;
            }
        };///.nanIfZero();

        return new FloatNormalized(delta).polar();
    }


    private LongSupplier narTimeDiff() {
        return () -> {
            NAR n = nar();
            return n != null ? n.time() : Long.MIN_VALUE;
        };
    }

    @SuppressWarnings("LambdaUnfriendlyMethodOverload")
    default AbstractSensor sense(Term id, FloatSupplier v) {
        return addSensor(new LambdaScalarSensor(id, v));
    }

    default DemultiplexedScalarSensor sense(FloatSupplier v, Digitize model, Term... states) {
        if (states.length < 2)
            throw new UnsupportedOperationException();

        return addSensor(new DigitizedScalar(
            new FloatCached(v, () -> nar().time()),
            model, nar(),
            states
        ));
    }

    //AutoCloseable onFrame(Consumer r);

    default DemultiplexedScalarSensor sense(int precision, Digitize model, FloatSupplier v, IntFunction<Term> levelTermizer) {
        return sense(v, model, Util.arrayOf(levelTermizer, 0, precision, Term[]::new));
    }

    /** X=cos(t), Y=sin(t) : 2 components */
    default DemultiplexedScalarSensor senseAngle(Term root, FloatSupplier angleInRadians) {
        return sense(2,
            (theta, digit, maxDigits) -> (float) switch (digit) {
                case 0 -> Fuzzy.unpolarize(Math.cos(theta));
                case 1 -> Fuzzy.unpolarize(Math.sin(theta));
                default -> throw new UnsupportedOperationException();
            }
            , angleInRadians,
            angle -> root.replace(varDep(1), Atom.atomic(angle == 0 ? 'x' : 'y'))
        );
    }

    @Deprecated default DemultiplexedScalarSensor senseAngle(Term root, FloatSupplier angleInRadians, int divisions) {
        if (divisions == 2) {
            return senseAngle(root, angleInRadians);
        }

        assert (root instanceof Compound && root.containsRecursively(varDep(1)));
        return senseAngle(divisions, angleInRadians,
                angle -> root.replace(varDep(1), the(angle))
                //angle -> $.inh($.pRadix(angle, 2, divisions-1), root)
        );
    }

    /**
     * TODO make sure modulo wrap-around works correctly
     */
    @Deprecated default DemultiplexedScalarSensor senseAngle(int divisions, FloatSupplier angleInRadians, IntFunction<Term> termizer) {
        return sense(divisions,
            FuzzyNeedle
            //scalarEncoder(divisions) //divisions <= 3 ? scalarEncoder(divisions) : BinaryNeedle //TODO refine
            , () -> (float) (MathUtils.normalizeAngle(angleInRadians.asFloat(), Math.PI) / (2 * Math.PI)), termizer
            //$.inst($.the(angle), ANGLE),
            //$.func("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/,
            //$.funcImageLast("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/,
            //$.inh( /*id,*/ $.the(angle),"ang") /*SETe.the($.the(angle)))*/,
            //DigitizedScalar.Needle
        );
    }

    default AbstractSensor sense(String id, FloatSupplier v) {
        return sense($$(id), v);
    }

    default DemultiplexedScalarSensor senseNumberBi(Term id, FloatSupplier v) {
        Digitize e = scalarEncoder(2);
        var vv = varDep(1);
        if (id.containsRecursively(vv)) {
            return sense(v, e, id.replace(vv, LOW), id.replace(vv, HIH));
        } else {
            return sense(v, e, inh(id, LOW), inh(id, HIH));
        }
    }

    default DemultiplexedScalarSensor senseNumberTri(Term id, FloatSupplier v) {
        Digitize e = scalarEncoder(3);
        var vv = varDep(1);
        if (id.containsRecursively(vv)) {
            return sense(v, e, id.replace(vv, LOW), id.replace(vv, MID), id.replace(vv, HIH));
        } else {
            return sense(v, e, inh(id, LOW), inh(id, MID), inh(id, HIH));
        }
    }

    default Sensor senseNumberN(Term id, FloatSupplier v, int states) {
        if (states == 1) {

            //HACK remove #1 from template
            if (id.hasAny(Op.VAR_DEP)) {
                assert(id.varDep()==1);
                id = id.replace(varDep(1), Int.i(0));
            }

            return sense(id, v);

        } else {
            Term ID = id;
            if (!(ID instanceof Compound && ID.containsRecursively(varDep(1))))
                throw new TermException("does not contain #1 for replacement with vector index", id);
            IntFunction<Term> stateTerm = x -> ID.replace(varDep(1),
                //the(x)
                the(states%2==1 ? x - states/2 /* balanced pos,neg indexing */ : x)
            );
            return senseNumberN(stateTerm, states, v);
        }
    }

    default Sensor senseNumberN(IntFunction<Term> stateTerm, int states, FloatSupplier v) {
        return sense(v, scalarEncoder(states), Util.arrayOf(stateTerm, new Term[states]));
    }

    default AbstractSensor senseNumber(FloatSupplier v, Term... states) {
        return sense(v, scalarEncoder(states.length), states);
    }

    /** default scalar encoder for given arity
     *  TODO refine
     * */
    private static Digitize scalarEncoder(int arity) {
        return
            FuzzyNeedle
//            arity <= 3 ?
//                FuzzyNeedle :
//                FuzzyNeedle
                //BinaryContinuous
            //Digitize.Binary
            //Digitize.Fluid
            //Digitize.Proportional
            //Digitize.ProportionalSqr
            //DigitizedScalar.FuzzyGaussian
        ;
    }


    default <C extends Bitmap2D> BitmapSensor<C> senseCamera(@Nullable Term id, C bc) {
        return addSensor(new BitmapSensor(bc, id));
    }

    default <C extends Bitmap2D> BitmapSensor<C> senseCamera(@Nullable IntIntToObjectFunction<Term> id, C bc) {
        return addSensor(new BitmapSensor(bc, id));
    }

    default <C extends Bitmap2D> BitmapSensor<C> addCameraCoded(@Nullable Term id, Supplier<BufferedImage> bc, int sx, int sy, int ox, int oy) {
        return addSensor(new BitmapSensor(new AutoEncodedBitmap(new MonoBufImgBitmap2D(bc), sx, sy, ox, oy), id));
    }

    default <C extends Bitmap2D> BitmapSensor<C> addCameraCoded(@Nullable Term id, C bc, int sx, int sy, int ox, int oy) {
        return addSensor(new BitmapSensor(new AutoEncodedBitmap(bc, sx, sy, ox, oy), id));
    }


    default BitmapSensor<ScaledBitmap2D> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new ScaledBitmap2D(w, pw, ph));
    }


    default BitmapSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCameraRetina($$(id), w, pw, ph);
    }


    default BitmapSensor<PixelBag> senseCameraRetina(Term id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new PixelBag(new MonoBufImgBitmap2D(w), pw, ph));
    }

    default BitmapSensor<PixelBag> senseCameraRetina(Term id, Bitmap2D w, int pw, int ph) {
        return senseCamera(id, new PixelBag(w, pw, ph));
    }


    default <C extends Bitmap2D> BitmapSensor<C> senseCamera(@Nullable String id, C bc) {
        return senseCamera(id != null ? $$(id) : null, bc);
    }
//    /**
//     * pixelTruth defaults to linear monochrome brightness -> frequency
//     */
//    default Bitmap2DSensor senseCamera(String id, java.awt.Container w, int pw, int ph) {
//        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
//    }
//
//    default Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Component w, int pw, int ph) {
//        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
//    }
//
//    default Bitmap2DSensor<WaveletBag> senseCameraFreq(String id, Supplier<BufferedImage> w, int pw, int ph) {
//        return senseCamera(id, new WaveletBag(w, pw, ph));
//    }

}
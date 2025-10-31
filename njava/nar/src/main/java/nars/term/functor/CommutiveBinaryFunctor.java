package nars.term.functor;

public abstract class CommutiveBinaryFunctor extends BinaryFunctor {


    CommutiveBinaryFunctor(String name) {
        super(name);
    }


    //    @Override
//    protected Term apply2(Evaluation e, Term x, Term y) {
//
//        return super.apply2(e, x, y);
//    }
//
//    @Override
//    protected final Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy) {
//        return computeXfromYandXY(e, y, x, xy);
//    }

//    protected static Term theCommutive(Functor func, Term x, Term y) {
//        if (x.compareTo(y) > 0) {
//            Term z = x;
//            x = y;
//            y = z;
//        }
//
//        return $.func(LightHeapTermBuilder.the, func, x, y);
//    }

}
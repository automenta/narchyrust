package nars.func;

import nars.$;
import nars.Op;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.functor.InlineBinaryFunctor;
import nars.term.functor.InlineCommutiveBinaryFunctor;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.*;

public enum MathFunc {
    ;

    public static Term add(Term... x) {
        return x.length == 2 ? add(x[0], x[1]) : add.theCommutive(x);
    }

    public static Term add(Term x, Term y) {
        return x instanceof Int xi && y instanceof Int yi ?
            Int.i(Int.i(xi) + Int.i(yi)) :
            add.theCommutive(x, y);
    }

    public static Term mul(Term x, Term y) {
        return x instanceof Int xi && y instanceof Int yi ?
            Int.i(Int.i(xi) * Int.i(yi)) :
            mul.theCommutive(x, y);
    }

    public static Term mul(Term x, int y) {
        if (y == 1) return x;

        if (x instanceof Int xi)
            return Int.i(Int.i(xi) * y);

        return mul.theCommutive(x, Int.i(y));
    }

    public static final ArithmeticCommutiveFunctor add = new ArithmeticCommutiveFunctor("add") {

        @Override
        public Term equality(Evaluation e, Compound x, Term y) {
            Subterms xa = args(x);
            if (xa.subs() == 2) {
                Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                if (xa0 instanceof Variable && y instanceof Int && xa1 instanceof Int)
                    return e.is(xa0, Int.i(Int.i(y) - Int.i(xa1))) ? True : False; //"equal(add(#x,a),y)"

                if (xa1 instanceof Variable && y.equals(xa0))
                    return e.is(xa1, Int.ZERO) ? True : False; //equal(add(#x,#y),#x) |- is(#y, 0)
                if (xa0 instanceof Variable && y.equals(xa1))
                    return e.is(xa0, Int.ZERO) ? True : False;  //equal(add(#y,#x),#x) |- is(#y, 0) //can this happen?
            } //TODO 3-ary

            //includes: (#x,add(#x,#x)) |- is(#x, 0)
            return null;
        }

    };
    public static final ArithmeticCommutiveFunctor mul = new ArithmeticCommutiveFunctor("mul") {


        @Override
        public @Nullable Term equality(Evaluation e, Compound x, Term y) {
            if (y instanceof Int yi) {
                Subterms xa = args(x, 2);
                if (xa != null) {
                    Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                    if (xa0 instanceof Variable xa0v && xa1 instanceof Int xa1i)
                        return division(e, yi, xa0v, xa1i);
                    if (xa1 instanceof Variable xa1v && xa0 instanceof Int xa0i)
                        return division(e, yi, xa1v, xa0i);
                }
                //TODO 3-ary
            } else if (y instanceof Variable) {
                Subterms xa = args(x, 2);
                if (xa != null) {
                    Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                    if (xa0 instanceof Variable && xa1 instanceof Variable) {
                        if (y.equals(xa0))
                            return e.is(xa1, Int.ONE) ? True : False; //equal(#y,mul(#x,#y)) |- is(#x, 1)
                        if (y.equals(xa1))
                            return e.is(xa0, Int.ONE) ? True : False; //equal(#y,mul(#y,#x)) |- is(#x, 1)
                    }
                }
                //TODO 3-ary
            }
            return null;

        }

        private static Term division(Evaluation e, Int y, Variable x, Int a) {
            int numerator = Int.i(y);
            int denominator = Int.i(a);
            Term z;
            if (numerator == denominator)
                z = numerator == 0 ? Op.NaN : Int.ONE;
            else if (denominator == 0)
                z = numerator > 0 ? Op.InfinityPos : Op.InfinityNeg;
            else
                z = $.the(((double) numerator) / denominator);

            return e.is(x, z) ? True : False; //"equal(mul(#x,a),y)"
        }


    };
    public static final Functor pow = new InlineBinaryFunctor("pow") {

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            if (x instanceof Int xi && y instanceof Int yi)
                return Int.i((int) Math.pow(Int.i(xi), Int.i(yi)));
            else
                return null;
        }

    };


    /**
     * TODO define better, ex: xor({a,b})?
     * TODO abstract CommutiveBooleanBidiFunctor
     *
     */
    public static final InlineCommutiveBinaryFunctor xor = new InlineCommutiveBinaryFunctor("xor") {

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            if (x instanceof Bool && y instanceof Bool && x != Null && y != Null) {
                return x == y ? False : True;
            }
            return null;
        }
    };

    public static void addTo(Subterms x, Term[] y) {
        int s = y.length;
        for (int i = 0; i < s; i++)
            y[i] = add(x.sub(i), y[i]);
    }

    public static final ArithmeticCommutiveFunctor max = new ArithmeticCommutiveFunctor("max") {
        @Override
        public Term equality(Evaluation e, Compound x, Term y) {
            Subterms xa = args(x);
            if (xa.subs() == 2) {
                Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                if (xa0 instanceof Int && xa1 instanceof Int) {
                    int a = Int.i(xa0), b = Int.i(xa1);
                    return e.is(y, Int.i(Math.max(a, b))) ? True : False;
                }
            }
            return null;
        }
    };

    public static final ArithmeticCommutiveFunctor min = new ArithmeticCommutiveFunctor("min") {
        @Override
        public Term equality(Evaluation e, Compound x, Term y) {
            Subterms xa = args(x);
            if (xa.subs() == 2) {
                Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                if (xa0 instanceof Int && xa1 instanceof Int) {
                    int a = Int.i(xa0), b = Int.i(xa1);
                    return e.is(y, Int.i(Math.min(a, b))) ? True : False;
                }
            }
            return null;
        }
    };
//    public static final InlineBinaryFunctor gte = new InlineBinaryFunctor("gte") {
//        @Override
//        protected Term compute(Evaluation e, Term x, Term y) {
//            if (x instanceof Int xi && y instanceof Int yi)
//                return $.the(Int.the(xi) >= Int.the(yi));
//            return null;
//        }
//    };
//
//    public static final InlineBinaryFunctor lte = new InlineBinaryFunctor("lte") {
//        @Override
//        protected Term compute(Evaluation e, Term x, Term y) {
//            if (x instanceof Int xi && y instanceof Int yi)
//                return $.the(Int.the(xi) <= Int.the(yi));
//            return null;
//        }
//    };
//
//    public static final InlineBinaryFunctor gt = new InlineBinaryFunctor("gt") {
//        @Override
//        protected Term compute(Evaluation e, Term x, Term y) {
//            if (x instanceof Int xi && y instanceof Int yi)
//                return $.the(Int.the(xi) > Int.the(yi));
//            return null;
//        }
//    };
//
//    public static final InlineBinaryFunctor lt = new InlineBinaryFunctor("lt") {
//        @Override
//        protected Term compute(Evaluation e, Term x, Term y) {
//            if (x instanceof Int xi && y instanceof Int yi)
//                return $.the(Int.the(xi) < Int.the(yi));
//            return null;
//        }
//    };

    public static final InlineBinaryFunctor mod = new InlineBinaryFunctor("mod") {
        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            if (x instanceof Int xi && y instanceof Int yi) {
                int yValue = Int.i(yi);
                if (yValue != 0) {
                    return Int.i(Int.i(xi) % yValue);
                }
            }
            return null;
        }
    };

    public static final InlineBinaryFunctor div = new InlineBinaryFunctor("div") {
        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            if (x instanceof Int xi && y instanceof Int yi) {
                int yValue = Int.i(yi);
                if (yValue != 0) {
                    return Int.i(Int.i(xi) / yValue);
                }
            }
            return null;
        }
    };

}
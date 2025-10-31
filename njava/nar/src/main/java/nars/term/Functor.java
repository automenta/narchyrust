package nars.term;

import jcog.The;
import jcog.func.TriFunction;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.functor.AbstractInlineFunctor;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.functor.LambdaFunctor;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;

/**
 * a functor is a target transform which immediately returns
 * a result Term from the TermContainer arguments of
 * a function target, for example: f(x) or f(x, y).
 */
public abstract class Functor extends Atomic implements BiFunction<Evaluation, Subterms, Term>, The {

	protected Functor(String name) {
		this(new Atom(name));
	}

	protected Functor(Atom name) {
		super(name.bytes());
	}

	@Deprecated
	public static Subterms args(Term x) {
		return _args((Subterms) x);
	}

	@Deprecated
	public static Term[] argsArray(Term x) {
		return args(x).arrayShared();
	}

	/**
	 * returns the arguments of an operation (task or target)
	 */
	public static Subterms _args(Subterms x) {
		//assert (x.opID() == INH.id && x.subIs(1, ATOM));
		return x.sub(0).subtermsDirect();
	}

	public static @Nullable Subterms args(Compound x, int requireArity) {
        var s = _args(x);
		return s.subs() == requireArity ? s : null;
	}

	public static Atomic func(Term x) {
		return isFunc(x) ? _func(x) : null;
	}

	public static Atomic _func(Term x) {
		return (Atomic) x.sub(1);
	}

	private static boolean hasFunc(Subterms x) {
		return x.ORrecurse(z -> z.hasAll(FuncBits), (z,s)->isFunc(z), null);
	}

	public static boolean isFunc(Term x) {
		if (x.INH()) {
			 //if (x.hasAll(FuncBits)) {
            	var xx = x.subtermsDirect();
				 return xx.subIs(0, PROD) && xx.subIs(1, ATOM);
			 //}
		}
		return false;
	}

	/**
	 * creates a new functor from a target name and a lambda
	 */
	public static LambdaFunctor f(String termAtom, Function<Subterms, Term> f) {
		return f(atom(termAtom), f);
	}

	/**
	 * creates a new functor from a target name and a lambda
	 */
	public static LambdaFunctor f(Atom termAtom, Function<Subterms, Term> f) {
		return new LambdaFunctor(termAtom, f);
	}

	public static LambdaFunctor f(String termAtom, int arityRequired, Function<Subterms, Term> ff) {
		return f(atom(termAtom), arityRequired, ff);
	}

	private static LambdaFunctor f(Atom termAtom, int arityRequired, Function<Subterms, Term> ff) {
		return f(termAtom, tt ->
			(tt.subs() == arityRequired) ? ff.apply(tt) : Null
		);
	}

	private static LambdaFunctor f(Atom termAtom, int minArity, int maxArity, Function<Subterms, Term> ff) {
		return f(termAtom, (tt) -> {
            var n = tt.subs();
			return ((n >= minArity) && (n <= maxArity)) ? ff.apply(tt) : Null;
		});
	}

	/**
	 * zero argument (void) functor (convenience method)
	 */
	private static LambdaFunctor f0(Atom termAtom, Supplier<Term> ff) {
		return f(termAtom, 0, (tt) -> ff.get());
	}

	public static LambdaFunctor f0(String termAtom, Supplier<Term> ff) {
		return f0(atom(termAtom), ff);
	}

//	public static LambdaFunctor r0(String termAtom, Supplier<Runnable> ff) {
//		Atom fName = Atomic.atom(termAtom);
//		return f0(fName, () -> new AbstractPred<>($.inst($.quote(Util.uuid64()), fName)) {
//
//			@Override
//			public boolean test(Object o) {
//				try {
//					Runnable r = ff.get();
//					r.run();
//					return true;
//				} catch (RuntimeException t) {
//					t.printStackTrace();
//					return false;
//				}
//			}
//		});
//	}

	/**
	 * one argument functor (convenience method)
	 */
	public static LambdaFunctor f1(Atom termAtom, UnaryOperator<Term> ff) {
		return f(termAtom, 1, (tt) -> ff.apply(tt.sub(0)));
	}

	public static LambdaFunctor f1(String termAtom, UnaryOperator<Term> ff) {
		return f1(atom(termAtom), /*safeFunctor*/(ff));
	}

	public static AbstractInlineFunctor f1Inline(String termAtom, UnaryOperator<Term> ff) {
		return new AbstractInlineFunctor1.MyAbstractInlineFunctor1Inline(termAtom, ff);
	}

	public static AbstractInlineFunctor f2Inline(String termAtom, BiFunction<Term, Term, Term> ff) {
		return new AbstractInlineFunctor2.MyAbstractInlineFunctor2Inline(termAtom, ff);
	}

	public static LambdaFunctor f1Const(String termAtom, Function<Term, Term> ff) {
		return f1(atom(termAtom), x ->
			((x == null) || x.hasVars()) ? null : ff.apply(x)
		);
	}

	/**
	 * two argument functor (convenience method)
	 */
	private static LambdaFunctor f2(Atom termAtom, BiFunction<Term, Term, Term> ff) {
		return f(termAtom, 2, tt -> ff.apply(tt.sub(0), tt.sub(1)));
	}

	/**
	 * two argument functor (convenience method)
	 */
	public static LambdaFunctor f2(String termAtom, BiFunction<Term, Term, Term> ff) {
		return f2(atom(termAtom), ff);
	}

	public static LambdaFunctor f3(String termAtom, TriFunction<Term, Term, Term, Term> ff) {
		return f3(atom(termAtom), ff);
	}

	/**
	 * three argument functor (convenience method)
	 */
	public static LambdaFunctor f3(Atom termAtom, TriFunction<Term, Term, Term, Term> ff) {
		return f(termAtom, 3, (tt) -> ff.apply(tt.sub(0), tt.sub(1), tt.sub(2)));
	}


//    private static UnaryOperator<Term> safeFunctor(UnaryOperator<Term> ff) {
//        return x -> x == null ? null : ff.apply(x);
//    }

	public static LambdaFunctor f2Or3(String termAtom, Function<Subterms, Term> ff) {
		return f(atom(termAtom), 2, 3, ff);
	}

    public static boolean evalable(Termlike x) {
        return x instanceof Subterms &&
				(x.hasAny(EQ) || hasFunc((Subterms)x));
				//x.hasAll(FuncBits);
    }

	public static Term argSub(Term x, int i) {
		return args(x).sub(i);
	}


    //	public static Term call(Evaluation e, Term a) {
//		return call(e, a, null);
//	}
//	public static Term call(Evaluation e, Term a, @Nullable Map<Term,Term> argTransform) {
//
//		Term x = a.sub(0);
//		if (a.EQ()) {
//			Term y = a.sub(1);
//			if (argTransform!=null) {
//				Term xx, yy;
//				xx = x.replace(argTransform);
//				yy = y.replace(argTransform);
//				if (x!=xx || y!=yy)
//					Util.nop();
//				x = xx; y = yy;
//			}
//			return Equal.compute(e, x, y);
//		} else {
//			Subterms A = x.subtermsDirect();
//			if (argTransform!=null) {
//				@Nullable Subterms B = transformSubs(A, s -> s.replace(argTransform));
//				if (B == null)
//					return Null;
//				if (B!=A)
//					Util.nop();//TEMPORARY
//				A = B;
//			}
//			return ((Functor) a.sub(1) /*Functor.func(a)*/).apply(e, A);
//		}
//	}

	@Override
	public final byte opID() {
		return ATOM.id;
	}

}
package nars.func;

import nars.$;
import nars.Op;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Int;
import nars.term.functor.InverseFunctor;
import nars.term.util.transform.InlineFunctor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.Objects;

import static nars.Op.PROD;
import static nars.func.MathFunc.add;
import static nars.func.MathFunc.mul;
import static nars.term.atom.Bool.Null;

public abstract class ArithmeticCommutiveFunctor extends Functor implements InlineFunctor<Evaluation>, InverseFunctor {

    ArithmeticCommutiveFunctor(String name) {
        super(name);
    }

    @Nullable
    private static Term[] addTo(TermList vector, Compound v0) {
        var V = v0.subtermsDirect().arrayClone();
        var v0s = v0.subs();
        var vectorSize = vector.size();
        for (var i = 1; i < vectorSize; i++) {
            var x = vector.get(i);
            if (x.subs() != v0s)
                return null;

            MathFunc.addTo(x.subtermsDirect(), V);
        }
        return V;
    }

    private static TermList prodSeparate(Term[] V, int arity) {
        var n = V.length;
        var r = new Term[arity];
        for (var i = 0; i < arity; i++) {
            var c = new Term[n];
            for (var j = 0; j < n; j++)
                c[j] = argSub(V[j], i);
            r[i] = PROD.the(c);
        }
        Arrays.sort(r); // Consider optimizing sorting if performance is critical
        return new TermList(r);
    }

    /**
     * factor duplicates from add->mul or mul->pow
     */
    private static void factorDuplicates(TermList xx, boolean isAdd) {
        xx.sortThis();

        var start = 0;
        var last = xx.sub(0);
        for (var i = 1; i < xx.size(); i++) {
            var wi = xx.sub(i);
            if (!last.equals(wi)) {
                last = wi;
                start = i;
            } else {
                if (i > start) {

                    var factor = Int.i((i + 1) - start);

                    xx.setFast(start, isAdd ?
                        mul(last, factor) :
                        pow(last, factor));

                    // Remove duplicates
                    for (var r = i - start; r > 0; r--)
                        xx.remove(start + 1);

                    i = start + 1;
                }
            }
        }
    }

    private static Term pow(Term last, Int factor) {
        return $.func(MathFunc.pow, last, factor);
    }

    @Override
    @Nullable
    public Term apply(Evaluation terms, Subterms x) {
        return reducedCommutive(x);
    }

    @Nullable
    private Term reducedCommutive(Subterms x) {
        var xs = x.subs();
        if (xs == 0) return Null;
        if (xs == 1) return x.sub(0);

        var y = reduceCommutive(x);
        if (y!=null && y.equals(x))
            return null;

        var ys = y!=null ? y.subs() : 0;
        return switch (ys) {
            case 0 -> Null;
            case 1 -> y.sub(0);
            default -> ys == xs && x.equals(y) ? null : $.func(this, y);
        };
    }

    @Nullable
    Subterms reduceCommutive(Subterms s) {
        //fast prefilter
        int intCounts = 0, prodCounts = 0, fnCounts = 0;
        int n = s.subs();
        var isAdd = this == add;
        var isMul = this == mul;
        for (int i = 0; i < n; i++) {
            var si = s.sub(i);
            switch (si.op()) {
                case INT -> {
                    intCounts++;
                    if (isAdd || isMul) {
                        //force evaluation for constant reductions in case only 1 int
                        int in = Int.i(si);
                        if ((isAdd && in == 0) || (isMul && (in == 1 || in == 0)))
                            fnCounts++;
                    }
                }
                case PROD -> { int sis = si.struct(); if (Op.hasAny(sis, Op.INT.bit)) prodCounts++; if (Op.hasAny(sis, Op.INH.bit)) fnCounts++; }
                case INH -> fnCounts++;
            }
        }
        if (reduceFast(intCounts, prodCounts, fnCounts))
            return s.sorted(); //just need to ensure sorted

        //TODO if prodCounts == 0 && fnCounts == 0: simple case


        // Assertion to ensure that the function is only called for addition or multiplication.
        if (!(isAdd || isMul))
            throw new UnsupportedOperationException("This function only supports addition or multiplication.");

        // Flattening and scalar accumulation
        TermList vector = null;
        var scalar = isAdd ? 0 : 1;
        var hasScalar = false;
        var xx = s.toList();
        var l = xx.listIterator();
        while (l.hasNext()) {
            var x = l.next();
            if (fnCounts > 0 && x instanceof Compound && Objects.equals(func(x), this)) {
                flatten(l, x);
            } else if (x instanceof Int xi) {
                // Accumulate scalar values
                var X = Int.i(xi);
                if (isAdd)
                    scalar += X;
                else
                    scalar *= X;
                hasScalar = true;
                l.remove();
            } else if (x.PROD()) {
                // Handle product terms for vector operations
                if (isAdd && hasScalar) //TODO except if scalar==0
                    return null;
                if (vector != null && vector.getFirst().subs() != x.subs())
                    return null; // Shape mismatch

                (vector == null ? (vector = new TermList(1)) : vector).add(x);
                l.remove();
            }
        }

        // Vector operations
        if (vector != null) {
            if (isAdd) {
                if (hasScalar)
                    return null; // Shape incompatible when adding a scalar to a vector

                var V = addTo(vector, (Compound) vector.getFirst());
                if (V == null)
                    return null; // Shape mismatch

                // Check if the vector can be separated into multiple additions
                var separate = true;
                var arity = -1;
                for (var v : V) {
                    if (!(v instanceof Compound)) {
                        separate = false;
                        break;
                    }
                    var vf = func(v);
                    if (vf == null || !vf.equals(add)) {
                        separate = false;
                        break;
                    }
                    var a = args(v).subs();
                    if (arity == -1) arity = a;
                    else if (a != arity) {
                        separate = false;
                        break;
                    }
                }

                if (separate)
                    return prodSeparate(V, arity);

                xx.add(PROD.the(V));

            } else if (isMul) {
                // For multiplication, only a single vector is allowed (dot product)
                if (vector.size() != 1)
                    return null; // Shape mismatch

                xx.add(vector.getFirst());
            }
        }

        // Add the accumulated scalar if it's not the identity element
        if (hasScalar && ((isAdd && scalar != 0) || (isMul && scalar != 1)))
            xx.add(Int.i(scalar));

        // Factor out duplicates (e.g., x + x => 2 * x)
        if (xx.size() > 1)
            factorDuplicates(xx, isAdd);

        return xx;
    }

    private boolean reduceFast(int intCounts, int prodCounts, int fnCounts) {
        return (intCounts + prodCounts) <= 1 && fnCounts == 0; //allow a mix of scalar and vectors
    }

    /** Flatten nested functors of the same type */
    private static void flatten(ListIterator<Term> l, Term x) {
        l.remove();
        args(x).forEach(l::add);
    }

    @Override
    public final Term applyInline(Subterms args) {
        return apply(null, args);
    }

    public final Term theCommutive(Term... x) {
        var y = reducedCommutive(new TermList(x));
        return y == null ? $.func(this, x) : y;
    }

}
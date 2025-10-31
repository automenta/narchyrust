package nars.term.util;

import jcog.data.list.Lst;
import jcog.util.ArrayUtil;
import nars.$;
import nars.NAL;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termlike;
import nars.term.atom.Bool;
import nars.term.atom.Img;
import nars.term.builder.TermBuilder;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static nars.Op.*;
import static nars.Term.nullIfNull;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Terms.rCom;

/**
 * utilities for transforming image compound terms
 */
public enum Image {
    ;

    public static final int ImageBits = PROD.bit | IMG.bit | INH.bit;

    public static Term imageInt(Term t, Term x) {
        return image(true, t, x);
    }

    public static Term imageExt(Term t, Term x) {
        return image(false, t, x);
    }

    private static Term image(boolean intOrExt, Term t, Term x) {
        return t.INH() ? nullIfNull(imageSubterms(intOrExt, t.subtermsDirect(), x.unneg())) : Null;
    }

    @Nullable
    private static Compound imageSubterms(boolean intOrExt, Subterms tt, Term x) {
        if (x instanceof Img || x instanceof Neg)
            throw new TermTransformException("invalid image transform", tt, x);

        int prodSub = intOrExt ? 1 : 0;

        Term prod = tt.sub(prodSub);
        if (!prod.PROD())
            return null;

        Subterms pp = prod.subtermsDirect();
        int n = pp.subs();
        if (n < NAL.term.imageTransformSubMin ||
            pp.containsInstancePN(intOrExt ? ImgExt : ImgInt))
            return null;


        Predicate<Term> matcher = x.unneg().equalsPN();
        int index = pp.indexOf(matcher);
        if (index == -1) return null;

        //boolean neg = (pp.sub(index) instanceof Neg);

        Term[] qq = new Term[n + 1];
        qq[0] = tt.sub(1 - prodSub);
        for (int i = 0; i < n; i++) {
            Term ppi = pp.sub(i);

            Term y;
            if (i == index) {
                y = ppi instanceof Neg ?
                    (intOrExt ? ImgIntNeg : ImgExtNeg) :
                    (intOrExt ? ImgInt : ImgExt);
                if (i < n - 1)
                    index = pp.indexOf(matcher, index); //find next occurrence
            } else
                y = ppi;

            qq[i + 1] = y;
        }

        return imageNew(intOrExt, x, qq);
    }


    @Nullable private static Compound imageNew(boolean intOrExt, Term x, Term[] qq) {
        Term q = PROD.the(qq);
        Term y = INH.the(intOrExt ?
                new Term[]{q, x} :
                new Term[]{x, q}
        );
        return y.unneg().INH() ? ((Compound) y) : null;
        //return ((Compound) y);
    }

    @Deprecated
    public static Term imageNormalize(Term _x) {
        return imageNormalize(_x, terms);
    }

    public static boolean imageNormalized(Term x) {
        x = x.unneg();
        return !(x instanceof Compound) || x.equals(imageNormalize(x));
    }

    @Deprecated
    public static Term _imageNormalize(Compound x) {
        return _imageNormalize(x, terms);
    }


    public static Term imageNormalize(Term x, TermBuilder B) {
        return x instanceof Compound xc ? imageNormalize(xc, B) : x;
    }

    public static Term imageNormalize(Compound _x, TermBuilder B) {

        boolean neg = _x instanceof Neg;
        Term x = neg ? _x.unneg() : _x;

        if (!x.INH() || !x.hasAll(ImageBits))
            return _x;

        Term y = _imageNormalize((Compound) x, B);
        return x == y ? _x : y.negIf(neg);
    }

    private static Term _imageNormalize(Compound x, TermBuilder B) {
        return (Term) normalize(x, true, false, B);
    }


//    public static boolean imageSubtermNormalizable(Term x) {
//        return !(x instanceof Compound)
//               ||
//               (!((Compound)x).NORMALIZED()
//                        ||
//                        (x.INH() && x.hasAll(ImageBits) &&
//                                normalize((Compound)x, false, false) == null));
//    }

//
//    @Deprecated public static Term normalize(Compound x, boolean actuallyNormalize, boolean onlyRecursionTest) {
//        return (Term)normalize(x, actuallyNormalize, onlyRecursionTest, Op.terms);
//    }

    /**
     * assumes that input is INH op has been tested for all image bits
     */
    private static @Nullable Termlike normalize(Subterms xx, boolean transform, boolean testOnly, TermBuilder B) {

        Term s = xx.sub(0);
        Subterms ss = null;
        boolean isInt = s.PROD() && (ss = s.subtermsDirect()).containsInstancePN(ImgInt) /* && !ss.containsInstance(Op.ImgExt)*/;

        Term p = xx.sub(1);
        Subterms pp = null;
        boolean isExt = p.PROD() && (pp = p.subtermsDirect()).containsInstancePN(ImgExt) /*&& !pp.containsInstance(Op.ImgInt)*/;

        if (isInt == isExt)
            return xx; //both or neither

        if (!transform && !testOnly)
            return null;

        Term subj, pred;
        if (isInt) {

            subj = ss.sub(0);
            if (subj instanceof Img)
                return Null; //produces invalid result (img in subj or pred of inh)

            Term[] predArgs = ss.subRangeReplace(1, ImgInt, p);
            if (testOnly)
                return _normalizeValid(subj, predArgs);

            pred = PROD.build(B, predArgs);

        } else { //isExt

            pred = pp.sub(0);
            if (pred instanceof Img)
                return Null; //produces invalid result (img in subj or pred of inh)

            Term[] subjArgs = pp.subRangeReplace(1, ImgExt, s);
            if (testOnly)
                return _normalizeValid(pred, subjArgs);

            subj = PROD.build(B, subjArgs);

        }

        Term sp = INH.build(B, subj, pred);

        return sp instanceof Compound spc ? imageNormalize(spc, B) : Null; //recurse
    }

    private static Term _normalizeValid(Term x, Term[] y) {
        return normalizeValid(x, y) ? null : Null;
    }

    private static boolean normalizeValid(Term x, Term[] y) {
        if (x.PROD())
            return x.subs() != y.length || !((Compound) x).equalTerms(y);
        else
            return !rCom(x, $.pFast(y));
    }


    public static boolean isRecursive(Term subject, Term predicate) {
        if (!(subject.hasAny(IMG) || predicate.hasAny(IMG)))
            return false;

        return normalize($.vFast(subject, predicate), true, true,
                terms
                //Op.__terms
        ) instanceof Bool;
    }

    @Nullable
    public static Compound alignTo(Compound target, Compound imageable, RandomGenerator rng) {
        if (!target.INH() || !imageable.INH() || !imageable.OR(Term::PROD) || target.equals(imageable))
            return null;

        Subterms ii = imageable.subtermsDirect();
        Term s = target.sub(0);
        if (ii.contains(s)) return null; //already shares a subterm in common
        Term p = target.sub(1);
        if (ii.contains(p)) return null; //already shares a subterm in common

        Lst<Term> y = new Lst<>(4);
        //TODO more optimized
        Predicate<Term> inh = Term::INH;
        y.addIf(imageInt(imageable, s), inh);
        y.addIf(imageExt(imageable, s), inh);
        y.addIf(imageInt(imageable, p), inh);
        y.addIf(imageExt(imageable, p), inh);
        return (Compound) y.get(rng);
    }


    @Nullable
    public static Compound[] align(Compound X, Compound Y, RandomGenerator rng, @Deprecated boolean novelRequired) {
        if (!alignable(X, Y)) return null;

        Subterms XX = X.subterms(), YY = Y.subterms();
        Term XX0 = XX.sub(0), XX1 = XX.sub(1);
        boolean xp0 = XX0.PROD(), xp1 = XX1.PROD();
        Term YY0 = YY.sub(0), YY1 = YY.sub(1);
        boolean yp0 = YY0.PROD(), yp1 = YY1.PROD();

        //already aligned somehow?
        if (novelRequired) {
            if (xp0 == yp0 && XX0.equals(YY0))
                return null;
            if (xp0 == yp1 && XX0.equals(YY1))
                return null;
            if (xp1 == yp0 && XX1.equals(YY0))
                return null;
            if (xp1 == yp1 && XX1.equals(YY1))
                return null;
        }

        /* code: -1=int, 0=subj/pred, +1=ext */

        int xa, xb, xd;
        int ya, yb, yd;

        if (!xp0 && !xp1) {

            if (!yp0 && !yp1)
                return null; //none

            xa = xb = 0;
            xd = 1;
        } else if (xp0 && xp1) {
            if (rng.nextBoolean()) {
                xd = +2;
                xa = -1;
                xb = +1;
            } else {
                xd = -1;
                xa = +1;
                xb = -1;
            }
        } else if (xp0) {
            xd = 1;
            xa = -1;
            xb = -1;
        } else /*if (xp1)*/ {
            xd = 1;
            xa = +1;
            xb = +1;
        }

        if (!yp0 && !yp1) {
            ya = yb = 0;
            yd = 1;
        } else if (yp0 && yp1) {
            if (rng.nextBoolean()) {
                yd = +2;
                ya = -1;
                yb = +1;
            } else {
                yd = -2;
                ya = +1;
                yb = -1;
            }
        } else if (yp0) {
            yd = 1;
            ya = -1;
            yb = -1;
        } else /* if (yp1) */ {
            yd = 1;
            ya = +1;
            yb = +1;
        }


        for (int xp = xa; xd > 0 ? xp <= xb : xp >= xb; xp += xd) {

            Subterms xs = xp == 0 ? X : (Subterms) XX.sub(xp == -1 ? 0 : 1);

            for (int yp = ya; yd > 0 ? yp <= yb : yp >= yb; yp += yd) {

                Subterms ys = yp == 0 ? Y : (Subterms) YY.sub(yp == -1 ? 0 : 1);

                var common = Terms.intersect(xs, ys, true,
                    x -> !(x instanceof Variable) && !(x instanceof Img)
                );
                if (!common.isEmpty()) {

                    Term[] XY = common.toArray(EmptyTermArray);
                    int XYn = XY.length;
                    if (XYn > 1)
                        ArrayUtil.shuffle(XY, rng); //TODO selection here can be prioritized heuristically
                    for (Term term : XY) {
                        Compound[] z = tryAlign(X, XX, xp, Y, YY, yp, term);
                        if (z != null)
                            return z;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static Compound[] tryAlign(Compound X, Subterms XX, int xp, Compound Y, Subterms YY, int yp, Term xy) {
        xy = xy.unneg();

        Compound xx = xp == 0 ? X : imageSubterms(xp == +1, XX, xy);
        if (xx != null) {
            Compound yy = yp == 0 ? Y : imageSubterms(yp == +1, YY, xy);
            if (yy != null)
                return new Compound[]{xx, yy};
        }
        return null;
    }

    /**
     * TODO requiring common subterms
     */
    public static boolean alignable(Term X, Term Y) {
        return X.INH() && Y.INH() &&
               !X.equals(Y) &&
               (prodSurface(X) || prodSurface(Y));
    }

    private static boolean prodSurface(Term X) {
        return (X.structSurface() & PROD.bit) != 0;
    }

}
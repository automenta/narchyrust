package nars.term.util.var;

import jcog.data.list.Lst;
import jcog.data.map.ObjIntHashMap;
import jcog.decide.Roulette;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Img;
import nars.term.util.Terms;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

import static nars.Op.EmptyTermArray;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    public static final DepIndepVarIntroduction the = new DepIndepVarIntroduction();

    /**
     * sum by complexity if passes include filter
     */
    public static final ToIntFunction<Term> depIndepFilter = t ->
        (t instanceof Variable) ? 0 : 1;
        //(t instanceof Variable) ? 0 : (t.hasAny(Op.VAR_INDEP.bit) ? 0 : 1);

    public static final ToIntFunction<Term> nonNegdepIndepFilter = t ->
        (t instanceof Variable || t instanceof Neg || t instanceof Img) ? 0 : 1;

//    private static final int MEMOIZE_CAPACITY = 32 * 1024;
//    private static final Function<Subterms, Term[]> select = Memoizers.the.memoizeByte(
//            DepIndepVarIntroduction.class.getSimpleName() + "_select",
//            Intermed.SubtermsKey::new,
//            DepIndepVarIntroduction::_select, MEMOIZE_CAPACITY);

    private DepIndepVarIntroduction() {

    }

    /**
     * if no variables are present in the target target, use the normalized variable which can help ensure avoidance of a need for full compound normalization
     */
//    private static final Variable UnnormalizedVarIndep = $.varIndep("_v");
//    private static final Variable UnnormalizedVarDep = $.varDep("_v");
//    private static final Variable FirstNormalizedVarIndep = $.varIndep(1);
//    private static final Variable FirstNormalizedVarDep = $.varDep(1);
    private static boolean validDepVarSuperterm(Op o) {
        //return /*o.statement ||*/ o == CONJ;
        return true;
    }

    private static boolean validIndepVarSuperterm(Op o) {
        return o.statement;
    }

//    @Override
//    public @Nullable Term apply(Compound x, Random rng, @Nullable Map<Term, Term> retransform) {
//        return x.hasAny(ConjOrStatementBits) ? super.apply(x, rng, retransform) : null;
//    }

    /**
     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
     * when there is a chocie, it prefers least aggressive introduction. and then random choice if
     * multiple equals are introducible
     *
     * @param superterm filter applies to the immediate superterm of a potential subterm
     */
    public static Term[] subtermRepeats(Subterms c, ToIntFunction<Term> countIf, int minCount) {
        ObjIntHashMap<Term> oi = Terms.subtermScore(c, countIf, minCount);
        return oi == null ? EmptyTermArray : keysArray(oi);
    }

    private static Term[] keysArray(ObjIntHashMap<Term> x) {
        //return oi.keysView().toArray(Op.EmptyTermArray);
        var e = new KeyExtractor(x);
        x.forEachKey(e);
        return e.y;
    }

    private static Term[] _select(Subterms x) {
        return subtermRepeats(x,
                NAL.term.VAR_INTRODUCTION_NEG_FILTER ?
                        nonNegdepIndepFilter : depIndepFilter,
                2);
    }

    @Override
    protected Term[] options(Subterms input) {
        //TODO drill-down to specific subterm containing duplicates
        return _select(input.sorted());
//        return j.volume() <= InterningTermBuilder.volMaxDefault ?
//                select.apply(j) : _select(j);
    }

    @Override
    public Term choose(Term x, Term[] y, RandomGenerator rng) {
        IntToFloatFunction curve =
                //n -> 1f / Util.cube(x[n].volume());
                //n -> 1f / Util.sqr(x[n].volume());
                //n -> (float)Math.pow(x[n].volume() + x[n].vars(), -1);
                n -> {
                    Term yy = y[n];
                    float yv = yy.complexity();
                    return //(float) Math.pow(
                            1 / (
                                    yv +
                                            (2 * yy.vars() / yv)   //penalty for erasing existing variables
                            );
                    //, -1.5);
                };
        //n -> (float) (1 / Math.sqrt(x[n].volume()))


        return y[Roulette.selectRouletteCached(y.length, curve, rng)];
    }

    @Override
    protected @Nullable Term introduce(Compound input, Term x) {

        Lst<byte[]> paths = input.pathsToList(x);

        int pSize = paths.size();
        if (pSize <= 1)
            return null;

//        if (pSize > 2) {
//            //TODO if paths > 2, consider removing some. easier for depvar's
//            //Util.nop();
//        }

        ObjectByteHashMap<Term> m = new ObjectByteHashMap<>(4);

        boolean depOrIndep = switch (input.commonParent(paths).op()) {
            case INH, SIM, IMPL -> false;
            default -> true;
        };

        for (byte[] p : paths) {
            Term t = null;
            int pathLengthMin1 = p.length - 1; /* dont include the selected target itself */
            for (int i = -1; i < pathLengthMin1; i++) {
                t = (i == -1) ? input : t.sub(p[i]);

                Op o = t.op();
                if (!depOrIndep && validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << p[i+1]);
                    m.updateValue(t, inside, prev -> (byte) (prev | inside));
                } else if (depOrIndep && validDepVarSuperterm(o))
                    m.addToValue(t, (byte) 1);
            }
        }

        return depOrIndep ?
                (m.anySatisfy(b -> b >= 2) ? $.varDep(input.vars() + 1) : null)
                :
                (m.anySatisfy(b -> b == 0b11) ? $.varIndep(input.vars() + 1) : null)
                ;

    }


    private static final class KeyExtractor implements Procedure<Term> {
        final Term[] y;

        int i;

        KeyExtractor(ObjIntHashMap<Term> x) {
            y = new Term[x.size()];
            i = 0;
        }

        @Override
        public void value(Term xx) {
            y[i++] = xx;
        }
    }
}
package nars.truth.dynamic;

import jcog.Is;
import jcog.WTF;
import jcog.util.ObjectLongLongPredicate;
import nars.Term;
import nars.Truth;
import nars.TruthFunctions;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.time.Tense;
import nars.truth.DynTaskify;

import static nars.NAL.dyn.DYN_IMPL_LIMITING;
import static nars.Op.*;

/*
Contraposition & Conversion

contraposition {
// Contraposition
//    a→b == ¬b→¬a		# contra positition [Lex contrapositionis]
// "If not smoking causes health, being not healthy may be the result of smoking"
// "If smoking causes not health, being healthy may be the result of not smoking"
//    contraposition truth fn inverts frequency, so (--B ==> --A) is wrong
    (--B ==> A), (  A ==> B), task("?"), --eqPN(A,B)  |- (--B ==>   A), (Belief:ContrapositionPP, Punctuation:Belief, Time:Belief)
    (  B ==> A), (  A ==> B), task("?"), --eqPN(A,B)  |- (  B ==>   A), (Belief:ContrapositionPN, Punctuation:Belief, Time:Belief)

// Conversion
//  If S can stand for P, P can to a certain low degree also represent the class S
//  If after S usually P happens, then it might be a good guess that usually before P happens, S happens.
conversion {
    (  P ==> S), (  S ==> P), task("?"), --eqPN(S,P) |- (  P ==>   S), (Belief:ConversionPP, Punctuation:Belief, Time:Belief)
    (--P ==> S), (  S ==> P), task("?"), --eqPN(S,P) |- (--P ==>   S), (Belief:ConversionPN, Punctuation:Belief, Time:Belief)

 */
@Is("Contraposition")
public class DynImplContra extends DynTruth {

    public static final DynImplContra DynImplConversion = new DynImplContra(true);
    public static final DynImplContra DynImplContraposition = new DynImplContra(false);

    private final boolean conOrContra;

    private DynImplContra(boolean conversionOrContraposition) {
        this.conOrContra = conversionOrContraposition;
    }

    @Override
    public int levelMax() {
        return DYN_IMPL_LIMITING ? 0 : super.levelMax();
    }

    public boolean validSubject(Term s) {
        return conOrContra || s instanceof Neg;
        // && !s.unneg().IMPL() /* prevent recursive impl */;
    }

    @Override
    public Truth truth(DynTaskify d) {
        Truth truth = d.getFirst().truth();
        return truth().truth(null, truth, d.eviMin());
        //return neg(NALTruth.Contraposition.apply(null, truth.neg(), (float) NAL.truth.CONF_MIN));
    }

    private TruthFunctions truth() {
        return conOrContra ? TruthFunctions.Conversion : TruthFunctions.Contraposition;
    }

    @Override
    public boolean decompose(Compound x, long start, long end, ObjectLongLongPredicate<Term> each) {
        Subterms xx = x.subterms();
        Term subj = xx.sub(0).negIf(!conOrContra); //assert(x.sub(0) instanceof Neg);
        Term pred = xx.sub(1);

        int sdt = x.dt(); if (sdt==DTERNAL) sdt = 0; //HACK
        int dt = (sdt == XTERNAL) ? XTERNAL :
            sdt + subj.unneg().seqDur() - pred.seqDur();

        Term y = IMPL.the(pred, dt, subj);
        if (!y.unneg().IMPL())
            return false; //failure to construct

        //int shift = ((dt == XTERNAL) ? 0 : dt) + sRange; //TODO test

        return each.accept(y, start /*- shift*/, end /*- shift*/);
//        throw new TODO();
    }


//    @Override public long[] occ(DynTaskify d) {
//        long[] se = super.occ(d);
//        if (se[0]!= Op.ETERNAL) {
//            Term x = d.get(0).term();
//            int dt = x.dt();
//            int shift = ((dt == Op.DTERNAL) ? 0 : dt) + ((Compound)x).seqDurSub(0);
//            if (shift!=0) {
//                se[0] += shift; se[1] += shift;
//            }
//        }
//        return se;
//    }

    @Override
    public Term recompose(Compound superterm, DynTaskify d) {
        Term x = d.getFirst().term();
        boolean xNeg = (x instanceof Neg);
        if (xNeg)
            x = x.unneg(); //TODO why can this happen?

        Term subj = x.sub(0), pred = x.sub(1);

        int sdt = x.dt();
        if (sdt == XTERNAL)
            throw new WTF();
        int dt =
            //(sdt == XTERNAL) ? XTERNAL :
            Tense.dither(-(sdt == DTERNAL ? 0 : sdt) - subj.unneg().seqDur() - pred.seqDur(), d.timeRes);

        return IMPL.the(pred.negIf(!conOrContra), dt, subj).negIf(xNeg);
    }

    @Override
    public int componentsEstimate() {
        return 1;
    }
}
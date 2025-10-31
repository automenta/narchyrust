//package nars.premise;
//
//import jcog.Util;
//import nars.Op;
//import nars.Deriver;
//import nars.NALTask;
//import nars.term.Term;
//import nars.term.Termed;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//
//public class SentencePremise extends AbstractPremise {
//
//    public final Term term;
//
//    /** warning: different from punc(), which refers to this premise itself, and will be COMMAND */
//    public final byte puncOut;
//
//    private final int hash;
//
//    public SentencePremise(Term term, byte puncOut, @Nullable Term why) {
//        super(why);
//
//        term = term.normalize();
//
//        this.term = term;
//        this.puncOut = puncOut;
//        this.hash = Util.hashCombine(term, puncOut);
//    }
//
//    @Override
//    public List<Termed> path() {
//        return List.of(term, Op.punc(puncOut));
//    }
//
//    @Override
//    public void pre(Deriver d) {
//        d.setPremiseCommand(this);
//    }
//
//    @Override
//    public String toString() {
//        return "$" + pri() + " " + from() + Character.toString(puncOut);
//    }
//
//    @Override
//    public final Term from() {
//        return term;
//    }
//
//
//    @Override
//    public final boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof SentencePremise)) return false;
//        SentencePremise that = (SentencePremise) o;
//        return hash == that.hash && puncOut == that.puncOut && term.equals(that.term);
//    }
//
//    @Override
//    public final int hashCode() {
//        return hash;
//    }
//
//    @Override
//    public final Term to() {
//        return from();
//    }
//
//    @Override
//    public final NALTask task() {
//        return null;
//    }
//
//}

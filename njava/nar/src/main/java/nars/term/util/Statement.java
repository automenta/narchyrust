package nars.term.util;

import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Neg;
import nars.term.atom.Bool;
import nars.term.atom.Img;
import nars.term.builder.TermBuilder;
import nars.term.util.conj.ConjList;
import nars.term.util.conj.ConjSeq;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.*;

//import nars.term.var.ellipsis.Ellipsis;

/**
 * STATEMENTS
 * inheritance -->
 * similarity  <->
 * implication ==>
 */
public enum Statement {
    ;

    public static Term statement(TermBuilder B, Op op, int dt, Term subj, Term pred) {
        if (subj == Null || pred == Null) return Null;

        if (subj instanceof Img || pred instanceof Img)
            throw new TermException("statement can not have image subterm", new TermList(subj, pred));

        if (op == IMPL) {
            if (subj == True) return pred;
            if (subj == False) return True; //according to tautological impl truth table
            //return Null; //return predicate.neg();

            if (pred == True) return True;
            if (pred == False) return False; //subject.neg();

            if (!NAL.IMPLICATION_CAN_CONTAIN_IMPLICATION && subj.hasAny(IMPL))
                return Null;

            //filter negated sequences in IMPL subject
            if (!NAL.temporal.IMPL_SUBJ_NEG_SEQ && subj instanceof Neg && subj.unneg().SEQ())
                return Null;

            if (dt == DTERNAL) dt = 0; //HACK temporarily use dt=0
        }


        boolean parallel = dt == DTERNAL || dt == 0;
        if (parallel) {
            if (op != IMPL && subj.equals(pred)) {
                /* (a --> a), (a <-> a) */
                return True;
            }

            if (subj instanceof Bool && pred instanceof Bool) {
                if (subj == True && pred == False) return False;
                if (subj == False && pred == True) return False;
            }
        }

        if (subj instanceof Bool || pred instanceof Bool)
            return Null;

        boolean negate = false;

        if (op == IMPL) {
            if (pred instanceof Neg) {
                pred = pred.unneg();
                negate = true;
            }

//          if (subj.unneg().DELTA() && subj.unneg().sub(0).IMPL())
//              return Null;

            if (pred.DELTA() && pred.sub(0).IMPL())
                return Null;

            if (pred.IMPL()) {

                //not when only inner xternal, since that transformation destroys temporal information

                Term inner = pred.sub(0);
                Term newPred = pred.sub(1);

                Term newSubj = ConjSeq.conjAppend(subj, dt, inner, B);
                if (newSubj == Null) return Null;

                int newDT = pred.dt();

                if (newPred instanceof Neg) {
                    newPred = newPred.unneg();
                    negate = !negate;
                }
                if (dt != newDT || !newSubj.equals(subj) || !newPred.equals(pred))
                    return statement(B, IMPL, newDT, newSubj, newPred).negIf(negate); //recurse
            }


            if (!pred.CONDABLE())
                return Null;

        } else if (op == INH || op == SIM) {
            if (subj instanceof Neg) {
//TODO                throw new TermException("Neg inh/sim component", subj);
                negate = true;
                subj = subj.unneg();
            }
            if (pred instanceof Neg) {
//TODO                throw new TermException("Neg inh/sim component", pred);
                negate = !negate;
                pred = pred.unneg();
            }
        }

        return statementMiddle(B, op, dt, subj, pred, negate);
    }

    private static Term statementMiddle(TermBuilder B, Op op, int dt, Term subj, Term pred, boolean negate) {
        //TODO simple case when no CONJ or IMPL are present

        if (op == IMPL && dt != XTERNAL && reducibleImpl(subj, pred)) {

            //subtract common subject components from predicate

            int subjRange = subj.unneg().seqDur();

            try (ConjList predEvents = ConjList.conds(pred, subjRange + dt, true, false).inhExploded(B)) {

                int removed = predEvents.removeAll(subj.unneg(), 0, !(subj instanceof Neg));
                if (removed == -1)
                    return False.negIf(negate);
                else if (removed == +1) {
                    Term predNew = predEvents.term(B);
                    if (predNew instanceof Bool)
                        return predNew.negIf(negate); //collapse

                    if (!pred.equals(predNew)) {
                        long shift = predEvents.shift();


                        dt = shift == ETERNAL ? 0 /*??*/ : Tense.occToDT(shift - subjRange);

                        if (predNew instanceof Neg) { //attempt to exit infinite loop of negations
                            predNew = predNew.unneg();
                            negate = !negate;
                        }

                        return statement(B, IMPL, dt, subj, predNew).negIf(negate); //recurse

                    }
                }
            }
        }

        return statementNew(op, dt, subj, pred, B).negIf(negate);
    }

    private static boolean reducibleImpl(Term subj, Term pred) {
        assert (!(pred instanceof Neg));
        Term su = subj.unneg(), pu = pred.unneg();
        return (reducibleConds(su) || reducibleConds(pu)) &&
                commonEventStructure(su, pu);
    }

    private static boolean reducibleConds(Term su) {
        return su.dt() != XTERNAL && su.CONDS();
    }

    /**
     * final step
     */
    private static Term statementNew(Op op, int dt, Term subject, Term predicate, TermBuilder B) {

        if (op == IMPL) {
            if (dt == 0) dt = DTERNAL; //HACK generalize to DTERNAL ==>
//            if (!subject.unneg().EVENTABLE())
//                return Null;
//            if (!predicate/*.unneg()*/.EVENTABLE())
//                return Null;
        } /*else {
//            if (!inhSimComponentValid(subject) || !inhSimComponentValid(predicate))
//                return Null;
        }*/

        if (dt == DTERNAL) {
            Term su = subject.unneg();
            if (op != IMPL || !su.CONJ() || ((su.dt() != XTERNAL && !su.SEQ()))) {
                Term pu = predicate.unneg();
                if (su.equals/*Root*/(pu))
                    return True.negIf(subject instanceof Neg != predicate instanceof Neg);
            }

            if (op != IMPL) {

                Term sp = cocontainment(subject, predicate);
                if (sp != null)
                    return sp;

                Term ps = cocontainment(predicate, subject);
                if (ps != null)
                    return ps;

                if (Terms.rCom(subject, predicate))
                    return Null; //HACK recursive

            }
        }

        if (op == SIM) {
            if (subject.compareTo(predicate) > 0) {
                //swap to natural order
                Term x = predicate;
                predicate = subject;
                subject = x;
            }
        }

        if (op == INH && Image.isRecursive(subject, predicate))
            return Null;

        return B.compoundNew(op, dt, subject, predicate);
    }

    @Nullable
    private static Term cocontainment(Term x, Term y) {
        if (x.CONJ()) {
            if (x.SEQ() || x.TEMPORAL_VAR()) return Null;
            if (x.complexity() > y.complexity()) {
                Subterms z = x.subtermsDirect();
                for (Term zz : z) {
                    switch (zz.equalsPolarity(y)) {
                        case +1 -> {return True;}
                        case -1 -> {return False;}
                    }
                }
//                if (z.contains(y)) return True;
//                if (z.containsNeg(y)) return False;
            }
        }

        return null;
    }

}
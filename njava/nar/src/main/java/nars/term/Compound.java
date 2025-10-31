/*
 * CompoundTerm.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http:
 */
package nars.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Hashed;
import jcog.WTF;
import jcog.data.list.Lst;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.io.TermIO;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.builder.TermBuilder;
import nars.term.compound.SeparateSubtermsCompound;
import nars.term.util.TermTransformException;
import nars.term.util.conj.Cond;
import nars.term.util.conj.CondMatcher;
import nars.term.util.conj.ConjBundle;
import nars.term.util.conj.ConjList;
import nars.term.var.Variable;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static jcog.Util.hashCombine;
import static nars.Op.*;
import static nars.io.TermIO.outNegByte;
import static nars.time.Tense.occToDT;

public abstract class Compound /*IPair,*/ extends Term implements Subterms {

    public final byte opID;
    /**
     * is this term is its own root
     */
    protected transient boolean root;
    /**
     * is this term is its own concept
     */
    private transient boolean concept;

    protected Compound(int opID) {
        this.opID = (byte) opID;
    }

    @Override public final boolean IMPL() {
        return opID == IMPL.id;
    }

    @Override public final boolean CONJ() { return opID == CONJ.id; }

    @Override public boolean PROD() {
        return opID == PROD.id;
    }

    @Override public boolean INH() {
        return opID == INH.id;
    }

    @Override public boolean SIM() {
        return opID == SIM.id;
    }

    @Override public boolean SAM() {
        return opID == DIFF.id;
    }

    @Override public boolean STATEMENT() {
        return isAny(Statements);
    }

    @Override public boolean COMMUTATIVE() {
        return isAny(Commutatives) && subs() > 1;
    }

    @Override public boolean EQ() { return opID == EQ.id; }

    @Override public boolean SETe() {
        return opID == SETe.id;
    }

    @Override public boolean SETi() {
        return opID == SETi.id;
    }

    @Override public boolean SET() {
        return isAny(Set);
    }

    @Override public boolean DELTA() {
        return opID == DELTA.id;
    }

    @Override public final boolean TEMPORAL() {
        return (structOp() & Temporals) != 0;
    }

    private static String toString(Compound c) {
        var sb = new StringBuilder(
                /* estimate */
                c.complexity() * 4
        );
        return c.appendTo(sb).toString();
    }

    private static int hash(int opID, int subtermsHash) {
        return hashCombine(subtermsHash, opID);
    }

    public static int hash1(int opID, Term onlySubterm) {
        return hash(opID, Subterms.hash(onlySubterm));
    }

    private static boolean decomposeConj(int dt, boolean decomposeParallel, boolean decomposeXternal) {
        return switch (dt) {
            case DTERNAL -> decomposeParallel;
            case XTERNAL -> decomposeXternal;
            default -> true;
        };
    }

    @Override
    public Term unneg() {
        return this instanceof Neg ? neg() : this;
    }

    /**
     * gets temporal relation value
     */
    @Override
    public abstract int dt();

    @Override
    public final byte opID() {
        return opID;
    }

    @Override
    public final String toString() {
        return toString(this);
    }

    @Override
    public boolean equalConcept(Term x) {
        return this == x || (x instanceof Compound c &&
            opID == c.opID &&
            concept().equals(c.concept()));
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Compound c && equalsCompound(c));
    }

    private boolean equalsCompound(Compound B) {
        int ao = opID;
        return ao == B.opID && equalHash(B) && equalDT(B, ao) && equalSubTerms(B);
    }

    private boolean equalDT(Compound B, int ao) {
        return (Temporals & (1 << ao)) == 0 || dt() == B.dt();
    }

    private boolean equalHash(Compound B) {
        return !(this instanceof Hashed) || !(B instanceof Hashed) || hashCode() == B.hashCode();
    }

    public final boolean equalSubTerms(Compound x) {
        return subtermsDirect().equalTerms(x.subtermsDirect());
    }

    @Override
    public final void write(ByteArrayDataOutput out) {
        if (this instanceof Neg) {
            writeNeg(out);
        } else {
            TermIO.writeCompoundPrefix(opID, dt(), out);
            if (this instanceof SeparateSubtermsCompound)
                subterms/*Direct*/().write(out);
            else
                Subterms.super.write(out);
        }
    }

    private void writeNeg(ByteArrayDataOutput out) {
        outNegByte(out);
        unneg().write(out);
    }

    /**
     * Compound default hashcode procedure
     */
    @Override
    public int hashCode() {
        var b = hash(opID, hashCodeSubterms());
        var dt = dt();
        return dt == DTERNAL ? b : hashCombine(b, dt);
    }

//    @Override
//    public int intifyRecurse(int vIn, IntObjectToIntFunction<Term> reduce) {
//        return reduce.intValueOf(
//                this instanceof SeparateSubtermsCompound ?
//                        subterms().intifyRecurse(vIn, reduce)
//                        : Subterms.super.intifyRecurse(vIn, reduce),
//                this);
//    }


//    /**
//     * TODO test
//     */
//    public boolean unifiesRecursively(Term x, Predicate<Term> preFilter) {
//
//        if (x instanceof Compound) {
////            int xv = x.volume();
//            if (!hasAny(Op.Variables) /*&& xv > volume()*/)
//                return false; //TODO check
//
//            //if (u.unifies(this, x)) return true;
//
//            int xOp = x.opID();
//            return !subterms().ANDrecurse(s -> s.hasAny(1 << xOp)/*t->t.volume()>=xv*/, s -> !(s instanceof Compound && s.opID() == xOp && preFilter.test(s) &&
//                    new UnifyAny().unify(x, s)), this);
//        } else {
//            return x instanceof Variable || containsRecursively(x);
//        }
//    }

    public boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return Subterms.super.recurseTermsOrdered(inSuperCompound, whileTrue, parent);
    }

    /**
     * very fragile be careful here
     */
    @Override
    public final boolean containsRecursively(Term x, @Nullable Predicate<Compound> inSubtermsOf) {
        return (inSubtermsOf == null || inSubtermsOf.test(this))
            &&
            (this instanceof SeparateSubtermsCompound ?
                    this.subterms().containsRecursively(x, inSubtermsOf) :
                    Subterms.super.containsRecursively(x, inSubtermsOf)
            );
    }

    @Override
    public boolean internables() {
        return subtermsDirect().internables();
    }

    public TermList condsAdd(@Nullable TermList x, boolean decomposeRootAuto, boolean decomposeE, boolean decomposeX) {
        if (!CONJ())
            throw new WTF();

        switch (dt()) {
            case DTERNAL, XTERNAL, 0 -> {
                if (decomposeRootAuto) {
                    var X = subtermsDirect();
                    if (x == null)
                        return X.toList();
                    else {
                        X.addAllTo(x);
                        return x;
                    }
                }
            }
        }
        var X = x == null ?
            new TermList(4 /* estimate */) :
            x;
        conds(X::add, decomposeE, decomposeX);
        return X;
    }

    @Override
    public final boolean SEQ() {
        return CONJ() && (!Tense.parallel(dt()) || subtermsDirect().hasSeq());
    }

    @Override
    public final int seqDur(boolean xternalSensitive) {

        if (this instanceof Neg /*|| DELTA()*/)
            return sub(0).seqDur(xternalSensitive);

        if (/*this instanceof CondAtomic || */!CONJ()) return 0;

        var dt = dt();
        if (dt == XTERNAL) {
            if (xternalSensitive)
                return XTERNAL;

            dt = 0;
        }

        var s = this.subtermsDirect();
        return switch (dt) {
            case DTERNAL, 0 -> s.seqDur(xternalSensitive);
            default -> seqDurSeq(s, dt, xternalSensitive);
        };
    }

    private static int seqDurSeq(Subterms ab, int dtInner, boolean xternalSensitive) {
        var a = ab.seqDurSub(0, xternalSensitive);
        if (a == XTERNAL) if (xternalSensitive) return XTERNAL; else a = 0;

        var b = ab.seqDurSub(1, xternalSensitive);
        if (b == XTERNAL) if (xternalSensitive) return XTERNAL; else b = 0;

        return a + Math.abs(dtInner) + b;
    }

    public final boolean condsOR(Predicate<Term> each, boolean decomposeDTernal, boolean decomposeXternal) {
        return condsOR((w, x)->each.test(x), 0, decomposeDTernal, decomposeXternal, false);
    }

    public final boolean condsOR(LongObjectPredicate<Term> each, long offset, boolean decomposeDTernal, boolean decomposeXternal, boolean inhExplode) {
        return CONJ() ? !condsAND(
                (when, what) -> !each.accept(when, what)
                , offset, decomposeDTernal, decomposeXternal, inhExplode) :
            each.accept(offset, this);
    }

    public final void conds(Consumer<Term> each, boolean decomposeConjDTernal, boolean decomposeXternal) {
        condsAND((subWhen, subWhat) -> {
            each.accept(subWhat);
            return true;
        }, 0, decomposeConjDTernal, decomposeXternal, false);
    }

    public final void conds(LongObjectProcedure<Term> each, long when, boolean decomposeConjDTernal, boolean decomposeXternal, boolean inhExplode) {
        condsAND((subWhen, subWhat) -> {
            each.value(subWhen, subWhat);
            return true;
        }, when, decomposeConjDTernal, decomposeXternal, inhExplode);
    }

    /**
     * iterates contained conds within a conjunction
     */
    public final boolean condsAND(LongObjectPredicate<Term> each, long offset, boolean decomposeParallel, boolean decomposeXternal, boolean inhExplode) {
        if (inhExplode && NAL.term.INH_BUNDLE && ConjBundle.containsBundledPossibly(this))
            each = condsANDBundledWrapper(each);

        if (CONJ()) {
            var dt = dt();
            if (decomposeConj(dt, decomposeParallel, decomposeXternal))
                return subtermsDirect().conjDecompose(each, offset, dt, decomposeParallel, decomposeXternal);
        }

        return each.accept(offset, this);
    }

    private static LongObjectPredicate<Term> condsANDBundledWrapper(LongObjectPredicate<Term> each) {
        var each0 = each;
        each = (when, what)-> ConjBundle.bundled(what) ?
            ConjBundle.eventsAND(what,
                x -> each0.accept(when, x))
                : each0.accept(when, what);
        return each;
    }

    /** for sequences: whether the event is the first, and whether to require the complete first cond */
    public boolean condStartEnd(Term e, boolean firstOrLast, boolean complete) {
        if (complete && Op.hasAny(structSubs(), CONJ))
            return condStartEndComplete(e, firstOrLast); //compound event subterms, like in a sequence
        else
            return (firstOrLast ? condFirst(e) : condLast(e));
    }

    private boolean condStartEndComplete(Term e, boolean firstOrLast) {
        var cc = Cond.conds(this, false, false);
        return e.equals((firstOrLast ? cc.getFirst() : cc.getLast()));
    }

    /** whether the condition is *in* the first of all sequence conditions */
    public final boolean condFirst(Term e) {
        var x = when(e, true);
        return x == DTERNAL || x == 0;
    }

    /** whether the condition is *in* the end of all sequence conditions */
    public final boolean condLast(Term e) {
        var w = when(e, false);
        return w != XTERNAL && (w == DTERNAL || w == seqDur());
    }


    /** @return offset of a matching cond if in a sequence
     *    XTERNAL if not found.
     *    DTERNAL if equal to the cond, or is a parallel/factored component */
    public int when(Term e, boolean fromStartOrEnd) {
        if (this == e) return DTERNAL;
        int vThis = complexity(), vE = e.complexity();
        if (vThis > vE) {
            if (CONDS()) {
                if (SEQ())
                    return condTimeSeq(e, fromStartOrEnd);
                else
                    return condOf(e) ? DTERNAL : XTERNAL;
            }
        } else if (vThis == vE && equals(e))
            return DTERNAL;

        return XTERNAL;
    }

    private int condTimeSeq(Term e, boolean fromStartOrEnd) {
        //certain sequence representations may be lengthier but still match, so dont test for volume containment here.
        var es = e.struct();
        if (impossibleSubStructure(es & ~CONJ.bit))
            return XTERNAL;

//        Set<Term> common = Cond.factoredEternalsCommon(this, e);
//        if (common!=null) {
//            return ((Compound)(Cond.eternalsRemove(this, common)))
//                    .condTimeSeq(Cond.eternalsRemove(e, common), fromStartOrEnd);
//        }

        var explode = Op.hasAny(es, INH) && ConjBundle.containsBundledPossibly(this);
        try (var Y = ConjList.conds(e, 0, true, false, explode)) {
            try (var X = ConjList.conds(this, 0, true, false, explode)) {
                try (var cc = new CondMatcher(X)) {
                    return cc.match(Y, fromStartOrEnd) ?
                        occToDT(cc.matchStart) : XTERNAL;
                }
            }
        }
    }

    @Override
    @Nullable
    public final Term normalize(byte varOffset) {

        var v0 = varOffset == 0;
        if (v0 && this.NORMALIZED())
            return this;

        if (this instanceof Neg)
            return _normalizeNeg(varOffset);

        var y = terms.normalize(this, varOffset);

        if (v0) y.setNormalized();

        return y;
    }

    private Term _normalizeNeg(byte varOffset) {
        var u = unneg();
        if (u instanceof Compound) {
            return _normalizeNegCompound(varOffset, u);
        } else if (u instanceof Variable uv){
            return _normalizeNegAtomic(uv, varOffset);
        } else
            return this;
    }

    private Term _normalizeNegCompound(byte varOffset, Term u) {
        var uu = u.normalize(varOffset);
        Compound y;
        if (u.equals(uu)) {
            y = this; //same
        } else {
            y = (Compound) uu.neg(); //different
        }
        if (varOffset == 0) y.setNormalized();
        return y;
    }

    private Term _normalizeNegAtomic(Variable x, byte varOffset) {

        Term y = x.normalize(varOffset);
        if (y == x) {
            //negated already normalized variable
            setNormalized();
            return this;
        } else
            return y.neg();

    }

    @Override
    public Term root() {
        if (this.root)
            return this;

        var neg = this instanceof Neg;
        Term x;
        if (neg) {
            x = unneg();
            if (!(x instanceof Compound xc) || xc.root) {
                this.root = true;
                return this;
            }
        } else
            x = this;

        Compound X = (Compound) x;
        return _root(X, x, terms.root(X), neg);
    }

    private @Nullable Term _root(Compound X, Term x, Term y, boolean neg) {
        if (!(y instanceof Compound Y))
            throw new TermTransformException("root fault", x, y);

        if (x.equals(y)) {
            X.root = true;
            if (neg)
                this.root = true;
            return this;
        } else {
            Y.root = true;

            if (neg) {
                var yn = y.neg();
                ((Compound) yn).root = true;
                y = yn;
            }

            return y;
        }
    }

    @Override
    public final Term concept() {
        if (this.concept)
            return this;

        if (this instanceof Neg)
            return unneg().concept();

        var y = root().normalize();
        validateConcept(y);
        return y;
    }

    private void validateConcept(Term y) {
//        if (this != y && opID != y.opID())
//            throw new TermTransformException(Op.UNCONCEPTUALIZABLE, this, y); //TODO other tests
        if (!(y instanceof Compound Y))
            throw new TermTransformException("concept()", this, y);

        assert !NAL.DEBUG || Y.NORMALIZED();

        Y.concept = true;
    }

    @Override
    public int struct() {
        return structSubs() | structOp();
    }

    @Deprecated public final Term dt(int dt) {
        return dt(dt, terms);
    }

    @Deprecated public final Term dt(int nextDT, TermBuilder b) {
        if (nextDT == 0) nextDT = DTERNAL; //HACK
        return nextDT != dt() ? op().build(b, nextDT, subtermsDirect()) : this;
    }

    @Override
    public boolean equalsRoot(Term y) {
        if (this == y) return true;

        if (!(y instanceof Compound Y) || opID != Y.opID)
            return false;

        if (this.equals(y))
            return true;

        var xx = root();
        var yy = y.root();
        return (xx != this || yy != y) && xx.equals(yy);
    }

    public final Lst<byte[]> pathsToList(Term x) {
        var paths = new Lst<byte[]>(2);
        pathsTo(x, (path, t) -> {
            paths.add(path.toArray());
            return true;
        });
        return paths;
    }

    private void pathsTo(Term target, BiPredicate<ByteList, Term> receiver) {
        pathsTo(target.equals(),
            x -> !x.impossibleSubTerm(target),
            receiver);
    }

    @Override
    public boolean TEMPORAL_VAR() {
        return dt()==XTERNAL || Subterms.super.TEMPORAL_VAR();
    }


    public final boolean condOf(Term x) {
        return condOf(x, +1);
    }

    /**
     * @param polarity +1 = positive, -1 = negated, 0 = positive or negated
     */
    public final boolean condOf(Term x, int polarity) {
        return switch (this.op()) {
            case INH -> condOfInh(x, polarity);
            case CONJ -> condOfConj(x, polarity);
            default -> {
//                if (polarity >= 0 && equals(x)) yield true;
//                if (polarity <= 0 && equalsNeg(x)) yield true;
                yield false;
            }
        };
    }

    private boolean condOfInh(Term x, int polarity) {
        if (!NAL.term.INH_BUNDLE)
            return false;

        var xNeg = x instanceof Neg;
        var xu = xNeg ? x.unneg() : x;
        if (!xu.INH())
            return false;
        if (!hasAll(xu.struct() & ~(CONJ.bit | NEG.bit)))
            return false;
        if (complexity() + (polarity == 1 ? 0 : +1) <= x.complexity())
            return false;

        //x = Image.imageNormalize(x); //assert (C.INH());

        return subtermsDirect().condOfInh(xu.subtermsDirect(), xNeg ? -polarity : polarity);
    }



    private boolean condOfConj(Term x, int polarity) {
        assert(CONJ());
        return
            polarity >= 0 && condOfConj(this, x)
            ||
            polarity <= 0 && condOfConj(this, x.neg());
    }

    private static boolean condOfConj(Compound c, Term x) {
        var cSeq = c.SEQ();
        var xConj = x.CONJ();

        var xSeq = xConj && x.SEQ();
        if (!cSeq && xSeq)
            return false; //impossible sequence in parallel

        if (c.impossibleSubStructure(
            (xConj ? x.structSubs() : x.struct()) & ~CONJ.bit
            //x.structure() & ~CONJ.bit
        ))
            return false;

        var cdt = c.dt();
        if (cdt == DTERNAL && !xConj)
            return c.contains(x); //simple parallel

        if (cSeq && xSeq)
            return c.condOfConjSeq(x); //exhaustive

        //complexity test valid only if both are not sequences, TODO prove
        if (c.complexity() <= x.complexity())
            return false;

        if (cdt == XTERNAL)
            return condOfXternal(c, x);
        else
            return c.condOfConjSeq(x);
    }

    private static boolean condOfXternal(Compound c, Term x) {
        return x.dt() == XTERNAL ?
                ((Compound) x).AND(xx -> condXternal(c, xx)) //both XTERNAL
                :
                condXternal(c, x);
    }

    private static boolean condXternal(Compound c, Term x) {
        return c.condsOR(x.equalsOrInCond(), !(x.CONJ() && x.dt()==DTERNAL), true);
        //return c.OR(xx.equalsOrInCond());
    }

    private static boolean impossibleCond(Compound c, Term x, boolean xConj) {
        return c.impossibleSubStructure(
                (xConj ? x.structSubs() : x.struct()) & ~CONJ.bit
                //x.structure() & ~CONJ.bit
        );
    }

    /** exhaustive
     * TODO optimize */
    private boolean condOfConjSeq(Term x) {
        if (seqDur() < x.seqDur())
            return false; //impossible

        try (var C = ConjList.conds(this, true, true)) {
            try (var X = ConjList.conds(x, true, true)) {
                return C.contains(X);
            }
        }
    }

}























































































    /*
    @Override
    public boolean equals(final Object that) {
        return (that instanceof Term) && (compareTo((Term) that) == 0);
    }
    */









































































































































































    /* UNTESTED
    public Compound clone(VariableTransform t) {
        if (!hasVar())
            throw new RuntimeException("this VariableTransform clone should not have been necessary");

        Compound result = cloneVariablesDeep();
        if (result == null)
            throw new RuntimeException("unable to clone: " + this);

        result.transformVariableTermsDeep(t);

        result.invalidate();

        return result;
    } */


/**
 * override in subclasses to avoid unnecessary reinit
 */
    /*public CompoundTerm _clone(final Term[] replaced) {
        if (Terms.equals(target, replaced)) {
            return this;
        }
        return clone(replaced);
    }*/





















































    /*static void shuffle(final Term[] list, final Random randomNumber) {
        if (list.length < 2)  {
            return;
        }


        int n = list.length;
        for (int i = 0; i < n; i++) {
            
            int r = i + (randomNumber.nextInt() % (n-i));
            Term tmp = list[i];    
            list[i] = list[r];
            list[r] = tmp;
        }
    }*/

/*        public static void shuffle(final Term[] ar,final Random rnd)
        {
            if (ar.length < 2)
                return;



          for (int i = ar.length - 1; i > 0; i--)
          {
            int index = randomNumber.nextInt(i + 1);
            
            Term a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
          }

        }*/
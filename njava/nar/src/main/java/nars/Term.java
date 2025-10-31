/*
 * Term.java
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
package nars;


import jcog.Is;
import jcog.The;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.Lst;
import nars.io.TermAppendable;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.atom.IntrinAtomic;
import nars.term.builder.InterningTermBuilder;
import nars.term.builder.TermBuilder;
import nars.term.util.TermEquality;
import nars.term.util.TermException;
import nars.term.util.Terms;
import nars.term.util.conj.ConjBundle;
import nars.term.util.transform.Replace;
import nars.term.var.Variable;
import nars.unify.Unify;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;


/**
 * The meaning "word or phrase used in a limited or precise sense"
 * is first recorded late 14c..
 * from Medieval Latin use of terminus to render Greek horos "boundary,"
 * employed in mathematics and logic.
 * Hence in terms of "in the language or phraseology peculiar to."
 * https://www.etymonline.com/word/term
 */
@Is({"Rewriting", "Regulated_rewriting"})
public abstract class Term implements Termlike, Termed, Comparable<Term> {

    private boolean pathsTo(ByteArrayList path, Predicate<Term> selector, Predicate<Compound> descendIf, BiPredicate<ByteList, Term> receiver) {
        if (selector.test(this) && !receiver.test(path == null ? Util.EmptyByteList : path, this))
            return false;

        if (this instanceof Compound c && descendIf.test(c)) {
            var ss = c.subtermsDirect();

            var n = ss.subs();
            if (n > 0) {
                int ppp;
                if (path == null) {
                    path = new ByteArrayList(n);
                    ppp = 0;
                } else
                    ppp = path.size();
                for (var i = 0; i < n; i++) {
                    if (i == 0)
                        path.add((byte) 0);
                    else
                        path.set(ppp, (byte) i);

                    if (!ss.sub(i).pathsTo(path, selector, descendIf, receiver))
                        return false;
                }
                path.removeAtIndex(ppp);
            }
        }

        return true;
    }

    public static Term nullIfNull(@Nullable Term maybeNull) {
        return (maybeNull == null) ? Bool.Null : maybeNull;
    }

    public static boolean commonEventStructure(Term x, Term y) {
        return x == y || hasCommon(x.unneg().struct() & (~(NEG.bit | CONJ.bit)), y.unneg().struct() & (~(NEG.bit | CONJ.bit)));
    }

    public static int hashShort(Term x, Term y) {
        var X = x.hashShort();
        var Y = x == y ? X : y.hashShort();
        return (Y << 16) | X;
    }

    public boolean ATOM() {
        return false;
    }

    public boolean VAR_DEP() {
        return false;
    }

    public boolean VAR_INDEP() {
        return false;
    }

    public boolean VAR_QUERY() {
        return false;
    }

    public boolean VAR_PATTERN() {
        return false;
    }

    public boolean INT() {
        return false;
    }

    public boolean INH() {
        return false;
    }

    public boolean SIM() { return false; }

    public boolean SAM() { return false; }

    public boolean IMPL() {
        return false;
    }

    public boolean CONJ() { return false; }

    public boolean PROD() { return false; }

    public boolean EQ() {
        return false;
    }

    public boolean SETe() {
        return false;
    }

    public boolean SETi() {
        return false;
    }

    public boolean SET() {
        return false;
    }

    public boolean DELTA() {
        return false;
    }

    public boolean STATEMENT() {
        return false;
    }

    public boolean COMMUTATIVE() {
        return false;
    }

    public boolean CONCEPTUALIZABLE() {
        return (structOp() & Conceptualizables) != 0;
    }

    public boolean CONDABLE() {
        return (structOp() & Condables) != 0;
    }

    /**
     * whether this contains events (aka conditions)
     */
    public final boolean CONDS() {
        return this instanceof Compound && switch (op()) {
            case CONJ -> true;
            case INH -> ConjBundle.bundled(this);
            default -> false;
        };
    }

    public final boolean TASKABLE() {
        return isAny(Taskables);
    }

    public boolean TEMPORAL() {
        return false;
    }

    /**
     * whether a conjunction is a sequence (includes check for factored inner sequence)
     */
    public boolean SEQ() {
        return false;
    }

    @Override
    public final Term term() {
        return this;
    }

    public final Op op() {
        return Op.op(opID());
    }

    public final int structOp() {
        return 1 << opID();
    }

    public abstract byte opID();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public final int hashShort() {
        var h = hashCode();
        return ((h & 0xffff) ^ (h >>> 16));
    }

    /**
     * returns the hashcode as if this term were negated
     */
    public int hashCodeNeg() {
        return Compound.hash1(NEG.id, this);
    }

    /**
     * rewraps a possible negation if a transform function results in a change to the unneg value
     */
    public final Term transformPN(Function<Term, Term> f) {
        return this instanceof Neg ?
            transformN(f) :
            f.apply(this);
    }

    public Term transformN(Function<Term, Term> f) {
        var x = unneg();
        var y = f.apply(x);
        return x == y ? this : y.neg();
    }

    public final boolean ANDrecurse(Predicate<Compound> superCompoundMust, Consumer<Term> each) {
        return ANDrecurse(superCompoundMust, (sub, sup) -> {
            each.accept(sub);
            return true;
        });
    }

    public final boolean ANDrecurse(Predicate<Compound> superCompoundMust, BiPredicate<Term, Compound> each) {
        return boolRecurse(superCompoundMust, each, null, true);
    }

    public final boolean ANDrecurse(Predicate<Compound> superCompoundMust, Predicate<Term> each) {
        return ANDrecurse(superCompoundMust, (s, p) -> each.test(s));
    }

    public final @Nullable Term replaceAt(ByteList path, Term replacement) {
        return replaceAt(path, 0, replacement);
    }

    public @Nullable Term replaceAt(ByteList path, int depth, Term replacement) {
        var ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw pathException("path overflow", path, depth, replacement);

        var src = (Compound) this;
        var css = src.subtermsDirect();

        var s = css.subs();

        var which = path.get(depth);
        if (which >= s)
            throw pathException("path subterm out-of-bounds", path, depth, replacement);

//        assert (which < s);

        var x = css.sub(which);
        var y = x.replaceAt(path, depth + 1, replacement);
        if (y == x) {
            return src; //unchanged
        } else {
            var target = css.arrayClone();
            target[which] = y;
            return src.op().the(src.dt(), target);
        }
    }

    private TermException pathException(String msg, ByteList path, int depth, Term replacement) {
        return new TermException(msg + ": " + path + " " + depth + " -> " + replacement, this);
    }

    public final boolean pathsTo(Predicate<Term> selector, Predicate<Compound> descendIf, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(null, selector, descendIf, receiver); //done, no subterms
    }

    public Term commonParent(Lst<byte[]> subpaths) {
        var n = subpaths.size(); //assert (subpathsSize > 1);

        var shortest = n > 1 ? (int) subpaths.minValue(b -> b.length) : subpaths.getFirst().length;
        int i;
        done:
        for (i = 0; i < shortest; i++) {
            byte needs = 0;
            for (var j = 0; j < n; j++) {
                var pi = subpaths.get(j)[i];
                if (j == 0)
                    needs = pi;
                else if (needs != pi)
                    break done;
            }
        }
        return i == 0 ? this : sub(subpaths.getFirst(), 0, i);
    }

    /**
     * extracts a subterm provided by the address tuple
     */
    public final Term sub(byte... path) {
        return sub(0, path.length, path);
    }

    /**
     * extracts a subterm provided by the address tuple
     * @return null if specified subterm does not exist
     */
    public final @Nullable Term subSafe(byte... path) {
        return subSafe(0, path.length, path);
    }

    public final Term sub(int start, int end, byte... path) {
        var ptr = this;
        for (var i = start; i < end; i++)
            ptr = ptr.sub(path[i]);
        return ptr;
    }

    public final @Nullable Term subSafe(int start, int end, byte... path) {
        var ptr = this;
        for (var i = start; i < end; i++) {
            if ((ptr = ptr.subSafe(path[i])) == null)
                return null;
        }
        return ptr;
    }

    private Term sub(byte[] path, int start, int end) {
        var ptr = this;
        for (var i = start; i < end; i++)
            ptr = ptr.sub(path[i]);
        return ptr;
    }

    /**
     * true if the operator bit is included in the enabld bits of the provided vector
     */
    public boolean isAny(int bitsetOfOperators) {
        return (structOp() & bitsetOfOperators) != 0;
    }

    public StringBuilder appendTo(StringBuilder s) {
        return (StringBuilder) new TermAppendable(s).appendSafe(this);
    }

    public boolean pathsTo(Term target, Predicate<Compound> superTermFilter, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                target.equals(),
                superTermFilter, //.and(x -> !x.impossibleSubTerm(target)),
                receiver);
    }

    @Override
    public int compareTo(Term t) {
        if (this == t) return 0;

        boolean atomic = this instanceof Atomic, batomic = t instanceof Atomic;
        if (atomic && !batomic) {
            return +1;
        } else if (batomic && !atomic) {
            return -1;
        } else if (!atomic) {
            //complexity decreasing
            var vc = Integer.compare(t.complexity(), this.complexity());
            if (vc != 0)
                return vc;
        }

        return compareTo(t, atomic);
    }

    private int compareTo(Term t, boolean atomic) {
        //opID icnreasing
        var o = this.opID();
        var c = Integer.compare(o, t.opID());
        if (c != 0)
            return c;
        else
            return atomic ? compareToAtomic(t) : compareToCompound(t, o);
    }

    private int compareToCompound(Term t, int op) {
        var c = Subterms.compare(this.subtermsDirect(), t.subtermsDirect());
        return c != 0 ? c : ((((1 << op) & Temporals) != 0) ?
                Integer.compare(dt(), t.dt()) : 0);
    }

    private int compareToAtomic(Term t) {
        return switch (this) {
            case Int ti ->
                Integer.compare(Int.i(ti), Int.i(t));
            case IntrinAtomic a when t instanceof IntrinAtomic ->
                Integer.compare(hashCode(), t.hashCode()); //same op, same hashcode
            default ->
                Util.compare(
                    ((Atomic) this).bytes(),
                    ((Atomic) t).bytes()
                );
        };
    }

    public abstract Subterms subterms();

    /**
     * for direct access to subterms only; implementations may return this instance if SameSubtermsCompound
     */
    public Subterms subtermsDirect() {
        return subterms();
    }


    public void printRecursive() {
        printRecursive(System.out);
    }

    public void printRecursive(PrintStream out) {
        Terms.printRecursive(out, this);
    }

    /**
     * returns this target in a form which can identify a concept, or Null if it can't
     * generally this is equivalent to root() but for compound it includes
     * unnegation and normalization steps. this is why conceptual() and root() are
     * different.  usually 'this'
     */
    public abstract Term concept();
    /**
     * the skeleton of a target, without any temporal or other meta-assumptions
     */
    public abstract Term root();

    /**
     * TODO make Compound only
     */
    public int dt() {
        return DTERNAL;
    }

    public Term normalize(byte offset) {
        return this;
    }

    public final Term normalize() {
        return normalize((byte) 0);
    }


    public final Term replace(Map<? extends Term, Term> m) {
        return replace(m, terms);
    }

    public final Term replace(Term from, Term to) {
        return replace(from, to, terms);
    }

    public final Term replace(Map<? extends Term, Term> m, TermBuilder B) {
        return Replace.replace(this, m, B);
    }

    /**
     * replaces the 'from' target with 'to', recursively
     */
    public final Term replace(Term from, Term to, TermBuilder B) {
        return Replace.replace(this, from, to, B);
    }

    /**
     * unwraps negation - only changes if this term is NEG
     */
    public Term unneg() {
        return this;
    }

    public Term neg() {
        return terms.neg(this);
    }

    public final Term negIf(boolean negate) {
        return negate ? neg() : this;
    }

    public boolean internable() {
        return this instanceof The && (
            !(this instanceof Compound c)
            ||
            ((InterningTermBuilder.internTemporal || !dtSpecial(dt())) && c.internables())
        );
    }

    public abstract boolean equalsRoot(Term x);

    public final boolean equalsPN(Term t) {
        return this == t || unneg().equals(t.unneg());
    }

    public final boolean equalsNeg(Term t) {
        if (this instanceof Neg)
            return !(t instanceof Neg) && unneg().equals(t);
        else if (t instanceof Neg)
            return /*!(this instanceof Neg) &&*/ equals(t.unneg());
        else
            return this instanceof Bool && t instanceof Bool && neg().equals(t);
    }

    public abstract boolean equalConcept(Term x);

    public Predicate<Term> equals() {
        return new TermEquality(this);
    }
    public Predicate<Term> equalsPN() {
        return new TermEqualityPN(this);
    }


    public Predicate<Term> equalsRoot() {
        return TEMPORALABLE() ? new TermRootEquality() : equals();
    }
    public final Predicate<Term> equalsOp() {
        return t -> t.opID()==opID();
    }

    /**
     * @param polarity +1 (positive, as-is)   0: pos or negative,    -1: negative
     */
    public Predicate<Term> equals(int polarity) {
        return switch (polarity) {
            case +1 -> equals();
            case -1 -> neg().equals(); //TODO optimize
            case 0 -> equalsPN();
            default -> throw new UnsupportedOperationException();
        };
    }

    public final Predicate<Term> unifiable(int vars, int dur) {
        if (this instanceof Compound) {
            return unifiableCompound(vars, dur);
        } else if (this instanceof Variable) {
            if (Op.hasAny(vars, structOp()))
                return x -> true;
        }

        return equals();
    }

    private Predicate<Term> unifiableCompound(int vars, int dur) {
        var qOp = opID();
        return x -> x instanceof Compound X &&
                X.opID == qOp &&
                //structured(x.structure()) &&
                //!invalidTargets.containsKey(x) &&
                Unify.isPossible(this, x, vars, dur);
    }

    @Deprecated public final int DT() {
        var x = dt();
        return switch(x) {
            case XTERNAL ->
                throw new WTF();
            case DTERNAL -> 0;
            default -> x;
        };
    }

    public boolean VAR() {
        return false;
    }

    public final Predicate<Term> equalsOrInCond() {
        var xc = complexity();
        return y -> {
            int yc = y.complexity();
            return xc == yc ?
                equals(y) :
                y instanceof Compound Y && xc < yc && Y.condOf(this);
        };
        //return equals().or(condIn());
    }

    /**
     * @return 0=not equal, -1=equalsNeg, +1=equals
     */
    public final int equalsPolarity(Term y) {
        var x = this;
        var invert = false;
        if (x instanceof Neg) {
            if (!(y instanceof Neg)) {
                x = x.unneg();
                invert = true;
            } else {
                x = x.unneg();
                y = y.unneg(); //unwrap both
            }
        } else {
            if (y instanceof Neg) {
                y = y.unneg();
                invert = true;
            }
        }

        return x.equals(y) ? (invert ? -1 : +1) : 0;
    }

    public final boolean containsRecursivelyOrEquals(Term x) {
        return equals(x) || containsRecursively(x);
    }

    public final Predicate<Termed> equalsTermed() {
        var x = equals();
        return z -> x.test(z.term());
    }

    private static class TermEqualityPN extends TermEquality {

        TermEqualityPN(Term x) {
            super(x.unneg());
        }

        @Override
        public boolean test(Term y) {
            return super.test(y.unneg());
        }
    }

    private final class TermRootEquality implements Predicate<Term> {
        @Nullable TermEquality t;

        @Override
        public boolean test(Term x) {
            if (x == Term.this) //fast test
                return true;
            if (x.opID()!=opID())
                return false;

            if (t == null) t = new TermEquality(root());

            return t.test(x.root());
        }
    }


//    enum TermWalk {
//        Left, //prev subterm
//        Right, //next subterm
//        Down, //descend, recurse, or equivalent to Right if atomic
//        Stop //CUT
//    }

    /**
     * sequence time range (in cycles) spanned by this term
     */
    public int seqDur(boolean xternalSensitive) {
        return 0;
    }

    public final int seqDur() {
        return seqDur(false);
    }

    public final int countRecursive(Predicate<? super Term> match) {
        return intifyRecurse(0, Subterms.counter(match));
    }

    public int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce) {
        //return intifyShallow(vIn, (vNext, subterm) -> subterm.intifyRecurse(vNext, reduce));
        var n = subs();
        for (var i = 0; i < n; i++)
            v = sub(i).intifyRecurse(v, reduce);
        return v;
    }
}
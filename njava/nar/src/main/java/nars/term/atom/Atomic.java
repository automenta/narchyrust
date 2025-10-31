package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Hashed;
import jcog.Util;
import nars.$;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.CondAtomic;
import nars.term.util.TermException;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static java.lang.System.arraycopy;
import static nars.Op.ATOM;
import static nars.Op.EmptySubterms;
import static nars.term.atom.Bool.Null;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class Atomic extends Term implements Hashed, CondAtomic {

    /*@Stable*/
    /** TODO make private again */
    public final transient byte[] bytes;

    protected final transient int hash;

    protected Atomic(Op o, byte num) {
        this(IntrinAtomic.termToId(o, num), o.id, num);
        assert num > 0;
    }

    protected Atomic(int hash, byte... raw) {
        this.bytes = raw;
        this.hash = hash;
    }

    protected Atomic(byte[] raw) {
        this(Util.hash(raw), raw);
    }

    public static Atom atom(String id) {
        return (Atom) atomic(id);
    }

    public static Atomic _atomic(int c) {
        return atomic((char)c);
    }

    public static Atomic atomic(char c) {
        return switch (c) {
            case Op.VarAutoSym -> Op.VarAuto;
            case Op.NullSym -> Null;
            case Op.imIntSym -> Op.ImgInt;
            case Op.imExtSym -> Op.ImgExt;
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> Int.pos[c - '0'];
            case Op.NaNChar -> Op.NaN;
            case Op.InfinityPosChar -> Op.InfinityPos;
            case Op.InfinityNegChar -> Op.InfinityNeg;
            default -> c < CharAtom.chars.length ? CharAtom.chars[c] : Op.terms.atomNew(String.valueOf(c));
        };
    }

    public static Atomic atomic(/*TODO CharSequence */ String id) {
        return switch (id.length()) {
            case 0 -> throw new RuntimeException("attempted construction of zero-length Atomic id");
            case 1 -> atomic(id.charAt(0));
            default -> switch (id) {
                case "true"  -> Bool.True;
                case "false" -> Bool.False;
                case "null"  -> Null;
                default -> Atom.quoteable(id) ?
                    $.quote(id) :
                    Op.terms.atomNew(Atom.validateAtomID(id));
            };
        };
    }


    @Override
    public byte opID() {
        return bytes[0];
    }

    protected Atomic(byte opID, byte[] s) {
        this(bytes(opID, s));
    }

    public static byte[] bytes(byte opID, String str) {
        return bytes(opID, str.getBytes());
    }

    private static byte[] bytes(byte opID, byte... stringbytes) {
        var slen = stringbytes.length;
        var b = new byte[slen + 3];
        b[0] = opID;
        b[1] = (byte) (slen >> 8 & 0xff);
        b[2] = (byte) (slen & 0xff);
        arraycopy(stringbytes, 0, b, 3, slen);
        return b;
    }


    public final byte[] bytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object u) {
        return (this == u) ||
            ((u instanceof Atomic a) && (
                hash == u.hashCode()) && Arrays.equals(bytes, a.bytes())
            );
    }

    @Override public String toString() {
        return new String(bytes, 3, bytes.length-3);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Override
    public final boolean ATOM() {
        return isAny(ATOM.bit);
    }

    @Override
    public final boolean TEMPORAL_VAR() {
        return false;
    }

    @Override
    public final boolean equalsRoot(Term x) {
        return equals(x);
    }

    @Override
    public final void write(ByteArrayDataOutput out) {
        if (this == Null)
            throw new NullPointerException("null output");
        out.write(bytes());
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varPattern() {
        return 0;
    }

    @Override
    public final Subterms subterms() { return EmptySubterms; }

    @Override
    public final boolean equalConcept(Term x) {
        return x instanceof Atomic && equals(x);
    }

    @Override
    public final Term concept() {
        if (!CONCEPTUALIZABLE())
            throw new TermException(Op.UNCONCEPTUALIZABLE, this);

        return this;
    }

    @Override
    public final Term root() { return this; }

    @Override
    public final int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        return reduce.intValueOf(v, this);
    }

    @Override
    public final int intifyRecurse(int vIn, IntObjectToIntFunction<Term> reduce) {
        return intifyShallow(vIn, reduce);
    }

    @Override
    public final boolean boolRecurse(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent, boolean andOrOr) {
        return whileTrue.test(this, parent);
    }

    @Override
    public final boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return whileTrue.test(this);
    }

    @Override
    public final int subs() {
        return 0;
    }

    @Override public final int vars() { return this instanceof Variable ? 1 : 0; }

    @Override public final int complexityConstants() { return this instanceof Variable ? 0 : 1; }

    @Override
    public final int complexity() {
        return 1;
    }

    @Override
    public final Term sub(int i, Term ifOutOfBounds) {
        return ifOutOfBounds;
    }

    @Override
    public final int struct() {
        return structOp();
    }

    @Override
    public final int structSubs() {
        return 0;
    }

    @Override
    public final int structSurface() {
        return 0;
    }

    public final int height() { return 1; }
}
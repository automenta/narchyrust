package nars.term.buffer;

import com.google.common.primitives.Ints;
import jcog.data.byt.DynBytes;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.builder.TermBuilder;
import nars.term.util.TermException;
import nars.term.util.map.ByteAnonMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.UnaryOperator;

import static nars.Op.DTERNAL;
import static nars.Op.NEG;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;

/**
 * Memoizable supplier of compounds.
 * Fast to construct but not immediately usable (or determined to be valid)
 * without calling the .term().
 * Consists of a flat linear tape of instructions which
 * when executed construct the target.
 */
public class TermBuffer implements Cloneable {
	public final ByteAnonMap sub;
	final DynBytes code = new DynBytes(INITIAL_CODE_SIZE);
	private final TermBuilder builder;

	/**
	 * When true, non-dynamic constant sequences will be propagated to inline future repeats in instantiation.
	 * If a non-deterministic functor evaluation occurs, it must not propagate
	 * because that will just cause the same value to be assumed when it should not be.
	 */
	int volRemain;

	/**
	 * Incremental mutation counter to detect unmodified segments that can be inlined / interned atomically.
	 */
	private int change;

	private transient byte[] bytes;
	private transient int readPtr;

	/**
	 * Constructs an empty TermBuffer with default TermBuilder.
	 */
	public TermBuffer() {
		this(Op.terms);
	}

	/**
	 * Constructs a TermBuffer with a specific TermBuilder.
	 *
	 * @param builder The TermBuilder to use for constructing Terms.
	 */
	private TermBuffer(TermBuilder builder) {
		this(builder, new ByteAnonMap());
	}

	public TermBuffer(Term x) {
		this();
		append(x);
	}

	/**
	 * Constructs a TermBuffer with a specific TermBuilder and ByteAnonMap.
	 *
	 * @param builder The TermBuilder to use for constructing Terms.
	 * @param sub     The ByteAnonMap for interning Terms.
	 */
	private TermBuffer(TermBuilder builder, ByteAnonMap sub) {
		this.builder = builder;
		this.sub = sub;
	}

	/**
	 * Expands a fragment into subterms.
	 *
	 * @param subterms     The current array of subterms.
	 * @param i            The starting index.
	 * @param fragment     The fragment Term to add.
	 * @param len          The expected length.
	 * @param fragmentLen  The length of the fragment.
	 * @return The expanded array of subterms.
	 */
	private static Term[] nextFrag(Term[] subterms, int i, Term fragment, byte len, int fragmentLen) {
		if (subterms == null)
			subterms = new Term[len];
		else if (subterms.length != len)
			subterms = Arrays.copyOf(subterms, len); // Used to grow OR shrink
		if (fragmentLen > 0)
			fragment.addAllTo(subterms, i); // TODO: recursively evaluate?
		return subterms;
	}

	/**
	 * Returns an empty Term based on the operator.
	 *
	 * @param op The operator.
	 * @return An empty Term corresponding to the operator.
	 */
	private static Term emptySubterms(Op op) {
		return switch (op) {
			case PROD -> Op.EmptyProduct;
			case CONJ -> True;
			default -> throw new TermException(op + " does not support empty subterms");
		};
	}

	/**
	 * Clones the current TermBuffer, ensuring all relevant fields are copied.
	 *
	 * @return A cloned TermBuffer instance.
	 */
	@Override
	public TermBuffer clone() {
		var y = new TermBuffer();//(TermBuffer) super.clone();
		y.code.write(this.code);
		y.volRemain = this.volRemain;
		y.change = this.change;
		y.bytes = this.bytes != null ? Arrays.copyOf(this.bytes, this.bytes.length) : null;
		y.readPtr = this.readPtr;
		y.sub.putAll(this.sub);
		return y;
	}

	/**
	 * Clears the TermBuffer.
	 * If 'code' is true, clears the bytecode tape.
	 * If 'uniques' is true, clears the interning map.
	 */
	public final void clear() {
		clear(true, true);
	}

	/**
	 * Clears the TermBuffer based on the provided flags.
	 *
	 * @param clearCode   Whether to clear the bytecode tape.
	 * @param clearUniques Whether to clear the interning map.
	 */
	public void clear(boolean clearCode, boolean clearUniques) {
		if (clearUniques) {
			sub.clear();
		}
		if (clearCode) {
			this.code.clear();
			change = 0;
		}
	}

	/**
	 * Replaces terms in the interning map using the provided UnaryOperator.
	 *
	 * @param m The UnaryOperator to apply to each Term.
	 * @return True if any replacements occurred, false otherwise.
	 */
	public boolean replace(UnaryOperator<Term> m) {
		return sub.updateMap(m);
	}

	/**
	 * Begins constructing a compound term with the given operator ID.
	 *
	 * @param oid The operator ID.
	 * @return The current TermBuffer instance.
	 */
	TermBuffer compoundStart(byte oid) {
        var c = this.code;
		if (((1 << oid) & Op.Temporals) != 0)
			c.writeByteInt(oid, DTERNAL);
		else
			c.writeByte(oid);
		return this;
	}
	private TermBuffer compoundStart(Compound x) {
		var c = this.code;
		byte oid = x.opID;
		if (((1 << oid) & Op.Temporals) != 0)
			c.writeByteInt(oid, x.dt());
		else
			c.writeByte(oid);
		return this;
	}

	/**
	 * Begins constructing a compound term with the specified operator and dt.
	 *
	 * @param o  The operator.
	 * @param dt The dt value.
	 * @return The current TermBuffer instance.
	 */
	TermBuffer compoundStart(Op o, int dt) {
        var oid = o.id;
		var c = this.code;
        if (o.temporal)
            c.writeByteInt(oid, dt);
        else
            c.writeByte(oid);
        return this;
	}


	/**
	 * Appends a compound term with the given operator and subterms.
	 *
	 * @param o    The operator.
	 * @param subs The subterms.
	 * @return The current TermBuffer instance.
	 */
	final TermBuffer appendCompound(Op o, Term... subs) {
		return appendCompound(o, DTERNAL, subs);
	}

	/**
	 * Appends a compound term with the given operator, dt, and subterms.
	 *
	 * @param o    The operator.
	 * @param dt   The dt value.
	 * @param subs The subterms.
	 * @return The current TermBuffer instance.
	 */
	final TermBuffer appendCompound(Op o, int dt, Term... subs) {
		return compoundStart(o, dt).appendSubterms(subs).compoundEnd();
	}

	/**
	 * Ends the construction of a compound term.
	 * Currently, no additional logic is required here.
	 *
	 * @return The current TermBuffer instance.
	 */
	final TermBuffer compoundEnd() {
		// Placeholder for any necessary finalization logic
		// Currently no action needed
		return this;
	}

	/**
	 * Begins constructing subterms with the specified length.
	 *
	 * @param subterms The number of subterms.
	 * @return The current TermBuffer instance.
	 */
	final TermBuffer subsStart(byte subterms) {
		code.writeByte(subterms);
		return this;
	}

	/**
	 * Begins constructing a negation term.
	 *
	 * @return The current TermBuffer instance.
	 */
	public final TermBuffer negStart() {
		code.writeByte(NEG.id);
		return this;
	}

	/**
	 * Ends the construction of a negation term.
	 *
	 * @return The current TermBuffer instance.
	 */
	private TermBuffer negEnd() {
		return compoundEnd();
	}

	/**
	 * Appends a Term to the buffer.
	 *
	 * @param x The Term to append.
	 * @return The current TermBuffer instance.
	 */
	public final TermBuffer append(Term x) {
        return switch (x) {
            case Atomic atomic -> appendInterned(x);
            case Compound terms -> append(terms);
            case null, default -> throw new TermException("Unsupported Term type: " + x.getClass());
        };
	}

	/**
	 * Appends a Compound Term to the buffer.
	 *
	 * @param x The Compound Term to append.
	 * @return The current TermBuffer instance.
	 */
	private TermBuffer append(Compound x) {
        return x instanceof Neg ? appendNeg(x) : appendCompound(x);
	}

	private TermBuffer appendCompound(Compound x) {
		return compoundStart(x).appendSubterms(x.subtermsDirect()).compoundEnd();
	}

	private TermBuffer appendNeg(Compound x) {
		return negStart().append(x.unneg()).negEnd();
	}

	/**
	 * Appends subterms to the buffer.
	 *
	 * @param s The Subterms to append.
	 * @return The current TermBuffer instance.
	 */
	private TermBuffer appendSubterms(Subterms s) {
		return subsStart((byte) s.subs()).appendAll(s).subsEnd();
	}

	/**
	 * Appends all provided Terms to the buffer.
	 *
	 * @param subs The array of Terms to append.
	 * @return The current TermBuffer instance.
	 */
	final TermBuffer appendAll(Term[] subs) {
		for (var x : subs)
			append(x);
		return this;
	}

	final TermBuffer appendAll(Subterms subs) {
        var n = subs.subs();
		for (var i = 0; i < n; i++)
			append(subs.sub(i));
		return this;
	}

	final TermBuffer appendAll(Iterable<Term> subs) {
		for (var x : subs)
			append(x);
		return this;
	}

	/**
	 * Appends an interned Term to the buffer.
	 *
	 * @param x The Term to intern and append.
	 * @return The current TermBuffer instance.
	 */
	private TermBuffer appendInterned(Term x) {
        return appendInterned(intern(x));
	}

	/**
	 * Appends an interned Term by its byte identifier.
	 *
	 * @param i The byte identifier of the interned Term.
	 * @return The current TermBuffer instance.
	 */
	private TermBuffer appendInterned(byte i) {
		if (i < 0)
			throw new TermException("Interned byte identifier out of range: " + i);
		code.writeByte((byte) (MAX_CONTROL_CODES + i));
		return this;
	}

	/**
	 * Interns a Term and returns its byte identifier.
	 *
	 * @param x The Term to intern.
	 * @return The byte identifier of the interned Term.
	 */
	private byte intern(Term x) {
		if (x == null || x == Null || x instanceof Neg)
			throw new TermException("Invalid Term for interning: " + x);
		return sub.intern(x);
	}

	/**
	 * Appends multiple subterms to the buffer.
	 *
	 * @param subs The array of Terms to append as subterms.
	 * @return The current TermBuffer instance.
	 */
	final TermBuffer appendSubterms(Term... subs) {
		return subsStart((byte) subs.length).appendAll(subs).subsEnd();
	}

	/**
	 * Constructs and returns the Term from the buffer.
	 *
	 * @return The constructed Term.
	 */
	public Term term() {
		return term(NAL.term.COMPOUND_VOLUME_MAX);
	}

	/**
	 * Constructs and returns the Term from the buffer with a specified volume limit.
	 *
	 * @param volMax The maximum allowed volume.
	 * @return The constructed Term.
	 */
	public Term term(int volMax) {
		this.volRemain = volMax;
		readPtr = 0;
		bytes = code.arrayDirect();
        var result = nextTerm();
		bytes = null; // Release reference
		return result;
	}

	/**
	 * Recursively constructs the next Term from the bytecode.
	 *
	 * @return The next constructed Term.
	 */
	private Term nextTerm() {
		if (readPtr >= bytes.length) {
			throw new TermException("Unexpected end of bytecode at position " + readPtr);
		}

        var start = readPtr++;
        var ctl = bytes[start];

		if (ctl >= MAX_CONTROL_CODES) {
			return nextInterned(ctl);
		} else if (ctl == NEG.id) {
            var negated = nextTerm();
			if (negated == Null) return Null;
			return negated.neg();
		}

        var op = Op.op(ctl);

		if (op.atomic) { // Alignment error or atomic operator where compound expected
			throw new TermException(TermBuffer.class + ": atomic operator found where compound operator expected: " + op);
		}

        var dt = op.temporal ? readDT(bytes) : DTERNAL;

  		int volRemaining = this.volRemain - 1 /* one for the compound itself */;

		byte len = bytes[readPtr++];
		if (len == 0)
			return emptySubterms(op);

        var subterms = new Term[len];
		for (var i = 0; i < len; i++) {
			if (volRemain <= 0)
				return Null; // Capacity reached

			Term nextSub = nextTerm();
			if (nextSub == Null)
				return Null;

      		//append next subterm
			subterms[i] = nextSub;

			volRemaining -= nextSub.complexity();
			if ((this.volRemain = volRemaining) <= 0)
				return Null; //capacity reached
		}

		return nextCompound(op, dt, subterms, start);
	}

	/**
	 * Constructs a new compound Term from the given components.
	 *
	 * @param op        The operator.
	 * @param dt        The dt value.
	 * @param subterms  The subterms.
	 * @param from      The starting byte position of the compound.
	 * @return The constructed compound Term.
	 */
	private Term nextCompound(Op op, int dt, Term[] subterms, int from) {
        var compound = newCompound(op, dt, subterms);
		// Optionally, perform ahead replacement if necessary
		// Example:
		// if (isConstant(subterms)) {
		//     replaceAhead(bytes, from, compound);
		// }
		return compound;
	}

	/**
	 * Constructs a new compound Term using the TermBuilder.
	 *
	 * @param op        The operator.
	 * @param dt        The dt value.
	 * @param subterms  The subterms.
	 * @return The constructed compound Term.
	 */
	private Term newCompound(Op op, int dt, Term[] subterms) {
		return op.build(builder, dt, subterms);
	}

	/**
	 * Retrieves the number of unique terms interned.
	 *
	 * @return The number of unique terms.
	 */
	private int uniques() {
		return sub.idToTerm.size();
	}

	/**
	 * Returns the byte identifier of an interned Term.
	 *
	 * @param x The Term to intern.
	 * @return The byte identifier of the interned Term.
	 */
	public final byte term(Term x) {
		return sub.interned(x);
	}

	/**
	 * Reads a 4-byte integer from the byte array starting at readPtr.
	 *
	 * @param ii The byte array.
	 * @return The integer value.
	 */
	private int readDT(byte[] ii) {
		if (readPtr + 4 > ii.length) {
			throw new TermException("Insufficient bytes to read dt at position " + readPtr);
		}
        var p = readPtr;
		readPtr += 4;
		return Ints.fromBytes(ii[p], ii[p + 1], ii[p + 2], ii[p + 3]);
	}

	/**
	 * Replaces ahead byte sequences with interned Terms to optimize the bytecode.
	 *
	 * @param ii    The byte array.
	 * @param from  The starting position of the sequence to replace.
	 * @param next  The Term to replace with.
	 */
	private void replaceAhead(byte[] ii, int from, Term next) {
        var to = readPtr;
        var span = to - from;

		if (span <= NAL.term.TERM_BUFFER_MIN_REPLACE_AHEAD_SPAN) {
			return; // Span too small to replace
		}

        var end = length();

		if (end - to < span) {
			return; // Not enough bytes to perform replacement
		}

        var interned = (byte) (MAX_CONTROL_CODES + intern(next));

        var afterTo = to;
		while (afterTo + span <= end) {
            var match = ArrayUtil.nextIndexOf(ii, afterTo, end, ii, from, to);
			if (match == -1) {
				break; // No more matches
			}

			// Replace the matched sequence with the interned byte
			code.set(match, interned);

			// Zero out the subsequent bytes to maintain span
			for (var i = 1; i < span; i++) {
				code.set(match + i, (byte) 0);
			}

			afterTo = match + span;
		}
	}

	/**
	 * Retrieves an interned Term based on its byte identifier.
	 *
	 * @param ctl The control byte.
	 * @return The interned Term.
	 */
	private Term nextInterned(byte ctl) {
        var internedId = (byte) (ctl - MAX_CONTROL_CODES);
		return sub.interned(internedId);
	}

	/**
	 * Returns the current length of the bytecode tape.
	 *
	 * @return The length of the bytecode tape.
	 */
	public int length() {
		return code.length;
	}

	/**
	 * Rewinds the bytecode tape to a specified position.
	 *
	 * @param codePos The position to rewind to.
	 */
	private void rewind(int codePos) {
		if (codePos < 0 || codePos > code.length) {
			throw new TermException("Invalid rewind position: " + codePos);
		}
		code.length = codePos;
	}

	/**
	 * Checks if the TermBuffer is empty.
	 *
	 * @return True if empty, false otherwise.
	 */
	public boolean isEmpty() {
		return code.length == 0 && (sub == null || sub.isEmpty());
	}

	/**
	 * Appends a Term to the buffer using a transformation function.
	 *
	 * @param x The Term to append.
	 * @param f The transformation function.
	 * @return True if the Term was appended, false otherwise.
	 */
	public boolean append(Term x, UnaryOperator<Term> f) {
		if (x instanceof Compound) {
			return appendCompound((Compound) x, f);
		} else {
			return appendAtomic(x, f);
		}
	}

	/**
	 * Appends an atomic Term using a transformation function.
	 *
	 * @param x The atomic Term to append.
	 * @param f The transformation function.
	 * @return True if the Term was appended, false otherwise.
	 */
	private boolean appendAtomic(Term x, UnaryOperator<Term> f) {
		if (x == Null) {
			return false;
		}

		@Nullable Term y = f.apply(x);
		if (y == Null) {
			return false;
		} else {
			if (x != y) {
				change++;
			}
			this.append(y);
			return true;
		}
	}

	/**
	 * Appends a compound Term using a transformation function.
	 *
	 * @param x The compound Term to append.
	 * @param f The transformation function.
	 * @return True if the Term was appended, false otherwise.
	 */
	private boolean appendCompound(Compound x, UnaryOperator<Term> f) {
		if (x instanceof Neg) {
			negStart();
			if (!append(x.unneg(), f)) {
				return false;
			}
			negEnd();
			return true;
		} else {
            var interned = term(x);
			if (interned != Byte.MIN_VALUE) {
				appendInterned(interned);
				return true;
			} else {
				return appendCompoundPos(x, f);
			}
		}
	}

	/**
	 * Appends a compound Term at its position using a transformation function.
	 *
	 * @param x The compound Term to append.
	 * @param f The transformation function.
	 * @return True if the Term was appended, false otherwise.
	 */
	private boolean appendCompoundPos(Compound x, UnaryOperator<Term> f) {
        var initialChange = this.change;
        var initialLength = this.length();

        var o = x.op();

		this.compoundStart(o, o.temporal ? x.dt() : DTERNAL);

        var xx = x.subtermsDirect();
		if (!append(xx, f)) {
			return false;
		}

		this.compoundEnd();

		if (change == initialChange && constant(xx)) {
			// Rewind and replace the compound with its interned version
			this.rewind(initialLength);
			this.appendInterned(x);
		}
		return true;
	}

	/**
	 * Determines if the given Subterms are constant.
	 *
	 * @param xx The Subterms to check.
	 * @return True if constant, false otherwise.
	 */
	protected boolean constant(Subterms xx) {
		return xx.isConstant();
	}

	/**
	 * Appends Subterms to the buffer using a transformation function.
	 *
	 * @param s The Subterms to append.
	 * @param f The transformation function.
	 * @return True if successful, false otherwise.
	 */
	private boolean append(Subterms s, UnaryOperator<Term> f) {
        var n = (byte) s.subs();
		this.subsStart(n);
		if (!s.ANDwithOrdered(this::append, f)) {
			return false;
		}
		this.subsEnd();
		return true;
	}

	/**
	 * Sets the remaining volume for term construction.
	 *
	 * @param v The volume to set.
	 * @return The current TermBuffer instance.
	 */
	public TermBuffer volRemain(int v) {
		volRemain = v;
		return this;
	}

	/**
	 * Retrieves the current change counter.
	 *
	 * @return The change counter.
	 */
	public int getChange() {
		return change;
	}

	/**
	 * Checks if a set of Subterms is constant.
	 *
	 * @param subterms The Subterms to check.
	 * @return True if all Subterms are constant, false otherwise.
	 */
	private boolean isConstant(Subterms subterms) {
		return subterms.isConstant();
	}

	/**
	 * Finalizes the buffer after appending subterms.
	 *
	 * @return The current TermBuffer instance.
	 */
    public TermBuffer subsEnd() {
		return this;
	}

	private static final int INITIAL_CODE_SIZE = 8;
	static final byte MAX_CONTROL_CODES = (byte) Op.count;

	public IndexedTermBufferCursor cursor() {
		return new IndexedTermBufferCursor(code
			.arrayDirect()
			//.arrayCopy();
		);
	}
}

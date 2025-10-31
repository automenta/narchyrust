package nars.term.buffer;

import com.google.common.primitives.Ints;
import nars.Op;
import nars.term.util.TermException;

import static nars.Op.DTERNAL;
import static nars.term.buffer.TermBuffer.MAX_CONTROL_CODES;

/**
 * A lightweight streaming cursor for traversing a TermBuffer's byte[] representation
 * without full deserialization or nested indexing.
 */
public class TermBufferCursor {

    protected final byte[] data;
    protected final int length;
    protected int pos; // current position in data

    protected byte currentCtl;     // current control byte: operator or interned ID
    protected int currentDT;       // dt if current operator is temporal
    protected int currentSubCount; // number of subterms for current compound

    protected boolean stop;

    public TermBufferCursor(byte[] buffer) {
        this(buffer, true);
    }

    protected TermBufferCursor(byte[] buffer, boolean read) {
        this.data = buffer;
        this.length = buffer.length;
        if (read && !(this.stop = (this.length > 0)))
            readNext(); // initialize first token
    }

    /**
     * Advances the cursor to the next token.
     * @return false if no more tokens available.
     */
    public boolean next() {
        if (stop) return false;
        readNext();
        return !stop;
    }

    protected void readNext() {
        if (pos >= length) {
            stop = true;
            return;
        }
        currentCtl = data[pos++];
        if (currentCtl >= MAX_CONTROL_CODES) {
            // Interned term
            currentDT = DTERNAL;
            currentSubCount = 0;
        } else {
            var op = Op.op(currentCtl);
            if (op.temporal) {
                if (pos + 4 > length)
                    throw new TermException("Unexpected EOF while reading dt for " + op);
                currentDT = Ints.fromBytes(data[pos], data[pos + 1], data[pos + 2], data[pos + 3]);
                pos += 4;
            } else {
                currentDT = DTERNAL;
            }
            if (pos >= length)
                throw new TermException("Unexpected EOF while reading subterm count for " + op);
            currentSubCount = op==Op.NEG ? 1 : data[pos++] & 0xFF;
        }
    }

    public boolean interned() {
        return !stop && currentCtl >= MAX_CONTROL_CODES;
    }

    public byte internedID() {
        if (!interned())
            throw new IllegalStateException("Current token is not an interned term");
        return (byte) (currentCtl - MAX_CONTROL_CODES);
    }

    public Op op() {
        if (interned()) throw new IllegalStateException("Current token is interned; no operator available");
        return Op.op(currentCtl);
    }

    public byte opID() {
        if (interned())
            throw new IllegalStateException("Current token is interned; no operator ID available");
        return currentCtl;
    }

    public int dt() {
        return currentDT;
    }

    public int subs() {
        return currentSubCount;
    }

    public void skipSubterm() {
        // If the current token is atomic, nothing to skip.
        if (stop || interned()) return;
        var toSkip = currentSubCount;
        for (var i = 0; i < toSkip; i++)
            skipTerm();
    }

    protected void skipTerm() {
        if (pos >= length) {
            stop = true;
            return;
        }
        var ctl = data[pos++];
        if (ctl >= MAX_CONTROL_CODES) {
            return;
        }
        var op = Op.op(ctl);
        if (op.temporal) {
            if (pos + 4 > length)
                throw new TermException("EOF skipping dt for " + op);
            pos += 4;
        }
        if (pos >= length)
            throw new TermException("EOF skipping subterm count for " + op);
        var count = data[pos++] & 0xFF;
        for (var i = 0; i < count; i++) {
            skipTerm();
        }
    }

    public boolean stop() {
        return stop;
    }

    public int position() {
        return pos;
    }

    public byte[] slice(int start, int end) {
        if (start < 0 || end > length || start > end) {
            throw new IndexOutOfBoundsException();
        }
        var slice = new byte[end - start];
        System.arraycopy(data, start, slice, 0, slice.length);
        return slice;
    }

    public void reset() {
        this.pos = 0;
        this.stop = (this.pos >= this.length);
        if (!stop) {
            readNext();
        }
    }
}

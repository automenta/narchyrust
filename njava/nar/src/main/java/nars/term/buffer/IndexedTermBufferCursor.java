package nars.term.buffer;

import com.google.common.primitives.Ints;
import nars.Op;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

import static nars.Op.DTERNAL;
import static nars.term.buffer.TermBuffer.MAX_CONTROL_CODES;

/**
 * An advanced cursor that extends TermBufferCursor by
 * maintaining nested compound context and providing indexing information.
 */
public class IndexedTermBufferCursor extends TermBufferCursor {

    // Stack to maintain context of nested compounds: tracks remaining subterms and current index
    private static class Context {
        final byte op;
        final int total;  // total subterms expected at this level
        int seen;         // number of subterms already processed
        Context(byte op, int total) { this.op = op; this.total = total; this.seen = -1; }
    }
    private final Deque<Context> contextStack = new ArrayDeque<>();

    public IndexedTermBufferCursor(byte[] buffer) {
        super(buffer, false);
        readNext(); // initialize first token
    }

    @Override
    public boolean next() {
        if (stop) return false;
        // Update parent's context if applicable
        if (!contextStack.isEmpty()) {
            Context parent = contextStack.peek();
            parent.seen++;

            Context n;
            // Pop contexts with all subterms processed
            while ((n = contextStack.peek())!=null && n.seen >= n.total) {
                contextStack.pop();
                n = contextStack.peek();
                if (n!=null)
                    n.seen++;
            }
        }
        readNext();
        return !stop;
    }

    @Override
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
            Op op = Op.op(currentCtl);
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
        // If compound with subterms, push a new context onto the stack with the current operator
        if (currentSubCount > 0) {
            // Use interned check to ensure we're not pushing context for atomic interned tokens
            if (currentCtl < MAX_CONTROL_CODES)
                contextStack.push(new Context(currentCtl, currentSubCount));
        }
    }

    /**
     * Returns the ordinal index of the current subterm within its parent compound.
     * Returns -1 if not within any compound.
     */
    public int sub() {
        // The top context corresponds to the current compound's child being processed
        var top = contextStack.peek();
        return top==null ? -1 : top.seen;
    }

    public byte parentOpID() {
        var top = contextStack.peek();
        return top==null ? -1 : top.op;
    }

    @Nullable
    public Op parentOp() {
        var o = parentOpID();
        return o >= 0 ? Op.op(o) : null;
    }
}

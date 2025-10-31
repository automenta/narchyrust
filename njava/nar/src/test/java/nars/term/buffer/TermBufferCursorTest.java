package nars.term.buffer;

import org.junit.jupiter.api.Test;

import static nars.$.$$c;
import static nars.Op.DTERNAL;

class TermBufferCursorTest {
    @Test
    void test1() {
        var c = new TermBuffer($$c("(&&,a,--b,(c-->d))")).cursor();
        while (!c.stop()) {
            if (c.interned()) {
                var internId = c.internedID();
                System.out.println(c.parentOp() + " " + c.sub() + "\t" + "interned(" + internId + ')');
            } else {
                var op = c.op();
                var dt = c.dt();
                System.out.print(op + (dt!=DTERNAL ? ", dt=" + dt : "") + "\t");
                var n = c.subs();
                System.out.println("subterms=" + n);
                // If we want to skip them entirely:
                //c.skipSubterm();
            }
            c.next();
        }
    }
    @Test
    void test2() {
        var c = new TermBuffer($$c("(&&,a,b)")).cursor();
        while (!c.stop()) {
            if (c.interned()) {
                var internId = c.internedID();
                System.out.println(c.parentOp() + " " + c.sub() + "\t" + "interned(" + internId + ')');
            } else {
                var op = c.op();
                var dt = c.dt();
                System.out.print(op + (dt!=DTERNAL ? ", dt=" + dt : "") + "\t");
                var n = c.subs();
                System.out.println("subterms=" + n);
                // If we want to skip them entirely:
                //c.skipSubterm();
            }
            c.next();
        }
    }
}
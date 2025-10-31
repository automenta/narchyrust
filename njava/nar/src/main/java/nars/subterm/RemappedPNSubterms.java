package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Hashed;
import nars.Op;
import nars.Term;
import nars.term.buffer.TermBuffer;

import java.util.function.Predicate;

import static nars.Op.NEG;
import static nars.io.TermIO.outNegByte;


/** extends RemappedSubterms with virtual negation of some subterms */
public abstract non-sealed class RemappedPNSubterms extends RemappedSubterms<Subterms> implements Hashed {

    /**
     * make sure to calculate hash code in implementation's constructor
     */
    int hash;

    /** make sure to calculate hash code in implementation's constructor */
    RemappedPNSubterms(Subterms base) {
        super(unwrap(base, Op.terms));
    }

    @Override
    public void write(ByteArrayDataOutput out) {
        var s = this.subs();
        out.writeByte(s);
        for (var i = 0; i < s; i++) {
            var x = subMap(i);
            if (x < 0) {
                outNegByte(out);
                x = (byte) -x;
            }
            mapTerm(x).write(out);
        }
    }

    @Override
    public void appendTo(TermBuffer b) {
        var s = this.subs();

        for (var i = 0; i < s; i++) {
            var x = this.subMap(i);
            if (x < 0) {
                b.negStart();
                x = (byte) -x;
            }
            b.append(this.mapTerm(x));
        }
    }
    /** TODO test and test subclasses */
    @Override public int structSubs() {
        var s = super.structSubs();
        return (s & NEG.bit) == 0 && wrapsNeg() ?
                s | NEG.bit : s;
    }

    @Override
    public int complexity() {
        return ref.complexity() + negs();
    }

    @Override
    public int complexityConstants() {
        return ref.complexityConstants() + negs();
    }

    @Override
    public int struct() {
        return super.struct() | (hasNegs() ? NEG.bit : 0);
    }

    @Deprecated protected boolean wrapsNeg() {
        var s = subs();
        for (var i = 0; i < s; i++)
            if (subMap(i) < 0)
                return true;
        return false;
    }

    private boolean hasNegs() {
        var s = subs();
        for (var i = 0; i < s; i++)
            if (subMap(i) < 0)
                return true;
        return false;
    }

    @Override protected int negs() {
        int n = 0, s = subs();
        for (var i = 0; i < s; i++)
            if (subMap(i) < 0)
                n++;
        return n;
    }

    @Override
    public Term sub(int i) {
        return mapTerm(subMap(i));
    }

    @Override
    public boolean ORunneg(Predicate<? super Term> t) {
        var n = subs();
        for (var i = 0; i < n; i++) {
            if (t.test(subUnneg(i)))
                return true;
        }
        return false;
    }

    @Override public final Term subUnneg(int i) {
        return mapTerm(Math.abs(subMap(i)));
    }

    /** @param xy should not equal 0 */
    final Term mapTerm(int xy) {
        //assert (xy != 0);
        var neg = (xy < 0);
        if (neg) xy = -xy;
        var y = ref.sub(xy - 1);
        return neg ? y.neg() : y;
    }

    protected abstract int subMap(int i);

    @Override
    public final int hashCode() {
        return hash;
    }

}
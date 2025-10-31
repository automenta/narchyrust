package nars.deriver.reaction.memoize;

import jcog.data.bit.MetalBitSet;
import nars.Deriver;
import nars.term.control.PREDICATE;

class MemoizeClear extends PREDICATE<Deriver> {

    private final int bits;

    public MemoizeClear(int bits) {
        super("memoizeClear");
        this.bits = bits;
    }

    @Override
    public boolean test(Deriver d) {
        var b = d.bits;
        if (b == null)
            d.bits = MetalBitSet.bits(bits * 2);
        else
            b.clear();
        return true;
    }

    @Override
    public float cost() {
        return 0;
    }
}

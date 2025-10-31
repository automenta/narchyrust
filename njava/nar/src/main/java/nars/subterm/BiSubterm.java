package nars.subterm;

import nars.Term;

import java.util.function.Consumer;

/**
 * Size 2 TermVector
 */
public final class BiSubterm extends CachedSubterms {

    private final Term x, y;

    public BiSubterm(Term x, Term y) {
        super(x, y);
        this.x = x; this.y = y;
    }
    /**
     * uses an array argument so that the input array can be used directly without needing to create a new one when it calls the superclass constructor
     */
    public BiSubterm(Term[] xy) {
        super(xy);
        assert(xy.length == 2);
        this.x = xy[0];
        this.y = xy[1];
    }

    @Override
    public Subterms sorted() {
        return x.compareTo(y) <= 0 ? this : reverse();
    }

    @Override
    public Subterms commuted() {
        return switch (x.compareTo(y)) {
            case  0 -> new UnitSubterm(x);
            case -1 -> this;
            case +1 -> reverse();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public final Term[] arrayClone() {
        return new Term[]{x, y};
    }

    @Override
    public final Term sub(int i) {
		return switch (i) {
			case 0 -> x;
			case 1 -> y;
			default -> throw new ArrayIndexOutOfBoundsException();
		};
    }

    @Override
    public final int subs() {
        return 2;
    }

//    @Override
//    public void forEach(Consumer<? super Term> a, int start, int stop) {
//        switch (start) {
//            case 0:
//                switch (stop) {
//                    case 0: return;
//                    case 1: a.accept(x); return;
//                    case 2: forEach(a); return;
//                }
//                break;
//            case 1:
//                switch (stop) {
//                    case 1: return;
//                    case 2: a.accept(y); return;
//                }
//                break;
//        }
//        throw new ArrayIndexOutOfBoundsException();
//    }

    @Override
    public final void forEach(Consumer<? super Term> a) {
        a.accept(x);
        a.accept(y);
    }

}
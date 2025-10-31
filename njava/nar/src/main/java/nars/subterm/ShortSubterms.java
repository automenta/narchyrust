package nars.subterm;

import jcog.math.NumberException;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.Term;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.CondAtomic;
import nars.term.atom.Int;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.roaringbitmap.RoaringBitmap;

/** Subterms implementation that holds Int's , specified by short[] */
public non-sealed class ShortSubterms extends CachedSubterms implements CondAtomic {

	public final short[] subs;

	private ShortSubterms(short[] subs, SubtermMetadataCollector meta) {
		super(meta);
		this.subs = subs;
		internableKnown = internables = true;
		normalizedKnown = normalized = true;
	}



	public static Subterms the(short[] x) {
		if (x.length == 0) return Op.EmptySubterms;
        var c = new SubtermMetadataCollector();
		for (var i : x) c.add(Int.i(i));
		return new ShortSubterms(x, c);
	}

	/** warning: short range not checked */
	public static Subterms the(int[] x) {
        var s = x.length;
		if (s == 0) return Op.EmptySubterms;
        var c = new SubtermMetadataCollector();
        var y = new short[s];
		for (int j = 0, xLength = x.length; j < xLength; j++) c.add(Int.i(y[j] = (short) x[j]));
		return new ShortSubterms(y, c);
	}
	public static Subterms the(byte[] x) {
        var s = x.length;
		if (s == 0) return Op.EmptySubterms;
        var c = new SubtermMetadataCollector();
        var y = new short[s];
		for (var j = 0; j < s; j++) c.add(Int.i(y[j] = x[j]));
		return new ShortSubterms(y, c);
	}

	/** warning: doesnt guarantee value is within Short's capacity */
	public static Subterms the(RoaringBitmap x) {
        var s = x.getCardinality();
		if (s == 0) return Op.EmptySubterms;
        var c = new SubtermMetadataCollector();
        var y = new short[s];
        var ii = x.getIntIterator();
		for (var j = 0; j < s; j++) c.add(Int.i(y[j] = validShort(ii.next())));
		return new ShortSubterms(y, c);
	}

	private static short validShort(int i) {
		if (i > Short.MAX_VALUE || i < Short.MIN_VALUE) throw new NumberException("exceeds Short range", i);
		return (short)i;
	}

	@Override
	public final Term sub(int i) {
		return Int.i(subs[i]);
	}

	@Override
	public final int subs() {
		return subs.length;
	}

	@Override
	public final boolean equals(Object obj) {
        return this == obj || (obj instanceof ShortSubterms oo ?
			hash == oo.hash && ArrayUtil.equals(subs, oo.subs) :
			obj instanceof Subterms ss && equalTerms(ss));
    }

	public void addAllTo(ShortProcedure each) {
		for (var x : subs) each.value(x);
	}
}
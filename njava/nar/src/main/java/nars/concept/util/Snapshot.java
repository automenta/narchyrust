package nars.concept.util;

import jcog.Util;
import nars.Concept;
import nars.Focus;
import nars.NAR;
import nars.Term;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/** a container for time-stamped expiring cache of data that can be stored in Concept meta maps */
public class Snapshot<X> {

	/** TODO use AtomicFieldUpdater */
	protected final AtomicBoolean busy = new AtomicBoolean(false);

	/** occurrence time after which the data should be invalidated or refreshed */
	protected volatile long nextUpdate = Long.MIN_VALUE;

	/** of type:
	 *     X, SoftReference<X>, WeakReference<X> */
	protected volatile Object value;


	public static @Nullable <X> X get(String id, Term src, int updatePeriod, BiFunction<Concept, X, X> updater, Focus f) {
		return get(id, src, f.time(), updatePeriod, updater, f.nar);
	}

	/** concept()'s the given term in the given NAR */
    public static @Nullable <X> X get(String id, Term src, long now, int updatePeriod, BiFunction<Concept, X, X> updater, NAR nar) {
		Concept c = nar.concept(src);
		return c != null ? get(id, c, now, updatePeriod, updater) : null;
	}

	private static @Nullable <X> X get(String id, Concept src, long now, int updatePeriod, BiFunction<Concept, X, X> updater) {
		return src.<Snapshot<X>>meta(id, Snapshot::new)
			.get(now, updatePeriod, existing -> updater.apply(src, existing));
	}


	public @Nullable X get() {
		Object v = value;
		return v instanceof Reference ? ((Reference<X>) v).get() : (X) v;
	}

	/** here the value may be returned as-is or wrapped in a soft or weak ref */
	protected Object got(X x) {
		return x;
	}

	/** updatePeriod = cycles of cached value before next expiration ( >= 0 )
	 * 			or -1 to never expire */
	public X get(long now, int updatePeriod, UnaryOperator<X> updater) {
		X current = get();
		if (Util.enterAlone(busy)) {
			try {
                if (current == null || now >= this.nextUpdate) {
					current = updater.apply(current);
					this.nextUpdate = updatePeriod >= 0 ? now + updatePeriod : Long.MIN_VALUE /* forever */;
					this.value = got(current);
				}
			} finally {
				Util.exitAlone(busy);
			}
		}
		return current;
	}
}
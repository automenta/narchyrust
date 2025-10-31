package nars.io;

import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import nars.task.util.PriBuffer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * buffers in a Map<> for de-duplication prior to a commit that flushes them as input to NAR
 * does not obey advised capacity
 * TODO find old implementation and re-implement this
 */
public abstract class MapBuffer<X extends Prioritized & Prioritizable> extends PriBuffer.SyncPriBuffer<X> implements Iterable<X> {

	final AtomicLong hit = new AtomicLong(0), miss = new AtomicLong(0);

	public final Map<X, X> map;

	protected MapBuffer(Map<X, X> map) {
		this.map = map;
	}

	@Override
	public Iterator<X> iterator() {
		return map.values().iterator();
	}

	@Override
	public int capacity() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public final boolean put(X n) {
		X p = map.putIfAbsent(n, n);
		if (p != null) {
			if (p != n)
				merge(p, n);
			hit.incrementAndGet();
			return false;
		} else {
			miss.incrementAndGet();
			return true;
		}
	}

	protected abstract void merge(X p, X n);

	/**
	 * TODO time-sensitive
	 */
	@Override
	@Deprecated public final void commit() {
		this.clear(target);
	}

	public void clear(Consumer<X> each) {
//		int num = map.size();
//		if (num > 0) {
//			map.values().removeIf(z->{ each.accept(z); return true; });
//		}
		clear(map, each, Integer.MAX_VALUE);
	}

	public static <X,Y> void clear(Map<X,Y> map, Consumer<Y> each, int limit) {
		var v = map.values().iterator();
		while (v.hasNext()) {
			each.accept(v.next());
			v.remove();
			if (--limit <= 0)
				break;
		}
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

}
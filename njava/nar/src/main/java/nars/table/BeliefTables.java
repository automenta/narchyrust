package nars.table;

import jcog.data.list.Lst;
import jcog.util.ArrayUtil;
import nars.*;
import nars.action.memory.Remember;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * composite of multiple sub-tables
 * the sub-tables and their rank can be modified at runtime.
 */
public class BeliefTables implements BeliefTable, BiConsumer<NALTask, Answer> {

	public final Lst<BeliefTable> tables;

	public BeliefTables(int capacity) {
		tables = new Lst<>(0, new BeliefTable[capacity]);
	}

	public BeliefTables(BeliefTable... b) {
		this(b.length);
		addAll(b);
	}

	/**
	 * stops after the first table accepts it
	 */
	@Override
	public void remember(Remember r) {
		for (var t : tablesArrayForStore())
			if (t == null || t.tryRemember(r))
				break;
	}

	@Override
	public final void match(Answer a) {
		throw new UnsupportedOperationException();
	}

	/** cacheDynamic */
	@Deprecated @Override public void accept(NALTask dynamicToCache, Answer a) {
        var t = mutableTable(tablesArrayForStore(), dynamicToCache.ETERNAL());
		if (t != null)
			t.remember(new RememberCached(dynamicToCache, a));
	}

	public int tableCount() {
		return tables.size();
	}

	private static final class RememberCached extends Remember {
		private final Answer a;

		RememberCached(NALTask y, Answer a) {
			this.a = a;
            this.input = y;
        }

		@Override @Deprecated public NAR nar() {
			return a.nar;
		}
	}

	public BeliefTable[] tablesArray() {
        var tables = this.tables.array();
		return tables == null ? EmptyBeliefTableArray : tables;
	}

	/** TODO filter either eternal or temporal depending on what is being stored */
	BeliefTable[] tablesArrayForStore() {
        var tables = tablesArray();
        var n = tables.length;
		for (var i = 0; i < n; i++) {
			if (tables[i] instanceof LazyBeliefTable l)
				tables[i] = l.get();
		}
		return tables;
	}

	@Override
	public Stream<? extends NALTask> taskStream() {
		return Stream.of(tablesArray()).filter(Objects::nonNull).flatMap(TaskTable::taskStream);
	}

	@Override
	public final boolean remove(NALTask x, boolean delete) {
		return tables.count(t -> t.remove(x, delete)) > 0;
	}

	/**
	 * gets first matching table of the provided type
	 */
	public @Nullable <X extends BeliefTable> X tableFirst(Class<? extends X> c) {
		for (var ii : tablesArray()) {
			if (ii == null) break;
			if (c.isInstance(ii)) return (X) ii;
		}
		return null;
	}

	@Override
	public void forEachTask(long minT, long maxT, Consumer<? super NALTask> x) {
		assert(minT != Long.MIN_VALUE || maxT != Long.MIN_VALUE); //trap ETERNAL..ETERNAL because this expects raw interval

		if (minT == Long.MIN_VALUE && maxT == Long.MAX_VALUE)
			forEachTask(x);
		else
			tables.forEach(t -> t.forEachTask(minT, maxT, x));
	}

	@Override
	public void taskCapacity(int newCapacity) {
		throw new UnsupportedOperationException("can only set capacity to individual tables contained by this");
	}

	@Override
	public void forEachTask(Consumer<? super NALTask> action) {
		tables.forEachWith(TaskTable::forEachTask, action);
	}

	@Override
	public int taskCount() {
		return (int) tables.sumOfInt(TaskTable::taskCount);
	}

	@Override
	public int capacity() {
		throw new UnsupportedOperationException();
	}


	@Override
	@Deprecated
	public final void clear() {
		tables.forEach(TaskTable::clear);
	}

	public final void delete() {
		tables.forEach(TaskTable::delete);
		Arrays.fill(tables.array(), null);
		tables.setArray(EmptyBeliefTableArray);
	}

	/**
	 * the result of this may not necessarily correspond with testing size=0.
	 * isEmpty() means something different in dynamic task table cases.
	 * TODO if any of the tables are dynamic then this can be cached and cause isEmpty() to always return false
	 */
	@Override
	public boolean isEmpty() {
		for (var ii : tablesArray()) {
			if (ii == null) break;
			if (!ii.isEmpty()) return false;
		}
		return true;
	}

	public void addFirst(BeliefTable t) {
		tables.add(0, t);
		tables.trimToSize();
	}

	public void add(BeliefTable t) {
		_add(t);
		tables.trimToSize();
	}

	public final void addAll(BeliefTable[] t) {
		for (var x : t)
			_add(x);
		tables.trimToSize();
	}

	private void _add(BeliefTable t) {
		assert(!(t instanceof BeliefTables)): "TODO inline";
		tables.add(t);
	}

	@Nullable public static BeliefTable mutableTable(BeliefTable[] siblingTables, boolean eternal) {
        var mutable = ArrayUtil.indexOf(siblingTables, BeliefTable.mutableTable(eternal));
		return mutable >= 0 ? siblingTables[mutable] : null;
	}

//	public void add(int i, BeliefTable t) {
//		if (t instanceof BeliefTables) {
//			//inline
//			for (BeliefTable tt : ((BeliefTables)t).tables)
//				tables.add(i++, tt);
//		} else {
//			tables.add(i, t);
//		}
//	}


//    @Override
//    public Task sample(long start, long end, Term template, NAR nar) {
//        Task ete = eternal.sample(start, end, template, nar);
//        if (ete != null && start == ETERNAL)
//            return ete; //eternal sought
//
//        Task tmp = temporal.sample(start, end, template, nar);
//        if (ete == null || tmp != null && tmp.contains(start, end))
//            return tmp; //interval found
//
//        if (tmp == null) return ete;
//        float e = TruthIntegration.eviInteg(ete, start, end, 1);
//        float t = TruthIntegration.eviInteg(tmp, start, end, 1);
//        return nar.random().nextFloat() < (t / Math.max(Float.MIN_NORMAL, (e + t))) ? tmp : ete;
//    }


//    static final int ORDERED = 0;
//    static final int SHUFFLE_FIRST_COME_FIRST_SERVE = 1;
//    static final int SHUFFLE_ROUND_ROBIN = 2;
//    static final int FAIR = 3;
//
//    protected static final int matchMode = FAIR;
//                MetalBitSet.IntBitSet nonEmpty = (MetalBitSet.IntBitSet) MetalBitSet.bits(size);
//                for (int i = 0; i < size; i++)
//                    if (!items[i].isEmpty()) nonEmpty.setFast(i);
//
//                int N = nonEmpty.cardinality();
//                if (N==0) {
//
//                } else if (N ==1) {
//                    match(a, nonEmpty.first(true));
//                } else {
//                    switch (matchMode) {
//                        case ORDERED: {
//                            for (int i = 0; i < size; i++) {
//                                if (nonEmpty.testFast(i)) {
//                                    match(a, items[i]);
//                                    if (a.ttl <= 0)
//                                        break;
//                                }
//                            }
//                            break;
//                        }
//                        case FAIR: {
//                            int each = a.ttl;
//                            for (int i = 0; i < size; i++) {
//                                if (nonEmpty.testFast(i)) {
//                                    a.ttl = each;
//                                    match(a, i);
//                                }
//                            }
//                            break;
//                        }
//
////                        case SHUFFLE_FIRST_COME_FIRST_SERVE: {
////                            int[] ne = nonEmpty.toIntArray();
////                            ArrayUtil.shuffle(ne, a.random());
////                            for (int i = 0; i < N; i++) {
////                                items[ne[i]].match(a);
////                                if (a.ttl <= 0)
////                                    break;
////                            }
////                            break;
////                        }
////
////                        case SHUFFLE_ROUND_ROBIN: {
////                            //fair round robin
////                            int ttlStart = a.ttl;
////                            assert (ttlStart > 0);
////                            int ttlFair = Math.max(1,
////                                (int)Math.ceil(((float)ttlStart) / N)
////                                //ttlStart
////                            );
////                            int[] ne = nonEmpty.toIntArray();
////                            ArrayUtil.shuffle(ne, a.random());
////                            int ttlUsed = 0;
////                            for (int i = 0; i < N; i++) {
////                                a.ttl = ttlFair;
////                                items[ne[i]].match(a);
////                                ttlUsed += ttlFair - a.ttl;
////                                if (ttlUsed >= ttlStart)
////                                    break;
////                            }
////                            break;
////                        }
//                        default:
//                            throw new UnsupportedOperationException();
//                    }
//                }
//
//            }
//        }
//    }

//    /** visit subtables in shuffled order, while predicate returns true */
//    public boolean ANDshuffled(Random rng, Predicate<BeliefTable> e)  {
//        BeliefTable[] items = this.items;
//        if (items == null)
//            return true; //?wtf
//
//        int n = Math.min(size, items.length);
//        switch (n) {
//            case 0:
//                return true;
//            case 1:
//                return e.test(items[0]);
//            case 2: {
//                int i = rng.nextInt(2);
//                return e.test(items[i])  && e.test(items[1 - i]);
//            }
//            case 3: {
//                int i = rng.nextInt(6);
//                int x, y, z;
//                switch (i) {
//                    case 0:
//                        x = 0;
//                        y = 1;
//                        z = 2;
//                        break;
//                    case 1:
//                        x = 0;
//                        y = 2;
//                        z = 1;
//                        break;
//                    case 2:
//                        x = 1;
//                        y = 0;
//                        z = 2;
//                        break;
//                    case 3:
//                        x = 1;
//                        y = 2;
//                        z = 0;
//                        break;
//                    case 4:
//                        x = 2;
//                        y = 0;
//                        z = 1;
//                        break;
//                    case 5:
//                        x = 2;
//                        y = 1;
//                        z = 0;
//                        break;
//                    default:
//                        throw new UnsupportedOperationException();
//                }
//                return e.test(items[x]) && e.test(items[y]) && e.test(items[z]);
//            }
//            default:
//                byte[] order = new byte[n];
//                for (byte i = 0; i < n; i++)
//                    order[i] = i;
//                ArrayUtil.shuffle(order, rng);
//                for (int i = 0; i < n; i++) {
//                    if (!e.test(items[order[i]]))
//                        return false;
//                }
//                return true;
//        }
//
//    }


//        if (Param.ETERNALIZE_FORGOTTEN_TEMPORALS) {
//            if (eternal != EternalTable.EMPTY && !r.forgotten.isEmpty() &&
//                    temporal.size() >= temporal.capacity() - 1 /* some tolerance for full test */) {
//
//                r.forgotten.forEach(t -> {
//                    if (!(t instanceof SignalTask) && !t.isEternal()) {
//                        //TODO maybe sort by evi decreasing
//                        Task e = eternal.eternalize(t, temporal.capacity(), temporal.tableDur(), n);
//                        if (e != null)
//                            eternal.addAt(r, n, e);
//                    }
//                });
//            }
//        }


}
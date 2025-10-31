package nars.term.util.map;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.map.UnifriedMap;
import jcog.data.set.UnenforcedConcatSet;
import nars.Term;
import nars.term.anon.Intrin;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * functionally equivalent to: Map<Term,X>
 * but with special support for AnonID'd terms
 */
public class TermHashMap<X> extends AbstractMap<Term, X> {

    public final ShortObjectHashMap<X> id;
    public final Map<Term, X> other;

    public TermHashMap() {
        this(new ShortObjectHashMap<>(16),
                new UnifriedMap<>(16)
                //new HashMap<>(16)
        );
    }

    public TermHashMap(ShortObjectHashMap<X> idMap, Map<Term, X> otherMap) {
        super();
        id = idMap;
        other = otherMap;
    }

    @Override
    public int size() {
        return id.size() + other.size();
    }

    public void clear() {

        //int sizeBeforeClear = id.size();
        if (!id.isEmpty())
            id.clear();
//        if (sizeBeforeClear > compactThreshold())
//            id.compact();


        other.clear();
        if (other instanceof UnifriedMap u)
            u.trimToSize();


        //other = null;
    }


    @Override
    public Set<Map.Entry<Term, X>> entrySet() {
        boolean hasID = !id.isEmpty();
        boolean hasOther = !other.isEmpty();
        if (!hasID) {
            return hasOther ? other.entrySet() : Collections.EMPTY_SET;
        } else {
            Set<Map.Entry<Term, X>> idEntries = new AnonMapEntrySet<>(id);
            return hasOther ? UnenforcedConcatSet.concat(idEntries, other.entrySet()) : idEntries;
        }
    }

    @Override
    public X compute(Term key, BiFunction<? super Term, ? super X, ? extends X> f) {
        short aid = Intrin.id(key);
        return aid != 0 ?
            id.updateValueWith(aid, () -> f.apply(key, null), (p, k) -> f.apply(k, p), key) :
            other.compute(key, f);

    }

    public X computeIfAbsent(Term key,
                             Function<? super Term, ? extends X> mappingFunction) {
        short aid = Intrin.id(key);
        return aid != 0 ?
                id.getIfAbsentPutWith(aid, mappingFunction::apply, key) :
                other.computeIfAbsent(key, mappingFunction);
    }


    @Override
    public final X get(Object key) {
        short aid = Intrin.id((Term) key);
        return aid != 0 ?
                id.get(aid) :
                other.get(key);
    }

    @Override
    public final X put(Term key, X value) {
        short aid = Intrin.id(key);
        return aid != 0 ?
                id.put(aid, value) :
                other.put(key, value);
    }

    @Override
    public X remove(Object key) {
        short aid = Intrin.id((Term) key);
        return aid != 0 ?
                id.remove(aid) :
                other.remove(key);
    }

    @Override
    public void forEach(BiConsumer<? super Term, ? super X> action) {
        if (!id.isEmpty())
            id.forEachKeyValue((x, y) -> action.accept(Intrin.term(x), y));
        if (!other.isEmpty()) {
            for (Map.Entry<Term, X> entry : other.entrySet()) {
                Term key = entry.getKey();
                X value = entry.getValue();
                action.accept(key, value);
            }
        }
    }

    private static final class AnonMapEntrySet<X> extends AbstractSet<Map.Entry<Term, X>> {
        private final ShortObjectHashMap<X> id;

        AnonMapEntrySet(ShortObjectHashMap<X> id) {
            this.id = id;
        }

        @Override
        public Iterator<Map.Entry<Term, X>> iterator() {
            return Iterators.transform(id.keyValuesView().iterator(), AnonEntry::new);
        }

        @Override
        public int size() {
            return id.size();
        }

    }

    private record AnonEntry<X>(ShortObjectPair<X> x) implements Map.Entry<Term, X> {

        @Override
        public Term getKey() {
            return Intrin.term(x.getOne());
        }

        @Override
        public X getValue() {
            return x.getTwo();
        }

        @Override
        public X setValue(X value) {
            throw new TODO();
        }
    }

}
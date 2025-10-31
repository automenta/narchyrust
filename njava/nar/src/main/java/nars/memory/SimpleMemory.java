package nars.memory;

import jcog.data.map.MRUMap;
import nars.Concept;
import nars.Term;
import nars.concept.PermanentConcept;

import java.util.Collections;
import java.util.Map;

/** simple concept index that uses a LRU-evicting LinkedHashMap */
public class SimpleMemory extends MapMemory {

    private final MyMRUMap _map;

    public SimpleMemory(int capacity) {
        this(capacity,  false);
    }

    public SimpleMemory(int capacity, boolean threadSafe) {
        this(capacity, 0.75F /*DEFAULT_LOAD_FACTOR*/, threadSafe);
    }

    protected SimpleMemory(int capacity, float loadFactor, boolean threadSafe) {
        super();
        map(synchronizedIf(_map = new MyMRUMap(capacity, loadFactor), threadSafe));
    }

    private static <X,Y> Map<X,Y> synchronizedIf(Map<X,Y> m, boolean threadSafe) {
        return threadSafe ? Collections.synchronizedMap(m) : m;
    }

    public void capacity(int c) {
        _map.setCapacity(c);
    }

    private final class MyMRUMap extends MRUMap<Term, Concept> {

        MyMRUMap(int capacity, float loadFactor) {
            super(capacity, loadFactor);
        }

        @Override
        protected void onEvict(Map.Entry<Term, Concept> entry) {
            Concept c = entry.getValue();
            if (c instanceof PermanentConcept)
                put(entry.getKey(), c); //reinsert
            else
                onRemove(c);
        }
    }
}
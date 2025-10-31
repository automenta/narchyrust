package nars.term.util.conj;

import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Optimized replacement for IntObjectHashMap<ConjTree> used in ConjTree.
 * This implementation is tailored for the specific usage patterns in ConjTree:
 * - Small number of entries (often just 1-2)
 * - Need to efficiently get min key
 * - Need to iterate over entries
 * - Need to support summing over values
 */
public class TimeMap<T> implements Iterable<IntObjectPair<T>> {

    private static final int DEFAULT_CAPACITY = 4;

    // Arrays to store keys and values
    private int[] keys;
    private Object[] values;
    private int size;

    public TimeMap() {
        this(DEFAULT_CAPACITY);
    }

    public TimeMap(int capacity) {
        this.keys = new int[capacity];
        this.values = new Object[capacity];
        this.size = 0;
    }

    /**
     * Returns the value associated with the given key, or null if not found
     */
    @SuppressWarnings("unchecked")
    public @Nullable T get(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return (T) values[i];
            }
        }
        return null;
    }

    /**
     * Put a key-value pair into the map. If the key already exists, the value is updated.
     * @return the previous value associated with key, or null if there was no previous value
     */
    @SuppressWarnings("unchecked")
    public @Nullable T put(int key, T value) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                T oldValue = (T) values[i];
                values[i] = value;
                return oldValue;
            }
        }

        ensureCapacity(size + 1);
        keys[size] = key;
        values[size] = value;
        size++;
        return null;
    }

    /**
     * Gets the value associated with the key if present, or computes it using the given function
     * @param key the key to look up
     * @param valueFunction the function to compute the value if not present
     * @return the existing value or the newly computed value
     */
    @SuppressWarnings("unchecked")
    public T getIfAbsentPut(int key, Supplier<T> valueFunction) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return (T) values[i];
            }
        }

        T newValue = valueFunction.get();
        ensureCapacity(size + 1);
        keys[size] = key;
        values[size] = newValue;
        size++;
        return newValue;
    }

    /**
     * Removes all entries for which the predicate returns true
     * @param predicate the predicate to test keys against
     * @return the number of entries removed
     */
    public int removeIf(IntPredicate predicate) {
        int removed = 0;
        for (int i = 0; i < size; i++) {
            if (predicate.test(keys[i])) {
                removeAt(i);
                i--;
                removed++;
            }
        }
        return removed;
    }

    /**
     * Remove the entry at the given index
     */
    private void removeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        System.arraycopy(keys, index + 1, keys, index, size - index - 1);
        System.arraycopy(values, index + 1, values, index, size - index - 1);
        size--;
        
        // Avoid memory leak
        values[size] = null;
    }

    /**
     * Counts entries where the provided bifunction returns true
     */
    public <A> int countWith(BiFunction<T, A, Boolean> function, A arg) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            T value = (T) values[i];
            if (function.apply(value, arg)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sums the result of applying the function to each value
     */
    public int sumOfInt(ToIntFunction<T> function) {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            T value = (T) values[i];
            sum += function.applyAsInt(value);
        }
        return sum;
    }

    /**
     * Gets the first value in the map
     */
    @SuppressWarnings("unchecked")
    public T getFirst() {
        if (size == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return (T) values[0];
    }

    /**
     * Gets the last value in the map
     */
    @SuppressWarnings("unchecked")
    public T getLast() {
        if (size == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return (T) values[size - 1];
    }

//    /**
//     * Returns the pair with the minimum key
//     */
//    public IntObjectPair<T> minBy(java.util.function.Function<IntObjectPair<T>, Integer> function) {
//        if (size == 0) {
//            throw new NoSuchElementException("Map is empty");
//        }
//
//        int minIdx = 0;
//        int minValue = keys[0];
//
//        for (int i = 1; i < size; i++) {
//            if (keys[i] < minValue) {
//                minValue = keys[i];
//                minIdx = i;
//            }
//        }
//
//        @SuppressWarnings("unchecked")
//        T value = (T) values[minIdx];
//        return PrimitiveTuples.pair(keys[minIdx], value);
//    }

    /**
     * Returns the minimum key in the map
     */
    public int min() {
        if (size == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        
        int min = keys[0];
        for (int i = 1; i < size; i++) {
            if (keys[i] < min) {
                min = keys[i];
            }
        }
        return min;
    }

    /**
     * Returns the size of the map
     */
    public int size() {
        return size;
    }

    /**
     * Returns whether the map is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clears the map
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            values[i] = null;
        }
        size = 0;
    }

    /**
     * Applies the function to each value
     */
    public void forEachValue(java.util.function.Consumer<T> function) {
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            T value = (T) values[i];
            function.accept(value);
        }
    }

    /**
     * Returns an iterable view of the key-value pairs
     */
    public Iterable<IntObjectPair<T>> keyValuesView() {
        return this;
    }

    /**
     * Ensure the capacity is at least the specified amount
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity > keys.length) {
            int newCapacity = Math.max(keys.length * 2, minCapacity);
            int[] newKeys = new int[newCapacity];
            Object[] newValues = new Object[newCapacity];
            
            System.arraycopy(keys, 0, newKeys, 0, size);
            System.arraycopy(values, 0, newValues, 0, size);
            
            keys = newKeys;
            values = newValues;
        }
    }

    /**
     * Returns the only entry in the map
     */
    public IntObjectPair<T> getOnly() {
        if (size != 1) {
            throw new IllegalStateException("Map does not contain exactly one entry");
        }
        @SuppressWarnings("unchecked")
        T value = (T) values[0];
        return PrimitiveTuples.pair(keys[0], value);
    }

    @Override
    public Iterator<IntObjectPair<T>> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public IntObjectPair<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                @SuppressWarnings("unchecked")
                T value = (T) values[index];
                IntObjectPair<T> pair = PrimitiveTuples.pair(keys[index], value);
                index++;
                return pair;
            }
        };
    }

}
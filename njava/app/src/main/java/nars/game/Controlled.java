package nars.game;

import jcog.data.iterator.ArrayIterator;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

/** indicates that an implementation can add to a provided Game sensors and/or actions */
public interface Controlled<C> {

    /** implementations can teach the controller the actions that control this instance
     *  TODO separate start(), stop() methods
     *  TODO ensure non-repeat calls
     * */
    void controlStart(C g);

    /** can return:
     *       null (default)
     *       a specific target instance TODO check for cycles
     *       a collection of objects or views
     *       a map of a hierarchical objects and views
     */
    @Nullable default Object view() {
        return null;
    }

    public static Pair<String,Iterable<?>> group(String name, Object... x) {
        return Tuples.pair(name, ArrayIterator.iterable(x));
    }
}
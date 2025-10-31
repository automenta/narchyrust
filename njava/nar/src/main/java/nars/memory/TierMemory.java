package nars.memory;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import nars.Concept;
import nars.NAR;
import nars.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** proxies to 1 of N sub-memories according to key */
public class TierMemory extends Memory {

    final Memory[] sub;
    private final ToIntFunction<Term> which;

    public TierMemory(ToIntFunction<Term> which, Memory... sub) {
        this.sub = sub; assert(sub.length > 1);
        this.which = which;
    }

    protected final int subMemory(Term key) { return which.applyAsInt(key); }

    @Override
    public Concept get(Term key, boolean createIfMissing) {
        return sub(key).get(key, createIfMissing);
    }

    public final Memory sub(Term key) {
        return sub[subMemory(key)];
    }

    @Override
    public void start(NAR nar) {
        super.start(nar);
        subStream().forEach(s -> s.start(nar));
    }

    @Override
    public void set(Term src, Concept target) {

        sub(src).set(src, target);
    }

    @Override
    public void clear() {
        for (Memory m : sub) m.clear();
    }

    @Override
    public int size() {
        return Util.sum(Memory::size, ArrayIterator.iterable(sub));
    }

    @Override
    public String summary() {
        return subStream().map(Memory::summary).collect(Collectors.joining(","));
    }

    @Override
    public @Nullable Concept remove(Term entry) {
        return sub(entry).remove(entry);
    }

    @Override
    public Stream<Concept> stream() {
        return subStream().flatMap(Memory::stream);
    }

    private Stream<Memory> subStream() {
        return Stream.of(sub);
    }
}
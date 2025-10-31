package nars.table;

import nars.BeliefTable;

import java.util.function.Supplier;

public class LazyBeliefTable extends EmptyBeliefTable {

    private final Supplier<BeliefTable> builder;
    private int cap;

    public static class EternalLazyBeliefTable extends LazyBeliefTable {

        public EternalLazyBeliefTable(Supplier<BeliefTable> builder) {
            super(builder);
        }
    }

    public LazyBeliefTable(Supplier<BeliefTable> builder) {
        this.builder = builder;
    }

    public BeliefTable get() {
        var y = builder.get();
        y.taskCapacity(cap);
        return y;
    }

    @Override public void taskCapacity(int newCapacity) {
        cap = newCapacity;
    }

}
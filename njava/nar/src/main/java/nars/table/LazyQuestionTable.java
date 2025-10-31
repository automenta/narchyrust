package nars.table;

import nars.table.question.EmptyQuestionTable;
import nars.table.question.QuestionTable;

import java.util.function.Supplier;

public class LazyQuestionTable extends EmptyQuestionTable {
    private final Supplier<QuestionTable> builder;
    private int cap;

    public LazyQuestionTable(Supplier<QuestionTable> builder) {
        this.builder = builder;
    }

    public QuestionTable get() {
        var y = builder.get();
        y.taskCapacity(cap);
        return y;
    }

    @Override
    public void taskCapacity(int newCapacity) {
        cap = newCapacity;
    }

}
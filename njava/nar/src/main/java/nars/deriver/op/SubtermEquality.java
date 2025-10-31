package nars.deriver.op;

import nars.*;
import nars.term.control.PREDICATE;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.Arrays;
import java.util.List;

import static nars.Premise.Belief;
import static nars.Premise.Task;

public final class SubtermEquality extends PREDICATE<Deriver> {

    private final ObjectBooleanPair<byte[]>[] paths;

    public SubtermEquality(List<ObjectBooleanPair<byte[]>> paths) {
        super($.func("SubtermEquality", $.p(paths.stream().map(z ->
                $.p(z.getTwo() ? Task : Belief, $.p(z.getOne()))
        ).toList())));
        if (paths.size() < 2) throw new UnsupportedOperationException();
        this.paths = paths.toArray(ObjectBooleanPair[]::new);
        Arrays.sort(this.paths, (x, y) -> {
            int l = Integer.compare(x.getOne().length, y.getOne().length); //prefer shorter length
            if (l != 0) return l;
            int b = Boolean.compare(y.getTwo(), x.getTwo()); //reversed, to prefer task
            if (b != 0) return b;
            return Arrays.compare(x.getOne(), y.getOne()); //exhaustive compare
        });
    }

    @Override
    public boolean test(Deriver d) {
        Premise premise = d.premise;
        Term x = get(paths[0], premise);
        if (x == null)
            return false;
        int n = paths.length;
        for (int i = 1; i < n; i++) {
            Term y = get(paths[i], premise);
            if (y == null || !x.equals(y))
                return false;
        }
        return true;
    }

    private static Term get(ObjectBooleanPair<byte[]> first, Premise p) {
        NALTask t = first.getTwo() ? p.task() : p.belief();
        return t == null ? null : t.term().subSafe(first.getOne());
    }

    @Override
    public float cost() {
        return 0.2f;
    }

}

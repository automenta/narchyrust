package nars.game.sensor;

import jcog.data.list.Lst;
import nars.$;
import nars.NAR;
import nars.Term;
import nars.game.Game;
import nars.term.util.TermException;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;

import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;

/** the selector lambda can return -1 to choose none, or Integer.MAX_VALUE to choose all
 * TODO mode for bitvector choice
 * */
public class SelectorSensor extends VectorSensor {

    private final List<SignalComponent> choices;
    private final IntIntToObjectFunction<Term> pattern;
    private final IntToIntFunction value;
    private final int values;
    private final int arity;

    public SelectorSensor(Term id, Term[] values, IntSupplier value, NAR nar) {
        this(id,
            values.length, 1,
            (ignored)->value.getAsInt(),
            pattern(id, values),
            nar);
    }

    private static IntIntToObjectFunction<Term> pattern(Term pattern, Term[] values) {
        Variable V = $.varDep(1);

        if (!pattern.containsRecursively(V))
            throw new TermException("missing variable: " + V, pattern);

        return (a, v) -> pattern.replace(V, values[v]);
    }

    public SelectorSensor(Term id, int values, int arity, IntToIntFunction value, IntIntToObjectFunction<Term> pattern, NAR n) {
        super(id, arity * values);

        assert(values > 1);
        assert(arity >= 1);

        choices = new Lst<>(arity * values);
        this.arity = arity;
        this.pattern = pattern;
        this.value = value;
        this.values = values;

    }

    @Override
    public void start(Game game) {
        super.start(game);

        for (int a = 0; a < arity; a++) {
            for (int v = 0; v < values; v++) {
                int V = v, A = a;
                choices.add(component(pattern.value(a, v),
                    () -> {
                        int y = value.applyAsInt(A);
                        return y == V || y == Integer.MAX_VALUE ? 1 : 0;
                }, game.nar));
            }
        }

    }

    @Override
    public Iterator<SignalComponent> iterator() {
        return choices.iterator();
    }
}
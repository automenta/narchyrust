package nars.deriver.reaction;

import jcog.data.list.Lst;
import nars.Deriver;
import nars.action.Action;
import nars.control.Cause;
import nars.term.Termed;
import nars.term.control.PREDICATE;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * an intermediate representation of a premise rule
 * with fully expanded opcodes
 *
 * instantiated for each NAR, because it binds the conclusion steps to it
 *
 * anything non-NAR specific (static) is done in PremiseDeriverSource as a
 * ready-made template to make constructing this as fast as possible
 * in potentially multiple NAR instances later
 *
 * TODO implementations:
 *      --UI event/submission/warnings
 *      --experience replay
 */
public abstract class Reaction<X> implements Termed {

    public Set<PREDICATE<X>> pre;

    @Nullable
    public String tag;

    protected Reaction() {

    }

    public abstract Action action(Cause<Reaction<Deriver>> why);

    public abstract Class<? extends Reaction<Deriver>> type();

    protected final Set<PREDICATE<X>> conditions() {
        var c = this.pre;
        return c!=null ? c :
            (this.pre = compileConditions());
    }

    abstract protected Set<PREDICATE<X>> compileConditions();

    @Deprecated public List<PREDICATE<X>> conditionsSortedByCost() {
        var l = new Lst<>(conditions());
        l.sort(PREDICATE.CostIncreasing);
        return l;
    }

}
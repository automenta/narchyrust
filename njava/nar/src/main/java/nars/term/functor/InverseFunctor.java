package nars.term.functor;

import nars.Term;
import nars.eval.Evaluation;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;

/** functors can optionally implement this interface to provide backward-solved solutions for missing values */
public interface InverseFunctor {

    /** called when evaluation an (x=y):
     *   'x' is an instance of this functor
     *   'y' is some value
     *  if invoked, the returned value rewrites the containing equal(x,y)
     *  else return null
     */
    @Nullable Term equality(Evaluation e, Compound x, Term y);
}
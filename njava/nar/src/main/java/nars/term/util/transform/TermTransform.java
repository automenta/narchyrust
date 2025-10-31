package nars.term.util.transform;

import nars.Term;

import java.util.function.UnaryOperator;

@FunctionalInterface public interface TermTransform extends UnaryOperator<Term> {

    //Term apply(Term t);

    TermTransform NullTransform = x -> x;

}
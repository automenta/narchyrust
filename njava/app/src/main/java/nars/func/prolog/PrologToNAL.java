package nars.func.prolog;

import alice.tuprolog.NumberTerm;
import alice.tuprolog.Struct;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;
import jcog.TODO;
import jcog.Util;
import jcog.data.map.UnifriedMap;
import nars.$;
import nars.Term;
import nars.term.Termlike;
import nars.term.var.Variable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Map;
import java.util.stream.StreamSupport;

import static nars.Op.CONJ;

public enum PrologToNAL {
    ;

    public static final Term QUESTION_GOAL = $.atomic("?-");

    public static Iterable<Term> N(Theory t) {
        return N((Iterable)t);
    }

    public static Iterable<Term> N(Iterable<alice.tuprolog.Term> t) {
        

        return StreamSupport.stream(t.spliterator(), false).map(PrologToNAL::N).toList();








    }

    private static Term N(alice.tuprolog.Term t) {
        switch (t) {
            case Struct s -> {
                String name = s.name();
                switch (name) {
                    /* "=:=": identity(X,Y) */
                    /* "=\=": --identity(X,Y) */
                    /* "=": unify(X,Y) */
                    /* "<": lessThan(X,Y) etc */

                    case ":-":
                        assert (s.subs() == 2);
                        Term pre = N(s.sub(1));
                        Term post = N(s.sub(0));


                        Term impl = $.impl(pre, post);
                        pre = impl.sub(0);
                        post = impl.sub(1);

                        if (pre.varQuery() > 0 && post.varQuery() > 0) {
                            MutableSet<Variable> prev = new UnifiedSet();
                            pre.ANDrecurse(Termlike::hasVarQuery, a -> {
                                if (a.VAR_QUERY())
                                    prev.add((Variable) a);
                                return true;
                            });
                            MutableSet<Variable> posv = new UnifiedSet();
                            post.ANDrecurse(Termlike::hasVarQuery, a -> {
                                if (a.VAR_QUERY())
                                    posv.add((Variable) a);
                                return true;
                            });

                            MutableSet<Variable> common = prev.intersect(posv);
                            int cs = common.size();
                            if (cs > 0) {
                                Map<Term, Term> x = new UnifriedMap(cs);
                                for (Variable c : common)
                                    x.put(c, $.varIndep(c.toString().substring(1)));

                                impl = impl.replace(x);
                            }
                        }

                        return impl;
                    case ",":
                        return CONJ.the(N(s.sub(0)), N(s.sub(1)));
                    default:
                        Term atom = $.atomic(name);
                        int arity = s.subs();
                        if (arity == 0) {
                            return atom;
                        } else {
                            return $.inh(
                                    $.p(Util.arrayOf(i -> N(s.sub(i)), 0, arity, Term[]::new)),
                                    atom);
                        }
                }
            }
            case Var var -> {
                return $.varQuery(var.name());
            }
            case NumberTerm.Int anInt -> {
                return $.the(anInt.intValue());
            }
            case null, default -> throw new TODO(t + " (" + t.getClass() + ") untranslatable");
        }
    }

}
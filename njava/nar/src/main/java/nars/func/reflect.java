/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.func;

import nars.$;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

/**
 * Produces canonical "Reflective-Narsese" representation of a parameter target
 *
 * @author me
 */
public enum reflect {
	;

	static final Atomic REFLECT_OP = Atomic.atomic("reflect");

    /**
     * <(*,subject,object) --> predicate>
     */
    public static @Nullable Term sop(Term subject, Term object, Term predicate) {
        return $.inh($.p(reflect(subject), reflect(object)), predicate);
    }

    public static @Nullable Term sopNamed(String operatorName, Compound s) {
        

        















        
        return $.inh($.p(reflect(s.sub(0)), reflect(s.sub(1))), $.quote(operatorName));
    }

    public static @Nullable Term sop(Subterms s, Term predicate) {
        return $.inh($.p(reflect(s.sub(0)), reflect(s.sub(1))), predicate);
    }

    public static @Nullable Term sop(String operatorName, Subterms c) {
        int n = c.subs();
        Term[] m = new Term[n];
        for (int i = 0; i < n; i++) {
            if ((m[i] = reflect(c.sub(i))) == null)
                return null;
        }

        

        















        
        return $.inh($.p(m), $.quote(operatorName));
    }

    public static @Nullable Term reflect(Term t) {
        if (t.subs() == 0)
            return t;

        var o = t.op();
        return switch (o) {
            case PROD -> t;
            default -> sop(o.toString(), t.subterms());
        };
    }

}
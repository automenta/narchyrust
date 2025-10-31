package nars.term.var;

import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.IntrinSubterms;
import nars.term.anon.Intrin;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import static nars.term.atom.Bool.Null;

public final class CommonVariable extends UnnormalizedVariable {

    /** provided by a sorted AnonVector */
    @Deprecated private final short[] vars; //TODO compute dynamically from bytes()

    public static Variable common(Variable A, Variable B) {
        int cmp = A.compareTo(B);
        if (cmp == 0)
            throw new WTF();
        if (cmp > 0) {
            Variable x = A;
            A = B;
            B = x;
        }

        IntrinSubterms z;
        boolean ac = A instanceof CommonVariable;
        boolean bc = B instanceof CommonVariable;
        if (!ac && !bc) {
            z = new IntrinSubterms(A, B);
        } else {
            ShortHashSet t;
            if (ac && bc) {
                short[] aaa = ((CommonVariable) A).vars;
                short[] bbb = ((CommonVariable) B).vars;
                t = new ShortHashSet(aaa.length + bbb.length);
                t.addAll(aaa);
                t.addAll(bbb);
            } else {

                CommonVariable C;
                Variable V;
                if (ac) {
                    C = ((CommonVariable) A);
                    V = B;
                } else {
                    C = ((CommonVariable) B);
                    V = A;
                }

                t = new ShortHashSet(C.vars.length+1);
                t.addAll(C.vars);

                if (!t.add(Intrin.id(V)))
                    return C; //subsumed
            }

            if (t.size() <= 2)
                throw new WTF();

            short[] tt = t.toSortedArray();

            if (ac && Arrays.equals(tt, ((CommonVariable)A).vars))
                return A; //subsumed
            if (bc && Arrays.equals(tt, ((CommonVariable)B).vars))
                return B; //subsumed

            z = new IntrinSubterms(tt);
        }
        return new CommonVariable(A.op(), z);
    }

    /** vars must be sorted */
    private CommonVariable(Op type, IntrinSubterms vars) {
        super(type, key(type, vars));
        this.vars = vars.subterms;
    }

    public static Term parse(Variable... cv) {
        if (cv.length < 2 || cv.length > NAL.unify.UNIFY_COMMON_VAR_MAX)
            return Null;

        SortedSet<Variable> s = new TreeSet<>(); //new MetalTreeSet();
        Collections.addAll(s, cv);

        int ss = s.size();
        return ss < 2 || ss > NAL.unify.UNIFY_COMMON_VAR_MAX ?
            Null :
            new CommonVariable(s.first().op(), new IntrinSubterms(s.toArray(Op.EmptyTermArray)));
    }

    @Override
    public String toString() {
        return new String(key(op(), common()));
    }

    public IntrinSubterms common() {
        return new IntrinSubterms(vars);
    }

    private static byte[] key(Op o, IntrinSubterms vars) {

        int n = vars.subs();
        DynBytes b = new DynBytes(1 + n * 2 /* perfect size if all variable id's < 10 */);
        b.writeByte(o.ch);
        for (int i = 0; i < n; i++) {
            NormalizedVariable v = (NormalizedVariable) vars.sub(i);

            b.writeByte(v.op().ch);
            b.writeNumber(v.id(), 36);
        }
        return b.compact();
    }


    /** includes but doesnt equal */
    public boolean contains(Variable x) {
        return x.opID()==opID() && _contains(x);
    }

    public boolean _contains(Variable x) {
        return x instanceof CommonVariable ?
            ArrayUtil.containsAll(vars, ((CommonVariable)x).vars) :
            ArrayUtil.contains(vars, Intrin.id(x));
    }


}
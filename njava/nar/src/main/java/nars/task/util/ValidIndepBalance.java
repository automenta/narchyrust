package nars.task.util;

import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import jcog.signal.meter.SafeAutoCloseable;
import nars.NALTask;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.Statements;
import static nars.Op.VAR_INDEP;

public final class ValidIndepBalance implements BiPredicate<ByteList, Term>, SafeAutoCloseable {

    private final UnifriedMap<Term, List<byte[]>> indepVarPaths;
    private final Lst</* length, */ byte[]> statements;
    private ByteByteHashMap count;

    private ValidIndepBalance(int vars) {
        indepVarPaths = new UnifriedMap<>(vars);
        statements = new Lst<>(0, new byte[vars][]);
    }

    private static boolean valid(Subterms x, boolean safe) {
        //return x.AND(safe ? s-> valid(s, true) : s->valid(s,false));
        //assert(x.hasAny(Statements)): "InDep variables must be subterms of statements";
        if (!x.hasAny(Statements))
            return indepFail(x, safe);

        for (Term y : x) {
            if (!valid(y, safe))
                return false;
        }
        return true;
    }

    public static boolean valid(Term x, boolean safe) {

        if (x instanceof Atomic)
            return true;

        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        int v = x.varIndep();
        return switch (v) {
            case 0  -> true;
            case 1  -> NALTask.fail(x, "singular indep variable", safe);
            default -> validN(x, safe, v);
        };
    }

    private static boolean validN(Term x, boolean safe, int v) {
        //indep appears in both? test for balance:
        Subterms xx = x.subterms();
        return balanceTest(x, xx) ?
                (v > 2 ? validN(x, v, safe) : valid2(xx, safe))
                :
                valid(xx, safe);
    }

    private static boolean balanceTest(Term x, Subterms xx) {
        return x.STATEMENT() && xx.subUnneg(0).hasVarIndep() && xx.subUnneg(1).hasVarIndep();
    }

    /**
     * special optimized 2-ary case; assumes both subterms of xy (length==2) have the indep var, and they must be equal
     */
    private static boolean valid2(Subterms xy, boolean safe) {
        Term x = xy.subUnneg(0), y = xy.subUnneg(1);
        return
            x.equals(y)
            ||
            firstIndepVar(x).equals(firstIndepVar(y))
            ||
            (!safe && NALTask.fail(x, "singular indep variable", safe));
    }

    private static Variable firstIndepVar(Term x) {
        if (x instanceof Variable v) return v;
        else if (x instanceof Atomic) return null;
        else return new FirstIndepVar().apply(x);
    }

    private static boolean validN(Term t, int vars /* estimate */, boolean safe) {
        try (ValidIndepBalance v = new ValidIndepBalance(vars)) {
            return v.test(t, safe);
        }
    }

    @Override
    public boolean test(ByteList path, Term indepVarOrStatement) {
        if (!path.isEmpty()) {
            ((indepVarOrStatement.VAR_INDEP()) ?
                indepVarPaths.getIfAbsentPut(indepVarOrStatement, () -> new Lst<>(2))
                :
                statements
            ).add(path.toArray());
        }
        return true;
    }

    private boolean test(Term t, boolean safe) {
        t.pathsTo(preFilter, x -> x.hasAny(Statements | VAR_INDEP.bit), this);
        return _validIndepBalance(t) || indepFail(t, safe);
    }

    private boolean _validIndepBalance(Term t) {
        if (indepVarPaths.anySatisfy(p -> p.size() < 2))
            return false;

        int nStatements = statements.size();
        if (nStatements > 1)
            statements.sortThisByInt(i -> i.length);

        boolean rootIsStatement = t.STATEMENT();

        byte[][] ss = statements.array();
        var count = this.count = new ByteByteHashMap(nStatements);
        nextPath: for (List<byte[]> varPaths : indepVarPaths) {

            if (!count.isEmpty())
                count.clear();

            for (byte[] path : varPaths) {

                if (rootIsStatement && branchOr((byte) -1, path[0]) == 0b11)
                    continue nextPath;

                int pSize = path.length;

                nextStatement: for (byte k = 0; k < nStatements; k++) {
                    byte[] statement = ss[k];
                    int statementPathLength = statement.length;
                    if (statementPathLength > pSize)
                        break;

                    for (int i = 0; i < statementPathLength; i++)
                        if (path[i] != statement[i])
                            break nextStatement;

                    if (branchOr(k, path[statementPathLength]) == 0b11)
                        continue nextPath;
                }
            }
            return false;
        }
        return true;
    }

    private static boolean indepFail(Termlike t, boolean safe) {
        return NALTask.fail(t, "indep variable unbalanced across statement", safe);
    }

    private byte branchOr(byte key, byte branch) {
        byte branchBit = (byte) (1 << branch);
        return count.updateValue(key, branchBit, x -> (byte) (x | branchBit));
    }

    @Override
    public void close() {
        indepVarPaths.delete();
        statements.delete();
        count = null;
    }

    private static final Predicate<Term> preFilter = (x) -> {
        int xo = x.opID();
        return (xo == VAR_INDEP.id)
                ||
                (x instanceof Compound && ((((1 << xo) & Statements) != 0 && x.hasVarIndep())));
    };

    private static final class FirstIndepVar implements BiPredicate<Term, Compound> {
        private Variable xv;

        @Override
        public boolean test(Term z, Compound zz) {
            if (z.VAR_INDEP()) {
                xv = (Variable) z;
                return true;
            }
            return false;
        }

        @Nullable public Variable apply(Term x) {
            return x.ORrecurse(Termlike::hasVarIndep, this, null) ?
                    xv : null;
        }
    }
}
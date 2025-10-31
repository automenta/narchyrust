package nars.deriver.op;

import nars.$;
import nars.Deriver;
import nars.Term;
import nars.premise.DerivingPremise;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.control.PREDICATE;
import nars.term.var.Variable;
import nars.unify.UnifyConstraint;

/** describes a derivation term unification procedure,
 *  including term patterns to unify, and constraints */
public final class Termify extends ProxyCompound {

    public final Term taskPattern;
    public final Term beliefPattern;
    public final UnifyConstraint<Deriver.PremiseUnify>[] constraints;
    public final int fwd;

    private final boolean taskEqualsBeliefPattern;

    public Termify(Term taskPattern, Term beliefPattern, UnifyConstraint<Deriver.PremiseUnify>[] constraints) {
        super((Compound)$.p(taskPattern, beliefPattern, $.p(PREDICATE.ids(constraints))));

        this.taskPattern = taskPattern;
        this.beliefPattern = beliefPattern;
        this.taskEqualsBeliefPattern = taskPattern.equals(beliefPattern);
        this.constraints = constraints;
        this.fwd = fwd(taskPattern, beliefPattern);
    }

    public void unify(DerivingPremise p, Deriver.PremiseUnify u, Term T, Term B) {
        u.clear(constraints);
        u.unify(
            taskPattern, beliefPattern,
            T, B,
            fwd >= 0,
            taskEqualsBeliefPattern,
            p
        );
    }

    /**
     * task,belief or belief,task ordering heuristic
     * +1 = task first, -1 = belief first, 0 = doesnt matter
     **/
    private static int fwd(Term T, Term B) {

        if (T.equals(B)) {
            return 0;
        } else {

            //which is a variable, match the other since it will be more specific and fail faster
            if (T instanceof Variable && B instanceof Variable) return 0;
            if (B instanceof Variable) return +1;
            if (T instanceof Variable) return -1;

            //which contains the other
            boolean Tb = T instanceof Compound && T.containsRecursively(B);
            if (Tb) return -1; //belief first as it is a part of Task
            boolean Bt = !Tb && B instanceof Compound && B.containsRecursively(T);
            if (Bt) return +1; //task first as it is a part of Belief

            boolean tCommute = T.COMMUTATIVE(), bCommute = B.COMMUTATIVE();
            if (bCommute && !tCommute) return +1;
            if (tCommute && !bCommute) return -1;

            //which has fewer variables
            int bv = B.vars(), tv = T.vars();
            if (tv > bv) return -1;
            if (bv > tv) return +1;
//
//                // which is more specific in its constant structure
//                int taskBits = Integer.bitCount(T.structure() & ~Op.Variables);
//                int belfBits = Integer.bitCount(B.structure() & ~Op.Variables);
//                if (taskBits > belfBits) return +1;
//                if (belfBits > taskBits) return -1;

            //first which is smaller
            return Integer.compare(B.complexity(), T.complexity());
        }

    }


}
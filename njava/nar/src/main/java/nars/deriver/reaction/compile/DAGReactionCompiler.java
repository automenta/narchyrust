package nars.deriver.reaction.compile;

import jcog.data.list.Lst;
import jcog.data.set.ArrayHashSet;
import nars.Deriver;
import nars.NAR;
import nars.action.Action;
import nars.deriver.reaction.Reaction;
import nars.deriver.reaction.ReactionModel;
import nars.term.control.*;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single-class DAG-based ReactionCompiler with:
 * 1) Multi-branch (Fork) construction from Reactions,
 * 2) DAG interning by signature,
 * 3) Full + partial factoring of top-level common predicates,
 * 4) IF-based splitting with coverage*balance*(1/cost) heuristic,
 * 5) Iterative optimization until no further cost improvement.
 */
public final class DAGReactionCompiler extends ReactionCompiler {

    /*=====================================================================*/
    /* DAG Interning                                                       */
    /*=====================================================================*/
    private static final Map<String, Node> internMap = new ConcurrentHashMap<>();

    private static Node intern(Node n) {
        //var sig = n.signature(); return internMap.merge(sig, n, (e, N) -> e);
        return n;
    }

    private static void gatherPredicates(Node n, int idx,
                                         HashMap<Object, IntArrayList> posMap,
                                         HashMap<Object, IntArrayList> negMap) {
        if (n instanceof TerminalNode t && t.value instanceof PREDICATE<?> p) {
            if (p instanceof NOT<?> notp)
                addOcc(negMap, notp.cond, idx);
            else
                addOcc(posMap, p, idx);
        } else if (n instanceof AndNode a) {
            for (var c : a.n)
                gatherPredicates(c, idx, posMap, negMap);
        }
    }

    private static Node removePred(Node b, Object pred, boolean isNeg) {
        if (b instanceof AndNode a) {
            var keep = new Lst<Node>(a.n.length);
            for (var c : a.n)
                if (!_removePred(pred, isNeg, c))
                    keep.add(removePred(c, pred, isNeg));
            return intern(new AndNode(keep));
        } else if (_removePred(pred, isNeg, b))
            return TERMINAL_TRUE;
        else
            return b;
    }

    private static boolean _removePred(Object pred, boolean isNeg, Node c) {
        return c instanceof TerminalNode t && t.value instanceof PREDICATE<?> q && _removePred(pred, isNeg, q);
    }

    private static boolean _removePred(Object pred, boolean isNeg, PREDICATE<?> q) {
        return (!isNeg && q == pred) || (isNeg && q instanceof NOT<?> nn && nn.cond == pred);
    }

    /*=====================================================================*/
    /* Helpers: Flatten AND, Short-circuit, etc.                           */
    /*=====================================================================*/

    static Node[] flattenAnd(Node[] kids) {
        var out = new Lst<Node>(kids.length);
        for (var k : kids) {
            if (k instanceof AndNode a) Collections.addAll(out, a.n);
            else out.add(k);
        }
        return out.toArray(EMPTY_NODE_ARRAY);
    }

    static Node[] shortCircuitAnd(Node[] kids) {
        var out = new Lst<Node>();
        for (var n : kids) {
            var sig = n.signature();
            if ("false".equals(sig)) return new Node[]{ n };
            if (!"true".equals(sig)) out.add(n);
        }
        return out.toArray(EMPTY_NODE_ARRAY);
    }

    private static void addOcc(HashMap<Object, IntArrayList> map, Object key, int idx) {
        map.computeIfAbsent(key, k -> new IntArrayList()).add(idx);
    }

    private static boolean disjoint(IntArrayList x, IntArrayList y) {
        if (x.size() > y.size()) {
            var tmp = x; x = y; y = tmp; }
        var st = new IntHashSet(x.size());
        for (var i = 0; i < x.size(); i++) st.add(x.get(i));
        for (var i = 0; i < y.size(); i++) if (st.contains(y.get(i))) return false;
        return true;
    }

    private static double ifScore(int posCount, int negCount, int total, double predCost) {
        if (total == 0) return 0;
        var coverage = (posCount + negCount) / (double) total;
        var balance = Math.min(posCount, negCount) / (double) Math.max(posCount, negCount);
        var costFactor = 1.0 / (1.0 + predCost);
        return coverage * balance * costFactor;
    }

    private static double costOfPred(Object p) {
        return (p instanceof PREDICATE<?> pp) ? pp.cost() : 1.0;
    }

    @Override
    public ReactionModel compile(ArrayHashSet<Reaction<Deriver>> reactions, NAR n) {
        if (reactions.isEmpty()) throw new IllegalStateException("No reactions");

        var rx = reactions.list;
        var s = rx.size();
        var actions = new Action[s];
        ForkNode forkRoot = new ForkNode();

        for (var i = 0; i < s; i++) {
            var r = rx.get(i);
            var act = r.action(n.causes.newCause(r));
            actions[i] = act;

            var nPre = r.pre.size();
            var chain = new Node[nPre + 1];
            var j = 0;
            for (var preCond : r.pre)
                chain[j++] = new TerminalNode(preCond);
            chain[nPre] = new TerminalNode(act);

            forkRoot.branches.add(intern(new AndNode(chain)));
        }
        var optimized = optimize(forkRoot);
        return new ReactionModel(optimized.predicate(), actions);
    }

    /*=====================================================================*/
    /* Repeated Optimization                                               */
    /*=====================================================================*/
    private Node optimize(Node root) {
        double oldCost, newCost = root.cost();
        int iter = 0;
        do {
            oldCost = newCost;
            root = factorRecursively(root).transform();
            root = ifRecursively(root).transform();
            newCost = root.cost();
            iter++;
        } while (newCost < oldCost);
        return root;
    }


    /*=====================================================================*/
    /* Full + Partial Factoring                                            */
    /*=====================================================================*/

    private Node factorRecursively(Node n) {
        if (n instanceof ForkNode f) {
            // Partial factoring first
            n = partialFactorInFork(f);
            // Full factoring
            n = fullFactorInFork((ForkNode) n);
            // Factor sub-branches
            var ff = (ForkNode) n;
            ff.branches.replaceAll(this::factorRecursively);
            return n;
        } else if (n instanceof AndNode a) {
            for (var i = 0; i < a.n.length; i++) {
                a.n[i] = factorRecursively(a.n[i]);
            }
        } else if (n instanceof IfNode i) {
            i.cond = factorRecursively(i.cond);
            i.trueBranch = factorRecursively(i.trueBranch);
            i.falseBranch = factorRecursively(i.falseBranch);
        }
        return n;
    }

    /**
     * Partial factoring: find top-level TerminalNodes that appear in ≥2 branches.
     * Factor each out into AND(term, subForkOfThatSubset), plus the branches that don't contain it.
     * Repeatedly choose the best coverage among remaining top-level terminals until no improvements.
     */
    private Node partialFactorInFork(ForkNode fork) {
        boolean changed;
        do {
            changed = false;
            var map = new HashMap<Object, IntArrayList>();
            for (var i = 0; i < fork.branches.size(); i++) {
                var tops = gatherTopTerminals(fork.branches.get(i));
                for (var term : tops) addOcc(map, term, i);
            }
            Object bestTerm = null;
            var bestCount = 1; // needs ≥2 to factor
            for (var e : map.entrySet()) {
                var c = e.getValue().size();
                if (c > bestCount) {
                    bestCount = c;
                    bestTerm = e.getKey();
                }
            }
            if (bestTerm != null) {
                fork = factorOutSubset(fork, bestTerm, map.get(bestTerm));
                changed = true;
            }
        } while (changed);
        return fork;
    }

    /**
     * Factor out bestTerm from the subset of branches that have it, leaving others.
     */
    private ForkNode factorOutSubset(ForkNode f, Object best, IntArrayList subset) {

        var y = new ForkNode();
        var subFork = new ForkNode();

        var yBranch = y.branches;
        var subBranch = subFork.branches;

        var fb = f.branches.size();
        for (var i = 0; i < fb; i++) {
            var b = f.branches.get(i);
            if (subset.contains(i)) subBranch.add(removeAll(b, best));
            else                    yBranch.add(b);
        }

        // Build AND(bestTerm, newForkOfSubset)
        yBranch.add(intern(new AndNode(new TerminalNode(best), subFork)));

        return y;
    }

    /**
     * Full factoring: if there's a top-level TerminalNode in all branches, hoist it out into AND.
     */
    private static Node fullFactorInFork(ForkNode fork) {
        if (fork.branches.size() <= 1) return fork;
        HashSet<Object> common = null;
        for (var branch : fork.branches) {
            var tops = gatherTopTerminals(branch);
            if (common == null) common = tops;
            else common.retainAll(tops);
            if (common.isEmpty()) break;
        }
        if (common.isEmpty()) return fork;

        // Remove each from the branches, build AND(common, newFork)
        var newFork = new ForkNode();
        List<Node> newBr = newFork.branches;
        for (var b : fork.branches)
            newBr.add(removeAll(b, common));

        var chain = new Node[common.size() + 1];
        var idx = 0;
        for (var c : common) chain[idx++] = new TerminalNode(c);
        chain[common.size()] = newFork;
        return intern(new AndNode(chain));
    }

    private static HashSet<Object> gatherTopTerminals(Node n) {
        var set = new HashSet<>();
        if (n instanceof AndNode a) {
            for (var c : a.n) {
                if (c instanceof TerminalNode t) set.add(t.value);
            }
        } else if (n instanceof TerminalNode t) {
            set.add(t.value);
        }
        return set;
    }
    private static Node removeAll(Node b, Object singleTerm) {
        // Remove singleTerm if found top-level
        if (b instanceof AndNode a) {
            var keep = new Lst<Node>();
            for (var c : a.n) {
                if (!(c instanceof TerminalNode t) || !singleTerm.equals(t.value))
                    keep.add(removeAll(c, singleTerm));
            }
            return intern(new AndNode(keep));
        } else if (b instanceof TerminalNode t) {
            if (singleTerm.equals(t.value))
                return new TerminalNode("true");
        }
        return b;
    }

    private static Node removeAll(Node b, HashSet<Object> manyTerms) {
        if (b instanceof AndNode a) {
            var keep = new Lst<Node>();
            for (var c : a.n) {
                if (!(c instanceof TerminalNode t) || !manyTerms.contains(t.value))
                    keep.add(removeAll(c, manyTerms));
            }
            return intern(new AndNode(keep));
        } else if (b instanceof TerminalNode t) {
            if (manyTerms.contains(t.value)) return TERMINAL_TRUE;
        }
        return b;
    }

    private static final TerminalNode TERMINAL_TRUE = new TerminalNode("true");

    /*=====================================================================*/
    /* IF Splitting                                                        */
    /*=====================================================================*/

    private static Node ifRecursively(Node n) {
        if (n instanceof ForkNode f) {
            f.branches.replaceAll(DAGReactionCompiler::ifRecursively);
            var candidate = tryIf(f);
            return (candidate != null) ? candidate : f;
        } else if (n instanceof AndNode a) {
            for (var i = 0; i < a.n.length; i++) {
                a.n[i] = ifRecursively(a.n[i]);
            }
        } else if (n instanceof IfNode i) {
            i.cond = ifRecursively(i.cond);
            i.trueBranch = ifRecursively(i.trueBranch);
            i.falseBranch = ifRecursively(i.falseBranch);
        }
        return n;
    }

    private static Node tryIf(ForkNode fork) {
        var b = fork.branches;
        var bs = b.size();
        if (bs < 2) return null;
        var posMap = new HashMap<Object, IntArrayList>();
        var negMap = new HashMap<Object, IntArrayList>();
        for (var i = 0; i < bs; i++) {
            gatherPredicates(b.get(i), i, posMap, negMap);
        }
        Object bestPred = null;
        var bestScore = 0.0;
        var total = bs;

        for (var e : posMap.entrySet()) {
            var p = e.getKey();
            var pPos = e.getValue();
            var pNeg = negMap.get(p);
            if (pNeg != null && disjoint(pPos, pNeg)) {
                var sc = ifScore(pPos.size(), pNeg.size(), total, costOfPred(p));
                if (sc > bestScore) {
                    bestScore = sc;
                    bestPred = p;
                }
            }
        }
        if (bestPred == null) return null;

        IntArrayList pPos = posMap.get(bestPred), pNeg = negMap.get(bestPred);
        List<Node> left = new Lst<>(), right = new Lst<>(), others = new Lst<>();
        for (var i = 0; i < total; i++) {
            var bi = b.get(i);
            var inPos = pPos.contains(i);
            var inNeg = pNeg.contains(i);
            if (inPos && !inNeg) left.add(removePred(bi, bestPred, false));
            else if (inNeg && !inPos) right.add(removePred(bi, bestPred, true));
            else others.add(bi);
        }
        var cond = intern(new TerminalNode(bestPred));
        var ifNode = new IfNode(cond, new ForkNode(), new ForkNode());
        ((ForkNode) ifNode.trueBranch).branches.addAll(left);
        ((ForkNode) ifNode.falseBranch).branches.addAll(right);

        var combined = new ForkNode();
        combined.branches.addAll(others);
        combined.branches.add(ifNode);

        return (combined.cost() < fork.cost()) ? combined : null;
    }

    /*=====================================================================*/
    /* Node Hierarchy                                                      */
    /*=====================================================================*/
    private interface Node {
        double cost();

        Node transform();

        String signature();

        /**
         * Transform Node tree to PREDICATE<Deriver>
         */
        default PREDICATE<Deriver> predicate() {
            switch (this) {
                case TerminalNode t -> {
                    var val = t.value;
                    return (PREDICATE<Deriver>)val;
                }
                case AndNode a -> {
                    List<PREDICATE<Deriver>> preds = new Lst<>(a.n.length);
                    for (var child : a.n) preds.add(child.predicate());
                    return AND.the(preds);
                }
                case ForkNode f -> {
                    SortedSet<PREDICATE<Deriver>> preds = new TreeSet<>();
                    for (var branch : f.branches) preds.add(branch.predicate());
                    return FORK.fork(preds);
                }
                case IfNode i -> {
                    return IF.the(i.cond.predicate(), i.trueBranch.predicate(), i.falseBranch.predicate());
                }
                case NotNode n -> {
                    return n.n.predicate().neg();
                }
                default -> throw new UnsupportedOperationException();
            }
        }
    }

    private static final class TerminalNode implements Node {
        final Object value;
        private String sig = null;

        TerminalNode(Object v) {
            value = v;
        }

        @Override
        public double cost() {
            var c = (value instanceof PREDICATE<?> p) ? p.cost() : 0.5;
            if (!Double.isFinite(c))
                return 0.5f;
            return c;
        }

        @Override
        public Node transform() {
            return this;
        }

        @Override
        public String signature() {
            var s = sig;
            if (s == null)
                s = this.sig = value.toString();
            return s;
        }
    }

    private static final class NotNode implements Node {
        Node n;

        NotNode(Node c) {
            n = c;
        }

        @Override
        public double cost() {
            return 0.2 + n.cost();
        }

        @Override
        public Node transform() {
            n = n.transform();
            return intern(this);
        }

        @Override
        public String signature() {
            return "NOT[" + n.signature() + "]";
        }
    }

    private static final class AndNode implements Node {
        Node[] n;

        AndNode(List<Node> l) {
            this(l.toArray(EMPTY_NODE_ARRAY));
        }

        AndNode(Node... c) {
            n = c;
        }

        @Override
        public double cost() {
            var sum = 0.0;
            for (var x : n) {
                var xc = x.cost();
                if (Double.isFinite(xc))
                    sum += xc;
            }
            return sum + 0.1;
        }

        @Override
        public Node transform() {
            for (var i = 0; i < n.length; i++)
                n[i] = n[i].transform();
            n = flattenAnd(n);
            n = shortCircuitAnd(n);
            return intern(this);
        }

        @Override
        public String signature() {
            var sb = new StringBuilder(1024).append("AND[");
            for (var i = 0; i < n.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(n[i].signature());
            }
            return sb.append(']').toString();
        }
    }

    private static final class ForkNode implements Node {
        final Lst<Node> branches = new Lst<>();

        @Override
        public double cost() {
            var c = 0.3;
            for (int i = 0, size = branches.size(); i < size; i++) {
                var x = branches.get(i).cost();
                if (Double.isFinite(x))
                    c += x;
            }
            return c;
        }

        @Override
        public Node transform() {
            branches.replaceAll(Node::transform);
            return intern(this);
        }

        @Override
        public String signature() {
            var parts = new TreeSet<String>();
            for (var b : branches) parts.add(b.signature());

            var sb = new StringBuilder(1024).append("FORK[");
            int i = 0;
            for (var p : parts) {
                if (i > 0) sb.append(',');
                sb.append(p);
                i++;
            }

            return sb.append(']').toString();
        }
    }

    private static final class IfNode implements Node {
        Node cond, trueBranch, falseBranch;

        IfNode(Node c, Node t, Node f) {
            cond = c;
            trueBranch = t;
            falseBranch = f;
        }

        @Override
        public double cost() {
            return 0.7 + cond.cost() + trueBranch.cost() + falseBranch.cost();
        }

        @Override
        public Node transform() {
            cond = cond.transform();
            trueBranch = trueBranch.transform();
            falseBranch = falseBranch.transform();
            return intern(this);
        }

        @Override
        public String signature() {
            return "IF{" + cond.signature() + "|"
                    + trueBranch.signature() + "|"
                    + falseBranch.signature() + "}";
        }
    }

    private static final Node[] EMPTY_NODE_ARRAY = new Node[0];

}
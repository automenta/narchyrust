package nars.deriver.reaction.compile;

import jcog.WTF;
import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import jcog.data.set.ArrayHashSet;
import jcog.tree.perfect.TrieNode;
import nars.Deriver;
import nars.NAL;
import nars.NAR;
import nars.action.Action;
import nars.control.Cause;
import nars.deriver.reaction.Reaction;
import nars.deriver.reaction.ReactionModel;
import nars.term.control.AND;
import nars.term.control.FORK;
import nars.term.control.PREDICATE;
import nars.term.control.ProfiledPREDICATE;
import nars.term.util.map.TermPerfectTrie;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import static java.lang.Float.POSITIVE_INFINITY;
import static java.lang.Float.isFinite;
import static jcog.Util.maybeEqual;


public class TrieReactionCompiler extends ReactionCompiler {

    private static void add(Reaction<Deriver> r, Lst<PREDICATE<Deriver>> pre, ArrayDeque<Cause<Reaction<Deriver>>> causes, Action[] hows, short i, TermPerfectTrie<PREDICATE<Deriver>, Action> paths) {

        var a = r.action(causes.removeFirst());

        pre.addFast((hows[i] = a)/*.deferred()*/);

        var added = paths.put(pre, a);
        if (added != null)
            throw new WTF("duplicate reaction:\n\t" + pre + "\n\t:=" + a.why.name + "\n\t =" + added.why.name);
    }

    private static <X> PREDICATE<X> compileInner(TrieNode<List<PREDICATE<X>>, Action> node) {
        var fanout = node.childCount();
        if (fanout == 0) {
            return compileEmpty(node);
        } else {
            var bb = new ArrayHashSet<PREDICATE<X>>(fanout);

            for (var n : node) {
                var branch = compileSeq(n);

                bb.add(branch);
            }

            return compileFork(bb.list);
            //return compileSwitch(bb, 2);
        }
    }

    @Nullable
    private static <X> PREDICATE<X> compileEmpty(TrieNode<List<PREDICATE<X>>, Action> node) {
        var cond = node.seq();
        return cond != null ? AND.the(cond) : null;
    }

    @Nullable
    private static <X> PREDICATE<X> compileSeq(TrieNode<List<PREDICATE<X>>, Action> n) {

        var conseq = n.hasChildren() ? compileInner(n) : null;

        var seq = n.seq().subList(n.start(), n.end());

        var b = conseq != null ? new Lst<>(seq) : seq;

        if (conseq != null) b.add(conseq);

        return AND.the(b);
    }

    private static <X> PREDICATE<X> compile(TermPerfectTrie<PREDICATE<X>, Action> paths) {
        var c = compileInner(paths.root);

        //this seems to factor additional conditions
        //TODO examine FORK branches that can be compiled
        c = c.transform(x ->
            x instanceof FORK f ? compileFork(f) : x);

        if (NAL.derive.AND_COMPILE) {
            //experimental bytecode generation
            c = c.transform(x ->
                x instanceof AND a ? ANDCompiler.compile(a) : x);
        }

        if (NAL.derive.PROFILE)
            c = c.transform(ProfiledPREDICATE.wrap());

        return c;
    }

    private static <X> @Nullable PREDICATE<X> compileFork(FORK<X> x) {
        return maybeEqual(
            compileFork(new Lst<>(x.branches()))
        , x);
    }


    private static <X> PREDICATE<X> compileFork(List<PREDICATE<X>> branches) {
        return CompileFork2.compileFork(branches);
        //return CompileFork1.compileFork(branches);
    }

    @Override
    public ReactionModel compile(ArrayHashSet<Reaction<Deriver>> reactions, NAR n) {
        var s = reactions.size();
        assert (s > 0);
        var hows = new Action[s];
        var paths = new TermPerfectTrie<PREDICATE<Deriver>, Action>(s);

        var causes = n.causes.newCauses(reactions.list.stream());

        var freq = freq(reactions);
        Comparator<PREDICATE> sort = (a, b) -> {
            if (a == b) return 0;
            var ab = Float.compare(
                    freq.getIfAbsent(_unneg(b), POSITIVE_INFINITY),
                    freq.getIfAbsent(_unneg(a), POSITIVE_INFINITY)); //decreasing
            //var ucc = Float.compare(_unneg(a).cost(), _unneg(b).cost());
            return ab != 0 ? ab : a.compareTo(b);
        };

        short i = 0;
        for (var r : reactions.list) {
            var pre = new Lst<PREDICATE<Deriver>>(0, new PREDICATE[r.pre.size() + 1]);
            pre.addAll(r.pre);
            pre.sort(sort);
            add(r, pre, causes, hows, i++, paths);
        }

        if (paths.isEmpty())
            throw new WTF("no reactions added");

        return new ReactionModel(compile(paths), hows);
    }

    private enum CompileFork2 {
        ;

        static <X> PREDICATE<X> compileFork(List<PREDICATE<X>> branches) {
            return switch (branches.size()) {
                case 0 -> null;
                case 1 -> branches.getFirst();
                default -> {
                    var c = condsRepeated(branches);
                    if (!c.isEmpty()) {
                        var bs = sort(c);
                        var y = factor(branches, bs);
                        if (y.size() < branches.size())
                            yield compileFork(y);
                    }
                    yield FORK.fork(branches);
                }
            };
        }


        private static <X> Stream<PREDICATE<X>> conditionsFlat(PREDICATE<X> b) {
            return switch (b) {
                case AND<X> and -> and.conditions().stream().flatMap(CompileFork2::conditionsFlat);

                // TODO, are there additional components in downstream FORK's like there are in AND's?
                //                case FORK fork -> {
                //                    Stream<PREDICATE<X>> ss = Streams.stream(fork.branches());
                //                    yield ss.flatMap(CompileFork2::components);
                //                }

                case null -> throw new NullPointerException();
                default -> Stream.of(b);
            };
        }

        private static <X> Map<PREDICATE<X>, IntHashSet> condsRepeated(List<PREDICATE<X>> branches) {
            var m = new HashMap<PREDICATE<X>, IntHashSet>();
            var n = branches.size();
            for (int b = 0; b < n; b++) {
                var B = b;
                Stream.of(branches.get(B))
                        .flatMap(CompileFork2::conditionsFlat)
                        .filter(z -> z.cost() < POSITIVE_INFINITY)
                        .forEach(c ->
                            m.computeIfAbsent(/*_unneg*/c,
                            k -> new IntHashSet(1)).add(B)
                        );
            }
            m.values().removeIf(z -> z.size() < 2);
            return m;
        }

        private static <X> List<Entry<PREDICATE<X>, IntHashSet>> sort(
                Map<PREDICATE<X>, IntHashSet> commonSubexprs) {
            ToDoubleFunction<Entry<PREDICATE<X>, IntHashSet>> score =
                    e -> factoringBenefit(e.getKey(), e.getValue().size());
            var factoringBenefitIncreasing = Comparator.comparingDouble(score).reversed();
            return commonSubexprs.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .sorted(factoringBenefitIncreasing)
                    .toList();
        }

        private static <X> double factoringBenefit(PREDICATE<X> predicate, int occurrences) {
            return predicate.cost() * (occurrences-1);
        }

        private static <X> List<PREDICATE<X>> factor(
                List<PREDICATE<X>> branches,
                List<Entry<PREDICATE<X>, IntHashSet>> conds) {

            if (conds.isEmpty())
                return branches;

            var factored = new Lst<>(branches);

            var processedIndices = new IntHashSet();
            IntPredicate processed = processedIndices::contains;

            /* recycled */
            var subfactored = new Lst<PREDICATE<X>>();
            var toRemove = new IntArrayList();

            for (Entry<PREDICATE<X>, IntHashSet> entry : conds) {
                var common = entry.getKey();
                var indices = entry.getValue();

                if (indices.anySatisfy(processed) ||
                        factoringBenefit(common, indices.size()) <= 0)
                    continue;


                for (int index : indices.toArray()) {
                    if (index < factored.size()) {
                        // index is in bounds
                        var branch = factored.get(index);
                        if (branch != null) {
                            subfactored.add(andWithout(branch, common));
                            toRemove.add(index);
                            processedIndices.add(index);
                        }
                    }
                }
                if (!subfactored.isEmpty()) {
                    toRemove.sortThis(); //to be safe?
                    toRemove.reverseThis();
                    toRemove.forEach(factored::nullify);
                    factored.add(AND.the(common, compileFork(subfactored)));
                    toRemove.clear();
                    subfactored.clear();
                }
            }

            factored.removeNulls();

            return factored;
        }

        private static <X> PREDICATE<X> andWithout(PREDICATE<X> branch, PREDICATE<X> common) {
            if (branch instanceof AND a) {
                var conditions = new Lst<>(a.conditions());
                if (conditions.remove(common))
                    return AND.the(conditions);
            } else if (branch.equals(common)) // || branch.equals(_unneg(common))) {
                return PREDICATE.TRUE;

            throw new IllegalStateException();
        }
    }

    @Deprecated private static class CompileFork1 {

        private static <X> PREDICATE<X> compileFork(List<PREDICATE<X>> x) {
            if (x == null)
                return null;

            UnifriedMap<PREDICATE, SubCond> branches = null;
            do {
                var n = x.size();
                if (n > 1) {

                    if (branches == null) branches = new UnifriedMap<>(n);
                    else branches.clear();

                    for (var b = 0; b < n; b++)
                        SubCond.bumpCond(x.get(b), b, branches);

                    if (!branches.isEmpty()) {
                        SubCond best = null;
                        for (var subCond : branches.values()) {
                            if (subCond.size() >= 2) {
                                if ((best == null || SubCond.subCondComparator.compare(subCond, best) > 0))
                                    best = subCond;
                            }
                        }
                        if (best != null) {
                            factor(x, best);
                            n = x.size();
                        } else
                            branches.clear();
                    }
                }

                if (n == 0) return null;
                if (n == 1) return x.getFirst();

            } while (!branches.isEmpty());

            return FORK.fork(x);
        }

        private static <X> void factor(List<PREDICATE<X>> x, SubCond best) {
            PREDICATE<X> common = best.p;
            SortedSet<PREDICATE<X>> bundle = null;
            var i = 0;
            var xx = x.iterator();
            while (xx.hasNext()) {
                var px = xx.next();
                if (best.branches.contains(i)) {
                    xx.remove();
                    if (px instanceof AND<X> pxa) {
                        if (bundle == null) bundle = new TreeSet<>();

                        //TODO optimize
                        //TODO scan recursively?
                        var pxSub = pxa.conditions();
                        pxSub.remove(common);

                        bundle.add(AND.the(pxSub));
                    }
                }
                i++;
            }
            if (bundle != null)
                x.add(AND.the(common, compileFork(new Lst<>(bundle))));
        }

        private static class SubCond {

            private static final Comparator<SubCond> subCondComparator = Comparator.comparingDouble(
                    SubCond::costIfBranch);

            final PREDICATE p;
            final RoaringBitmap branches = new RoaringBitmap();
            private final float pCost;

            private SubCond(PREDICATE p) {
                this.p = p;
                this.pCost = p.cost();
            }

            static void bumpCond(PREDICATE p, int branch, Map<PREDICATE, SubCond> conds) {
                if (p instanceof AND a) {
                    for (var pp : a.conditions())
                        add((PREDICATE) pp, branch, conds); //first layer only
                } else {
                    if (isFinite(p.cost()))
                        add(p, branch, conds);
                }
            }

            private static void add(PREDICATE p, int branch, Map<PREDICATE, SubCond> conds) {
                conds.compute(p, (xx, e) -> {
                    if (e == null)
                        e = new SubCond(xx);
                    e.branches.add(branch);
                    return e;
                });
            }

            @Override
            public String toString() {
                return p + " x " + branches;
            }

            int size() {
                return branches.getCardinality();
            }

            float costIfBranch() {
                return size() / (1 + pCost);
            }
        }


    }

    public static final float pathCost = 0.001f;


}
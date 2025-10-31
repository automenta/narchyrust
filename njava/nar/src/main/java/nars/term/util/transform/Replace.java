package nars.term.util.transform;

import jcog.Util;
import jcog.data.list.Lst;
import nars.Term;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.term.atom.IntrinAtomic;
import nars.term.builder.TermBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;


public enum Replace { ;

    public static Term replace(Term x, Map<? extends Term, Term> m, TermBuilder B) {
        return x instanceof Compound c ?
            replaceCompound(c, m, B) :
            replaceAtomic((Atomic)x, m);
    }

    public static Term replace(Term x, Term from, Term to, TermBuilder B) {
        return x instanceof Atomic ?
                replaceAtomic(x, from, to) :
                replaceCompound((Compound) x, from, to, B);
    }

    private static Term replaceAtomic(Term x, Term from, Term to) {
        return from instanceof Atomic && x.equals(from) ? to : x;
    }

    public static TermTransform replace(Map<Term,Term> m, TermBuilder B) {
        return switch (m.size()) {
            case 0 -> TermTransform.NullTransform;
            case 1 -> replace1(m, B);
            case 2 -> replace2(m, B);
            default -> new Replace.MapSubstN(Map.copyOf(m), B);
        };
    }

    private static TermTransform replace2(Map<Term,Term> m, TermBuilder B) {
        return new Replace.MapSubst2(Util.firstTwoEntries(new Term[4], m.entrySet()), B);
    }

    private static TermTransform replace1(Map<Term, Term> m, TermBuilder B) {
        Map.Entry<Term, Term> first = Util.firstEntry(m.entrySet());
        return replacing(first.getKey(), first.getValue(), B);
    }

    private static Term replaceN(Compound x, Map<? extends Term,Term> m, int ms, TermBuilder B) {
        Lst<Term> valid = null;
        var kStruct = 0;
        for (var e : m.entrySet()) {
            var k = e.getKey();

            var ks = k.struct();
            if (!x.impossibleSubStructure(ks) && !x.impossibleSubComplexity(k.complexity())) {
                if (valid == null) valid = new Lst<>(ms);
                //TODO else if (valid.size() >=2) //.. the list wont be used for MapN
                valid.addFast(k);
                kStruct |= ks;
            }
            ms--;
        }
        if (valid==null)
            return x;
        var validN = valid.size();
        return switch (validN) {
            case 1 -> {
                var a = valid.getFirst();
                yield replaceCompound(x, a, m.get(a), B);
            }
            case 2 -> {
                Term a = valid.get(0), b = valid.get(1);
                yield new MapSubst2(a, m.get(a), b, m.get(b), kStruct, B).apply(x);
            }
            default ->
                //TODO build key filter to sub-map only the applicable keys
                    new MapSubstN(m, kStruct, B).apply(x);
//                return Unverified.verify(new MapSubstN(m, kStruct, B) {
//
//                    @Override
//                    protected Term _applyCompound(Compound x, int yDt) {
//                        Term y = applyCompoundUnverified(x, x.op(), yDt);
//                        //        y = Term.nullIfNull(Unverified.verify(y, builder()));
//                        return y;
//                    }
//                }.apply(x), B);
        };
    }


    private static Term replaceCompound(Compound x, Term from, Term to, TermBuilder B) {
        return (from instanceof Compound && from.equals(x)) ? to :
                replacing(from, to, B).apply(x);
    }

    private static RecursiveTermTransform replacing(Term from, Term to, TermBuilder B) {
        return switch (from) {
            case IntrinAtomic i -> new SubstIntrinAtomic(i, to, B);
            case Atomic a -> new SubstAtomic(a, to, B);
            case null, default -> new SubstCompound((Compound) from, to, B);
        };
    }

    private static Term replace1(Term x, Map<? extends Term, Term> m, TermBuilder B) {
        var e = Util.firstEntry(m.entrySet());
        return replace(x, e.getKey(), e.getValue(), B);
    }

    private static Term replace2(Compound x, Map<? extends Term,Term> m, TermBuilder B) {
        var ab = Util.firstTwoEntries(new Term[4], m.entrySet());

        var xi = x.impossibleSubTerm();
        return xi.test(ab[0]) ?
            replaceCompound(x, ab[2], ab[3], B)
            :
            (xi.test(ab[2]) ?
                replaceCompound(x, ab[0], ab[1], B) :
                    new MapSubst2(ab, B).apply(x));
    }


    private static Term replaceAtomic(Atomic x, Map<? extends Term,Term> m) {
        var y = m.get(x);
        return y == null ? x : y;
    }

    private static Term replaceCompound(Compound x, Map<? extends Term, Term> m, TermBuilder B) {
        var ms = m.size();
        return switch (ms) {
            case 0 -> x; //shouldnt happen
            case 1  -> replace1(x, m, B);
            default -> replaceN(x, m, B, ms);
        };
    }

    private static Term replaceN(Compound x, Map<? extends Term,Term> m, TermBuilder B, int ms) {
        var y = m.get(x);
        return y != null ? y : (ms == 2 ?
                replace2(x, m, B) :
                replaceN(x, m, ms, B));
    }

    static final class MapSubst2 extends MapSubstWithStructFilter {
        final Term ay, by;
        final Predicate<Term> ax, bx;

        MapSubst2(Term[] ab, TermBuilder B) {
            this(ab[0], ab[1], ab[2], ab[3], B);
        }

        MapSubst2(Term ax, Term ay, Term bx, Term by, TermBuilder B) {
            this(ax, ay, bx, by, ax.struct() | bx.struct(), B);
        }

        MapSubst2(Term ax, Term ay, Term bx, Term by, int xStructure, TermBuilder B) {
            super(xStructure, B);
            this.ax = ax.equals();
            this.ay = ay;
            this.bx = bx.equals();
            this.by = by;
//            if ((ay.equals(bx)) || (by.equals(ax)))
//                throw new TermException(MapSubst2.class.getSimpleName() + " interlock", $.p(ax, ay, bx, by));
        }

        @Override
        public @Nullable Term xy(Term t) {
            if (ax.test(t)) return ay;
            else if (bx.test(t)) return by;
            else return null;
        }
    }

    static final class MapSubstN extends MapSubstWithStructFilter {
        private final Map<? extends Term,Term> xy;

        static int struct(Iterable<? extends Term> t) {
            var s = 0;
            for (var x : t)
                s |= x.struct();
            return s;
        }

        MapSubstN(Map<? extends Term,Term> xy, TermBuilder B) {
            this(xy, struct(xy.keySet()), B);
        }

        MapSubstN(Map<? extends Term,Term> xy, int structure, TermBuilder B){
            super(structure, B);
            this.xy = xy;
        }

        @Override
        public @Nullable Term xy(Term t) {
            return xy.get(t);
        }

    }


    private static final class SubstCompound extends RecursiveTermTransform {

        private final Compound from;
        private final Term to;
        private final int fromStructure;
        private final int fromVolume;

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        SubstCompound(Compound from, Term to, TermBuilder B) {
            builder = B;
            this.from = from;
            this.to = to;
            this.fromStructure = from.struct();
            this.fromVolume = from.complexity();
        }

        @Override
        public @Nullable Term applyCompound(Compound c) {
            var cv = c.complexity();
            if (cv == fromVolume && from.equals(c))
                return to;
            else if (cv > fromVolume && !c.impossibleSubStructure(fromStructure)/* && !c.impossibleSubVolume(fromVolume)*/)
                return super.applyCompound(c);
            else
                return c;
        }


    }

    private abstract static class AbstractSubstAtomic extends RecursiveTermTransform {
        protected final int fromStruct;
        protected final Term to;

        private AbstractSubstAtomic(int fromStruct, Term to, TermBuilder B) {
            this.fromStruct = fromStruct;
            this.to = to;
            this.builder = B;
        }

        @Override @Nullable
        public final Term applyCompound(Compound x) {
            return x.hasAny(fromStruct) ? super.applyCompound(x) : x;
        }
    }

    private static final class SubstIntrinAtomic extends AbstractSubstAtomic {

        private final IntrinAtomic from;
        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        SubstIntrinAtomic(IntrinAtomic from, Term to, TermBuilder B) {
            super(((Term)from).structOp(), to, B);
            this.from = from;
        }

        @Override
        public Term applyAtomic(Atomic x) {return from == x ? to : x; }
    }

    private static final class SubstAtomic extends AbstractSubstAtomic {

        private final Atomic from;

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        SubstAtomic(Atomic from, Term to, TermBuilder B) {
            super(from.structOp(), to, B);
            this.from = from;
        }

        @Nullable @Override
        public Term applyAtomic(Atomic x) {
            return from.equals(x) ? to : x;
        }

    }


}
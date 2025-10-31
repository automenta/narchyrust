package nars.term.util;

import jcog.bloom.StableBloomFilter;
import jcog.data.bit.MetalBitSet;
import jcog.data.map.ObjIntHashMap;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import nars.NALTask;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TmpTermList;
import nars.task.util.TaskHasher;
import nars.term.Compound;
import nars.term.Neg;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static nars.Op.NEG;
import static nars.Op.statementClosure;
import static nars.Term.commonEventStructure;
import static nars.term.atom.Bool.Null;


/**
 * Static utility class for static methods related to Terms
 * <p>
 * Also serves as stateless/memory-less transient static (on-heap) TermIndex
 *
 * @author me
 */
public enum Terms {
	;



	/**
	 * sort and deduplicates the elements; returns new array if modifications to order or deduplication are necessary
	 */
	public static Term[] commute(Term... x) {
		int len = x.length;
		return switch (len) {
			case 0 -> Op.EmptyTermArray;
			case 1 -> x;
			case 2 -> commute2(x);
			case 3 -> commute3(x);
			default -> ArrayUtil.maybeEqualIdentity(
				commuteTerms(x, x.length,false), x
				//commuteTermsTermList(x)
				//new MetalTreeSet<>(x).toArray(Op.EmptyTermArray)
				//new SortedList<>(x, new Term[x.length]).toArrayRecycled(Term[]::new) //slow
			);
		};
	}

	/** allows modification of the input array TODO fix */
	public static Term[] commuteMutable(Term[] x, int n) {
		return switch (n) {
			case 0 -> Op.EmptyTermArray;
			case 1 -> x;
			case 2 -> {
				Term x0 = x[0];
				int c = x0.compareTo(x[1]);
				if (c == 0) yield new Term[] {x0};
				if (c > 0) ArrayUtil.swap(x, 0, 1); yield x; }
			case 3 -> { commute3(x); yield ArrayUtil.removeNulls(x); }  //TODO mutable version of this
			default -> commuteTerms(x, n,true);
		};
	}

	/**
	 * doesnt deduplicate
	 */
	public static Term[] sort(Term... x) {
		//TODO optimized sort3
		return switch (x.length) {
			case 0 -> Op.EmptyTermArray;
			case 1 -> x;
			case 2 -> sort2(x);
			default -> ArrayUtil.maybeEqualIdentity(new TmpTermList(x.clone()).sortedArrayTake(), x);
		};
	}


	/** n-array commute function optimized to avoid unnecessary comparisons by grouping by term properties like volume first.
	 * TODO use op if that is the 2nd comparison property
	 * */
	private static Term[] commuteTerms(Term[] x, int n, boolean modifyInputArray) {
		short[] volumes = new short[n];
		int volMin = Integer.MAX_VALUE, volMax = Integer.MIN_VALUE;
		boolean allDecreasing = true;
		for (int i = 0; i < n; i++) {

			short v = (short) x[i].complexity();

			volumes[i] = v;

			volMin = Math.min(volMin, v); volMax = Math.max(volMax, v);

			if (allDecreasing && i > 1 && volumes[i-1] <= v)
				allDecreasing = false;
		}
		if (allDecreasing) {
			//already in sorted order guaranteed by the volume being strictly decreasing
			return x;
		}

		Term[] y = modifyInputArray ? x : Arrays.copyOf(x, n);


		MetalBitSet nulls = MetalBitSet.bits(n);
		if (volMax <= volMin) {
			//flat
			Arrays.sort(y, 0, n);
			//TODO maybe use ArrayUtil.quickSort and nullify as soon as a .compare returns 0.  avoids the subsequent .equals tests
			nullDups(y, 0, n, nulls);
		} else {
			//sort within the spans where the terms have equal volumes (divide & conquer)

			QuickSort.quickSort(0, n,
				(a, b) -> Integer.compare(volumes[b], volumes[a]),
				(a, b) -> ArrayUtil.swapObjShort(y, volumes, a, b));

			int vs = volumes[0];
			//span start
			int s = 0;
			for (int i = 1; i <= n; i++) {
				int vi = i < n ? volumes[i] : -1;
				if (vi != vs) {
					if (i - s > 1) {
						//sort span
						//TODO optimized 2-array compare swap
						Arrays.sort(y, s, i);
						nullDups(y, s, i, nulls);
					}
					//next span
					s = i;
					vs = vi;
				}
			}

		}

		return nulls.isEmpty() && y.length==n ? y : ArrayUtil.removeAll(y, n, nulls);
	}

	private static void nullDups(Term[] y, int start, int end, MetalBitSet nulls) {
		Term prev = y[start];
		for (int i = start+1; i < end; i++) {
			Term next = y[i];
			if (prev.equals(next)) {
				y[i] = null;
				nulls.set(i);
			} else {
				prev = next;
			}
		}
	}

	private static Term[] commute3(Term[] t) {
    /*
    //https://stackoverflow.com/a/16612345
    if (el1 > el2) Swap(el1, el2)
    if (el2 > el3) {
        Swap(el2, el3)
        if (el1 > el2) Swap(el1, el2)
    }*/

		Term a = t[0], b = t[1], c = t[2];
		int ab = a.compareTo(b);
		if (ab == 0) {
			return commute(a, c); //a=b, so just combine a and c (recurse)
		} else if (ab > 0) {
			Term x = a;
			a = b;
			b = x;
		}
		int bc = b.compareTo(c);
		if (bc == 0) {
			return new Term[]{a, b}; //b=c so just combine a and b
		} else if (bc > 0) {
			Term x = b;
			b = c;
			c = x;
			int ab2 = a.compareTo(b);
			if (ab2 == 0) {
				return new Term[]{a, c};
			} else if (ab2 > 0) {
				Term y = a;
				a = b;
				b = y;
			}
		}
		//already sorted
		return t[0] == a && t[1] == b ? t : new Term[]{a, b, c};
	}


	private static Term[] commute2(Term[] t) {
		Term a = t[0], b = t[1];
		int ab = a.compareTo(b);
		if (ab < 0) return t;
		else if (ab > 0) return new Term[]{b, a};
		else /*if (c == 0)*/ return new Term[]{a};
	}

	private static Term[] sort2(Term[] t) {
		Term a = t[0], b = t[1];
		int ab = a.compareTo(b);
		if (ab < 0) return t;
		else if (ab > 0) return new Term[]{b, a};
		else /*if (c == 0)*/ return new Term[]{a, a};
	}

	public static void printRecursive(PrintStream out, Term x) {
		printRecursive(out, x, 0);
	}

	static void printRecursive(PrintStream out, Term x, int level) {

		for (int i = 0; i < level; i++)
			out.print("  ");

		out.print(x);
		out.print(" (");
		out.print(x.op() + "[" + x.getClass().getSimpleName() + "] ");
		out.print("c" + x.complexityConstants() + ",v" + x.complexity() + ",dt=" + x.dt() + ",dtRange=" + x.seqDur() + ' ');
		out.print(Integer.toBinaryString(x.struct()) + ')');
		out.println();


		for (Term z : x.subterms()) {
			printRecursive(out, z, level + 1);
		}


	}

	/**
	 * for printing complex terms as a recursive tree
	 */
	public static void printRecursive(Term x, Consumer<String> c) {
		printRecursive(x, 0, c);
	}

	private static void printRecursive(Term x, int level, Consumer<String> c) {


        for (Term z : x.subterms())
			printRecursive(z, level + 1, c);


        String line = "  ".repeat(Math.max(0, level)) +
                x;
        c.accept(line);
	}


	public static @Nullable Term[] concat(Term[] a, Term... b) {

		if (a.length == 0) return b;
		if (b.length == 0) return a;

		int L = a.length + b.length;

		Term[] arr = new Term[L];

		int l = a.length;
		System.arraycopy(a, 0, arr, 0, l);
		System.arraycopy(b, 0, arr, l, b.length);

		return arr;
	}


	/**
	 * dangerous because some operations involving concepts can naturally reduce to atoms, and using this interprets them as non-existent
	 */
	@Deprecated
	public static @Nullable Compound compoundOrNull(@Nullable Term t) {
		return t instanceof Compound ? (Compound) t : null;
	}


	public static boolean allNegated(Subterms subterms) {
		return subterms.hasAny(NEG) && subterms.AND(t -> t instanceof Neg);
	}


	/**
	 * counts the repetition occurrence count of each subterm within a compound
	 */
	public static @Nullable ObjIntHashMap<Term> subtermScore(Subterms c, ToIntFunction<Term> score, int minTotalScore) {
		ObjIntHashMap<Term> uniques = subtermScore(c, score);
		if (uniques.isEmpty()) return null;

		uniques.values().removeIf(s -> s < minTotalScore);

		return uniques.isEmpty() ? null : uniques;
	}

	private static ObjIntHashMap<Term> subtermScore(Subterms c, ToIntFunction<Term> score) {
		ObjIntHashMap<Term> uniques = new ObjIntHashMap<>(c.complexity() /* estimate */);

		c.recurseTermsOrdered(z -> true, subterm -> {
			int s = score.applyAsInt(subterm);
			if (s > 0)
				uniques.addToValue(subterm, s);
			return true;
		}, null);

		return uniques;
	}

	/**
	 * a Set is already duplicate free, so just sort it
	 */
	public static Term[] commute(Collection<? extends Term> s) {
		Term[] x = s.toArray(Op.EmptyTermArray);
		return (x.length >= 2) && (!(s instanceof SortedSet)) ? commute(x) : x;
	}


	public static StableBloomFilter<Term> termBloomFilter(Random rng, int cells) {
		return new StableBloomFilter<>(
			cells, 2, 5f / cells, rng,
			new TermHasher());
	}

	public static StableBloomFilter<NALTask> taskBloomFilter(Random rng, int cells) {
		return new StableBloomFilter<>(
			cells, 2, 5f / cells, rng,
			new TaskHasher());
	}


	@Nullable public static Term withoutAll(Term container, Predicate<Term> filter) {
		Subterms cs = container.subterms();
		MetalBitSet match = cs.indicesOfBits(filter);
		int n = match.cardinality();
		if (n == 0) {
			return container; //no matches
		} else {
			Term[] remain = cs.removing(match);
			return remain == null || remain.length == 0 ? null : container.op().the(container.dt(), remain);
		}
	}


	public static boolean rComEq(Term x, Term y) {
		return x==y ||
				(x instanceof Compound == y instanceof Compound && x.equals(y)) || rCom(x,y);
	}

	/** whether x is recursively contained by y, or vice-versa */
	public static boolean recCom(Term x, Term y) {
		x = x.unneg(); y = y.unneg();
		int xv = x.complexity(), yv = y.complexity();
		if (xv == yv) return false; //TODO equals?
		if (xv < yv) {
			Term c = x;
			x = y;
			y = c;
		}
		if (xv == 1) return false; //both atoms
		//yv < xv
		return x.containsRecursively(y);
	}

	/** TODO rename */
	public static boolean rCom(Term x, Term y) {

		x = x.unneg(); y = y.unneg();

		if (!(x instanceof Compound X) && !(y instanceof Compound Y))
			return false; //both atomics, done

		if (!commonEventStructure(x,y))
			return false;

		int xv = x.complexity(), yv = y.complexity();

		if (xv < yv) {
			Term c = x;
			x = y;
			y = c;
		}

		Term yu = y;//.unneg();
		if (yu.CONJ() && rComEvent((Compound) yu, x))
			return true;

		Term xu = x;//.unneg();
		if (xu.CONJ() && rComEvent((Compound) xu, y))
			return true;

		return xv != yv &&
				statementClosure.test((Compound) x) &&
				((Compound) x).containsRecursively(y, statementClosure);
	}

	private static boolean rComEvent(Compound x, Term y) {
		Term Y = y.unneg();
		//return x.OR(z -> rComEq(z.unneg(), Y));
		return x.ORunneg(z -> rComEq(z, Y));
//		for (Term z : x)
//			if (rComEq(z.unneg(), Y))
//				return true;
//		return false;
	}
	private static boolean _rComEvent(Compound x, Term y) {
		Term Y = y.unneg();
		return x.condsOR(z -> rComEq(z.unneg(), Y), true, true);
	}

	public static Term intersect(/*@NotNull*/ Op o, Subterms a, Subterms b) {
		if (a instanceof Term && a.equals(b))
			return (Term) a;

		if (!Op.hasCommon(a, b))
			return Null;


		SortedSet<Term> ab = a.collect(b.subs() > 3 ? b.toSet()::contains : b.contains(false,false), new TreeSet<>());
		int ssi = ab == null ? 0 : ab.size();
		return switch (ssi) {
			case 0 -> Null;
			case 1 -> ab.first();
			default -> o.the(ab);
		};
	}
	public static Set<Term> intersect(Subterms a, Subterms b, boolean pn, Predicate<Term> include) {

		if (!Op.hasCommon(a, b))
			return Collections.EMPTY_SET;

		int as = a.subs(), bs = b.subs();
		boolean eq = false;
		if (bs > as) {
			Subterms c = a; a = b; b = c; //swap so b < a
		} else if (bs == as) {
			eq = a.equals(b);
		}

		Set<Term> ab = null; //
		int n = b.subs();
		Predicate<Term> aContains = null;
		for (int i = 0; i < n; i++) {
			Term bi = b.sub(i);
			if (pn) bi = bi.unneg();

			if (include.test(bi)) {
				if (!eq && aContains == null) aContains = a.contains(pn, false);

			    if (eq || aContains.test(bi)) {
					if (ab == null) ab = new UnifiedSet<>(Math.min(as, bs) - i);

					ab.add(bi);
				}
			}
		}

		return ab==null ? Collections.EMPTY_SET : ab;
	}

	public static Term union(/*@NotNull*/ Op o, Subterms a, Subterms b) {
		if (a == b)
			return a instanceof Term && ((Term)a).opID()==o.id ? (Term)a : o.the(a);

		boolean bothTerms = a instanceof Term && b instanceof Term;
		if (bothTerms && a.equals(b))
			return (Term) a;

		int as = a.subs(), bs = b.subs();
		Collection<Term> t = new UnifiedSet<>(as+bs);
		a.addAllTo(t);
		b.addAllTo(t);
		if (bothTerms) {
			int maxSize = Math.max(as, bs);
			if (t.size() == maxSize)
				return (Term) (as > bs ? a : b);
		}
		return o.the(t);
	}

}
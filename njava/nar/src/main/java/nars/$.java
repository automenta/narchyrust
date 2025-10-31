package nars;


import com.google.common.base.Splitter;
import jcog.Str;
import jcog.TODO;
import jcog.Util;
import jcog.io.BinTxt;
import jcog.util.ArrayUtil;
import nars.subterm.IntrinSubterms;
import nars.subterm.ShortSubterms;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.anon.Intrin;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.builder.TermBuilder;
import nars.term.compound.LightCompound;
import nars.term.obj.JsonTerm;
import nars.term.util.SetSectDiff;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;
import nars.truth.PreciseTruth;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.hipparchus.fraction.Fraction;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.lang.Character.isDigit;
import static nars.Op.*;

/***
 *     oooo       oo       .o.       ooooooooo.
 *    `888b.      8'      .888.      `888   `Y88.
 *     88`88b.    88     .8"888.      888   .d88'  .ooooo oo oooo  oooo   .ooooo.  oooo d8b oooo    ooo
 *     88  `88b.  88    .8' `888.     888ooo88P'  d88' `888  `888  `888  d88' `88b `888""8P  `88.  .8'
 *     88    `88b.88   .88ooo8888.    888`88b.    888   888   888   888  888ooo888  888       `88..8'
 *     88      `8888  .8'     `888.   888  `88b.  888   888   888   888  888    .o  888        `888'
 *     8o        `88 o88o     o8888o o888o  o888o `V8bod888   `V88V"V8P' `Y8bod8P' d888b        .8'
 *                                                      888.                                .o..P'
 *                                                      8P'                                 `Y8P'
 *                                                      "
 *
 *                                              NARquery
 *                                          Core Utility Class
 */
public enum $ {
	;

//	static {
//		try {
//			/*
//			{
//				match: "*::*",
//				inline: ["nars/term/control/FORK::test","nars/term/control/AND*::test"]
//			}
//			*/
//			var y = ManagementFactory.getPlatformMBeanServer().invoke(
//					new ObjectName("com.sun.management:type=DiagnosticCommand"),
//					"compilerDirectivesAdd",
//					new Object[]{new String[]{"/tmp/compiler.json"}},
//					new String[]{"[Ljava.lang.String;"}
//			);
//			//System.out.println(y);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}

	/** narsese normalized term parse */
	public static Term $(String term) throws Narsese.NarseseException {
		return Narsese.term(term, true);
	}

	/**
	 * unsafe narsese normalized term parse: doesnt throw exception, but may throw RuntimeException
	 */
	public static Term $$(String term) {
		try {
			return $(term);
		} catch (Narsese.NarseseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * narsese (non-normalized) term parse.
	 * doesnt throw exception, but may throw RuntimeException
	 */
	public static Term $$$(String term) {
		try {
			return Narsese.term(term, false);
		} catch (Narsese.NarseseException e) {
			throw new RuntimeException(term,e);
		}
	}

	static final Atom emptyQuote = (Atom) Atomic.atomic("\"\"");
	public static Atom quote(String text) {
		return text.isEmpty() ? emptyQuote :
			(Atom) Atomic.atomic(Str.quote(text));
	}

	public static Atom quote(Object text) {
		return quote(text.toString());
	}

	public static Term[] the(String... id) {
		return Util.map(Atomic::atomic, new Term[id.length], id);
	}


	public static Atomic the(char c) {
		return isDigit(c) ? Int.i(Character.digit(c, 10)) : Atomic.atomic(String.valueOf(c));
	}

	/**
	 * Op.INHERITANCE from 2 Terms: subj --> pred
	 * returns a Term if the two inputs are equal to each other
	 */
	public static Term inh(Term subj, Term pred) {
		return INH.the(subj, pred);
	}

	public static Term inh(Term subj, String pred) {
		return inh(subj, $$(pred));
	}

	public static Term inh(String subj, Term pred) {
		return inh($$(subj), pred);
	}

	public static Term inh(String subj, String pred)  {
		return inh($$(subj), $$(pred));
	}

	/** quick inh builder for internal use */
	public static Compound _inh(Term subj, Term pred) {
		return new LightCompound(INH.id, subj, pred);
		//TODO could also be CachedCompound
	}

	public static Term sim(Term subj, Term pred) {
		return SIM.the(subj, pred);
	}

	public static Term func(String opTerm, Term... arg) {
		return func(Atomic.atomic(opTerm), arg);
	}

	public static Term func(String opTerm, Subterms arg) {
		return func(Atomic.atomic(opTerm), arg);
	}

	public static Term func(String opTerm, String... arg) throws Narsese.NarseseException {
		return func(Atomic.atomic(opTerm), array(arg));
	}

	/**
	 * function ((a,b)==>c) aka: c(a,b)
	 */
	public static Term func(Atomic opTerm, Term... arg) {
		return INH.the(PROD.the(arg), opTerm);
	}
	public static Term func(TermBuilder B, Atomic opTerm, Term... arg) {
		return INH.build(B, PROD.build(B, arg), opTerm);
	}

	public static Term func(Atomic opTerm, Subterms arg) {
		return INH.the(PROD.the(arg), opTerm);
	}

	public static Term impl(Term a, Term b) {
		return IMPL.the(a, b);
	}

	public static Term impl(Term a, int dt, Term b) {
		return IMPL.the(a, dt, b);
	}

	public static Term p(Collection<Term> t) {
		return p(t.toArray(EmptyTermArray));
	}

	public static Term p(Term... t) {
		return PROD.the(t);
	}

	public static Term pOrOnly(Term... t) {
		return t.length == 1 ? t[0] : p(t);
	}

	/**
	 * creates from a sublist of a list
	 */

	public static Term p(List<Term> l, int from, int to) {
		if (from == to)
			return EmptyProduct;

		Term[] x = new Term[to - from];

		for (int j = 0, i = from; i < to; i++)
			x[j++] = l.get(i);

		return p(x);
	}

	public static Term p(String x, Term y) {
		return p($$(x), y);
	}
	public static Term p(String x, int y) {
		return p(x, the(y));
	}
	public static Term p(String x, String y, Term t) {
		return p($$(x), $$(y), t );
	}
	public static Term p(Term t, String x, String y) {
		return p(t, $$(x), $$(y));
	}

	public static Term p(int x, String y) {
		return p(the(x), $$(y));
	}

	public static Term p(Term x, String y) {
		return p(x, $$(y));
	}

	public static Term p(String... t) {
		return t.length == 0 ? EmptyProduct : p(the(t));
	}

	public static Term p(int... t) {
		return t.length == 0 ? EmptyProduct : p(ints(t));
	}

	public static Term p(short... t) {
		return t.length == 0 ? EmptyProduct : PROD.the(shortSubs(t));
	}

	/**
	 * encodes a boolean bitvector as an Int target, or if larger than 31 bits, as an Atom string
	 */
	public static Term p(boolean... t) {
		if (t.length == 0) return EmptyProduct;

		if (t.length < 32) {
			int b = 0;
			for (int i = 0; i < t.length; i++) {
				if (t[i]) b |= 1 << i;
			}
			return Int.i(b);
		} else {
			throw new TODO();
		}
	}

	/**
	 * warning: generic variable
	 */
	public static Variable v(/**/ Op type, String name) {
		//special case: interpret normalized variables
		switch (name.length()) {
			case 1 -> {
				char c0 = name.charAt(0);
				if (isDigit(c0))
					return v(type, (byte) (c0 - '0'));
			}
			case 2 -> {
				char d0 = name.charAt(0);
				if (isDigit(d0)) {
					char d1 = name.charAt(1);
					if (isDigit(d1))
						return v(type, (byte) ((d0 - '0') * 10 + (d1 - '0')));
				}
			}
		}
		return new UnnormalizedVariable(type, type.ch + name);
	}

	public static Variable varDep(int i) {
		return v(VAR_DEP, (byte) i);
	}

	public static Variable varDep(String s) {
		return v(VAR_DEP, s);
	}

	public static Variable varIndep(int i) {
		return v(VAR_INDEP, (byte) i);
	}

	public static Variable varIndep(String s) {
		return v(VAR_INDEP, s);
	}

	public static Variable varQuery(int i) {
		return v(VAR_QUERY, (byte) i);
	}

	public static Variable varQuery(String s) {
		return v(VAR_QUERY, s);
	}

	public static Variable varPattern(int i) {
		return v(VAR_PATTERN, (byte) i);
	}

	/**
	 * Try to make a new compound from two components. Called by the logic rules.
	 * <p>
	 * A -{- B becomes {A} --> B
	 *
	 * @param subj The first component
	 * @param pred The second component
	 * @return A compound generated or null
	 */
	public static Term inst(Term subj, Term pred) {
		return INH.the(SETe.the(subj), pred);
	}

	public static <T extends Term> T instprop(Term subject, Term predicate) {
		return (T) INH.the(SETe.the(subject), SETi.the(predicate));
	}

	public static <T extends Term> T prop(Term subject, Term predicate) {
		return (T) INH.the(subject, SETi.the(predicate));
	}

	public static Term p(char[] c, CharToObjectFunction<Term> f) {
		Term[] x = new Term[c.length];
		for (int i = 0; i < c.length; i++) {
			x[i] = f.valueOf(c[i]);
		}
		return p(x);
	}

	public static <X> Term p(X[] x, Function<X, Term> toTerm) {
		return p(terms(x, toTerm));
	}

	public static <X> Term[] terms(X[] x, Function<X, Term> f) {
		return Util.map(f, new Term[x.length], x);
	}

	private static Term[] array(Collection<? extends Term> t) {
		return t.toArray(EmptyTermArray);
	}

	private static Term[] array(String... s) throws Narsese.NarseseException {
		int l = s.length;
		Term[] tt = new Term[l];
		for (int i = 0; i < l; i++)
			tt[i] = $(s[i]);

		return tt;
	}

	public static Term seti(Collection<Term> t) {
		return SETi.the(array(t));
	}

	public static Term sete(RoaringBitmap b) {
		return SETe.the(intArray(b));
	}

	public static Term sete(RichIterable<Term> b) {
		return SETe.the(b.toSet());
	}


	public static short[] shortArray(RoaringBitmap b) {
		short[] x = new short[b.getCardinality()];
		int i = 0;
		PeekableIntIterator bb = b.getIntIterator();
		while (bb.hasNext())
			x[i++] = (short) bb.next(); //TODO check valid int/short range
		return x;
	}

	/**
	 * result will be sorted
	 */
	public static Subterms shortSubs(RoaringBitmap b) {
		return ShortSubterms.the(b);
	}

	/**
	 * result is not necessarily sort, so pre-sort if necessary
	 */
	public static Subterms shortSubs(short... b) {
		return ShortSubterms.the(b);
	}

	public static Subterms shortSubs(byte... b) {
		return ShortSubterms.the(b);
	}

	public static Subterms intSubs(int... x) {
		//1. check if all the int's are within Short range, use short subs
		boolean nonShort = false;
		int n = x.length;
		for (int X : x) {
			if (!(X >= Short.MIN_VALUE && X <= Short.MAX_VALUE)) {
				nonShort = true;
				break;
			}
		}
		if (nonShort) {
			return terms.subterms(ints(x));
		} else {
			return ShortSubterms.the(x);
		}
	}


	public static Term[] intArray(RoaringBitmap b) {
		int size = b.getCardinality();
		switch (size) {
			case 0:
				return EmptyTermArray;
			case 1:
				return new Term[]{Int.i(b.first())};
			default:
				Term[] t = new Term[size];
				int k = 0;
                PeekableIntIterator ii = b.getIntIterator();
				while (ii.hasNext())
					t[k++] = Int.i(ii.next());
				return t;
		}
	}

	/**
	 * unnormalized variable
	 */
	public static Variable v(char ch, String name) {
		return v(op(String.valueOf(ch)), name);
	}

	/**
	 * normalized variable
	 */
	public static NormalizedVariable v(/**/ Op type, byte id) {
		return NormalizedVariable.varNorm(type, id);
	}


	/**
	 * alias for disjunction
	 */
	public static Term or(Term... x) {
		return DISJ(x);
	}

	/**
	 * alias for conjunction
	 */
	public static Term and(Term... x) {
		return CONJ.the(x);
	}


	/**
	 * create a literal atom from a class (it's name)
	 */
	public static Atom the(Class c) {
		return (Atom) Atomic.atomic(c.getName());
	}

	/**
	 * gets the atomic target of an integer, with specific radix (up to 36)
	 */
	public static Atomic intRadix(int i, int radix) {
		//TODO if radix==10, return Int.the(i) ?
		return quote(Integer.toString(i, radix));
	}

	public static Atomic the(int v) {
		return Int.i(v);
	}

	public static Truth tt(float f, double c) {
		return
			PreciseTruth.byConf(f, c);
			//new MutableTruth(f, c2e(c));
	}

	public static Truth tt(double f, double c) {
		return tt((float)f, c);
	}

	@Nullable public static PreciseTruth t(float f, double c) {
		return PreciseTruth.byConf(f, c);
	}

	public static PreciseTruth t(double f, double c) {
		return t((float)f, c);
	}

	/**
	 * negates each entry in the array
	 */
	public static Term[] neg(Term[] array) {
		Util.map(Term::neg, array, array);
		return array;
	}

	public static Term p(byte[] array) {
		return PROD.the(shortSubs(array));
	}

	public static Term[] ints(short... i) {
		int l = i.length;
		Term[] x = new Term[l];
		for (int j = 0; j < l; j++) x[j] = the(i[j]);
		return x;
	}

	public static Term[] ints(int... i) {
		int l = i.length;
		Term[] x = new Term[l];
		for (int j = 0; j < l; j++) x[j] = the(i[j]);
		return x;
	}

	/**
	 * use with caution
	 */
	public static Term the(boolean b) {
		return b ? Bool.True : Bool.False;
	}

	public static Term the(double x) {
		if (x != x)
			return NaN;

		double rx = Util.round(x, 1);
		if (Util.equals(rx, x)) {
			//HACK BigNum
			return rx < Integer.MAX_VALUE && rx > Integer.MIN_VALUE + 1 ? Int.i((int) rx) : quote(Double.toString(rx));
		} else
			return the(new Fraction(x));
	}

	public static double doubleValue(Term x) {
		if (x.INT()) {
			return Int.i(x);
		} else if (x.ATOM()) {
			return Double.parseDouble(unquote(x));
		} else {
			throw new TODO();
		}
	}

	private static final Atomic DIV = atomic("div");
	public static Term the(Fraction o) {
		return func(DIV, the(o.getNumerator()), the(o.getDenominator()));
	}

	public static Term the(Object x) {
        return switch (x) {
            case Termed tt -> tt.term();
            case Number n -> the(n);
            case String s -> atomic(s);
            case URL u -> the(u);
            case null, default -> throw new UnsupportedOperationException(x + " termize fail");
        };
    }

	public static Atomic atomic(String x) {
		return Atomic.atomic(x);
	}

	public static Term the(Path file) {
		return the(file.toUri());
	}

	public static Path file(Term x) {
		throw new TODO();
	}

	public static Term the(Number n) {
        return switch (n) {
            case Integer i -> Int.i(i);
            case Long l -> Atomic.atomic(Long.toString(l));
            case Short s -> Int.i(s);
            case Byte b -> Int.i(b);
            case Float v -> {
                float d = n.floatValue();
                int id = (int) d;
                yield d == d && Util.equals(d, id) ? Int.i(id) : Atomic.atomic(n.toString());
            }
            case null, default -> {
                double d = n.doubleValue();
                int id = (int) d;
                yield d == d && Util.equals(d, id) ? Int.i(id) : Atomic.atomic(n.toString());
            }
        };
	}

	public static Term pRadix(int x, int radix, int maxX) {
		return p(radixArray(x, radix, maxX));
	}


	public static int[] radix(int x, int radix, int maxValue) {
		assert (x >= 0);
		x %= maxValue; //auto-wraparound

		int decimals = (int) Math.ceil(Math.log(maxValue) / Math.log(radix));
		int[] y = new int[decimals];
		int X = -x;
		int yi = 0;
		do {
			y[yi++] = -(X % radix);
			X /= radix;
		} while (X <= -1);
		return y;
	}

	/**
	 * most significant digit first, least last. padded with zeros
	 */
	public static Term[] radixArray(int x, int radix, int maxX) {

		int[] r = radix(x, radix, maxX);

		//$.the(BinTxt.symbols[r[i]]);
		Term[] y = new Term[r.length];
		for (int j = 0, xxLength = r.length; j < xxLength; j++)
			y[j] = Int.i(r[j]);
		return y;
	}


	public static Term pRecurse(boolean innerStart, Term... t) {
		int j = t.length - 1;
		int n = innerStart ? 0 : j - 1;
		Term inner = t[n];
		Term nextInner = inner.PROD() ? inner : p(inner);
		while (--j > 0) {
			n += innerStart ? +1 : -1;
			Term next = t[n];


			nextInner = next.PROD() ? p(ArrayUtil.add(next.subterms().arrayShared(), nextInner)) : (innerStart ? p(nextInner, next) : p(next, nextInner));

		}
		return nextInner;
	}


	public static @Nullable Compound inhRecurse(Term... t) {
		int tl = t.length;
		Term bottom = t[--tl];
		Term nextInner = inh(t[--tl], bottom);
		while (nextInner != null && tl > 0) {
			nextInner = inh(t[--tl], nextInner);
		}
		return (Compound) nextInner;
	}


	public static String unquote(Term s) {
		return Str.unquote(s.toString());
	}


//    /**
//     * instantiate new Javascript context
//     */
//    public static NashornScriptEngine JS() {
//        return (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
//    }

	public static int intValue(Term intTerm) throws NumberFormatException {
		if (intTerm instanceof Int ii)
			return Int.i(ii);

		throw new NumberFormatException();
	}

	public static int intValue(Term intTerm, int ifNotInt) {
		return intTerm.INT() ? Int.i(intTerm) : ifNotInt;
	}

	public static Term fromJSON(String j) {
		return JsonTerm.the(j);
	}

	public static Compound pFast(Subterms x) {
		return LightCompound.the(PROD, x);
	}

	public static Compound pFast(Term... x) {
		return LightCompound.the(PROD, x);
	}

	/**
	 * assumes subterms is sorted
	 */
	public static Compound sFast(Subterms x) {
		if (x.subs() == 0) throw new UnsupportedOperationException();
		return LightCompound.the(SETe, x.commuted());
	}

//	public static Term sFast(Term[] x) {
//		return sFast(true, x);
//	}
//
//	public static Compound sFast(SortedSet<Term> x) {
//		return sFast(false, x.toArray(EmptyTermArray));
//	}
//
//	public static Compound sFast(Collection<Term> x) {
//		return sFast(false, Terms.commute(x));
//	}

//	public static Compound sFast(boolean sort, Term[] x) {
//		if (x.length == 0) throw new TermException("empty set");
//		if (sort && x.length > 1)
//			x = Terms.commute(x);
//		return LightCompound.the(SETe, x);
//		//new LighterCompound(Op.SETe, x);
//	}

	public static Term pFast(Collection<? extends Term> x) {
		return pFast(x.toArray(EmptyTermArray));
	}

//	public static Subterms vFast(Collection<Term> t) {
//		return vFast(t.toArray(EmptyTermArray));
//	}

	/**
	 * on-stack/on-heap cheaply constructed Subterms
	 */
	public static Subterms vFast(Term... t) {
		switch (t.length) {
			case 0:
				return EmptySubterms;
			case 1: {
				Term a = t[0];
				if (Intrin.intrin(a.unneg()))
					return new IntrinSubterms(a);
				break;
			}
			case 2: {
				Term a = t[0], b = t[1];
				if (Intrin.intrin(a.unneg()) && Intrin.intrin(b.unneg()))
					return new IntrinSubterms(a, b);
				break;
			}
			case 3: {
				Term a = t[0], b = t[1], c = t[2];
				if (Intrin.intrin(a.unneg()) && Intrin.intrin(b.unneg()) && Intrin.intrin(c.unneg()))
					return new IntrinSubterms(a, b, c);
				break;
			}
		}

		return new TermList(t);
	}


	public static Term[] $(String[] s) {
		return Util.map(x -> {
			try {
				return $(x);
			} catch (Narsese.NarseseException e) {
				throw new RuntimeException(e);
			}
		}, Term[]::new, s);
	}

	public static Term func(Atomic f, List<Term> args) {
		return func(f, args.toArray(EmptyTermArray));
	}

	public static Term funcImg(String f, Term... x) {
		return funcImg(atomic(f), x);
	}

	public static Term funcImg(Atomic f, Term... x) {
//        if (x.length > 1) {
		Term[] xx = ArrayUtil.insert(0, x, f);
		xx[x.length] = ImgExt;
		return INH.the(x[x.length - 1], PROD.the(xx));
//        } else {
//            return $.func(f, x);
//        }
	}


	public static Term diff(Term a, Term b) {
		//throw new TODO("use setAt/sect methods");
		Op aop = a.op();
		if (aop.id == b.opID()) {
			switch (aop) {
				case SETi:
					return SetSectDiff.differenceSet(SETi, (Compound)a, (Compound)b);
				case SETe:
					return SetSectDiff.differenceSet(SETe, (Compound)a, (Compound)b);
			}
		}
		//throw new UnsupportedOperationException();
		return CONJ.the(a, b.neg());
	}

	public static Term identity(Object x) {

		if (x instanceof Term t)
			return (t);
		else if (x instanceof Termed tt) {
			Term u = tt.term();
			if (u != null)
				return u;
			//else: probably still in the constructor before x.term() has been set, continue:
		}

		if (x instanceof String s)
			return Atomic.atomic(s);
		else {
			Class<?> c = x.getClass();
			Term idHash = intRadix(System.identityHashCode(x), 36);
			return p(quote(c.isSynthetic() ? c.getSimpleName() : c.getName()), idHash);
		}

	}

	public static Atom uuid() {
		return uuid(null);
	}

	public static Atom uuid(@Nullable String prefix) {
		String u = BinTxt.uuid64();
		return quote(prefix != null ? prefix + u : u);
	}

	public static Term the(URL u) {

		if (u.getQuery() != null)
			throw new TODO();

		String schemeStr = u.getProtocol();
		String authorityStr = u.getAuthority();
		String pathStr = u.getPath();

		return URI(schemeStr, authorityStr, pathStr);
	}

	public static Term the(URI u) {

		if (u.getFragment() != null || u.getQuery() != null)
			throw new TODO();

		String schemeStr = u.getScheme();
		String authorityStr = u.getAuthority();
		String pathStr = u.getPath();

		return URI(schemeStr, authorityStr, pathStr);
	}

	/**
	 * https://en.wikipedia.org/wiki/Uniform_Resource_Identifier
	 * <p>
	 * the URI scheme becomes the inheritance subject of the operation. so path components are the pred as a product. query can be the final component wrapped in a set to distinguish it, and init can be a json-like set of key/value pairs. the authority username/password can be special fields in that set of pairs.
	 * <p>
	 * TODO authority, query
	 */
	public static Term URI(String schemeStr, @Nullable String authority, String pathStr) {
        /*
        URI = scheme:[//authority]path[?query][#fragment]
        authority = [userinfo@]host[:port]

                  userinfo     host        port
          ┌─┴─┬──┴────┬┴┐
          https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top
         └┬┘└──────────────┴────────┴───────┬────-┴┬┘
         scheme           authority                 path                   query          fragment

          ldap://[2001:db8::7]/c=GB?objectClass?one
          └─┬┘ └───────┬─────┘└─┬─┘ └──────┬──────┘
         scheme    authority  path       query

          mailto:John.Doe@example.com
          └──┬─┘ └─────────┬────────┘
          scheme         path

          news:comp.infosystems.www.servers.unix
          └─┬┘ └───────────────┬───────────────┘
         scheme              path

          tel:+1-816-555-1212
          └┬┘ └──────┬──────┘
        scheme     path

          telnet://192.0.2.16:80/
          └──┬─┘ └──────┬──────┘│
          scheme    authority  path

          urn:oasis:names:specification:docbook:dtd:xml:4.1.2
          └┬┘ └──────────────────────┬──────────────────────┘
        scheme                     path

        */

		Atom scheme = (Atom) atomic(schemeStr); //TODO cache these commonly used

		//TODO use more reliable path parser
		List<String> pathComponents = Splitter.on('/').omitEmptyStrings().splitToList(pathStr);

		Term path = p(pathComponents.toArray(ArrayUtil.EMPTY_STRING_ARRAY));
		/*TODO parse*/
		return inh(authority == null || authority.isEmpty() ? path : PROD.the(INH.the(path, /*TODO parse*/atomic(authority))), scheme);
	}


	public static Term seteShort(RoaringBitmap why) {
		Subterms ss = shortSubs(why);
		return terms.compoundNew(SETe, DTERNAL, ss);
		//return SETe.the(ss);
		//return LightCompound.the(Op.SETe, ss);
	}

	public static Term seteShort(short[] why) {
		Arrays.sort(why);
		Subterms ss = shortSubs(why);
		return terms.compoundNew(SETe, DTERNAL, ss);
		//return SETe.the(ss);
		//return LightCompound.the(Op.SETe, ss);
	}

    public static Compound $$c(String s) {
        return (Compound) $$$(s);
    }
	public static Variable $$v(String s) {
		return (Variable) $$$(s);
	}
}
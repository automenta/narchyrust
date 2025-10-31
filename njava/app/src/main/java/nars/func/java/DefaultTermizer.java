package nars.func.java;

import com.google.common.collect.ImmutableSet;
import jcog.TODO;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.data.map.CustomConcurrentHashMap;
import nars.$;
import nars.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;


/**
 * Created by me on 8/19/15.
 */
public class DefaultTermizer implements Termizer {


	public static final Variable INSTANCE_VAR = $.varDep("instance");
	//    static final Term TRUE_TERM =
//            Atomic.the("TRUE");
//            //Bool.True;
//    static final Term FALSE_TERM =
//            TRUE_TERM.neg();
//            //Bool.False;
	static final Set<Class> classInPackageExclusions = ImmutableSet.of(
		Class.class,
		Object.class,
		Float.class,
		Double.class,
		Boolean.class,
		Character.class,
		Long.class,
		Integer.class,
		Short.class,
		Byte.class,
		Class.class
	);
	final Map<Term, Object> termToObj = new CustomConcurrentHashMap<>(STRONG, EQUALS, STRONG /*SOFT*/, IDENTITY, 64);

	/*final HashMap<Term, Object> instances = new HashMap();
	final HashMap<Object, Term> objects = new HashMap();*/
	final Map<Object, Term> objToTerm = new CustomConcurrentHashMap<>(STRONG /*SOFT*/, IDENTITY, STRONG, EQUALS, 64);

	public DefaultTermizer() {
		termToObj.put(Bool.True, true);
		termToObj.put(Bool.False, false);
		objToTerm.put(true, Bool.True);
		objToTerm.put(false, Bool.False);
	}

	private static Bool booleanToBoolTerm(boolean b) {
		return b ? Bool.True : Bool.False;
	}

	protected static Term number(Number o) {
		return $.the(o);
	}

	private static boolean reportClassInPackage(Class oc) {
		if (classInPackageExclusions.contains(oc)) return false;
        else
            return !Term.class.isAssignableFrom(oc) && !oc.isPrimitive();
    }

	/**
	 * (#arg1, #arg2, ...), #returnVar
	 */

	private static Term[] getMethodArgVariables(Method m) {
		String varPrefix = m.getName() + '_';
		int n = m.getParameterCount();
		Term args = $.p(getArgVariables(varPrefix, n));

		return m.getReturnType() == void.class ? new Term[]{
			INSTANCE_VAR,
			args
		} : new Term[]{
			INSTANCE_VAR,
			args,
			$.varDep(varPrefix + "_return")
		};
	}

	private static Term[] getArgVariables(String prefix, int numParams) {
		List<Variable> list = new Lst<>(numParams);
		for (int i = 0; i < numParams; i++)
			list.add($.varDep(prefix + i));
		return list.toArray(EmptyTermArray);
	}

	public static Term classTerm(Class c) {
		return Atomic.atom(c.getSimpleName());
	}

	public static Term termClassInPackage(Class c) {
		return $.p(termPackage(c.getPackage()), classTerm(c));
	}

	public static Term termPackage(Package p) {
		return $.p(p.getName().split("\\."));
	}

	/**
	 * generic instance target representation
	 */
	public static Term instanceTerm(Object o) {
		return $.p(System.identityHashCode(o), 36);
	}

	private static boolean cacheableInstance(Object o) {
		return true;
	}

	public void put(Term x, Object y) {
		assert (x != y);
		termToObj.put(x, y);
		objToTerm.put(y, x);
	}

	public void remove(Term x) {
		objToTerm.remove(termToObj.remove(x));
	}

	public void remove(Object x) {
		termToObj.remove(objToTerm.remove(x));
	}

	/**
	 * dereference a target to an object (but do not un-termize)
	 */
	@Override
	public @Nullable Object object(Term t) {

		if (t == NULL) return null;
		else if (t.INT())
			return Int.i(t);
		else {
			Object x = termToObj.get(t);
			return x == null ? t : x;  /** if null, return the target intance itself */
		}

	}

	@Nullable
	Term obj2term(@Nullable Object o) {
		@Nullable Term result;

        switch (o) {
            case null -> result = NULL;
            case Term term -> result = term;
            case String s -> result = $.quote(o);
            case Boolean b -> result = booleanToBoolTerm(b);
            case Character character -> result = $.quote(String.valueOf(o));
            case Number number -> result = number(number);
            case Class oc -> result = classTerm(oc);
            case Path path -> result = $.the(path);
            case URI uri -> result = $.the(uri);
            case URL url -> result = $.the(url);
            case int[] ints -> result = $.p(ints);
            case Object[] objects -> {
                List<Term> arg = Arrays.stream(objects).map(this::term).toList();
                result = arg.isEmpty() ? EmptyProduct : $.p(arg);
            }
            case List list -> {
                if (!((Collection) o).isEmpty()) {
                    Collection c = (Collection) o;

                    List<Term> arg = (List<Term>) new Lst(c.size());
                    for (Object x : c)
                        arg.add(term(x));

                    result = arg.isEmpty() ? EmptyProduct : $.p(arg);
                } else
                    result = EmptyProduct;
            }
            case Set set -> {
                Collection<Term> arg = (Collection<Term>) ((Collection) o).stream().map(this::term).toList();
                result = arg.isEmpty() ? EmptyProduct : SETe.the(arg);
            }
            case Map mapo -> {

                List<Term> components = new Lst(mapo.size());
                mapo.forEach((k, v) -> {

                    Term tv = obj2term(v);
                    Term tk = obj2term(k);

                    if ((tv != null) && (tk != null)) {
                        components.add(
                                INH.the(tv, tk)
                        );
                    }
                });
                result = components.isEmpty() ? EmptyProduct : SETe.the(components);
            }
            default -> result = instanceTerm(o);
        }

		return result;
	}

	protected @Nullable Term classInPackage(Term classs, @Deprecated Term packagge) {

		return null;
	}

	public Term[] terms(Object[] args) {
		return Util.map(this::term, Term[]::new, args);
	}

	@Override
	public @Nullable Term term(@Nullable Object o) {
		if (o instanceof Term tt)
			return tt;

		if (o == null)
			return NULL;
		else if (o instanceof Boolean bb) {
			return booleanToBoolTerm(bb);
		} else if (o instanceof Number) {
            return switch (o) {
                case Byte b -> Int.i(b.intValue());
                case Short s  -> Int.i(s.intValue());
                case Integer i  -> Int.i(i.intValue());
                case Long l0 when Math.abs(l0) < Integer.MAX_VALUE - 1 -> Int.i(l0.intValue());
				case Long l -> $.atomic(Long.toString(l)); // beyond an Int's capacity
                case Float v -> $.the(v.doubleValue());
                case Double d -> $.the(d.doubleValue());
                default -> throw new TODO("support: " + o + " (" + o.getClass() + ')');
            };
		} else if (o instanceof String ss)
			return Atomic.atomic(ss);

		Term y = obj2termCached(o);
		if (y != null)
			return y;

		if (o instanceof Object[] oo)
			return PROD.the(terms(oo));

		return null;
	}

	public @Nullable Term obj2termCached(@Nullable Object o) {

        switch (o) {
            case null -> {
                return NULL;
            }
            case Term term -> {
                return term;
            }
            case Integer i -> {
                return Int.i(i);
            }
            default -> {
            }
        }


        Term oe;
		if (cacheableInstance(o)) {
			oe = objToTerm.get(o);
			if (oe == null) {
				Term ob = obj2term(o);
				if (ob != null) {
					objToTerm.put(o, ob);
					return ob;
				} else
					return $.atomic("Object_" + System.identityHashCode(o));
			}
		} else {
			oe = obj2term(o);
		}

		return oe;
	}

	protected void onInstanceChange(Term oterm, Term prevOterm) {

	}

	protected void onInstanceOfClass(Object o, Term oterm, Term clas) {

	}

//    
//    public static <T extends Term> Map<Atomic,T> mapStaticClassFields( Class c,  Function<Field, T> each) {
//        Field[] ff = c.getFields();
//        Map<Atomic,T> t = $.newHashMap(ff.length);
//        for (Field f : ff) {
//            if (Modifier.isStatic(f.getModifiers())) {
//                T xx = each.apply(f);
//                if (xx!=null) {
//                    t.put(Atomic.the(f.getName()), xx);
//                }
//            }
//        }
//        return t;
//    }


}
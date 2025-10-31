package nars.memory;

import nars.Concept;
import nars.NAR;
import nars.Term;
import nars.concept.NodeConcept;
import nars.concept.PermanentConcept;
import nars.term.Functor;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public abstract class Memory {

	/**
	 * useful for map-like impl
	 */
	static final BiFunction<? super Concept, ? super Concept, ? extends Concept> setOrReplaceNonPermanent = (prev, next) ->
		prev instanceof PermanentConcept && !(next instanceof PermanentConcept) ? prev : next;

	protected NAR nar;

    /**
	 * internal get procedure (synchronous)
	 *
	 * @return
	 */
	public abstract Concept get(Term key, boolean createIfMissing);

	/**
	 * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
	 * be replaced with a non-Permanent concept
	 */
	public abstract void set(Term src, Concept target);

	public final void set(Concept t) {
		set(t.term(), t);
	}

	public final void set(Functor t) {
        var c = new NodeConcept.FunctorConcept(t);
		nar.conceptBuilder.start(c);
		set(t, c);
	}

	public abstract void clear();

	public void start(NAR nar) {
		this.nar = nar;
	}

	/**
	 * # of contained terms
	 */
	public abstract int size();

	/**
	 * a string containing statistics of the index's current state
	 */
	public abstract String summary();

	public abstract @Nullable Concept remove(Term entry);

	public void print(PrintStream out) {
		stream().forEach(out::println);
		out.println();
	}

	public abstract Stream<Concept> stream();

	/**
	 * default impl
	 *
	 * @param c
	 */
	public void forEach(Consumer<? super Concept> c) {
		stream().forEach(c);
	}

    /**
	 * for performance, if lookup of a Concept instance is performed using
	 * a supplied non-deleted Concept instance, return that Concept directly.
	 * ie. it assumes that the known Concept is the active one.
	 * <p>
	 * this can be undesirable if the concept index has an eviction mechanism
	 * which counts lookup frequency, which would be skewed if elision is enabled.
	 */
	public boolean elideConceptGets() {
		return true;
	}

	/**
	 * called after each removal
	 */
	protected final void onRemove(Concept c) {
		if (c instanceof PermanentConcept)
			set(c); //HACK
		else {
//			if (traceRemove) traceRemove(c);
			c.delete();
			nar.emotion.conceptDelete.increment();
		}
	}

//	boolean traceRemove = false;
//	private void traceRemove(Concept c) {
//		System.out.println("(" + size() + ") forget: " + c.term);
//	}

	public boolean delete(Term x) {
		@Nullable var c = remove(x.concept());
		if (c!=null) {
			c.delete();
			return true;
		}
		return false;
	}
}
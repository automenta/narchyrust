package nars.memory;

import com.github.benmanes.caffeine.cache.*;
import com.google.common.util.concurrent.MoreExecutors;
import jcog.exe.Exe;
import nars.Concept;
import nars.NAL;
import nars.NAR;
import nars.Term;
import nars.concept.PermanentConcept;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;


public class CaffeineMemory extends Memory implements Executor, /*CacheLoader<Term, Concept>,*/ RemovalListener<Term, Concept> {

	private static final boolean EXECUTOR_INLINE = true;

	private final Cache<Term, Concept> concepts;
	private final boolean weightDynamic;
	private transient Function<Term, Concept> conceptConstructor;
	//    private DurLoop cleanup;

	private volatile boolean clearing = false;

	public static final Weigher<Term,Concept> UniformWeigher = (k, v) ->
		v instanceof PermanentConcept ? 0 : 1;
	public static final Weigher<Term,Concept> ComplexityWeigher = (k, v) ->
		v instanceof PermanentConcept ? 0 : k.complexity();
	public static final Weigher<Term,Concept> ComplexityConstantsWeigher = (k, v) ->
		v instanceof PermanentConcept ? 0 : (int)k.complexityConstants();




	public CaffeineMemory(int sizeMax) {
		this(Caffeine.newBuilder().initialCapacity(sizeMax).maximumWeight(sizeMax).weigher(
			UniformWeigher
		), false);
	}

	public CaffeineMemory(ToIntFunction<Term> staticWeigher, long weightMax) {
		this((k, v) -> v instanceof PermanentConcept ? 0 : staticWeigher.applyAsInt(k)
		, weightMax);
	}
	public CaffeineMemory(Weigher<Term,Concept> w, long weightMax) {
		this(Caffeine.newBuilder().maximumWeight(weightMax).weigher(w), false);
	}

	public CaffeineMemory(long weightMax, ToIntFunction<Concept> dynamicWeigher) {
		this(Caffeine.newBuilder().maximumWeight(weightMax).weigher((Term k, Concept v) ->
			v instanceof PermanentConcept ? 0 : dynamicWeigher.applyAsInt(v)
		), true);
	}


	private CaffeineMemory(Caffeine b, boolean weightDynamic) {
		super();
		this.weightDynamic = weightDynamic;
		b.removalListener(this);

		b.executor(EXECUTOR_INLINE ? MoreExecutors.directExecutor() : this);

		b.scheduler(Scheduler.systemScheduler());
		//b.scheduler(Scheduler.disabledScheduler());

		this.concepts = b.<Term, Concept>build(/*this*/);
	}

	public static CaffeineMemory soft() {
		return new CaffeineMemory(Caffeine.newBuilder().softValues(), false);
	}

	public static CaffeineMemory weak() {
		return new CaffeineMemory(Caffeine.newBuilder().weakValues(), false);
	}



	@Override
	public Stream<Concept> stream() {
		return map().values().stream().filter(Objects::nonNull);
	}


	@Override
	public void start(NAR nar) {
		conceptConstructor = nar.conceptBuilder::apply;
		super.start(nar);
	}

	/**
	 * caffeine may measure accesses for eviction
	 */
	@Override
	public final boolean elideConceptGets() {
		return false;
	}

	@Override
	public @Nullable Concept remove(Term x) {
		return map().remove(x);
	}

	private ConcurrentMap<Term, Concept> map() {
		return concepts.asMap();
	}

	@Override
	public void set(Term src, Concept target) {
		map().merge(src, target, setOrReplaceNonPermanent);
	}



	@Override
	public void clear() {
		synchronized(concepts) {
			clearing = true;
			concepts.invalidateAll();
			clearing = false;
		}
	}

	@Override
	public void forEach(Consumer<? super Concept> c) {
		map().values().forEach(c);
	}

	@Override
	public int size() {
		return (int) concepts.estimatedSize();
	}


	@Override
	public Concept get(Term x, boolean createIfMissing) {
		Concept y = createIfMissing ?
			concepts.get(x, conceptConstructor) :
			concepts.getIfPresent(x);

		if (createIfMissing && weightDynamic /*&& y != null*/)
			concepts.put(x, y);

		return y;
	}


	@Override
	public String summary() {

		String s = concepts.estimatedSize() + " concepts, ";

		if (NAL.DEBUG)
			s += ' ' + concepts.stats().toString();

		return s;
	}

	/**
	 * this will be called from within a worker task
	 */
	@Override
	public final void onRemoval(Term key, Concept value, RemovalCause cause) {
		if (value != null && cause.wasEvicted())
			onRemove(value);
//		if (value instanceof PermanentConcept && !clearing) {
//			//reinsert HACK  TODO shouldn't happen
//			set(value.term(), value);
//		}
	}

	@Override
	public final void execute(Runnable r) {
		NAR nar = this.nar;
		(nar !=null ? nar.exe : Exe.executor()).execute(r);
	}


}
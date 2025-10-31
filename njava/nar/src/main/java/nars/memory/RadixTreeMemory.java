package nars.memory;

import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.Util;
import jcog.data.byt.ByteSequence;
import jcog.tree.radix.ConcurrentRadixTree;
import jcog.tree.radix.MyRadixTree;
import nars.Concept;
import nars.NAR;
import nars.Term;
import nars.concept.PermanentConcept;
import nars.term.Termed;
import nars.term.util.map.TermRadixTree;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static jcog.Util.PHI_min_1f;

/**
 * concurrent radix tree index
 * TODO restore byte[] sequence writing that doesnt prepend atom length making leaves unfoldable by natural ordering
 */
public class RadixTreeMemory extends Memory implements Consumer<NAR> {


    public final ConceptRadixTree concepts;

	float maxIterationRemovalPct = 0.05f;
	float overflowSafetyPct = 0.1f;

	private static ByteSequence key(Term k) {
		return TermRadixTree.termByVolume(k.concept());
	}

	public RadixTreeMemory(int sizeLimit) {

		this.concepts = new ConceptRadixTree(sizeLimit);

	}

	@Override
	public Stream<Concept> stream() {
		return Streams.stream(concepts);
	}

	@Override
	public void start(NAR nar) {
		super.start(nar);

		nar.onDur(this);
	}



	private int sizeEst() {
		return concepts.sizeEst();
	}


	private static boolean removeable(Concept c) {
		return !(c instanceof PermanentConcept);
	}


	@Override
	public Concept get(Term t, boolean createIfMissing) {
		ByteSequence k = key(t);

		ConceptRadixTree c = this.concepts;

		return createIfMissing ?
			c.putIfAbsent(k, () -> nar.conceptBuilder.apply(t, null))
			:
			c.get(k);
	}

	@Override
	public void set(Term src, Concept target) {

		ByteSequence k = key(src);

		concepts.acquireWriteLock();
		try {
			Termed existing = concepts.get(k);
			if (existing != target && !(existing instanceof PermanentConcept)) {
				concepts.put(k, target);
			}
		} finally {
			concepts.releaseWriteLock();
		}
	}

	@Override
	public void clear() {
		concepts.clear();
	}

	@Override
	public void forEach(Consumer<? super Concept> c) {
		concepts.forEach(c);
	}

	@Override
	public int size() {
		return concepts.size();
	}


	@Override
	public String summary() {

		return concepts.sizeEst() + " concepts";
	}


	@Override
	public @Nullable Concept remove(Term entry) {
		ByteSequence k = key(entry);
		Concept result = concepts.get(k);
		if (result != null) {
			boolean removed = concepts.remove(k);
			if (removed)
				return result;
		}
		return null;
	}


	private void onRemoval(Concept value) {
		onRemove(value);
	}

	@Override
	public void accept(NAR eachFrame) {
		concepts.forgetNext();
	}


	public class ConceptRadixTree extends ConcurrentRadixTree<Concept> {

		private final int sizeLimit;

		/**
		 * since the terms are sorted by a volume-byte prefix, we can scan for removals in the higher indices of this node
		 */
		private MyRadixTree.ByteNode volumeWeightedRoot(RandomGenerator rng) {

			List<MyRadixTree.ByteNode<Concept>> l = concepts.root.out();
			int levels = l.size();
			float r = Util.sqr(rng.nextFloat());
			return l.get(Math.round((levels - 1) * (1 - r)));
		}


		ConceptRadixTree(int sizeLimit) {
			this.sizeLimit = sizeLimit;
		}

		@Override
		public final Concept put(Concept value) {
			return super.put(key(value.term()), value);
		}

		@Override
		public boolean onRemove(Concept c) {
			if (removeable(c)) {
				onRemoval(c);
				return true;
			} else {
				return false;
			}

		}

		final AtomicBoolean forgetting = new AtomicBoolean();


		private void forgetNext() {

			if (!forgetting.weakCompareAndSetAcquire(false, true))
				return;

			try {
				int sizeBefore = super.sizeEst();

				int overflow = sizeBefore - sizeLimit;
				if (overflow < 0)
					return;

				int maxConceptsThatCanBeRemovedAtATime = (int) Math.max(1, sizeBefore * maxIterationRemovalPct);

				if ((((float) overflow) / sizeLimit) > overflowSafetyPct) {
					//major collection, strong
					concepts.acquireWriteLock();
				} else {
					//minor collection, weak
					if (!concepts.tryAcquireWriteLock())
						return;
				}

				try {
					SearchResult s = null;
					RandomGenerator rng = nar.random();
					int iterationLimit = 16;
					while ((iterationLimit-- > 0) && ((super.sizeEst() - sizeLimit) > maxConceptsThatCanBeRemovedAtATime)) {

						ByteNode subRoot = volumeWeightedRoot(rng);

						if (s == null)
							s = concepts.random(subRoot, PHI_min_1f, rng);

						ByteNode f = s.found;

						if (f != null) {
							if (f != subRoot) {
								int subTreeSize = concepts.sizeIfLessThan(f, maxConceptsThatCanBeRemovedAtATime);

								if (subTreeSize > 0) {
									concepts.removeWithWriteLock(s, true);
								}
							}

							s = null; //try again
						}

					}

					if (iterationLimit == 0)
						throw new TODO();

				} finally {
					concepts.releaseWriteLock();
				}
			} finally {
				forgetting.setRelease(false);
			}

		}

	}
}
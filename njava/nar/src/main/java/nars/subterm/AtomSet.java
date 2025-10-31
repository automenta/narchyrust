package nars.subterm;

import jcog.data.byt.DynBytes;
import jcog.data.map.MRUMap;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.term.util.TermException;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AtomSet implements Predicate<Atomic>, Consumer<Atomic> /* TODO Subterms? */ {

	public final void addAll(Iterable<Term> atoms) {
		atoms.forEach(this::add);
	}

	public final void add(Term x) {
		accept((Atomic)x);
	}

	/** TODO test */
	public static class ByteArrayAtomSet extends AtomSet {

		/** TODO avoid storing duplicates in this list */
		final ShortArrayList offsets;
		int maxLen;
		final DynBytes buffer;

		public ByteArrayAtomSet(int sizeEstimate) {
			 offsets = new ShortArrayList(sizeEstimate);
			 buffer = new DynBytes(8 * sizeEstimate);
		}

		public boolean test(Atomic A) {
            var a = A.bytes();
			if (a.length > maxLen)
				return false; //impossible, longer than entire buffer

            var b = buffer.arrayDirect();
            var s = this.offsets.shortIterator();
            var aLen = a.length;
			while (s.hasNext()) {
				int start = s.next(); int end = s.next();
				if (end - start == aLen && Arrays.equals(b, start, end, a, 0, aLen))
					return true;
			}
			return false;
		}

		@Override
		public void accept(Atomic atom) {
			//TODO subclass with Set to filter duplicates
            var start = buffer.length;
            var a = atom.bytes();
            var end = a.length + start;
			//TODO verify < Short.MAX
			offsets.add((short)start); offsets.add((short)end);
			maxLen = Math.max(maxLen, end-start);
			buffer.write(a);
		}
	}

	public abstract static class ContainingAllAtomsFrom implements Predicate<Term> {

		/** HACK stupid novelty filter */
		final MRUMap<Term,Term> tried = new MRUMap<>(32);
		final int minSize;
		final int structureNecessary;

		private ContainingAllAtomsFrom(int minSize, int structureNecessary) {
			this.minSize = minSize;
			this.structureNecessary = structureNecessary;
		}

		protected abstract boolean _test(Term x);

		public static Predicate<Term> containsAllAtomsFrom(Compound q) {
			var atomSet = q.recurseSubtermsToSet(Op.AtomicConstant);
            var minSize = atomSet.size();
			var structureNecessary = q.struct() & ~(Op.Variables);
			return switch (minSize) {
				case 0 -> throw new TermException("contains no atomic constants", q);
				case 1 -> new ContainingAllAtomsFrom1(minSize, structureNecessary, atomSet);
				default -> new ContainingAllAtomsFromN(minSize, structureNecessary, atomSet);
			};
		}

		@Override public boolean test(Term z) {
            return z.hasAll(structureNecessary) &&
				   (minSize <= 1 || minSize < z.complexity()) &&
				   tried.put(z, z) == null &&
				   _test(z);
        }

		private static class ContainingAllAtomsFromN extends ContainingAllAtomsFrom {

			final Term[] atoms;

			ContainingAllAtomsFromN(int minSize, int structureNecessary, Set<Term> atomSet) {
				super(minSize, structureNecessary);
				atoms = atomSet.toArray(Op.EmptyTermArray);
			}

			@Override
			protected boolean _test(Term x) {
                var cx = (Compound)x;
				for (var a : atoms)
					if (!cx.containsRecursively(a))
						return false;
				return true;
			}
		}

		private static class ContainingAllAtomsFrom1 extends ContainingAllAtomsFrom {
			final Term the;

			ContainingAllAtomsFrom1(int minSize, int structureNecessary, Set<Term> atomSet) {
				super(minSize, structureNecessary);
				the = atomSet.iterator().next();
			}

			@Override
			protected boolean _test(Term x) {
				return x instanceof Compound && x.containsRecursively(the);
			}
		}
	}

//				} else if (minSize <= 3) {
//					//experimental
//					//TODO special case for size == 1
//					AtomSet.ByteArrayAtomSet atoms = new AtomSet.ByteArrayAtomSet(_atoms.size());
//					atoms.addAll(_atoms);
//					test = atoms;

}
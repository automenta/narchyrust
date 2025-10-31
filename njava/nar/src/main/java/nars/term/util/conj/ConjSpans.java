//package nars.term.util.conj;
//
//import jcog.data.bit.MetalBitSet;
//import nars.NALTask;
//import org.eclipse.collections.api.list.MutableList;
//import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
//import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
//
//import java.util.List;
//import java.util.function.IntPredicate;
//
//import static nars.Op.ETERNAL;
//import static nars.time.Tense.dither;
//
//public enum ConjSpans { ;
//
//	//TODO: boolean inclStart, boolean inclEnd, int intermediateDivisions
//
//	/** returns null on failure */
//	public static boolean add(List<NALTask> tt, int dither, IntPredicate componentPolarity, ConjBuilder b) {
//		int n = tt.size();
//		if (n == 0)
//			return false; //nothing
////
////		if (n == 1) {
////			//optimized case
////			NALTask t = tt.get(0);
////			long s = t.start();
////			if (s == ETERNAL) {
////				if (!b.add(ETERNAL, t.term().negIf(!componentPolarity.test(0))))
////					return null;
////			}
////			long e = t.end();
////			return b.add(s, t.term()) && ((e==s) || b.add(e, t.term())) ? b : null; //TODO maybe negIf
////		}
//
//
//		assert(n < 32); //HACK for MetalBitSet
//
//		LongObjectHashMap<MetalBitSet> inter = new LongObjectHashMap<>(n*2);
//
//		//collect extents of tasks
//		long dur = Long.MAX_VALUE;
//		for (int i = 0, ttSize = tt.size(); i < ttSize; i++) {
//			NALTask t = tt.get(i);
//			long s = t.start();
//			if (s == ETERNAL) {
//				if (!b.add(ETERNAL, t.term().negIf(!componentPolarity.test(i))))
//					return false;
//			} else {
//				s = dither(s, dither, -1);
//				long e = dither(t.end(), dither, +1);
//				dur = Math.min(dur, e - s);
//				inter.getIfAbsentPut(s, MetalBitSet::full).set(i);
//			}
//		}
//		assert(dur!=Long.MAX_VALUE); //it would mean that all events are eternal
//
//		//add adjusted endpoint
//		for (int i = 0, ttSize = tt.size(); i < ttSize; i++) {
//			NALTask t = tt.get(i);
//			long s = t.start();
//			if (s != ETERNAL) {
//				long e = dither(t.end() - dur, dither, +1);
//				if (e != s)
//					inter.getIfAbsentPut(e, MetalBitSet::full).set(i);
//			}
//		}
//
//		MutableList<LongObjectPair<MetalBitSet>> w = inter.keyValuesView().toSortedList();
//		int wn = w.size();
//		//add intermediate overlapping events
//		for (int i = 0, ttSize = tt.size(); i < ttSize; i++) {
//			NALTask t = tt.get(i);
//			long s = t.start();
//			if (s == ETERNAL) continue; //ignore, already added
//			s = dither(s, dither, -1);
//			long e = dither(t.end() - dur, dither, +1);
//			for (int j = 0; j < wn; j++) {
//				LongObjectPair<MetalBitSet> ww = w.get(j);
//				long wj = ww.getOne();
//				if (wj > s && wj < e)
//					ww.getTwo().set(i);
//			}
//		}
//
////		Term[] terms = Util.map(n, Term[]::new, k ->
////			tt.get(k).term().negIf(!componentPolarity.test(k)));
//
//		//add to builder
//		for (int j = 0; j < wn; j++) {
//			LongObjectPair<MetalBitSet> ww = w.get(j);
//			long W = ww.getOne();
//			int k = -1;
//			while ((k = ww.getTwo().next(true, k+1, n))!=-1) {
//				if (!b.add(W, tt.get(k).term().negIf(!componentPolarity.test(k))))
//					return false;
//			}
//		}
//		return true;
//	}
//}
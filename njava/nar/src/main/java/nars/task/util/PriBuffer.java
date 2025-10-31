package nars.task.util;

import jcog.TODO;
import jcog.event.Off;
import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import jcog.util.ConsumerX;
import jcog.util.PriReturn;
import nars.NAL;
import nars.NALTask;
import nars.NAR;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * regulates a flow of supplied tasks to a target consumer
 * TODO some of this can be moved to util/
 */
@Deprecated public abstract class PriBuffer<T extends Prioritized & Prioritizable> implements Consumer<T> {


	/**
	 * returns
	 * true if the implementation will manage async target suppliying,
	 * false if it needs periodic flushing
	 */
	public abstract void start(ConsumerX<T> target, NAR nar);

	public void stop() {
	}

	/**
	 * returns the input task, or the existing task if a pending duplicate was present
	 * @return boolean if accepted?
	 */
	public abstract boolean put(T x);


	public abstract void clear();

	/**
	 * known or estimated number of tasks present
	 */
	public abstract int size();

	public abstract int capacity();

	/**
	 * estimate current utilization: size/capacity (as a value between 0 and 100%)
	 */
	public final float load() {
		return ((float) size()) / capacity();
	}

	//TODO
	//final AtomicLong in = new AtomicLong(), out = new AtomicLong(), drop = new AtomicLong(), merge = new AtomicLong();

	@Override
	public final void accept(T task) {
		put(task);
	}



	public abstract static class SyncPriBuffer<X extends Prioritizable> extends PriBuffer<X> {
		protected ConsumerX<X> target;
		NAR nar;
		private Off onCycle;

		@Override
		public synchronized void start(ConsumerX<X> target, NAR nar) {
			this.nar = nar;
			this.target = target;
			this.onCycle = nar.onDur(this::commit);
		}

		@Override
		public final synchronized void stop() {
			this.onCycle.close();
			this.onCycle = null;
			this.target = null;
			this.nar = null;
		}

		protected abstract void commit();
	}

	/**
	 * buffers and deduplicates in a Bag<Task,Task> allowing higher priority inputs to evict
	 * lower priority inputs before they are flushed.  commits involve a multi-thread shareable drainer task
	 * allowing multiple inputting threads to fill the bag, potentially deduplicating each's results,
	 * while other thread(s) drain it in prioritized order as input to NAR.
	 */
	@Deprecated public static class PriBagTaskBuffer extends SyncPriBuffer<NALTask> {

		public final IntRange capacity = new IntRange(0, 0, 4 * 1024);

		/**
		 * perceptual valve
		 * dilation factor
		 * input rate
		 * tasks per cycle
		 */
		public final FloatRange valve = new FloatRange(0.5f, 0, 1);

		private transient long prev = Long.MIN_VALUE;
		/**
		 * temporary buffer before input so they can be merged in case of duplicates
		 */
		public final Bag<NALTask, NALTask> tasks;

		@Override
		public int capacity() {
			return capacity.intValue();
		}

		{
			Bag<NALTask, NALTask> activates = new PriArrayBag<>(NAL.taskPriMerge, 0) {
				@Override
				protected int histogramBins(int s) {
					return 0; //disabled
				}

				/**
				 * merge in the pre-buffer
				 */
				@Override
				protected float merge(NALTask existing, NALTask incoming, float incomingPri) {
					return NALTask.merge(existing, incoming, merge, PriReturn.Result);
				}

			};
			PriMap<NALTask> conceptPriMap = new PriMap<>() {
				/**
				 * merge in the post-buffer
				 */
				@Override
				public float merge(Prioritizable existing, NALTask incoming, float pri, PriMerge merge) {
					return NALTask.merge((NALTask) existing, incoming, merge, PriReturn.Delta);
				}
			};
			tasks = new BufferedBag<>(activates, conceptPriMap);
		}
//                new PriArrayBag<ITask>(PriMerge.max, new HashMap()
//                        //new UnifiedMap()
//                ) {
//                    @Override
//                    protected float merge(ITask existing, ITask incoming) {
//                        return TaskBuffer.merge(existing, incoming);
//                    }
//                };

		//new HijackBag...


		/**
		 * @capacity size of buffer for tasks that have been input (and are being de-duplicated) but not yet input.
		 * input may happen concurrently (draining the bag) while inputs are inserted from another thread.
		 */
		public PriBagTaskBuffer(int capacity, float rate) {
			this.capacity.set(capacity);
			this.valve.set(rate);
			this.tasks.setCapacity(capacity);
		}


		@Override
		public void clear() {
			tasks.clear();
		}

//        final AtomicBoolean busy = new AtomicBoolean(false);

		@Override
		public int size() {
			return tasks.size();
		}


		@Override
		public boolean put(NALTask x) {
            return tasks.put(x)==x;
        }

		@Override
		public void start(ConsumerX<NALTask> target, NAR nar) {
			prev = nar.time();
			super.start(target, nar);
		}

		final AtomicInteger pending = new AtomicInteger();

		@Override
		public void commit() {

			long now = nar.time();

			long dt = now - prev;

			prev = now;

			Bag<NALTask, NALTask> b = this.tasks;

			b.setCapacity(capacity.intValue());
			b.commit(null);

			int s = b.size();
			if (s > 0) {
				int cc = target.concurrency();
				int toRun = cc - pending.getOpaque();
				if (toRun >= 0) {
					int n = Math.min(s, batchSize((((float) toRun) / cc) * dt / nar.dur()));
					if (n > 0) {
						//TODO target.input(tasks, n, target.concurrency());


						if (toRun == 1 || n == 1) {
							//one at a time
							b.sampleOrPop(null, true, n, target);
						} else {
							//batch
							int remain = n;
							int nEach = (int) Math.ceil(((float) remain) / toRun);


							for (int i = 0; i < toRun && remain > 0; i++) {
								int asked = Math.min(remain, nEach);
								remain -= asked;
								throw new TODO();
							}
						}
					}

				}
			}


		}


		/**
		 * TODO abstract
		 */
		protected int batchSize(float durs) {
			return (int) Math.ceil(Math.min(durs, 1.0f) * capacity() * valve.floatValue());

		}


	}


}
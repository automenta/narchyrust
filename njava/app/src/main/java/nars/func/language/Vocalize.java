package nars.func.language;

import com.google.common.collect.TreeBasedTable;
import jcog.data.list.Lst;
import nars.NAR;
import nars.Term;
import nars.Truth;
import nars.truth.util.TruthAccumulator;
import nars.util.NARPart;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

public class Vocalize extends NARPart {

//    public static final Term PREPOSITION = $.the("preposition");
//    public static final Term PRONOUN = $.the("pronoun");
    /**
     * when, word, truth
     */
    final TreeBasedTable<Long, Term, TruthAccumulator> vocalize = TreeBasedTable.create();
    private final Consumer<Term> speak;
    private final float durationsPerWord;
    private float energy;
    private static final float expectationThreshold = 0.5f;

    public Vocalize(NAR nar, float durationsPerWord, Consumer<Term> speak) {
        super();
        this.durationsPerWord = durationsPerWord;
        this.speak = speak;
        this.energy = 0;
        nar.add(this);
    }

    @Override
    protected void starting(NAR nar) {
        on(
                nar.onDur(() -> {
                    energy = Math.min(1f, energy + 1f / (this.durationsPerWord));
                    if (energy >= 1f) {
                        energy = 0;
                        next();
                    }
                }),
                nar.eventClear.on(this::clear)
        );

    }

    public void speak(@Nullable Term word, long when, @Nullable Truth truth) {
        if (word == null)
            return;


//        if (when < nar.time() - nar.dur() * durationsPerWord) {
//            return;
//        }

        TruthAccumulator ta;
        synchronized (vocalize) {
            ta = vocalize.get(when, word);
            if (ta == null) {
                ta = new TruthAccumulator();
                vocalize.put(when, word, ta);
            }
        }

        ta.add(truth);

//        System.out.println(when + " " + word + " " + truth);

    }

    private void clear() {
        synchronized (vocalize) {
            vocalize.clear();
        }
    }

    public boolean next() {


        float dur = nar.dur() * durationsPerWord;
        long now = nar.time();
        long startOfNow = now - (int) Math.ceil(dur);
        long endOfNow = now + (int) Math.floor(dur);


        Lst<Pair<Term, Truth>> pending = new Lst<>(0);
        synchronized (vocalize) {


            SortedSet<Long> tt = vocalize.rowKeySet().headSet(endOfNow);

            if (!tt.isEmpty()) {
                LongArrayList ll = new LongArrayList(tt.size());
                for (Long aLong : tt)
                    ll.add(aLong);


                ll.forEach(t -> {
                    Set<Map.Entry<Term, TruthAccumulator>> entries = vocalize.row(t).entrySet();
                    if (t >= startOfNow) {
                        for (Map.Entry<Term, TruthAccumulator> e : entries) {
                            Truth x = e.getValue().commitSum();
                            if (x.expectation() > expectationThreshold)
                                pending.add(Tuples.pair(e.getKey(), x));
                        }
                    }
                    entries.clear();
                });
            }
        }
        if (pending.isEmpty())
            return true;

        //System.out.println("pre-vocalize: " + pending);

        Term spoken = decide(pending);
        if (spoken != null)
            speak.accept(spoken);

        return true;
    }

    /**
     * default greedy decider by truth expectation
     */
    private static @Nullable Term decide(Lst<Pair<Term, Truth>> pending) {
        return pending.max((a, b) -> {
            double ta = a.getTwo().expectation();
            double tb = b.getTwo().expectation();
            if (ta > tb) {
                return Double.compare(ta, tb);
            } else {
                return a.getOne().compareTo(b.getOne());
            }
        }).getOne();
    }
}
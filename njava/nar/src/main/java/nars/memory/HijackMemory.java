package nars.memory;

import jcog.Log;
import jcog.Str;
import jcog.Util;
import jcog.pri.NLink;
import jcog.pri.Prioritized;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriMult;
import jcog.signal.FloatRange;
import jcog.sort.QuickSort;
import nars.Concept;
import nars.NAR;
import nars.Term;
import nars.concept.PermanentConcept;
import nars.term.Termed;
import nars.time.part.DurLoop;
import org.HdrHistogram.Histogram;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.Str.n4;
import static nars.Op.XTERNAL;

/**
 * Created by me on 2/20/17.
 */
public class HijackMemory extends Memory {

    private static final Logger logger = Log.log(HijackMemory.class);
    /**
     * eliding is faster but records less accurate access statistics.
     * but if eliding, cache statistics will not reflect the full amount of access otherwise would be necessary
     */
    private static final boolean ElideGets = false;
    public final PriHijackBag<Term, NLink<Concept>> bag;
    public final FloatRange add = new FloatRange(0.01f /* main temperature */, 0, 1);
    public final FloatRange boost;


    public final FloatRange updateRate = FloatRange.unit(
            0.01f
            //0.001f
    );

    /** TODO tune */
    public final FloatRange forget = FloatRange.unit(
            0.01f
            //0.02f
            //0.005f
            //0.001f
            //(float) Ewma.halfLife(64)
    );

    private static final boolean TRACE_AGE = false;
    private final Histogram age = TRACE_AGE ? new Histogram(3) : null;

    private static final boolean
        logAtExit = false,
        logAtExitDetailed = false;

    private transient float priAdd;
    private transient float priBoost;
    private transient DurLoop updater;
    private int updateIndex = 0;
    private long now;


    public HijackMemory(int capacity, int reprobes) {
        super();

        var add = this.add.floatValue();
        var boost =
                //add / Math.min(1, (reprobes / 2f));
                //add/2;
                add;
        this.boost = new FloatRange(boost, 0, 1);

        logger.info("{} concepts, {} reprobes", capacity, reprobes);

        this.bag = new PriHijackBag<>(PriMerge.plus, capacity, reprobes) {

            {
                resize(capacity());
            }

            @Override
            public Term key(NLink<Concept> value) {
                return value.id.term;
            }

            @Override
            protected boolean regrowForSize(int s, int sp) {
                return false;
            }

            @Override
            protected boolean reshrink(int length) {
                return false;
            }

            @Override
            public int spaceMin() {
                return capacity();
            }

            @Override
            protected boolean replace(NLink<Concept> incoming, float inPri, NLink<Concept> existing, float exPri) {
                return HijackMemory.replace(incoming, existing) &&
                        super.replace(incoming, inPri, existing, exPri);
            }

            @Override
            public void onRemove(NLink<Concept> value) {
                HijackMemory.this.onRemove(value.id);
            }
        };
        updatePriIncrements();

        if (logAtExit)
            logAtExit();
    }

    private void logAtExit() {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            synchronized(HijackMemory.class) {
                //bag.forEach(System.out::println);

                NLink<Concept>[] l = bag.stream().toArray(NLink[]::new);
                QuickSort.sort(l, NLink::priElseZero);
                for (var z : l) {
                    //bag.stream().sorted((a,b) -> Float.compare(a.priElseZero(), b.priElseZero())).forEach(
                    //z -> {
                    System.out.println(n4(z.priElseZero()) + "\t" + z.id);
                    if (logAtExitDetailed) {
                        z.get().print();
                        System.out.println();
                    }
                }
                //);
            }
        }));
    }

    private static boolean replace(NLink<Concept> incoming, NLink<Concept> existing) {
        return !(existing.id instanceof PermanentConcept);

//        return existingNotPermanent
//                &&
//                incoming.id.term().volume() <= existing.id.term().volume()
//                ;
    }

    private void updatePriIncrements() {
        priAdd = add.floatValue();
        priBoost = boost.floatValue();
    }

    @Override
    public void start(NAR nar) {
        super.start(nar);
        updater = nar.onDur(this::update);
    }

    /**
     * measures accesses for eviction, so do not elide
     */
    @Override
    public final boolean elideConceptGets() {
        return ElideGets;
    }

    private void update() {

        Consumer<NLink<Concept>> r = TRACE_AGE ? this::recordAge : null;

        var u = this.updateRate.floatValue();
        var f = forget.floatValue();
        var updater =
            f > Prioritized.EPSILON ? Util.compose(
                new PriMult<>(1-f)
                //new PriAdd(-f)
            , r) : r;

        this.now = TRACE_AGE ? nar.time() : XTERNAL;

        var capacity = bag.capacity();
        var n = 1 + (int) (u * capacity);
        var start = updateIndex;

        bag.forEachIndex(start, n, updater);

        //next updateIndex:
        updateIndex = (start + n) % capacity;

        updatePriIncrements();

        if (age!=null) {
//            float minBagSizePctThresh = 0.25f;
//            if (TRACE_AGE && nar.random().nextFloat() < 0.005f && age.getTotalCount() > bag.size() * minBagSizePctThresh)


            if (age.getTotalCount() >= 2L * bag.size()) {
                if (TRACE_AGE)
                    logger.info("{} fill={}\nConcept Age:\n{}", this, n4(((double)this.size())/capacity), Str.histogramString(age, true));
                age.reset();
            }
        }
    }

    private void recordAge(NLink<Concept> x) {
        var t = now - x.id.creation;
        age.recordValue(
            t
            //Math.max(0, t)
        );
    }

    @Override
    public Concept get(Term key, boolean createIfMissing) {
        var x = bag.get(key);
        if (x != null) {
            if (createIfMissing)
                boost(x);
            return x.id;
        } else {
            return createIfMissing ? create(key) : null;
        }
    }

    private @Nullable Concept create(Term x) {
        var c = nar.conceptBuilder.apply(x);
        var inserted = bag.put(insertion(x, c));
        if (inserted == null) {
            return null; //not inserted
            //return c;
        } else {
            return inserted.id;
        }
    }

    private float priPut(Term x) {
        return pri(x, true);
    }

    private float pri(Termed x, boolean addOrBoost) {
        return
            (addOrBoost ? priAdd : priBoost)
             // * Math.pow(0.5, x.term().volume()/32f)
             // * Math.pow(x.term().volume(), -0.25f)
             // * Math.pow(x.voluplexity(), -0.25f);
        ;
    }

    private void boost(NLink<Concept> x) {
        x.priAdd(pri(x.id, false));
        //bag.pressurize(boost);
    }


    @Override
    public void set(Term x, Concept c) {
        var existing = bag.get(x);
        if (existing == null || (existing.id != c && !(existing.id instanceof PermanentConcept))) {
            var inserted = bag.put(insertion(x, c));
            if (inserted == null && (c instanceof PermanentConcept))
                throw new RuntimeException("unresolvable hash collision between PermanentConcepts: " + null + ' ' + c);
        }
    }

    private NLink<Concept> insertion(Term x, Concept c) {
        return new NLink<>(c, priPut(x));
    }

    @Override
    public void clear() {
        bag.clear();
    }

    @Override
    public void forEach(Consumer<? super Concept> c) {
        for (var k : bag)
            c.accept(k.id);
    }

    @Override
    public int size() {
        return bag.size(); /** approx since permanent not considered */
    }

    @Override
    public String summary() {
        return bag.size() + " concepts";
    }

    @Override
    public @Nullable Concept remove(Term entry) {
        var e = bag.remove(entry);
        return e != null ? e.id : null;
    }

    @Override
    public Stream<Concept> stream() {
        return bag.stream().map(z -> z.id);
    }

}
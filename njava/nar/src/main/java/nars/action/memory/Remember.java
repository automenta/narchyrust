package nars.action.memory;

import jcog.signal.meter.SafeAutoCloseable;
import nars.*;
import nars.concept.TaskConcept;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public class Remember implements SafeAutoCloseable {

    /**
     * input as perceived
     */
    public NALTask input;

    /** input as finally remembered */
    public @Nullable NALTask stored;

    @Deprecated public Focus focus;

    public transient TaskConcept concept;

    public boolean activate = true;

    protected Remember() {

    }

    public Remember(Focus f) {
        focus(f);
    }

    public Remember(Focus f, boolean activate) {
        this(f);
        this.activate = activate;
    }

    public Remember(NALTask x, TaskConcept c, Focus f) {
        concept = c;
        focus = f;
        input(x);
    }

    public final Remember focus(Focus f) {
        this.focus = f;
        return this;
    }

    public final boolean input(NALTask x) {
        this.input = x.the();

        verify(x);

        var remembered = false;
        if (tryRemember()) {
            remembered();
            remembered = true;
        }

        if (!remembered || input != stored)
            input.delete();
        if (!remembered && x != input)
            x.delete();

        return remembered;
    }

    private boolean tryRemember() {
        var concept = concept();
        return concept != null &&
            (this.concept = concept).table(input.punc(), true).tryRemember(this);
    }

    private void verify(NALTask x) {
        if (NAL.DEBUG)
            _verify(x);
    }

    private void _verify(NALTask x) {
        if (NAL.test.DEBUG_ENSURE_DITHERED_TRUTH && x.BELIEF_OR_GOAL())
            Truth.assertDithered(x.truth(), nar());

        if (NAL.test.DEBUG_ENSURE_DITHERED_DT || NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
            int d = nar().timeRes();
            if (d > 1) {
//                if (!x.isInput()) {
                if (NAL.test.DEBUG_ENSURE_DITHERED_DT)
                    Tense.assertDithered(x.term(), d);
                if (NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE)
                    Tense.assertDithered(x, d);
//                }
            }
        }
    }

    @Override public final void close() {
        clear();
        focus(null);
    }

    protected void clear() {
        this.stored = this.input = null;
        this.concept = null;
    }

    private void remembered() {
        if (activate)
            focus.activate(stored);
    }

    public final Term target() {
        return (concept != null && input == stored) ? concept.term() : stored.term();
    }


    @Override
    public String toString() {
        return Remember.class.getSimpleName() + '(' + input + ')';
    }

    @Nullable
    protected TaskConcept concept() {
        var c = concept;
        return c !=null ? c : nar().conceptualizeTask(input);
    }

    public final boolean stored() {
        return stored != null;
    }

    public final void unstore(NALTask x) {
        if (stored == x)
            stored = null;
    }

    public final void store(NALTask x) {
        stored = x;
    }

    @Nullable /* HACK */ public NAR nar() {
        Focus f = this.focus;
        return f!=null ? f.nar : null;
    }

    /** current time */
    public final long time() {
        return nar().time();
    }

    public final float dur() {
        return nar().dur();
        //return what.dur();
    }

    public final void rememberNext(NALTask z) {
        input(z);
        clear();
    }

    @Deprecated public final int timeRes() {
        NAR n = nar();
        return n!=null ? n.timeRes() : 1;
    }

//    @Nullable public static Remember clone(@Nullable Remember r) {
//        return r == null ? null : new Remember(r.focus);
//    }
}
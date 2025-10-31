package nars.util;

import jcog.Log;
import jcog.WTF;
import jcog.event.OffOn;
import jcog.thing.Parts;
import jcog.thing.SubPart;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.Term;
import nars.term.Termed;
import nars.time.ScheduledTask;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 *
 */
public abstract class NARPart extends Parts<NAR> implements Termed, OffOn, SubPart<NAR> {

    protected static final Logger logger = Log.log(NARPart.class);

    public final Term id;

    protected NARPart() {
        this((NAR) null);
    }

    @Deprecated protected NARPart(NAR nar) {
        this((Term) null);
        this.nar = nar; //HACK
        if (nar != null)
            on();
    }

    protected NARPart(@Nullable Term id) {
        this.id = id != null ? id : $.identity(this);
    }

    public static Term id(@Nullable Term id, NARPart x) {
        /*NARPart.singleton() ? (x.id) : */
        return id == null ? x.term() : $.p(id, $.identity(x));
    }
    /**
     * resume
     */
    public final void on() {
        nar.add(this);
    }

    @Override public final void close() {
        delete();
    }

    public boolean delete() {

        Term id = this.id;
        logger.debug("delete {}", id);


        sub.removeIf((p) -> {
            ((NARPart)p).delete();
            return true;
        });
        whenDeleted.close();

        NAR n = this.nar;
        if (n != null) {
            n.remove(this);
        } else {
            //experimental hard stop
            try {
                stopping(null);
            } catch (RuntimeException t) {
                logger.warn("stop {}", id, t);
            }
        }

        if (NAL.NAR_PARTS_NULLIFY_NAR_ON_DELETE)
            this.nar = null;
        return true;
    }

    /**
     * optional event occurrence information.  null if not applicable.
     */
    public ScheduledTask event() {
        return null;
    }


    /** MAKE SURE NOT TO CALL THIS DIRECTLY; IT WILL BE INVOKED.  LIKELY YOU WANT: n.start(x) NOT x.start(a) */
    @Override protected final void start(NAR nar) {

        NAR prevNar = this.nar;
        if (!(prevNar == null || prevNar == nar))
            throw new WTF("NAR mismatch");

        logger.debug("start {}", id);

        starting(this.nar = nar);

        startLocal();
    }



    @Override
    protected final void stop(NAR nar) {
        logger.debug(" stop {}", id);

        stopping(nar);
    }


    protected void starting(NAR nar) {

    }

    protected void stopping(NAR nar) {

    }


    @Override
    public final Term term() {
        return id;
    }


    @Override
    public final String toString() {
        return id.toString();
    }


    /**
     * pause, returns a one-use resume ticket
     */
    @Deprecated public final Runnable pause() {
        NAR n = this.nar;
        if (n != null) {
            if (n.stop(this)) {
                logger.debug("pause {}", this);
                return () -> {
                    NAR nn = this.nar;
                    if (nn == null) {
                        //deleted or unstarted
                    } else {
                        if (nn.add(this))
                            logger.debug("resume {}", this);
                    }
                };
            }
        }
        //return new SelfDestructAfterRunningOnlyOnce(nn);
        return () -> {};
    }


}
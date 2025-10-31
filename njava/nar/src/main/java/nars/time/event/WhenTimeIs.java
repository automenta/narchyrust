package nars.time.event;

import nars.$;
import nars.NAR;
import nars.Term;
import nars.time.ScheduledTask;

import java.util.function.Consumer;

public abstract class WhenTimeIs extends ScheduledTask {



    public static WhenTimeIs then(long whenOrAfter, Object then) {
        return then instanceof Runnable ? new WhenTimeIs_Run(whenOrAfter, (Runnable) then) : new WhenTimeIs_Consume(whenOrAfter, (Consumer) then);
    }

    private static final class WhenTimeIs_Consume extends WhenTimeIs {
        private final Consumer<NAR> then;

        private WhenTimeIs_Consume(long whenOrAfter, Consumer<NAR> then) {
            super(whenOrAfter);
            this.then = then;
        }

        @Override
        public void accept(NAR nar) {
            then.accept(nar);
        }

        @Override
        protected Object _id() {
            return then;
        }
    }

    private static final class WhenTimeIs_Run extends WhenTimeIs {
        private final Runnable then;

        private WhenTimeIs_Run(long whenOrAfter, Runnable then) {
            super(whenOrAfter);
            this.then = then;
        }


        @Override
        public void accept(NAR nar) {
            then.run();
        }

        @Override
        protected Object _id() {
            return then;
        }
    }

    WhenTimeIs(long next) {
        this.next = next;
    }

    @Override
    public Term term() {
        return $.p($.identity(_id()), $.the(next));
    }

    protected abstract Object _id();

}
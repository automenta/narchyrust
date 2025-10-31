package nars.time;

import jcog.TODO;
import jcog.event.Off;
import nars.NAR;

import java.util.function.Consumer;

public enum Every {

    Cycle {
        @Override
        public Off on(NAR nar, float amount, Consumer<NAR> c) {
            if (amount != 1) throw new TODO();
            return nar.onCycle(c);
        }
    },

    Duration {
        @Override
        public Off on(NAR nar, float amount, Consumer<NAR> c) {
            return nar.onDur(c).durs(amount);
        }
    },

    Second {
        @Override
        public Off on(NAR nar, float amount, Consumer<NAR> c) {
            throw new TODO();
        }
    };

    //etc..

    public abstract Off on(NAR nar, float amount, Consumer<NAR> c);
}
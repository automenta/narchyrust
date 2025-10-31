package nars.game;

import jcog.Is;
import nars.term.Termed;

import java.util.function.Consumer;

/**
 * base interface for a repeatedly invoked procedure context
 * consisting of one or a group of concepts, sharing:
 *          resolution
 *          priority
 *          cause channel
 **/
public interface FocusLoop<X> extends Termed, Consumer<X> {

    /** the components of the sensor, of which there may be one or more concepts*/
    Iterable<? extends Termed> components();

    @Is("Quantization") default float resolution() { return 0; }

    /** initialization procedure */
    default void start(X x) { }

//    static Stream<? extends Termed> components(Stream<? extends FocusLoop> s) {
//        return s.flatMap(x -> Streams.stream(x.components()));
//    }

}
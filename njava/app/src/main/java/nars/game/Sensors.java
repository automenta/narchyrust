package nars.game;

import com.google.common.collect.Streams;
import jcog.data.list.FastCoWList;
import nars.focus.PriAmp;
import nars.game.sensor.Sensor;
import nars.term.Termed;

import java.util.stream.Stream;

public class Sensors {

    /**
     * TODO distinguish this priority node from a default non-vector priority node
     * TODO final */
    public PriAmp pri;

    public final FastCoWList<Sensor> sensors = new FastCoWList<>(Sensor[]::new);

    public final <S extends Sensor> S addSensor(S s) {
        var st = s.term();
        if (sensors.OR(e -> st.equals(e.term())))
            throw new RuntimeException("sensor exists with the ID: " + st);

        sensors.add(s);
        return s;
    }

    public final Stream<? extends Termed> components() {
        return sensors.stream().flatMap(s -> Streams.stream(s.components()));
    }

    public final int size() {
        return sensors.size();
    }

    public Stream<Sensor> stream() {
        return sensors.stream();
    }

}
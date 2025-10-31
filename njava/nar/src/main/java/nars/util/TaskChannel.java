package nars.util;

import jcog.util.ConsumerX;
import nars.Task;
import nars.Term;
import nars.control.Cause;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class TaskChannel {

    private final Term uniqueCause;
    private final Cause why;

    public TaskChannel(Cause cause) {
        this.why = cause;
//        this.id = why.id;
        this.uniqueCause = cause.ID;
    }

    protected void preAccept(Task x) {
//            if (x instanceof Task) {
//        ((NALTask) x).why(uniqueCause);
//            } else if (x instanceof Remember) {
//                preAccept(((Remember) x).input); //HACK
//            }
    }


    @Override
    public String toString() {
        return why.name + "<-" + super.toString();
    }

    public float pri() {
        return why.pri();
    }

    public final void accept(Task x, Consumer<? super Task> target) {
        preAccept(x);
        target.accept(x);
    }

    public final void acceptAll(Stream<? extends Task> xx, ConsumerX<? super Task> target) {
        target.acceptAll(xx.peek(this::preAccept));
    }

    public final void acceptAll(Iterable<? extends Task> xx, ConsumerX<? super Task> target) {
        for (Task x : xx)
            preAccept(x);
        target.acceptAll(xx);
    }

    public final void acceptAll(Collection<? extends Task> xx, ConsumerX<? super Task> target) {
        switch (xx.size()) {
            case 0 -> {
            }
            case 1 -> accept(xx instanceof List<? extends Task> l ? l.getFirst() : xx.iterator().next(), target);
            default -> acceptAll((Iterable<Task>) xx, target);
        }
    }

    public final void acceptAll(Task[] xx, ConsumerX<? super Task> target) {
        switch (xx.length) {
            case 0 -> {
            }
            case 1 -> accept(xx[0], target);
            default -> {
                for (Task x : xx)
                    preAccept(x);
                target.acceptAll(xx);
            }
        }
    }
}
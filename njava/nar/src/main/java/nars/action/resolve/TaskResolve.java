package nars.action.resolve;

import nars.Deriver;
import nars.NALTask;
import nars.Premise;
import nars.focus.time.TaskWhen;
import nars.link.TaskLink;
import nars.premise.NALPremise;

/**
 * resolves a tasklink to a single-premise
 * TODO share common parts with BeliefResolve
 */
public class TaskResolve extends Answerer {


    public TaskResolve(TaskWhen timing, TaskResolver resolver) {
        super(timing, resolver);

        tasklink();//taskCommand();
        hasBeliefTask(false);
    }

    @Override
    protected final void run(Deriver d) {

        Premise p = d.premise;

        NALTask t = resolver.resolveTask(p.from(), puncSample(p, d), timing, d);
        if (t != null) {
            d.add(NALPremise.the(t, p.self() ?
                            null :
                            p.to(),
                    false));
        }
    }

    private static byte puncSample(Premise p, Deriver d) {
        return TaskLink.parentLink(p).punc(
            d.rng
            //((DefaultBudget)d.focus.budget).puncSelect, d.rng
        );
    }


}
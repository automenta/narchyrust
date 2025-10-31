package nars.focus.util;

import jcog.data.list.Lst;
import jcog.pri.PLink;
import jcog.pri.bag.Sampler;
import jcog.random.RandomBits;
import nars.Deriver;
import nars.Focus;
import nars.NALTask;
import nars.control.DefaultBudget;
import nars.table.dynamic.TruthCurveBeliefTable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class TaskBagAttentionSampler {
    public final Lst<NALTask> tasks = new Lst<>();

    private final Consumer<NALTask> addTask = tasks::addFast;
    final Function<PLink<NALTask>, Sampler.SampleReaction> sample = this::sample;
    private final Deriver deriver;

    public TaskBagAttentionSampler(Deriver d) {
        this.deriver = d;
    }

    public Lst<NALTask> seed(Focus f, int iter, RandomBits rng) {
        tasks.clear();
        tasks.ensureCapacity(iter);

        f.attn.sample(this, iter, ((DefaultBudget) f.budget).puncSeed, rng);

        if (tasks.size() > 1) {
            tasks.shuffleThis(rng);
            //seeds.sortThisByFloat(z -> -z.priElseZero());
        }

        return tasks;
    }

    @Nullable
    private NALTask refine(NALTask x) {
        //return x;
        return x instanceof TruthCurveBeliefTable.CurveTask c ? c.task(deriver.focus.when(), deriver.nar) : x;
    }

    int samplesRemain, itersRemain;

    private Sampler.SampleReaction sample(PLink<NALTask> taskLink) {
        var task = taskLink.id;
        var deleted = task.isDeleted();

        var stop = --itersRemain <= 0;

        if (!deleted/* || acceptDeleted*/)
            stop = sample(task, stop);

        var remove = deleted /*|| pop*/;
        return Sampler.SampleReaction.the(remove, stop);
    }

    private boolean sample(NALTask task, boolean stop) {
        var t = refine(task);
        if (t!=null) {
            addTask.accept(t);
            if (--samplesRemain <= 0)
                stop = true;
        }
        return stop;
    }
}

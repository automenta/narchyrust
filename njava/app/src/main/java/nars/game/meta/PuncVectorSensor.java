package nars.game.meta;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.pri.PriReference;
import nars.*;
import nars.game.Game;
import nars.game.sensor.SignalComponent;
import nars.game.sensor.VectorSensor;
import nars.term.Termed;
import nars.term.atom.Atomic;

import java.util.Arrays;
import java.util.Iterator;

import static nars.NALTask.i;

abstract class PuncVectorSensor<X> extends VectorSensor {

    final double[] punc;
    final float[] pct;
    private final Term template;

//    public double mean;

    final SignalComponent[] c;

    PuncVectorSensor(Term template) {
        super(template, 4);
        this.template = template;
        punc = new double[4];
        pct = new float[4];
        c = new SignalComponent[4];
    }

    @Override
    public void start(Game game) {
        super.start(game);
        for (var i = 0; i < 4; i++)
            component(i, game.nar);
    }

    private void component(int i, NAR nar) {
        var punc = Op.punc(NALTask.p(i));
        c[i] = component(
            componentID(punc)
            //Op.punc($.p(w.id, MetaGame.pri), NALTask.p(i)),
            //$.inh($.p(w.id, Op.punc(NALTask.p(i))), MetaGame.active)
            ,() -> pct[i], nar);
    }

    abstract protected Iterable<X> src();

    /** accumulate the next value's punctuation data into punc[] */
    abstract protected void load(X x);

    protected Term componentID(Atomic punc) {
        return template.replace($.varDep(1), punc);
    }

    @Override
    public void accept(Game g) {
        Arrays.fill(punc, 0);
        var n = 0;
        var b = src();
        for (var x : b) {
            load(x);
            n++;
        }
        commit(g, n);
        super.accept(g);
    }

    private void commit(Game g, int n) {
        double sum;
        if (n > 0 && (sum = Util.sum(punc)) > Float.MIN_NORMAL) {
            //normalize
            for (var i = 0; i < 4; i++)
                pct[i] = (float) (punc[i] / sum);
//            mean = sum / n;
        } else {
            Arrays.fill(pct, 0.5f);
//            mean = 0;
        }
    }


    @Override
    public Iterator<SignalComponent> iterator() {
        return ArrayIterator.iterate(c);
    }

    public static class TaskBagPuncPriSensor extends PuncVectorSensor<PriReference<? extends Termed>> {
        private final Focus f;

        public TaskBagPuncPriSensor(Term taskTemplate, Focus f) {
            super(taskTemplate);
            this.f = f;
        }

        @Override
        protected Iterable<PriReference<? extends Termed>> src() {
            return f.attn._bag;
        }

        @Override
        protected void load(PriReference<? extends Termed> l) {
            var t = (NALTask)l.get();
            var pri =
                t.priElseZero(); //Task pri
                //l.priElseZero(); //Link pri
            punc[i(t.punc())]+= pri;
        }
    }
}
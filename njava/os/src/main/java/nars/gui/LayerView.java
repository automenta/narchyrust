package nars.gui;

import jcog.data.list.Lst;
import jcog.nndepr.layer.LinearLayer;
import jcog.optimize.BayesOptimizer;
import jcog.tensor.Models;
import jcog.tensor.Tensor;
import jcog.tensor.deprtensor.Tens0r;
import jcog.tensor.experimental.CellularAutomataLinear;
import jcog.tensor.experimental.ModelsExperimental;
import jcog.tensor.model.DNCMemory;
import jcog.tensor.model.ODELayer;
import jcog.tensor.rl.blackbox.BayesZeroPolicy;
import jcog.tensor.rl.blackbox.BlackboxPolicy;
import jcog.tensor.rl.blackbox.PopulationPolicy;
import jcog.tensor.rl.pg.StreamAC;
import jcog.tensor.rl.pg.util.StreamXUtil;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.ContainerSurface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.Draw;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class LayerView extends Gridding implements Runnable {
    private final Stream<?> m;
    private List<Surface> layers;

    public LayerView(Object m) {
        this(Set.of(m));
    }

    public LayerView(Collection<?> m) {
        this(m.stream());
    }

    public LayerView(Stream<?> m) {
        aspect(VERTICAL);
        this.m = m;
    }

    @Nullable
    private static Surface layerView(UnaryOperator<Tensor> x) {
        switch (x) {
            case ODELayer o -> {
                return new Gridding(new LayerView(o.h0), new LayerView(o.h1), new LayerView(o.w), new LayerView(o.bias));
            }
            case Models.Layers a -> {
                return new Gridding(
                        a.layer.stream().map(LayerView::layerView)
                                .filter(Objects::nonNull)
                ).vertical();
            }
            case StreamAC.LayerNormNetwork lnl -> {
                return new Gridding(
                    Stream.of(lnl.l1, lnl.l2, lnl.l3).map(LayerView::layerView)
                ).vertical();
            }
            case CellularAutomataLinear ca -> {
                int r = ca.registers.length, c = ca.registers[0].length;

                var registers = new Gridding();
                for (int i = 0; i < r; i++)
                    for (int j = 0; j < c; j++)
                        registers.add(new TensorView(ca.registers[i][j]));

                return new Gridding(
                    new TensorView(ca.cells),
                    new TensorView(ca.cellGrads),
                    registers,
                    new LayerView(ca.bias)
                ).vertical();
            }

            case StreamXUtil.LayerNormLinear lnl -> {
                return new Gridding(
                    new LayerView(lnl.norm),
                    new LayerView(lnl.linear)
                );
            }
            case Models.LayerNorm ln -> {
                return new Gridding(
                        new TensorView(ln.beta),
                        new TensorView(ln.gamma)
                );
            }
            case Models.LowRankLinear lr -> {
                return new Gridding(
                        new TensorView(lr.U),
                        new TensorView(lr.V)
                );
            }
            case Models.MixtureOfExperts moe -> {
                return new Gridding(
                        new Gridding(moe.experts.stream().map(LayerView::layerView)).horizontal(),
                        layerView(moe.gating)
                ).vertical();
            }
            case ModelsExperimental.FastSlowNetwork f -> {
                var g = new Gridding(
                        new TensorView(f.fastWeight),
                        new TensorView(f.weight)
                );
                if (f.bias != null)
                    g.add(new TensorView(f.bias));
                return g.horizontal();
            }
            case Models.ULT u -> {
                return new Gridding(
                        new Gridding(
                                new TensorView(u.Wk),
                                new TensorView(u.Wv),
                                new TensorView(u.Wq)
                        ).horizontal(),
                        new TensorView(u.Wo)
                ).vertical();
            }
            case Models.Linear l -> {
                var g = new Gridding(VERTICAL);
                g.add(new TensorView(l.weight));
                if (l.bias != null)
                    g.add(new TensorView(l.bias));
                return g;
            }
            case null, default -> {
                return null;
            }
        }
    }

    @Override
    public void run() {
        if (m == null) return;

        if (this.layers == null) {
            this.layers = new Lst<>();
            m.forEach(x -> {
                if (x instanceof Models.Layers xl)
                    layers.add(new LayerView(xl.layer));
                else if (x instanceof LinearLayer ll)
                    layers.add((new MLPLayerView(ll, false /*i == layers.length-1*/)));
                else if (x instanceof Tens0r t)
                    layers.add(new Tens0rView(t));
                else if (x instanceof Tensor a)
                    layers.add(new TensorView(a));
                else if (x instanceof UnaryOperator l) {
                    var v = layerView(l);
                    if (v!=null)
                        layers.add(v);
                }
            });
            set(this.layers);
        }
        if (this.layers != null) {
            for (Surface l : this.layers) {
                if (l instanceof ContainerSurface c) {
                    c.forEachRecursively(cc -> {
                        if (cc instanceof Runnable lr)
                            lr.run();
                    });
                } else if (l instanceof Runnable lr)
                    lr.run();
            }
        }
    }

    abstract static class AbstractTensorView extends Gridding implements Runnable {

        AbstractTensorView(Surface... s) {
            super(s);
        }

        public void run() {
            forEachRecursively(x -> {
            //forEach(x -> {
                if (x instanceof BitmapMatrixView v)
                    v.updateIfShowing();
            });
        }
    }

    static class Tens0rView extends AbstractTensorView {
        Tens0rView(Tens0r t) {
            super(
                    new BitmapMatrixView(t.data)
//                new BitmapMatrixView(t.grad)
            );
        }
    }

    static class TensorView extends AbstractTensorView {
        TensorView(Tensor t) {
            this(t.array());
        }

        TensorView(double[][] data) {
            super(new BitmapMatrixView(data));
        }

        TensorView(double[] data) {
            super(
                new BitmapMatrixView(data)
//                new BitmapMatrixView(t.rows(), t.cols(),
//                        (r,c,i)->Draw.colorBipolar((float)t.data.get(i)))
//                new BitmapMatrixView(t.grad)
            );
        }
    }

    static class MLPLayerView extends AbstractTensorView {

        MLPLayerView(LinearLayer l, boolean showOutput) {
            super(
                    new BitmapMatrixView(l.in),
                    new BitmapMatrixView(l.W),
                    //new BitmapMatrixView(l.delta, Draw::colorBipolar),
//                    new BitmapMatrixView(l.dW, Draw::colorBipolar),
                    showOutput ? new BitmapMatrixView(l.out) : new EmptySurface() //HACK
            );
        }


    }

    public static class BlackboxPolicyView extends Gridding implements Runnable {
        BlackboxPolicyView(PopulationPolicy.CMAESPopulation p) {
            super(
                BitmapMatrixView.fromDoubles(p::best, p.parameters(), 1, Draw::colorBipolar),
                BitmapMatrixView.fromDoubles(p::policy, p.parameters(), 1, Draw::colorBipolar)
            );
        }

        public BlackboxPolicyView(BlackboxPolicy p) {
            super(
                BitmapMatrixView.fromDoubles(p::policy, p.parameters(), 1, Draw::colorBipolar)
            );
            if (p instanceof BayesZeroPolicy b) {
                if (b.b.model instanceof BayesOptimizer.GaussianProcess gp) {
                    add(BitmapMatrixView.fromDoubles(() -> gp.alpha, b.b.capacity(), 1, Draw::colorBipolar));
                    add(BitmapMatrixView.fromDoubles2D(() -> gp.K, b.b.capacity(), b.b.capacity(), Draw::colorBipolar));
                } else if (b.b.model instanceof BayesOptimizer.NeuralSurrogate ns) {
                    var nsn = ns.net;
                    addAll(
                        new BitmapMatrixView(nsn.weightsIH),
                        new BitmapMatrixView(nsn.biasH),
                        new BitmapMatrixView(nsn.weightsHO),
                        new BitmapMatrixView(nsn.biasO)
                    );
                }
            }
        }

        public void run() {
            forEach(x -> {
                if (x instanceof BitmapMatrixView v)
                    v.updateIfShowing();
            });
        }
    }

    public static class DNCMemoryView extends AbstractTensorView {
        public DNCMemoryView(DNCMemory x) {
            super(
                new LayerView.TensorView(x.read),
                new LayerView.TensorView(x.write),
                new LayerView.TensorView(x.use),
                new LayerView.TensorView(x.memory)
            );
        }
    }
}
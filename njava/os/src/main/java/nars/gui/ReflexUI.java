package nars.gui;

import jcog.agent.Agent;
import jcog.data.list.Lst;
import jcog.lstm.LSTM;
import jcog.nndepr.EvolvingGraphNN;
import jcog.nndepr.MLP;
import jcog.nndepr.RecurrentNetwork;
import jcog.nndepr.ntm.NTM;
import jcog.nndepr.spiking0.critterding.CritterdingBrain;
import jcog.tensor.model.DNCMemory;
import jcog.tensor.rl.blackbox.BlackboxPolicy;
import jcog.tensor.rl.blackbox.CMAESZeroPolicy;
import jcog.tensor.rl.pg.*;
import nars.NAR;
import nars.game.action.util.Reflex;
import nars.game.action.util.Reflex0;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.TsneRenderer;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.Force2D;
import spacegraph.space2d.meta.Triggering;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.stream.Stream;

import static spacegraph.space2d.container.grid.Gridding.VERTICAL;

public class ReflexUI {

    @Deprecated public static Surface reflexUI(Reflex0 r) {
        return reflexUI(r, r.agent, r::afterFrame);
    }
    public static Surface reflexUI(Reflex r) {
        return reflexUI(r, r.agent(), r::afterFrame);
    }

    public static Surface reflexUI(Object rlb, Object agent, Consumer<Runnable> onFrame) {

        @Deprecated var updates = new Lst<Runnable>();
        var main = new Gridding();

        var stats = new Gridding(VERTICAL);
        main.add(stats);

        final Map<Class, BiFunction<?, Object, Surface>> nnbuilders = Map.of(
                MLP.class, (x, rel) -> {
                    var y = new LayerView(Stream.of(((MLP) x).layers));
                    updates.add(y);
                    return y;
                },
                AbstractPG.class, (_x, rel) -> {
                    var x = (AbstractPG) _x;
                    switch (x) {
                        case SAC s -> {
                            var a = new LayerView(s.policy.layer);
                            var b = new LayerView(s.q1.layer);
                            var c = new LayerView(s.q2.layer);
                            updates.addAll(a, b, c);
                            return new Gridding(a, b, c);
                        }
                        case VPG r -> {
                            var a = new LayerView(r.policy);
                            var b = new LayerView(r.value);
                            updates.addAll(a, b);
                            return new Gridding(a, b);
                        }

                        case ReinforceODE.ReinforceNeuralODE no -> {
                            var l = new LayerView(no.ode);
                            updates.add(l);
                            return l;
                        }
                        case ReinforceODE.ReinforceLiquid no -> {
                            var a = new LayerView(no.h0);
                            var b = new LayerView(no.h1);
                            var c = new LayerView(no.w);
                            updates.addAll(a, b, c);
                            return new Gridding(a, b, c);
                        }
                        case Reinforce rr -> {
                            var a = new LayerView(rr.policy);
                            updates.add(a);
                            return a;
                        }
                        case DDPG d -> {
                            var a = new LayerView(d.policy.layer);
                            var b = new LayerView(d.value.layer);
                            updates.addAll(a, b);
                            return new Gridding(a, b);
                        }
                        case DDPGAuto d -> {
                            var a = new LayerView(d.policy.layer);
                            var b = new LayerView(d.value.layer);
                            updates.addAll(a, b);
                            return new Gridding(a, b);
                        }
                        case StreamAC s -> {
                            var g = new Gridding();
                            for (var ss : new Object[] {
                                s.policy,
                                s.value
                                //s.policyTraces, s.valueTraces
                            }) {
                                var y = new LayerView(ss);
                                updates.add(y);
                                g.add(y);
                            }
                            return g;
                        }
                        case null, default -> throw new UnsupportedOperationException();
                    }
                },
                LSTM.class, (x, rel) -> {
                    var y = new LSTMView((LSTM) x);
                    updates.add(y);
                    return y;
                },
                NTM.class, (x, rel) -> {
                    var y = new LSTMView.NTMView((NTM) x);
                    updates.add(y);
                    return y;
                },
                RecurrentNetwork.class, (x, rel) -> {
                    var y = new LSTMView.RecurrentView((RecurrentNetwork) x);
                    updates.add(y);
                    return y;
                },
                CritterdingBrain.class, (x, rel) -> {
                    var y = new LSTMView.CritterdingView((CritterdingBrain) x);
                    updates.add(y);
                    return y;
                },
                DNCMemory.class, (x, rel) -> {
                    var y = new LayerView.DNCMemoryView((DNCMemory)x);
                    updates.add(y);
                    return y;
                },
                BlackboxPolicy.class, (x, rel) -> {
                    var y = new LayerView.BlackboxPolicyView((BlackboxPolicy) x);
                    updates.add(y);
                    return new Gridding(y);
                },
//                PopulationPolicy.Population.class, (x, rel) -> {
//                    var y = new LayerView.BlackboxPolicyView((PopulationPolicy.CMAESPopulation) x);
//                    updates.add(y);
//                    return y;
//                },
                EvolvingGraphNN.class, (x, rel) -> {
                    var g = new Graph2D<EvolvingGraphNN.Node>()
                            .update(new Force2D<>())
                            .render((node, graph) -> {
//                                float speed = 4;
//                                if (node.id.isInput())
//                                    node.move(0, +speed);
//                                else if (node.id.isOutput())
//                                    node.move(0, -speed);

                                node.pri = (float) Math.min(1, node.id.importance());

                                node.id.outs.forEach(e -> {
//                                    if (e.to == node.id)
//                                        throw new UnsupportedOperationException();
                                    var tgt = graph.node(e.to);
                                    if (tgt == null)
                                        return;

                                    var edge = graph.edge(node, tgt);
                                    var w = e.weight;
                                    var edgeMin = 0.002f;

                                    var edgeMax = 0.25f;
                                    edge.weight = edgeMin + (float) (Math.min(edgeMax, Math.abs(w))) / 10;
                                    //edge.weight = edgeMin;
                                    //float alpha = 0.5f + (float) Util.abs(w);

                                    float red;
                                    red = w > 0 ? (float) Math.min(1, w) : 0;
                                    float green;
                                    green = w < 0 ? (float) Math.min(1, -w) : 0;
                                    edge.color(red, green, 0.1f, 0.75f);
                                });
                            });
                    updates.add(() -> {
                        g.set(((EvolvingGraphNN) x).nodes());
                    });
                    SpaceGraph.window(g.widget(), 1000, 1000);
                    return new Gridding();
                    //return g;
                }
//				double[][].class, (x, rel)->{
//					var v = new BitmapMatrixView((double[][])x);
//					updates.add(v::updateIfShowing);
//					return v;
//				}
        );

        if (agent instanceof VPG.PGAgent p)
            agent = p.pg;
        if (agent instanceof StreamAC s) {
            plot("PolicyLoss", () -> s.policyLoss, stats, updates);
            plot("ValueLoss", () -> s.valueLoss, stats, updates);
            plot("tdErr", () -> s.tdErr, stats, updates);
            plot("entropy", () -> s.entropy, stats, updates);
            plot("policyTrace_l1", s.policyTraces::normL1, stats, updates);
            plot("valueTrace_l1", s.valueTraces::normL1, stats, updates);
        }
        if (agent instanceof DDPG dg) {
            plot("PolicyLoss", () -> dg.policyLoss, stats, updates);
            plot("ValueLoss", () -> dg.valueLoss, stats, updates);
            plot("Replay_TdErr_Mean", dg.memory::priMean, stats, updates);
            //"Replay_TdErr_Max", () -> dg.memory.priMax(),
        }
        if (agent instanceof DDPGAuto dg) {
            plot("PolicyLoss", () -> dg.policyLoss, stats, updates);
            plot("ValueLoss", () -> dg.valueLoss, stats, updates);
            plot("Replay_TdErr_Mean", dg.memory::priMean, stats, updates);
            //"Replay_TdErr_Max", () -> dg.memory.priMax(),
        }
        if (agent instanceof SAC sac) {
            var sp = new Plot2D(history);
            sp.addSeries("PolicyLoss", () -> Math.abs(sac.policyLoss));
            sp.addSeries("Q1Loss", () -> Math.abs(sac.q1Loss));
            sp.addSeries("Q2Loss", () -> Math.abs(sac.q2Loss));
            stats.add(sp);
            updates.add(sp::update);
        }
        if (agent instanceof VPG pg) {
            plot("PolicyLoss", () -> pg.policyLoss, stats, updates);
            plot("ValueLoss", () -> pg.valueLoss, stats, updates);
//            plot("PolicyLoss", () -> pg.policyLoss, "ValueLoss", () -> pg.valueLoss, stats, updates);
            plot("Entropy", () -> pg.entropyCurrent, stats, updates);
        } else if (agent instanceof AbstractReinforce rr) {
            plot("PolicyLoss", () -> rr.policyLoss, stats, updates);
            plot("Entropy", () -> rr.entropyCurrent, stats, updates);
        }

//        if (agent instanceof PolicyAgent d) {
//            main.add(
//                    new Gridding(VERTICAL,
////							new ImmediateMatrixView(()->Util.toFloat(d.input), d.inputs, 1),
////TODO
////							new ImmediateMatrixView(()->d.actionPrev, d.actions, 1),
////							new ImmediateMatrixView(d.actionPrev),
//                            //new ImmediateMatrixView(rlb.actionPrev),
//                            new ImmediateMatrixView(d.actionNext)
//                    )
//            );
//            var err = new Plot2D(history);
//            err.add("ErrMean", () -> d.errMean);
//            err.add("ErrMin", () -> d.errMin);
//            err.add("ErrMax", () -> d.errMax);
//
//            if (d.policy instanceof BranchedPolicy qq) {
//                Gridding g = new Gridding();
//                for (var a : qq.actions)
//					g.add(new ObjectSurface(a, 6, nnbuilders, ObjectSurface.builtin));
//                main.add(g);
//            }
//        }


//        if (agent instanceof EmbeddedNAgent) {
//            main.add(top(((EmbeddedNAgent) agent).nar));
//            main.add(GameUI.gameUI(((EmbeddedNAgent) agent).game));
//        }

        if (agent instanceof Agent aa)
            plot("Reward", () -> aa.reward, stats, updates);

        if (rlb instanceof Reflex pf) {
            plot("Reward", pf::reward, stats, updates);

            {
                //plot("Action Delta", () -> Util.sumAbs(pf.delta) / pf.delta.length, stats, updates);

                var a2 = new BitmapMatrixView(pf.model.actionNext);
                stats.add(a2);
                updates.add(a2::updateIfShowing);

//                if (pf.model instanceof Reflex.DirectStrategy ds) {
//                    var a1 = new BitmapMatrixView(ds.localActionPrev);
//                    //var d = new BitmapMatrixView(ds.delta);
//                    stats.add(a1);
//                    updates.add(a1::updateIfShowing);
////                    stats.add(d);
////                    updates.add(d::updateIfShowing);
//                }
            }
        }

        main.add(new ObjectSurface(rlb, 6, nnbuilders, ObjectSurface.builtin));
        if (!(rlb instanceof Reflex0))
            main.add(new ObjectSurface(agent, 6, nnbuilders, ObjectSurface.builtin));

        onFrame.accept(() -> {
            if (main.visible())
                for (var u : updates) u.run();
        });

        return main;
    }

    private static int history = 750;

    private static Triggering<Surface> cmaesView(NAR nar, CMAESZeroPolicy x) {
        final var table = new Table[]{Table.create()};
        var outputs = x.outputs;
        for (var i = 0; i < outputs; i++)
            table[0].addColumns(DoubleColumn.create(Integer.toString(i)));
        table[0].addColumns(DoubleColumn.create("reward"));

        //table.addColumns(DoubleColumn.create("score"));

        var history = 200;

        var g = TsneRenderer.view(table[0], null, outputs, outputs);
        var y = new Triggering<>(g.widget(), nar::onDur, ()->{
            if (table[0].rowCount() >= history && !nar.rng().nextBoolean(0.01f)) {
                g.layout();
                return; //HACK
            }
            var reward = x.reward;
            if (reward!=reward) {
                g.layout();
                return;
            }

            if (table[0].rowCount() >= history)
                table[0] = table[0].dropRows(0);

            var r = table[0].appendRow();
            for (var i = 0; i < outputs; i++)
                r.setDouble(i, x.policy[i]);
            r.setDouble(outputs, reward);

            g.set(TsneRenderer.rows(table[0]));
        });
        return y;
    }

    private static void plot(String l, DoubleSupplier y, Gridding stats, Lst<Runnable> updates) {
        var p = new Plot2D(history);
        p.addSeries(l, y);
        stats.add(p);
        updates.add(p::update);
    }

    private static void plot(String l1, DoubleSupplier y1, String l2, DoubleSupplier y2, Gridding stats, Lst<Runnable> updates) {
        var p = new Plot2D(history);
        p.addSeries(l1, y1);
        p.addSeries(l2, y2);
        stats.add(p);
        updates.add(p::update);
    }


}

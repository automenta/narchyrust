package nars.gui;

import jcog.Log;
import jcog.exe.Exe;
import jcog.io.BinTxt;
import nars.NAR;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import org.slf4j.Logger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.Submitter;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemUI extends Gridding {

    private static final Logger logger = Log.log(MemUI.class);

    private final NAR nar;

    public MemUI(NAR nar) {

        this.nar = nar;

        set(
                Submitter.text("Load", u -> {
                    System.err.println("Load: TODO");
                    //throw new TODO();
                }),
                Submitter.text("Save", u -> {
                    if (u.isEmpty())
                        u = tmpFile(".tasks");
                    save(u);
                }),
                Submitter.text("Concepts", u -> {
                    if (u.isEmpty())
                        u = tmpFile(".concepts");
                    try {
                        saveConcepts(u);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                })
                /*"mem", () -> {
                    Gridding g = new Gridding();
                    if (n.memory instanceof HijackMemory) {
                        HijackMemory h = (HijackMemory)n.memory;
                        BagSpectrogram<PLink<Concept>> s = new BagSpectrogram<>(() ->
                                //TODO loop through the bag at an increasing offseted window
                                h.bag
                                , 128, 1024, n) {

                            final int ops = Op.values().length;

                            @Override
                            protected int colorize(PLink<Concept> cc) {
                                Concept c = cc.id;
                                return Draw.colorHSB(((float) c.opID()) / ops, 0.5f + 0.5f * cc.priElseZero(), 0.05f + 0.95f * cc.priElseZero());
                            }
                        };
                        g.add(s);
                    } else {
                        return new VectorLabel("TODO");
                    }
                    g.add(new ObjectSurface(n.memory));
                    return g;
                }*/

                //new PushButton("List", ((GraphBagFocus) f).bag::print) //TODO better
//			new PushButton("Impile", () -> SpaceGraph.window(impilePanel(w), 500, 500)) //TODO better


                //memSave(nar)
//                new PushButton("Prune Beliefs", () -> {
//                    nar.runLater(() -> {
//                        //nar.logger.info("Belief prune start");
////                        final long scaleFactor = 1_000_000;
//                        //Histogram i = new Histogram(1<<20, 5);
//                        Quantiler q = new Quantiler(128 * 1024);
//                        long now = nar.time();
//                        float dur = nar.dur();
//                        nar.tasks(true, false, false, false).forEach(t ->
//                                {
//                                    try {
//                                        float c = (float) w2cSafe(t.evi(now, dur));
//                                        //i.recordValue(Math.round(c * scaleFactor));
//                                        q.add(c);
//                                    } catch (Throwable e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                        );
//                        //System.out.println("Belief evidence Distribution:");
//                        //Texts.histogramPrint(i, System.out);
//
//                        //float confThresh = i.getValueAtPercentile(50)/ scaleFactor;
//                        float confThresh = q.quantile(0.5f);
//                        final int[] removed = {0};
//                        nar.tasks(true, false, false, false, (c, t) -> {
//                            try {
//                                if (w2cSafe(t.evi(now, dur)) < confThresh)
//                                    if (c.remove(t))
//                                        removed[0]++;
//                            } catch (Throwable e) {
//                                e.printStackTrace();
//                            }
//                        });
//                        //nar.logger.info("Belief prune finish: {} tasks removed", removed[0]);
//                    });
//                })

        );

    }

    private void saveConcepts(String u) throws FileNotFoundException {
        PrintStream ps = new PrintStream(new FileOutputStream(u));

        nar.concepts().forEach(c -> {
            try {
                var C = c.term();
                BeliefTables b = (BeliefTables) c.beliefs(), g = (BeliefTables) c.goals();
                b.tables.forEach(x -> {
                    if (x instanceof TemporalBeliefTable)
                        ps.println(C + " belief " + ((TemporalBeliefTable)x).stats());
                });
                g.tables.forEach(x -> {
                    if (x instanceof TemporalBeliefTable)
                        ps.println(C + " goal " + ((TemporalBeliefTable)x).stats());
                });
            } catch (Throwable t) {
                //HACK just in case
            }
        });

        ps.flush();
        ps.close();

    }


    Surface memSave() {
        var path = new TextEdit(40);
        path.text(tmpFile(".concepts"));

        Object currentMode = null;
        var mode = new ButtonSet(ButtonSet.Mode.One,
                new CheckBox("txt"), new CheckBox("bin")
        );
        return new Gridding(
                path,
                new Gridding(
                        mode,
                        new PushButton("save").clicked(() -> save(path.text()))
                ));
    }

    private String tmpFile(String ext) {
        try {
            return Files.createTempFile(nar.self().toString(), BinTxt.toString(System.currentTimeMillis()) + ext).toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final AtomicBoolean busy = new AtomicBoolean();

    private void save(String path) {
        if (!busy.compareAndSet(false,true)) {
            System.err.println(this + " busy");
            return;
        }

        Exe.runLater(() -> {
            try {
                logger.info("save start {} {}", nar.self(), path);
                nar.output(new File(path), false);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                busy.set(false);
                logger.info("save end {} {}", nar.self(), path);
            }
        });
    }

}
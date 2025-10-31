package nars.experiment;

import jcog.signal.tensor.ArrayTensor;
import nars.game.util.GameStats;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.collection.MutableContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.widget.meter.WavePlot;

import java.util.Objects;
import java.util.stream.Stream;

import static spacegraph.SpaceGraph.window;

public class GameStatsUI extends Gridding {

    public GameStatsUI(String... files) {
        this(Stream.of(files).map(z -> {
            try {
                return GameStats.load(z);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).toArray(GameStats[]::new));
    }

    public GameStatsUI(GameStats... stats) {
        Timeline2D time = new Timeline2D(
            0, Stream.of(stats).mapToInt(s -> s.frames).max().getAsInt(), new Stacking());

        add(time.withControls());
        for (GameStats g : stats) {
            ((MutableContainer)time.the()).add(new WavePlot(
                new ArrayTensor(g.happiness.toArray()),
                    0, g.happiness.size(), 400, 400)
                );
        }
    }

    public static void main(String[] args) {
        if (args.length == 0)
            System.err.println("usage: java GameStatsUI file1 ...[fileN]");
        window(new GameStatsUI(args), 800, 600);
    }
}
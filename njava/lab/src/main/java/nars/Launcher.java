package nars;

import jcog.Log;
import jcog.WTF;
import nars.game.Game;
import org.reflections.Reflections;
import org.slf4j.Logger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.meta.obj.ObjectSurface2;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static spacegraph.SpaceGraph.window;

public class Launcher {

    static final float fps = 25;


    public static void main(String[] args) {


        var envs = new Reflections("nars")
                .getSubTypesOf(Game.class);

        List<Experiment> list = envs.stream().map(Experiment::new).collect(toList());
        Surface m = new Splitting(
                new ObjectSurface2(
                        list
                ),
                0.1f
                ,new ObjectSurface2(
                        List.of(new MainRunner(() -> GUI.main(new String[]{})))
//                            List.of(new MainRunner(OSMTest.class))
                )
        ).resizeable();
        window(m, 800, 600);


//        GraphEdit2D g = new GraphEdit2D();
//        SpaceGraph.window(                 g, 800, 800        );

//        GraphEdit2D g = GraphEdit2D.graphWindow(800,800);
//        g.add(m).posRel(0.5f, 0.5f, 0.75f);

    }

    static class MainRunner implements Runnable {
        final Runnable env;

        MainRunner(Runnable env) {
            this.env = env;
        }

        @Override
        public void run() {

            Thread t = new Thread(env);
            t.setUncaughtExceptionHandler((T,u) -> {
                System.err.println(u);
            });
            t.start();
        }

        @Override
        public String toString() {
            return env.toString();
        }
    }

    static class Experiment implements Runnable {
        private static final Logger logger = Log.log(Experiment.class);

        final Class<? extends Game> env;

        Experiment(Class<? extends Game> env) {
            this.env = env;
        }

        @Override
        public void run() {

            logger.info("run {}", env);

            Player p = new Player().fps(fps);

            Game g = null;
            try {
                g = env.getConstructor().newInstance();
            } catch (Exception e1) {
                try {
                    g = env.getConstructor(Term.class).newInstance($.uuid());
                } catch (Exception e2) {
                    try {
                        g = env.getConstructor(NAR.class).newInstance(p.nar);
                    } catch (Exception e3) {
                    }
                }
            }

            if (g == null)
                throw new WTF(env + " constructor unsupported");

            p.start();
            p.nar.add(g);

        }

        @Override
        public String toString() {
            return env.getSimpleName();
        }
    }

}
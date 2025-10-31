package nars.game.util;

import jcog.Log;
import jcog.io.bzip2.BZip2InputStream;
import jcog.io.bzip2.BZip2OutputStream;
import nars.game.Game;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.slf4j.Logger;

import java.io.*;
import java.util.function.Consumer;

public class GameStats implements Consumer<Game>, Serializable {

    private transient final Game game;

    /** TODO DoubleStatistics */
    double happinessSum;
    /** TODO DoubleStatistics */
    double dexSum;

    public int frames;

    public final FloatArrayList happiness = new FloatArrayList(128*1024);

//    static class WaveStats {
//        /** pcm samples */
//        final FloatArrayList value = new FloatArrayList();
//
//        //TODO frequency domain metrics
//    }
//
//    final Map<Term,WaveStats> actions = new HashMap();

    protected GameStats()  {
        this.game = null;
    }

    public GameStats(Game g)  {
        this(g, true);
    }

     /** TODO saveOnGameStop */
     public GameStats(Game g, boolean saveOnExit)  {
        this.game = g;
        g.afterFrame(this);

        if (saveOnExit) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                save();
            }));
        }
    }

    private static final Logger logger = Log.log(GameStats.class);

    public synchronized void save() {
        try {
            var file = File.createTempFile(game.getClass().getSimpleName() + "_"+ game.id + "_"
                    /*"_" + System.currentTimeMillis()*/, "");
            logger.info("saving {}", file);
            FileOutputStream fos = new FileOutputStream(file);
            BZip2OutputStream zos = new BZip2OutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(zos);
            oos.writeObject(this);
            oos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameStats load(String filename) throws IOException, ClassNotFoundException {
        File f = new File(filename);
        FileInputStream fis = new FileInputStream(f);
        BZip2InputStream zis = new BZip2InputStream(fis);
        ObjectInputStream ois = new ObjectInputStream(zis);
        GameStats g = (GameStats) ois.readObject();
        ois.close();
        return g;
    }

    @Override
    public void accept(Game g) {
        double h = g.happiness();
        happiness.add((float)h);
        if (h == h) happinessSum += h;

        double d = g.dexterity();
        if (d == d) dexSum += d;

        frames++;
    }

    public double happinessMean() {
        return happinessSum / frames;
    }
    public double dexterityMean() {
        return dexSum / frames;
    }
}
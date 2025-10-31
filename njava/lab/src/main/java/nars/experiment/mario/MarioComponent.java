package nars.experiment.mario;

import jcog.Util;
import jcog.signal.FloatRange;
import nars.experiment.mario.level.LevelGenerator;
import nars.experiment.mario.sprites.Mario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class MarioComponent extends JComponent implements Runnable, KeyListener, FocusListener {

    public static final int DIFFICULTY = 1;
    public final Thread thread;
    public final FloatRange fps = new FloatRange(25, 1, 60);
    private Graphics og, g;

    public void startGame() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int levelType = rng.nextBoolean() ?
                LevelGenerator.TYPE_UNDERGROUND
                :
                LevelGenerator.TYPE_OVERGROUND;

        startLevel(rng.nextLong(), DIFFICULTY, levelType);
    }

    public boolean paused;
    private final int width;
    private final int height;
    private GraphicsConfiguration graphicsConfiguration;
    public Scene scene;

    public MapScene mapScene;

    public final BufferedImage image; {
        image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        image.setAccelerationPriority(1f);
    }

    public MarioComponent(int width, int height) {
        this.setFocusable(true);
        this.setEnabled(true);
        this.width = width;
        this.height = height;

        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);


        setFocusable(true);

        thread = new Thread(this, getClass().getSimpleName() + " Game Thread");



        addKeyListener(this);
        addFocusListener(this);

    }

    private void toggleKey(int keyCode, boolean isPressed) {
        if (keyCode == KeyEvent.VK_LEFT) {
            Scene.key(Mario.KEY_LEFT, isPressed);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            Scene.key(Mario.KEY_RIGHT, isPressed);
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            Scene.key(Mario.KEY_DOWN, isPressed);
        }
        if (keyCode == KeyEvent.VK_UP) {
            Scene.key(Mario.KEY_UP, isPressed);
        }
        if (keyCode == KeyEvent.VK_A) {
            Scene.key(Mario.KEY_SPEED, isPressed);
        }
        if (keyCode == KeyEvent.VK_S) {
            Scene.key(Mario.KEY_JUMP, isPressed);
        }
        if (isPressed && keyCode == KeyEvent.VK_ESCAPE) {


            toTitle();


        }
    }

    @Override
    public void paint(Graphics g) {
    }

    @Override
    public void update(Graphics g) {
    }



    @Override
    public void run() {

        graphicsConfiguration = getGraphicsConfiguration();
        g = getGraphics();
        og = image.getGraphics();

        Art.init(graphicsConfiguration);

        scene = mapScene = new MapScene(graphicsConfiguration, this, new Random().nextLong());

        toTitle();

        boolean running = true;
        while (running) {

            next(og, g);

            int delay = Math.round(1000 / fps.floatValue());
            Util.sleepMS(delay);
        }

        Art.stopMusic();
    }

    private void next(Graphics og, Graphics g) {
        if (!paused)
            scene.tick();

        og.setColor(Color.BLACK);
        og.fillRect(0, 0, 320, 240);

        float alpha = 0;
        scene.render(og, alpha);

        if (width != 320 || height != 240)
            g.drawImage(image, 0, 0, width, height, null);
        else
            g.drawImage(image, 0, 0, null);
    }

    private static void drawString(Graphics g, String text, int x, int y, int c) {
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            g.drawImage(Art.font[ch[i] - 32][c], x + i * 8, y, null);
        }
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        toggleKey(arg0.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        toggleKey(arg0.getKeyCode(), false);
    }

    public void startLevel(long seed, int difficulty, int type) {
        scene = new LevelScene(graphicsConfiguration, this, seed, difficulty, type);
        scene.init();
    }

    public void levelFailed() {
        startGame();
    }

    @Override
    public void keyTyped(KeyEvent arg0) {
    }

    @Override
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
    }

    public void levelWon() {
        scene = mapScene;
        MapScene.startMusic();
        mapScene.levelWon();
    }

    public void win() {
        scene = new WinScene(this);
        scene.init();
    }

    public void toTitle() {
        Mario.resetStatic();
        startGame();
    }

    public void lose() {
        scene = new LoseScene(this);
        scene.init();
    }

}
package nars.experiment;


import jcog.Util;
import jcog.cluster.BagClustering;
import jcog.signal.FloatRange;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.Player;
import nars.Term;
import nars.game.Game;
import nars.game.action.BiPolarAction;
import nars.game.sensor.Sensor;
import nars.gui.NARui;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.BitmapSensor;
import nars.sensor.PixelBag;
import nars.term.var.Variable;
import nars.truth.Truther;
import nars.video.SwingBitmap2D;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.meta.Surfaces;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.text.VectorLabel;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static nars.$.*;
import static spacegraph.SpaceGraph.window;

/**
 * NARkanoid
 */
public class ArkaNAR extends Game {

    private static final boolean showClustering = false;

    private static boolean ManualOverrides = false;

    /** a hint */
    private static final boolean rewardPaddleNearBallX = false, rewardBounce = false;

    /** a hint */
    final boolean numeric;


//    @Deprecated static final boolean hint2 = false;


    public final FloatRange paddleSpeed = new FloatRange(20, 1, 20);
    public final FloatRange ballSpeed = new FloatRange(4f, 0.04f, 10);
    public final FloatRange paddleWidth;
    public final AtomicBoolean bricksEnabled;
    //final int visW = 48, visH = 32;
    //final int visW = 24, visH = 16;
    final int visW, visH;
    final Arkanoid noid;
    //final int visW = 8, visH = 6;
    //static final int visW = 12, visH = 8;
    BitmapSensor<ScaledBitmap2D> cc;


    public ArkaNAR() {
        this($.$$("a"));
    }

    public ArkaNAR(Term id) {
        this(id,
            true, false,
                16, 10
                //80, 40
                //40, 20
                //18, 16
                //8,5
            //true, true, 0, 0 //numeric only
            //false, false, 9, 8 //one-player Pong
        );
    }

    public ArkaNAR(Term id, boolean bricks, boolean numeric, int visW, int visH) {
        super(id
            //,GameTime.durs(2) //frameskip
        );
        this.numeric = numeric;
        this.visW = visW;
        this.visH = visH;

        noid = new Arkanoid(bricks);
        paddleWidth = noid.PADDLE_WIDTH;
        bricksEnabled = noid.bricksEnabled;

        initPushButton();
        //initUnipolar();
//        initBipolarRelative();
        //initPWM();


        if (visW > 0 && visH > 0) {

//            cc = senseCamera(
//                    //(x, y) -> inh(p(x, y), id),
//                    (x, y) -> inh(id, p(x, y)),
//                    new ScaledBitmap2D(
//                            new SwingBitmap2D(noid)
//                            , visW, visH
//                    )/*.blur()*/);
            PixelBag b = new PixelBag(new SwingBitmap2D(noid),
                visW, visH
            );
            //b.setZoom(0.1f);
            b.setZoom(0);
            cc = addSensor(
                new BitmapSensor(b, (x, y) -> inh(id, p(x, y))));

            //cc.resolution(0.25f);
        }


        if (numeric) {
            int digits = 3;

            float resX = 0.1f;
            float resY = 0.1f;

            Variable v = varDep(1);
            List<Sensor> l = List.of(
                    senseNumberN(inh(id, p(p("p", "x"), v)), (() -> noid.paddle.x / noid.getWidth()), digits).freqRes(resX),
                    senseNumberN(inh(id, p(p("bp", "dx"), v)), (() -> 0.5f + 0.5f * (noid.ball.x - noid.paddle.x) / noid.getWidth()), digits).freqRes(resX),
                    senseNumberN(inh(id, p(p("b", "x"), v)), (() -> (noid.ball.x / noid.getWidth())), digits).freqRes(resX),
                    senseNumberN(inh(id, p(p("b", "y"), v)), (() -> 1f - (noid.ball.y / noid.getHeight())), digits).freqRes(resY)
            );

//            nar.runLater(()->
//                    window(NARui.beliefCharts(
//                            l.stream().flatMap(x -> Streams.stream(x.components()))
//                                    .collect(toList()), nar), 500, 500)
//            );

        }

        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/

        afterFrame(noid::next);

//        reward("alive",
//                //() -> 1 - Math.min(1, noid.die - noid.prevDie)
//                Reward.attack(() -> alive() ? 1 : 0, 10)
////            EternalDefaultTable.ifNot(1, () -> 1 - Math.min(1, noid.die - noid.prevDie))
//        );
//        nar.runLater(()->{
//            alive.addGuard(true,false);
//        });


        if (rewardBounce) {
            var b = reward("bounce", () -> alive() ? (noid.paddleHitsBall ? 1 : 0.5f) : 0);
            b.sensor.truther(new Truther.PolarTruther(() -> nar));
        }

        var score = reward("score",
            //Reward.release(() -> noid.score > noid.prevScore ? 1 : 0, 4)
            () -> {
                if (alive()) {
                    if (noid.bricksEnabled.getOpaque())
                        return noid.score > noid.prevScore ? 1 : /*Float.NaN*/ 0.5f;
                    else
                        return 1;
                }
                return 0;
            }
        );
        score.sensor.truther(new Truther.PolarTruther(()->nar));

        if (rewardPaddleNearBallX) {
            reward("near",
                    //Reward.release(() -> noid.score > noid.prevScore ? 1 : 0, 4)
                    () -> alive() ?
                            (1 - Math.abs(noid.ball.x - noid.paddle.x) / noid.getWidth())
                            : 0
            ).resolution(0.1f);
        }

//        if (hint2) {
//            float sharp = 2;
////            reward("align",
////            () -> (float)Math.pow(1 - Math.abs(noid.ball.x - noid.paddle.x) / noid.getWidth(), sharp)
////            ).resolution(0.1f);
//            reward("(enough,L)",
//                    () -> alive() ? (float) Math.pow(1 - Math.max(noid.ball.x - noid.paddle.x, 0) / noid.getWidth(), sharp) : 0
//            ).resolution(0.1f);
//            reward("(enough,R)",
//                    () -> alive() ? (float) Math.pow(1 - Math.max(noid.paddle.x - noid.ball.x, 0) / noid.getWidth(), sharp) : 0
//            ).resolution(0.1f);
//        }

//        nar.onTask(t->{
//            if (t.isGoal()) {
//                if (!t.isInput()) {
//                    if (t.isEternal())
//                        System.err.println(t);
//                    else
//                        System.out.println(t);
//                }
//            }
////           if (t.isQuest()) {
////               nar.concepts.stream().filter(x -> x.op() == IMPL && x.sub(1).equals(t.target())).forEach(i -> {
////                   //System.out.println(i);
////                   //nar.que(i.sub(0), QUEST, t.start(), t.end());
////                   nar.want(i.sub(0), Tense.Present,1f, 0.9f);
////               });
////           }
//        },GOAL);

    }

    public static class Pong {
        static int GAMES =
            //1;
            2;
            //3;
            //4;
        static final float fps = 30;

        public static void main(String[] args) {
            Player p = new Player().fps(fps);
            for (int i = 0; i < GAMES; i++) {
                ArkaNAR a = new ArkaNAR(
                    GAMES > 1 ? p("n", "a" + i) : $$("n"),
                    false, false, 9, 8
                );
                a.ballSpeed.set(8);
                p.add(a);
            }

            p.complexMax = 14;
            p.start();
            p.nar.runLater(() -> {
                window(new Gridding(p.games().map(z -> ((ArkaNAR) z).view())), 800, 600);
            });
        }
    }

    public static void main(String[] args) {
        float fps = 50;
        int GAMES =
            4;
            //2;
            //1;

        Player p = new Player().fps(fps);
        for (int i = 0; i < GAMES; i++) {
            ArkaNAR a = new ArkaNAR(
                GAMES > 1 ? p("n", "a" + i) : $$("n")
            );
            if (ManualOverrides && GAMES==1)
                a.keyWindow();
//            a.focus().log();
            p.add(a);
        }

//        p.meta = false;
//        p.volMax = 14;
        p.start();

        p.nar.runLater(() -> {
            window(new Gridding(p.games().map(z -> ((ArkaNAR) z).view())), 800, 600);
        });

//        p.nar.freqResolution.set(0.1f);  //lo-res
//            n.confResolution.set(0.05f);

        if (showClustering) {
            p.nar.runLater(() -> {
                Util.sleepMS(2000);
                var cc = p.nar.focus.stream()
                        .flatMap(f -> f.get().localInstances(BagClustering.class))
                        //.limit(1)
                        .map(c -> NARui.clusterView(c, p.nar))
                        .toList();
                window(new Gridding(cc), 400, 400);
            });
        }
    }

    public final ArkaNAR ballSpeed(float v) {
        ballSpeed.set(v);
        return this;
    }


    private boolean alive() {
        return noid.die <= 0;
    }

    public Surface view() {
        if (visW > 0)
            return new VectorSensorChart(cc, this).withControls();
        else
            return Surfaces.TODO.get();
    }

    private void initBipolarRelative() {
        actionBipolar(id,
                dx -> noid.paddle.move(dx * paddleSpeed()) ? dx : 0);
    }

    private void initPWM() {
        BiPolarAction lr = actionBipolar(inh(id, $.varDep(1)),
                //new BiPolarAction.Analog(),
                (x) -> noid.paddle.move(x * paddleSpeed()) ? x : 0
        );
    }

    volatile boolean leftOverride = false, rightOverride = false;


    private void initPushButton() {
        BiPolarAction lr = actionToggle(
                inh(id, "L"),
                inh(id, "R"),
                z -> alive() && ((leftOverride && !rightOverride) ||
                        (!(leftOverride && rightOverride) && z && move(-1))),
                z -> alive() && ((rightOverride && !leftOverride) ||
                        (!(leftOverride && rightOverride) && z && move(+1)))
        );
        afterFrame(() -> {
            if (leftOverride && rightOverride) return;
            else if (leftOverride) move(-1);
            else if (rightOverride) move(+1);
//            else if (leftGoaled) move(-1);
//            else if (rightGoaled) move(+1);/
        });


//        lr.pos.goalDefault($.t(0f, 0.0001f), nar);
//        lr.neg.goalDefault($.t(0f, 0.0001f), nar);
    }

    public void keyWindow() {

        VectorLabel v = new VectorLabel("use keys");
//        CheckBox b = new CheckBox("manual override");
        window(new Scale(
                new Widget(new Gridding(v)) {

//                    @Override
//                    protected void paintIt(GL2 gl, ReSurface r) {
//                        if (b.get()) {
//                            if (!leftOverride && !rightOverride) {
//                                //set both to override stop when no keys pressed
//                                leftOverride = rightOverride = true;
//                            }
//                        }
//                        super.paintIt(gl, r);
//                    }

                    @Override
                    public boolean key(com.jogamp.newt.event.KeyEvent e, boolean pressedOrReleased) {
                        switch (e.getKeyCode()) {
                            case com.jogamp.newt.event.KeyEvent.VK_UP -> leftOverride = rightOverride = pressedOrReleased;
                            case com.jogamp.newt.event.KeyEvent.VK_LEFT ->
                                    leftOverride = pressedOrReleased;
                            case com.jogamp.newt.event.KeyEvent.VK_RIGHT ->
                                    rightOverride = pressedOrReleased;
                        }
                        v.text(leftOverride + "," + rightOverride);
                        return false;
                    }
                }
//                   row(new PushButton("<-",()->{
//                    }))
                , 0.5f)
                , 400, 400);

    }

    private boolean move(float speed) {
        return noid.paddle.move(speed * paddleSpeed());
    }

    private void initUnipolar() {
        float res = 0.05f;
        final float thresh = 0.25f;
        action(inh(id, "L"),
                //u -> u > 0.5f && noid.paddle.move(-paddleSpeed * 2 * Util.sqr(2 * (u - 0.5f))) ? u : 0);
                u -> u > thresh && noid.paddle.move(-paddleSpeed() * Math.max(0, (u - thresh)) / (1 - thresh)) ? u : 0).freqRes(res);
        action(inh(id, "R"),
                //u -> u > 0.5f && noid.paddle.move(+paddleSpeed * 2 * Util.sqr(2 * (u - 0.5f))) ? u : 0);
                u -> u > thresh && noid.paddle.move(+paddleSpeed() * Math.max(0, (u - thresh)) / (1 - thresh)) ? u : 0).freqRes(res);
    }


    public float paddleSpeed() {
        return paddleSpeed.asFloat();
    }

    /**
     * https:
     */
    class Arkanoid extends Canvas implements KeyListener {

        static final int SCREEN_WIDTH = 250;
        static final int SCREEN_HEIGHT = 250;
        static final int BLOCK_TOP_MARGIN = 15;
        static final int BLOCK_BETWEEN_MARGIN = 3;
        float BALL_RADIUS = 15.0f * 1.25f;
        final FloatRange PADDLE_WIDTH = new FloatRange(0.16f, 0, 1);
        static final float PADDLE_HEIGHT = 20.0f;
        static final float BLOCK_WIDTH = 40.0f;
        static final float BLOCK_HEIGHT = 15.0f;
        public AtomicBoolean bricksEnabled = new AtomicBoolean(true);
        static final int COUNT_BLOCKS_X = 5;
        static final int COUNT_BLOCKS_Y = 2; /* 3 */
        static final float FT_STEP = 4.0f;
        public final Paddle paddle = new Paddle(SCREEN_WIDTH / 2f, SCREEN_HEIGHT - PADDLE_HEIGHT);
        public final Ball ball = new Ball(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
        public final Collection<Brick> bricks = Collections.newSetFromMap(new ConcurrentHashMap());
        final AtomicInteger brickSerial = new AtomicInteger(0);
        int score = 0;


        /* GAME VARIABLES */
        int die = 0;
        float BALL_VELOCITY = 2f;
        private int prevScore = 0;
        private static final int AFTERLIFE_TIME = 20;
        private boolean paddleHitsBall = false;

        Arkanoid(boolean bricksEnabled) {
            this.bricksEnabled.set(bricksEnabled);

            setSize(SCREEN_WIDTH, SCREEN_HEIGHT);

//            this.setUndecorated(false);
//            this.setResizable(false);
//
//
//            if (visible)
//                this.setVisible(true);

            paddle.x = SCREEN_WIDTH / 2f;


//            setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
//
//            new Timer(1000 / FPS, (e) -> {
//                repaint();
//            }).start();

            reset();
        }

        @Override
        public void paint(Graphics g) {
//            super.paint(g);

            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (die > 0) return;

            ball.draw(g);
            paddle.draw(g);
            for (Brick brick : bricks)
                brick.draw(g);
        }

        void increaseScore() {
            score++;
            if (score == (COUNT_BLOCKS_X * COUNT_BLOCKS_Y))
                win();
        }

        protected void win() {
            reset();
        }

        protected void die() {
            die = AFTERLIFE_TIME;
        }

        boolean intersects(GameObject mA, GameObject mB) {
            return mA.right() >= mB.left() && mA.left() <= mB.right()
                    && mA.bottom() >= mB.top() && mA.top() <= mB.bottom();
        }

        boolean testCollision(Paddle mPaddle, Ball mBall) {
            if (!intersects(mPaddle, mBall))
                return false;
            mBall.velocityY = -BALL_VELOCITY;
            mBall.velocityX = mBall.x < mPaddle.x ? -BALL_VELOCITY : BALL_VELOCITY;
            return true;
        }

        void testCollision(Brick mBrick, Ball mBall) {
            if (!intersects(mBrick, mBall))
                return;

            mBrick.destroyed = true;

            increaseScore();

            float overlapLeft = mBall.right() - mBrick.left();
            float overlapRight = mBrick.right() - mBall.left();
            float overlapTop = mBall.bottom() - mBrick.top();
            float overlapBottom = mBrick.bottom() - mBall.top();

            boolean ballFromLeft = overlapLeft < overlapRight;
            boolean ballFromTop = overlapTop < overlapBottom;

            float minOverlapX = ballFromLeft ? overlapLeft : overlapRight;
            float minOverlapY = ballFromTop ? overlapTop : overlapBottom;

            if (minOverlapX < minOverlapY) {
                mBall.velocityX = ballFromLeft ? -BALL_VELOCITY : BALL_VELOCITY;
            } else {
                mBall.velocityY = ballFromTop ? -BALL_VELOCITY : BALL_VELOCITY;
            }
        }

        void initializeBricks(Collection<Brick> bricks) {
            bricks.clear();

            int BLOCK_LEFT_MARGIN = Math.round((SCREEN_WIDTH  - (COUNT_BLOCKS_X * (BLOCK_WIDTH + 3)) - BLOCK_WIDTH)/2);
            if (bricksEnabled.getOpaque()) {
                for (int iX = 0; iX < COUNT_BLOCKS_X; ++iX) {
                    for (int iY = 0; iY < COUNT_BLOCKS_Y; ++iY) {
                        bricks.add(new Brick(
                                (iX + 1) * (BLOCK_WIDTH + BLOCK_BETWEEN_MARGIN) + BLOCK_LEFT_MARGIN,
                                (iY + 2) * (BLOCK_HEIGHT + BLOCK_BETWEEN_MARGIN) + BLOCK_TOP_MARGIN));
                    }
                }
            }
        }

        public void reset() {
            initializeBricks(bricks);
            ball.x = SCREEN_WIDTH / 2f;
            ball.y = bricksEnabled.getOpaque() ? SCREEN_HEIGHT / 2f : 0;
            ball.setVelocityRandom();
        }

        public float next() {
            if (die > 0) {
                die--;
                if (die == 0)
                    reset();
                else
                    return Float.NaN;
            }

            prevScore = score;

            BALL_VELOCITY = ballSpeed.floatValue();


            ball.update(paddle);

            paddleHitsBall = testCollision(paddle, ball);


            Iterator<Brick> it = bricks.iterator();
            while (it.hasNext()) {
                Brick brick = it.next();
                testCollision(brick, ball);
                if (brick.destroyed)
                    it.remove();
            }


            return score;
        }

        @Override
        public void keyPressed(KeyEvent event) {


            switch (event.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    paddle.move(-paddleSpeed());
                    break;
                case KeyEvent.VK_RIGHT:
                    paddle.move(+paddleSpeed());
                    break;
                default:
                    break;
            }
        }


        @Override
        public void keyReleased(KeyEvent event) {
//            switch (event.getKeyCode()) {
//                case KeyEvent.VK_LEFT:
//                case KeyEvent.VK_RIGHT:
//                    break;
//                default:
//                    break;
//            }
        }

        @Override
        public void keyTyped(KeyEvent arg0) {

        }

        abstract static class GameObject {
            abstract float left();

            abstract float right();

            abstract float top();

            abstract float bottom();
        }

        abstract class Rectangle extends GameObject {

            public float x;
            public float y;
            protected float sizeY;

            @Override
            float left() {
                return x - sizeX() / 2.0f;
            }

            @Override
            float right() {
                return x + sizeX() / 2.0f;
            }

            @Override
            float top() {
                return y - sizeY() / 2.0f;
            }

            @Override
            float bottom() {
                return y + sizeY() / 2.0f;
            }

            abstract public float sizeX();

            float sizeY() {
                return sizeY;
            }
        }

        class Paddle extends Rectangle {


            Paddle(float x, float y) {
                this.x = x;
                this.y = y;
                this.sizeY = PADDLE_HEIGHT;
            }

            /**
             * returns percent of movement accomplished
             */
            public synchronized boolean move(float dx) {
                float px = x;
                x = Util.clamp(x + dx, sizeX(), SCREEN_WIDTH - sizeX());
                return !Util.equals(px, x, 1f);
            }


            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect((int) (left()), (int) (top()), (int) sizeX(), (int) sizeY());
            }

            public void set(float freq) {
                x = freq * SCREEN_WIDTH;
            }

            public float moveTo(float target, float paddleSpeed) {
                target *= SCREEN_WIDTH;

                if (Math.abs(target - x) <= paddleSpeed) {
                    x = target;
                } else if (target < x) {
                    x -= paddleSpeed;
                } else {
                    x += paddleSpeed;
                }

                x = Math.min(x, SCREEN_WIDTH - 1);
                x = Math.max(x, 0);

                return x / SCREEN_WIDTH;
            }

            @Override
            public float sizeX() {
                return PADDLE_WIDTH.floatValue() * SCREEN_WIDTH;
            }
        }

        class Brick extends Rectangle implements Comparable<Brick> {

            int id;
            boolean destroyed;

            Brick(float x, float y) {
                this.x = x;
                this.y = y;
                //this.sizeX = BLOCK_WIDTH;
                this.sizeY = BLOCK_HEIGHT;
                this.id = brickSerial.incrementAndGet();
            }

            @Override
            public float sizeX() {
                return BLOCK_WIDTH;
            }

            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect((int) left(), (int) top(), (int) sizeX(), (int) sizeY());
            }

            @Override
            public int compareTo(Brick o) {
                return Integer.compare(id, o.id);
            }
        }

        class Ball extends GameObject {

            public float x;
            public float y;
            public float velocityX;
            public float velocityY;

            Ball(int x, int y) {
                this.x = x;
                this.y = y;
                setVelocityRandom();
            }

            void setVelocityRandom() {
                this.setVelocity(BALL_VELOCITY, (float) (Math.random() * -Math.PI * (2 / 3f) + -Math.PI - Math.PI / 6));
            }

            public void setVelocity(float speed, float angle) {
                this.velocityX = (float) Math.cos(angle) * speed;
                this.velocityY = (float) Math.sin(angle) * speed;
            }

            void draw(Graphics g) {
                g.setColor(Color.WHITE);
                int r = (int) BALL_RADIUS * 2;
                g.fillOval((int) left(), (int) top(), r, r);
            }

            void update(Paddle paddle) {
                x += velocityX;
                y += velocityY;

                if (left() < 0)
                    velocityX = BALL_VELOCITY;
                else if (right() > SCREEN_WIDTH)
                    velocityX = -BALL_VELOCITY;
                if (top() < 0) {
                    velocityY = BALL_VELOCITY;
                } else if (bottom() > SCREEN_HEIGHT) {
                    velocityY = -BALL_VELOCITY;
                    x = paddle.x;
                    y = paddle.y - 50;
                    die();
                }

            }

            @Override
            float left() {
                return x - BALL_RADIUS;
            }

            @Override
            float right() {
                return x + BALL_RADIUS;
            }

            @Override
            float top() {
                return y - BALL_RADIUS;
            }

            @Override
            float bottom() {
                return y + BALL_RADIUS;
            }

        }


    }
}
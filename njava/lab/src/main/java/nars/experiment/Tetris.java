package nars.experiment;

import jcog.Fuzzy;
import jcog.Util;
import jcog.math.FloatDifference;
import jcog.math.FloatSupplier;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import jcog.signal.MutableEnum;
import jcog.signal.wave2d.AbstractBitmap2D;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.Player;
import nars.Term;
import nars.func.java.Opjects;
import nars.game.Game;
import nars.game.reward.Reward;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.BitmapSensor;
import nars.sensor.PixelBag;
import nars.term.atom.Atomic;
import nars.truth.Truther;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.meter.ImmediateMatrixView;
import spacegraph.video.Draw;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Float.NaN;
import static jcog.Config.INT;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends Game {

    static final float fps =
        50
        //30
        //20
        //16
    ;

    /** downsampled superpixel mean convolution */
    private final boolean bitmapSuperVision = false;
    private final BitmapSensor<PixelBag> superVision;

    private static void runTetris(Tetris g) {
        Player p = new Player(g).fps(fps);
        //p.nalStructural = false;
        //p.gameReflex = true;
//            p.selfMetaReflex = p.subMetaReflex = true;
        p.start();
        window(new Gridding(p.the(Tetris.class).map(Tetris::view)), 400, 800);
    }

    public Tetris fallTime(int t) {
        fallTime.set(t);
        return this;
    }


    public static class Tetris_Classic {
        public static void main(String[] args) {
            runTetris(new Tetris());
        }
    }
    public static class Dotris_Small {
        public static void main(String[] args) {
            runTetris(new Tetris.Dotris($.$$("t"), 5, 12));
        }
    }
    
    /** TODO abstract better w/superclass */
    public static class Dotris extends Tetris {
        public Dotris(Term t, int w, int h) {
            super(t, w, h);
            mode.set(Tetris.PieceSupplier.Simple);
        }

        @Override
        @Deprecated protected void rotation() {
            //nothing
        }
    }

    public static class Multi {
        final int GAMES;

        Multi(int games, int tetris_width, int tetris_height, PieceSupplier mode) {
            this.GAMES = games;
            Player p = new Player(GAMES == 1 ?
                Stream.of(new Tetris())
                :
                IntStream.range(0, GAMES).mapToObj(i -> new Tetris(
                    $.p("tetris", Character.toString('a' + i)),
                    tetris_width, tetris_height).mode(mode))
            ).fps(fps).start();

            window(new Gridding(p.the(Tetris.class).map(Tetris::view)), 800, 950);
        }

        public static void main(String[] args) {
            new Multi(3, tetris_width, tetris_height, PieceSupplier.Tetris);
        }
    }

    public static class MultiEasy {
        public static void main(String[] args) {
            new Multi(4, 7, 7, PieceSupplier.Simple);
        }
    }


    static final int tetris_width  = INT("TETRIS_WIDTH",   8),
                     tetris_height = INT("TETRIS_HEIGHT", 16);

    static boolean opjects;

    static boolean bitmapVision = true;

    private final boolean drop = false;
    private final boolean speed = false;

    public boolean rewardAlive = true;

    public boolean rewardDensity = true;
    public boolean rewardDensityChange = false;
    public boolean rewardDensityPerLine;

    public boolean rewardLow = true;
    public boolean rewardLowChange = false;

    public boolean rewardCorrect = false;
    public boolean rewardScore = false;

    public boolean rewardMomentum = false;

    private Opjects opj;


    public enum PieceSupplier {
        None {
            @Override
            public @Nullable TetrisPiece next() {
                return null;
            }
        },
        Simple {
            final int[][] CENTER_5_X_5 = {
                    TetrisPiece.EMPTY_ROW
                    , TetrisPiece.EMPTY_ROW
                    , TetrisPiece.CENTER
                    , TetrisPiece.EMPTY_ROW
                    , TetrisPiece.EMPTY_ROW};
            final TetrisPiece b = new TetrisPiece() {{
                setShape(0, CENTER_5_X_5);
                setShape(1, CENTER_5_X_5);
                setShape(2, CENTER_5_X_5);
                setShape(3, CENTER_5_X_5);
            }};
            @Override
            public TetrisPiece next() {
                return b;
            }
        },
        Block {
            @Override
            public TetrisPiece next() {
                return TetrisModel.PossibleBlocks.Square.shape;
            }
        },
        Tetris {
            final List<TetrisPiece> x = Stream.of(TetrisModel.PossibleBlocks.values()).map(posBlocks -> posBlocks.shape).toList();
            @Override
            public TetrisPiece next() {
                return x.get(ThreadLocalRandom.current().nextInt(x.size()));
            }
        };

        @Nullable
        public abstract TetrisPiece next();
    }

    public final MutableEnum<PieceSupplier> mode = new MutableEnum<>(PieceSupplier.Tetris);

    public final FloatRange debounceDurs = new FloatRange(1, 0, 2);
    private final int width, height;
    public IntRange fallTime, afterlifeTime;

    BitmapSensor<Bitmap2D> vision;

    /** higher number is more lenient height penalty */
    static final float lowPower =
        1;
        //2;

    /** lower value makes smaller amounts of density appear more */
    static final float densityPower =
        1;
        //0.5f;


//    /** LR and rotate sensitivity (action threshold) */
//    public final FloatRange sensitivity = new FloatRange(0.9f, 0, 1);
    private final TetrisModel state;
    private final Term tLEFT =
            //$.the("L");
            $.inh(id, "L");
    //$.inh(id, NAct.NEG);
    private final Term tRIGHT =
            //$.the("R");
            $.inh(id, "R");
    //$.inh(id, NAct.POS);
    private final Term tROT =
            //$.the("rotate");
            //$.inh("rotate", id);
            $.inh(id, "rotate");
    private final Term tFALL =
            //$.the("fall");
            //$.inh("fall", id);
            $.inh(id, "fall");
    private final AbstractBitmap2D grid;

    //private final FloatToFloatFunction sensitivityThresh = (x)->NAct.sensitivityToThresh(sensitivity).asFloat();

    public Tetris() {
        this(Atomic.atom("tetris"), tetris_width, tetris_height);
    }

    public Tetris(Term id, int width, int height) {
        super(id);

        this.width = width;
        this.height = height;

        state = opjects ?
            actionsReflect() :
            new TetrisModel(width, height);
        fallTime = state.fallTime;
        afterlifeTime = state.afterlifeTime;

        if (speed) {
            var speedAction = action($.inh(id, "speed"), s -> {
                if (!state.running)
                    return NaN;
                else {
                    this.fallTime.setLerp(s);
                    return s;
                }
            });
        }

        FloatSupplier rowsFilled = () -> {
            //if (state.dead()) return 0;
            return state.rowsFilled;
        };


        FloatSupplier low = () -> {
            if (state.dead()) return 0;
            int filled = state.rowsFilled;
            return (float) (1 - Math.pow((float) filled / height, lowPower));
        };

        if (rewardAlive)
            reward($.inh(id, "alive"), /*Reward.attack(*/
                ()->state.dead() ? 0 : 1
            /*,20)*/);

        if (rewardScore) {
            var rs = reward($.inh(id, "score"), new FloatSupplier() {
                //                final FloatToFloatFunction f = new Percentilizer(100);
                //final FloatToFloatFunction g = new FloatNormalizer(0, 1000);
                private float r(float r) {
                    return Fuzzy.unpolarize(Util.clampSafe(r, -1, +1));
                }

                @Override
                public float asFloat() {
                    return state.dead() ? 0 : /**f.valueOf*/(r(state.score()));
                }
            });
            rs.sensor.truther(new Truther.PolarTruther(()->nar));
        }

        if (rewardLow)
            reward($.inh(id, "low"), low);
        if (rewardCorrect)
            reward($.inh(id, "correct"), new StabilitySupplier());
        if (rewardDensity) {
            if (!rewardDensityPerLine) {
                reward($.inh(id, "dense"), new DensitySupplier());
            } else {
                for (int r = 0; r < height; r++) {
                    int R = r;
                    reward($.inh(id, $.p(r, "dense")), ()-> {
                        if (state.dead()) return 0;
                        double filled = 0;
                        for (int c = 0; c < width; c++)
                            filled+= state.grid(c, R) > 0 ? 1 : 0;
                        if (filled==0) return 1;
                        else return ((float)filled)/width;
                    });
                }
            }
        }
        if (rewardLowChange) {
            var dLow = new FloatDifference(rowsFilled, this::time).composeIfFinite(d ->
                            state.dead() || state.empty() || d <= -(height - 2) ? 0 :
                                    (d < 0 ? 1 : (d > 0 ? 0 : 0.99f)) //happy if same or lower
                    //(d > 0 ? 0 : 1) //happy if same or lower
            );
            if (rewardMomentum) dLow = Reward.sustain(dLow, 1);
            reward($.inh(id, "lower"), dLow)/*.strength(s)*/;
        }

        if (rewardDensityChange) {
            var dChange = new FloatDifference(new DensitySupplier(), this::time).compose(d ->
                    state.dead() ? 0 : (d == 0 ? 0.5f : (d > 0 ? 1 : 0))
            );
            if (rewardMomentum) dChange = Reward.sustain(dChange, 1);
            reward($.inh(id, "denser"), dChange)/*.strength(s)*/;
        }


        LRtoggle();

        rotation();

        if (drop)
            drop();

        this.grid = new AbstractBitmap2D(state.width, state.height) {
            @Override
            public float value(int x, int y) {
                return (float) state.seen[y * w + x];
            }// > 0 ? 1 : 0;
        };

        vision = bitmapVision ? addSensor(new BitmapSensor<>(grid,
                (x, y) -> $.inh(id, $.p(x, y))
                //(x, y) -> $.inh(id, $.p("v", $.p(x, y)))
                //(x, y) -> $.inh($.p(x, y), id),
                //(x, y) -> $.p(id, $.p($.the(x), $.the(y))) //FLAT PRODUCT
        )) : null;

        superVision = bitmapSuperVision ? addSensor(new BitmapSensor<>(
                new PixelBag(grid, grid.width() / 2, grid.height() / 2).setZoom(0),
                (x, y) -> $.inh($.p("mean", id), $.p(x, y))
        )) : null;

        beforeFrame(()-> {
            state.nextMode = this.mode.get();
            state.next();
            if (grid!=null)
                grid.updateBitmap();
        });
    }

    @Override
    protected void init() {
        super.init();
        if (opj!=null)
            nar.add(opj); //HACK
    }


    private TetrisModel actionsReflect() {

        opj = new Opjects(focus());
        opj.exeThresh.set(0.5f);
        opj.methodExclusions.add("toVector");

        Term oi = id; //$.p("opjects", id);
        return opj.a(oi, TetrisModel.class, tetris_width, tetris_height);
    }

    private void LRtoggle() {
        actionKey(actionToggle(tLEFT, tRIGHT,
            /*debounce*/(b -> b && state.act(TetrisModel.actions.LEFT )),
            /*debounce*/(b -> b && state.act(TetrisModel.actions.RIGHT))
            //,sensitivityThresh, q()
        ), 'a', 's');
    }

    protected void rotation() {
        actionKey(actionPushButton(tROT, debounce(
            b -> b && state.act(TetrisModel.actions.CW))),' ');
    }

    protected void drop() {
        actionPushButton(tFALL, debounce(b -> b && state.act(TetrisModel.actions.FALL)));
    }

    private BooleanPredicate debounce(BooleanPredicate a) {
        return debounce(a, () -> debounceDurs.asFloat() * fallTime.intValue());
    }


//    void actionsTriState() {
//
//
//        actionTriState($.inh("X", id), i -> switch (i) {
//            case -1 -> state.act(TetrisModel.actions.LEFT);
//            case +1 -> state.act(TetrisModel.actions.RIGHT);
//            default -> true;
//        });
//
//
//        actionPushButton(tROT, () -> state.act(TetrisModel.actions.CW));
//
//
//    }

    public static class TetrisPiece {
        public static int[] CENTER = {0, 0, 1, 0, 0};
        static int[] EMPTY_ROW = {0, 0, 0, 0, 0};
        static int[] PAIR1 = {0, 0, 1, 1, 0};
        static int[] PAIR2 = {0, 1, 1, 0, 0};
        static int[] MIDDLE = {0, 1, 1, 1, 0};
        static int[] LINE1 = {0, 1, 1, 1, 1};
        static int[] LEFT1 = {0, 1, 0, 0, 0};
        static int[] RIGHT1 = {0, 0, 0, 1, 0};
        int[][][] thePiece = new int[4][5][5];
        int currentOrientation;

        void setShape(int Direction, int[]... rows) {
            thePiece[Direction] = rows;
        }

        @Override
        public String toString() {
            var shapeBuffer = new StringBuilder();
            int[][] p = thePiece[currentOrientation];
            for (var i = 0; i < p.length; i++) {
                for (var j = 0; j < p[i].length; j++)
                    shapeBuffer.append(' ').append(p[i][j]);
                shapeBuffer.append('\n');
            }
            return shapeBuffer.toString();
        }
    }

    public static class TetrisModel {

        public final FloatRange blur = new FloatRange(0.1f, 0, 1);

        public final IntRange afterlifeTime = new IntRange(4, 0, 100);

        public final IntRange fallTime = new IntRange(
                INT("TETRIS_FALL_TIME", 2),
                1,
                INT("TETRIS_FALL_MAX", 10)
        );

        PieceSupplier mode = PieceSupplier.None, nextMode;

        public int width, height;

        /** grid with current block */
        public double[] seen;

        /** grid without the current block */
        public double[] grid;

        public boolean running = true;

        /** what is the current_score */
        public float score;

        public int time;
        int currentRotation;

        /** position of the falling block */
        int currentX = -1, currentY;
        int blockID = -1;

        /** > 0:  have we reached the end state yet*/
        int dead;


        private int rowsFilled, rowsFilledAtLastSpawn;

        TetrisPiece currentPiece;

        private final boolean resetXonNext = true;

        public TetrisModel(int width, int height) {
            this.width = width;
            this.height = height;
            grid = new double[width * height];
            seen = new double[width * height];
            reset();
        }


        protected void reset() {
            if (currentX < 0)
                currentX = width / 2 - 1;
            currentY = 0;
            currentRotation = 0;
            score = 0;
            Arrays.fill(grid, 0);
            running = true;
        }

        private void toVector(boolean monochrome, double[] target) {
            fade(target);

            float decayRate = this.blur.floatValue();
            var oneMinDecayRate = 1 - decayRate;

            // Add new values
            var x = 0;
            for (var i : grid) {
                double newValue =
                    i;
//                        monochrome ? ((i > 0) ? +1 : -1) :
//                        ((i > 0) ? i : -1);
                target[x++] += newValue * oneMinDecayRate;
            }
            writeCurrentBlock(0.5f, target);
        }

        private void fade(double[] target) {
            float decayRate = this.blur.floatValue();
            // Apply decay to the existing values
            for (int i = 0; i < target.length; i++)
                target[i] *= decayRate;
        }

        private void writeCurrentBlock(float color, double[] f) {
            if (currentPiece==null) return;

            var thisPiece = currentPiece.thePiece[currentRotation];

            if (color == -1)
                color = 1;//currentBlockId + 1;
            for (var y = 0; y < thisPiece[0].length; ++y)
                for (var x = 0; x < thisPiece.length; ++x)
                    if (thisPiece[x][y] != 0)
                        f[i(currentX + x, currentY + y)] = color;

        }

        public final int gameOver() {
            int o = this.dead;
            if (o > 0)
                this.dead--;
            return o;
        }


        /* This code applies the action, but doesn't do the default fall of 1 square */
        synchronized boolean act(actions theAction) {
            var nextRotation = currentRotation;
            var nextX = currentX;
            var nextY = currentY;

            switch (theAction) {
                case CW ->
                    nextRotation = (currentRotation + 1) % 4;
                case CCW -> {
                    nextRotation = currentRotation - 1;
                    if (nextRotation < 0) nextRotation = 3;
                }
                case LEFT  -> nextX = currentX - 1;
                case RIGHT -> nextX = currentX + 1;
                case FALL -> {
                    nextY = currentY;
                    boolean isInBounds = true, isColliding = false;
                    while (isInBounds && !isColliding) {
                        nextY++;
                        isInBounds = inBounds(nextX, nextY, nextRotation);
                        if (isInBounds)
                            isColliding = colliding(nextX, nextY, nextRotation);
                        else
                            break;
                    }
                    nextY--;
                }
                default -> throw new RuntimeException("unknown action");
            }

            if (inBounds(nextX, nextY, nextRotation) && !colliding(nextX, nextY, nextRotation)) {
                currentRotation = nextRotation;
                currentX = nextX;
                currentY = nextY;
                return true;
            }

            return false;
        }

        /**
         * Calculate the learn array position from (x,y) components based on
         * worldWidth.
         * Package level access so we can use it in tests.
         *
         * @param x
         * @param y
         *
         */
        private int i(int x, int y) {
            return y * width + x;
        }

        /**
         * Check if any filled part of the 5x5 block array is either out of bounds
         * or overlapping with something in wordState
         *
         * @param checkX           X location of the left side of the 5x5 block array
         * @param checkY           Y location of the top of the 5x5 block array
         * @param checkOrientation Orientation of the block to check
         * @return
         */
        private boolean colliding(int checkX, int checkY, int checkOrientation) {
            if (currentPiece == null) return false;
            var result = false;
            var thePiece = currentPiece.thePiece[checkOrientation];
            var ll = thePiece.length;
//            try {

                for (var y = 0; y < thePiece[0].length && !result; ++y) {
                    var i = checkY + y;
                    var b1 = !(i < 0 || i >= height);
                    for (var x = 0; x < ll && !result; ++x) {
                        var i1 = checkX + x;
                        result = (!b1 || i1 < 0 || i1 >= width || grid(i1, i) != 0) && thePiece[x][y] != 0;
                    }
                }
//            } catch (ArrayIndexOutOfBoundsException e) {
//                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::colliding called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
//                System.err.println("Error: The Exception was: " + e);
//                Thread.dumpStack();
//                System.err.println("Returning true from colliding to help save from error");
//                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
//                is_game_over = 1;
//            }
            return result;
        }

        private boolean collidingCheckOnlySpotsInBounds(int checkX, int checkY, int checkOrientation) {
            var result = false;
            var thePiece = currentPiece.thePiece[checkOrientation];
            var ll = thePiece.length;
//            try {

                for (var y = 0; y < thePiece[0].length && !result; ++y) {
                    var i1 = checkY + y;
                    if (i1 >= 0 && i1 < height)
                        for (var x = 0; x < ll && !result; ++x) {
                            var i = checkX + x;
                            result = thePiece[x][y] != 0 && i >= 0 && i < width && grid(i, i1) != 0;
                        }
                }
//            } catch (ArrayIndexOutOfBoundsException e) {
//                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::collidingCheckOnlySpotsInBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
//                System.err.println("Error: The Exception was: " + e);
//                Thread.dumpStack();
//                System.err.println("Returning true from colliding to help save from error");
//                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
//                is_game_over = 1;
//            }
            return result;
        }

        /**
         * This function checks every filled part of the 5x5 block array and sees if
         * that piece is in bounds if the entire block is sitting at (checkX,checkY)
         * on the board.
         *
         * @param checkX           X location of the left side of the 5x5 block array
         * @param checkY           Y location of the top of the 5x5 block array
         * @param checkOrientation Orientation of the block to check
         * @return
         */
        private boolean inBounds(int checkX, int checkY, int checkOrientation) {
            var result = false;
            if (currentPiece == null)
                return true;

//            try {
                var thePiece = currentPiece.thePiece[checkOrientation];

                var finished = false;
                for (var y = 0; !finished && y < thePiece[0].length; ++y) {
                    var i1 = checkY + y;
                    var b = i1 >= 0 && i1 < height;
                    for (var x = 0; !finished && x < thePiece.length; ++x) {
                        var i = checkX + x;
                        finished = (!b || i < 0 || i >= width) && thePiece[x][y] != 0;
                    }
                }

                result = !finished;

//            } catch (ArrayIndexOutOfBoundsException e) {
//                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::inBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
//                System.err.println("Error: The Exception was: " + e);
//                Thread.dumpStack();
//                System.err.println("Returning false from inBounds to help save from error.  Not sure if that's wise.");
//                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
//                is_game_over = true;
//            }

            return result;
        }

        boolean nextInBounds() {
            return inBounds(currentX, currentY + 1, currentRotation);
        }

        boolean nextColliding() {
            return colliding(currentX, currentY + 1, currentRotation);
        }

        /*Ok, at this point, they've just taken their action.  We now need to make them fall 1 spot, and check if the game is over, etc */
        private void update() {

            if (!inBounds(currentX, currentY, currentRotation))
                System.err.println("In GameState.Java the Current Position of the board is Out Of Bounds... Consistency Check Failed");


            if (!nextInBounds() || nextColliding()) {
                running = false;
                writeCurrentBlock(-1, grid);
            } else {
                if (++time % this.fallTime.intValue() == 0)
                    currentY += 1;
            }
        }

        public void spawnBlock() {
            blockID++;
            running = true;
            this.rowsFilledAtLastSpawn = rowsFilled;

            currentPiece = mode.next();

            currentRotation = 0;
            if (resetXonNext)
                currentX = width / 2 - 2;
            currentY = -4;

            var hitOnWayIn = false;
            while (!inBounds(currentX, currentY, currentRotation)) {
                hitOnWayIn = collidingCheckOnlySpotsInBounds(currentX, currentY, currentRotation);
                currentY++;
            }

            boolean is_game_over = hitOnWayIn || colliding(currentX, currentY, currentRotation);
            if (is_game_over) {
                this.dead = afterlifeTime.intValue();
                reset();
                running = false; //HACK undoes running=true in reset()
            }

        }


        void checkScore() {
            var numRowsCleared = 0;

            for (var y = height - 1; y >= 0; --y)
                if (isRow(y, true)) {
                    removeRow(y);
                    numRowsCleared += 1;
                    y += 1;
                }

            var rowsFilled = 0;
            for (var y = 0; y < height; y++) {
                for (var x = 0; x < width; x++)
                    if (grid(x,y)!=0) { rowsFilled++; break; }
            }


            var prevRows = this.rowsFilled;
            this.rowsFilled = rowsFilled;

            var diff = prevRows - rowsFilled;

            score = diff >= height - 1 ? NaN : diff;
            //System.out.println(rowsFilled + "rows filled,\t" + score + " score");
        }

        /**
         * Check if a row has been completed at height y.
         * Short circuits, returns false whenever we hit an unfilled spot.
         *
         * @param y
         * @return
         */
        public boolean isRow(int y, boolean filledOrClear) {
            int bound = width;
            for (int x = 0; x < bound; x++) {
                double s = grid(x, y);
                if (filledOrClear ? s == 0 : s != 0)
                    return false;
            }
            return true;
        }

        private double grid(int x, int y) {
            return grid[i(x, y)];
        }

        /**
         * Dec 13/07.  Radkie + Tanner found 2 bugs here.
         * Bug 1: Top row never gets updated when removing lower rows. So, if there are
         * pieces in the top row, and we clear something, they will float there.
         *
         * @param y
         */
        void removeRow(int y) {
            if (!isRow(y, true)) {
                System.err.println("In GameState.java remove_row you have tried to remove a row which is not complete. Failed to remove row");
                return;
            }

            for (var x = 0; x < width; ++x) {
                var linearIndex = i(x, y);
                grid[linearIndex] = 0f;
            }


            for (var ty = y; ty > 0; --ty)
                for (var x = 0; x < width; ++x) {
                    var linearIndexTarget = i(x, ty);
                    var linearIndexSource = i(x, ty - 1);
                    grid[linearIndexTarget] = grid[linearIndexSource];
                }


            for (var x = 0; x < width; ++x) {
                var linearIndex = i(x, 0);
                grid[linearIndex] = 0f;
            }

        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        /**
         * Utility methd for debuggin
         */
        protected void printState() {
            var index = 0;
            for (var i = 0; i < height - 1; i++) {
                for (var j = 0; j < width; j++) System.out.print(grid[i * width + j]);
                System.out.print("\n");
            }
            System.out.println("-------------");

        }

        protected void next() {
            if (gameOver() > 0) {
                fade(seen);
                return;
            }

            var running = this.running;
            if (running)
                update();

            if (!running || nextMode!=mode)  {
                mode = nextMode;
                spawnBlock();
            }

            checkScore();

            if (!dead())
                toVector(false, seen);
        }

        private float score() {
            return score;
        }

        public boolean dead() {
            return dead > 0;
        }

        public boolean empty() {
            for (double v : this.grid)
                if (v != 0)
                    return false;
            return true;
        }

        public enum actions {

            /**
             * Action value for a move left
             */
            LEFT,
            /**
             * Action value for a move right
             */
            RIGHT,
            /**
             * Action value for a clockwise rotation
             */
            CW,
            /**
             * Action value for a counter clockwise rotation
             */
            CCW,
            /**
             * The no-action Action
             */
            NONE,
            /**
             * fall down
             */
            FALL,
        }

        public enum PossibleBlocks {
            Line(new TetrisPiece() {{
                setShape(0, CENTER
                        , CENTER
                        , CENTER
                        , CENTER
                        , EMPTY_ROW);
                setShape(1, EMPTY_ROW
                        , EMPTY_ROW
                        , LINE1
                        , EMPTY_ROW
                        , EMPTY_ROW);
                setShape(2, CENTER
                        , CENTER
                        , CENTER
                        , CENTER
                        , EMPTY_ROW);
                setShape(3, EMPTY_ROW
                        , EMPTY_ROW
                        , LINE1
                        , EMPTY_ROW
                        , EMPTY_ROW);
            }}),
            Square(new TetrisPiece() {{
                setShape(0, EMPTY_ROW
                        , PAIR1
                        , PAIR1
                        , EMPTY_ROW
                        , EMPTY_ROW);
                setShape(1, EMPTY_ROW
                        , PAIR1
                        , PAIR1
                        , EMPTY_ROW
                        , EMPTY_ROW);
                setShape(2, EMPTY_ROW
                        , PAIR1
                        , PAIR1
                        , EMPTY_ROW
                        , EMPTY_ROW);
                setShape(3, EMPTY_ROW
                        , PAIR1
                        , PAIR1
                        , EMPTY_ROW
                        , EMPTY_ROW);
            }}),
            Tri(new TetrisPiece() {{

                setShape(0, EMPTY_ROW
                        , CENTER
                        , MIDDLE
                        , EMPTY_ROW
                        , EMPTY_ROW);


                setShape(1, EMPTY_ROW
                        , CENTER
                        , PAIR1
                        , CENTER
                        , EMPTY_ROW);


                setShape(2, EMPTY_ROW
                        , EMPTY_ROW
                        , MIDDLE
                        , CENTER
                        , EMPTY_ROW);

                setShape(3, EMPTY_ROW
                        , CENTER
                        , PAIR2
                        , CENTER
                        , EMPTY_ROW);
            }}),
            SShape(new TetrisPiece() {{

                setShape(0, EMPTY_ROW
                        , LEFT1
                        , PAIR2
                        , CENTER
                        , EMPTY_ROW);
                setShape(1, EMPTY_ROW
                        , PAIR1
                        , PAIR2
                        , EMPTY_ROW
                        , EMPTY_ROW);
                setShape(2, EMPTY_ROW
                        , LEFT1
                        , PAIR2
                        , CENTER
                        , EMPTY_ROW);
                setShape(3, EMPTY_ROW
                        , PAIR1
                        , PAIR2
                        , EMPTY_ROW
                        , EMPTY_ROW);
            }}),
            ZShape(new TetrisPiece() {{

                setShape(0, EMPTY_ROW
                        , CENTER
                        , PAIR2
                        , LEFT1
                        , EMPTY_ROW);
                setShape(1, EMPTY_ROW
                        , PAIR2
                        , PAIR1
                        , EMPTY_ROW
                        , EMPTY_ROW);
                setShape(2, EMPTY_ROW
                        , CENTER
                        , PAIR2
                        , LEFT1
                        , EMPTY_ROW);
                setShape(3, EMPTY_ROW
                        , PAIR2
                        , PAIR1
                        , EMPTY_ROW
                        , EMPTY_ROW);
            }}),
            LShape(new TetrisPiece() {{

                setShape(0, EMPTY_ROW
                        , CENTER
                        , CENTER
                        , PAIR1
                        , EMPTY_ROW);

                setShape(1, EMPTY_ROW
                        , EMPTY_ROW
                        , MIDDLE
                        , LEFT1
                        , EMPTY_ROW);

                setShape(2, EMPTY_ROW
                        , PAIR2
                        , CENTER
                        , CENTER
                        , EMPTY_ROW);

                setShape(3, EMPTY_ROW
                        , RIGHT1
                        , MIDDLE
                        , EMPTY_ROW
                        , EMPTY_ROW);
            }}),
            JShape(new TetrisPiece() {{
                setShape(0, EMPTY_ROW
                        , CENTER
                        , CENTER
                        , PAIR2
                        , EMPTY_ROW);
                setShape(1, EMPTY_ROW
                        , LEFT1
                        , MIDDLE
                        , EMPTY_ROW
                        , EMPTY_ROW);
                setShape(2, EMPTY_ROW
                        , PAIR1
                        , CENTER
                        , CENTER
                        , EMPTY_ROW);
                setShape(3, EMPTY_ROW
                        , EMPTY_ROW
                        , MIDDLE
                        , RIGHT1
                        , EMPTY_ROW);
            }});

            public TetrisPiece shape;

            PossibleBlocks(TetrisPiece shape) {
                this.shape = shape;
            }
        }


    }

    private static Splitting view(Tetris t) {
        return new Splitting<>(
                new ObjectSurface(t),

                0.9f,

                t.vision != null ?
                        new VectorSensorChart(t.vision, t).withControls()
                        :
                        tetrisView(t)

        ).resizeable();
    }


    public static ImmediateMatrixView tetrisView(Tetris t) {
        return new ImmediateMatrixView(t.width, t.height,
                (x, y, i) -> Draw.colorBipolar(t.grid.value(x, y)));
    }


    private class DensitySupplier implements FloatSupplier {
        int lastBlock = -1;
        float density;
//        private static final float power =
//            //1;
//            0.5f;

        @Override
        public float asFloat() {
            if (state.dead())
                return 0;
            int nextBlock = state.blockID;
            if (nextBlock != lastBlock) {
                //only update score after piece lands
                lastBlock = nextBlock;
                int cellsFilled = Util.count(s -> s > 0, state.grid);
                float _score = cellsFilled / (1f + state.rowsFilled * state.width);
                density = (float) Math.pow(_score, densityPower);

//                int rowsFilled = state.rowsFilled;
//                lastScore = rowsFilled > 0 ? ((float) cellsFilled) / (rowsFilled * state.width) :
//                        0;
//
//                /*normalize by shortness*/
//                lastScore *= 1 - ((float)state.rowsFilled)/height, power);
            }
            return density;
        }
    }

    /** counts proportion of columns which do not have empty spaces beneath a filled space
     *  TODO generalize to a 'lack-of-error' rate with multiple empty spaces per column each counted
     * */
    private class StabilitySupplier implements FloatSupplier {

        @Override
        public float asFloat() {
            if (state.dead()) return 0;
            int colsError = 0;
            for (int col = 0; col < state.width; col++) {
                boolean fill = false;
                boolean emptyBelowFill = false;
                for (int row = 0; row < state.height; row++) {
                    double g = state.grid(col, row);
                    boolean rowFill = g != 0;
                    if (rowFill)
                        fill = true;
                    else {
                        if (fill)
                            emptyBelowFill = true;
                    }
                }
                if (emptyBelowFill)
                    colsError++;
            }
            return 1 - (((float) colsError) / state.width);
        }
    }

    public static class MiniTris {
        public static void main(String[] args) {
            Tetris t = new Tetris(Atomic.atom("tetris"), 6, 12);
            Player p = new Player(t).fps(50);

            p.start();

            window(new Gridding(p.the(Tetris.class).map(Tetris::view)), 800, 800);

        }
    }


    public final Tetris mode(PieceSupplier mode) {
        this.mode.set(mode);
        return this;
    }

}
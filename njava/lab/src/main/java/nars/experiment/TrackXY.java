package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.Fuzzy;
import jcog.TODO;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.math.FloatSupplier;
import jcog.signal.FloatRange;
import jcog.signal.wave2d.ArrayBitmap2D;
import jcog.test.control.TrackXYModel;
import nars.*;
import nars.game.Game;
import nars.game.sensor.VectorSensor;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.BitmapSensor;
import nars.task.SerialTask;
import nars.truth.proj.TruthProjection;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Containers;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.video.Draw;

import java.util.Set;
import java.util.stream.Stream;

import static nars.$.$$;
import static spacegraph.space2d.container.grid.Containers.col;

public class TrackXY extends Game {

    /** when enabled, if the desired action is impossible, feedback is disabled */
    private static final boolean FEEDBACK_QUELL =
        //true;
        false;

    private static final boolean controlPosOrVel =
        true;
        //false;

    public static void main(String[] args) {

        TrackXY g = new TrackXY($$("t"), 4, 1);

        Player p = new Player(g);
        p.arith = false;
        p.fps(40).start();

        //p.timeShiftEpochs = 4;
        //p.timeRangeOctaves = 1;
//        p.timeRangeBase = 2;
//        p.meta = false;

//        p.nalDelta = p.implBeliefify = p.implGoalify = p.implCache = false;
        //p.nalStructural = false;
        //p.nalSim = false;
//        p.inperience = false;
//        p.varIntro = false;
//
//        p.threads = 2;
//        p.gameReflex = true;

//        p.timeRes = 10;
        //p.freqRes = 0.1f;
//        p.volMax = 16;
//        p.timeRangeOctaves = 2;
//        //p.timeRes = 20;
//        p.freqRes = 0.1f;
//        p.threads = 4;


        //p.gameReflex = true;
        //p.selfMetaReflex = true;
        //p.subMetaReflex = true;

        //p.linkPriOp = true;
//        p.simplicityControl = true;
        //p.inOutPri = true;


//        g.onFrame(new Runnable() {
//
//            final List<Term> v = g.vocabulary.stream().toList();
//
//            @Override
//            public void run() {
//                int n = 3;
//                TmpTermList l = new TmpTermList(n);
//                for (int i = 0; i < n; i++) {
//                    Term vi = v.get(g.rng().nextInt(v.size()));
//                    if (g.rng().nextBooleanFast8(0.5f))
//                        vi = vi.neg();
//                    l.add(vi);
//                }
//                Term c = Op.CONJ.the((Subterms)l);
//                if (c.CONDS()) {
//                    //System.out.println(c);
//                    g.focus().link(AtomicTaskLink.link(c).priPunc(BELIEF,0.5f));
//                }
//            }
//        });

//        g.onFrame(new Runnable() {
//            @Override
//            public void run() {
//                if (g.rng().nextBoolean(0.05f)) {
//                    print("(&&,(t-->0),  (t-->(sx,0)),(t-->near))");
//                    print("(&&,(t-->0),  (t-->(sx,1)),(t-->near))");
//                    print("(&&,(t-->1),  (t-->(sx,0)),(t-->near))");
//                    print("(&&,(t-->1),  (t-->(sx,1)),(t-->near))");
//                    System.out.println();
////                    print("(((t-->1) &&   (t-->L)) ==>+50 (t-->near))");
////                    print("(((t-->1) && --(t-->L)) ==>+50 (t-->near))");
////                    print("(((t-->1) &&   (t-->R)) ==>+50 (t-->near))");
////                    print("(((t-->1) && --(t-->R)) ==>+50 (t-->near))");
//                }
//            }
//
//            private void print(String s) {
//                try {
//                    System.out.println(s + " " + g.nar.beliefTruth(s, g.time()));
//                } catch (Narsese.NarseseException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });

//        g.onFrame(new Runnable() {
//            final List<NALTask> tasks;
//
//            {
//                try {
//                    tasks = List.of(
//                            p.nar.task("((t-->0) && --(t-->L))!"),
//                            p.nar.task("((t-->2) && --(t-->R))!"),
//                            p.nar.task("(&&,(t-->0),--/\\(t-->0),(t-->R))!"),
//                            p.nar.task("(&&,(t-->2),--/\\(t-->2),(t-->L))!")
//                    );
//                } catch (Narsese.NarseseException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            @Override
//            public void run() {
//                g.focus().acceptAll(tasks);
//            }
//        });


    }

    private static final boolean targetCam = true;

    private static final boolean rewardSeparateXY = true;

    private static final boolean speedControl = false;


    /** 0 to disable */
    static int curNumerics =
        2;
        //4;
        //3;
        //0;

    static boolean targetNumerics = false;

    static float camResolution =
        //1;
        //0.5f;
        0.02f;
        //0.1f;
        //0.05f;

    static float rewardResolution =
            0.005f; //camResolution;

    private static final boolean rewardAbsolute = true;
//    private static boolean rewardDelta = false;

    final int rewardExp =
        1;
        //3;
        //2;

    private final Lst extraSensors;
    private final TrackXYModel track;
    //	static boolean debug = true;
    public static boolean ui = true;
    private final BitmapSensor<ArrayBitmap2D> cam;


    public TrackXY(int w, int h) {
        this($.atomic("t"), w, h);
    }

    public TrackXY(Term id, int W, int H) {
        super(id);
        this.track = new TrackXYModel(W, H);

        float rNumeric =
            0.01f;
            //Math.max(0.01f, 0.5f / (H > 1 ? Math.max(W, H) : W));

        if (curNumerics>0) {

            senseNumberN(
                i -> $.inh(id, $.atomic("sx" + i)),
                curNumerics,
                () -> track.cx/(W-1)).freqRes(rNumeric);
            if (H > 1)
                senseNumberN(
                    i -> $.inh(id, $.atomic("sy" + i)),
                    curNumerics,
                    () -> track.cy/(H-1)).freqRes(rNumeric);
        }


        if (targetNumerics) {
            senseNumberBi($.inh(id, $.p("tx", $.varDep(1))),
                () -> track.tx/(W-1)).freqRes(rNumeric);
            if (H > 1)
                senseNumberBi($.inh(id, $.p("ty", $.varDep(1))),
                    () -> track.ty/(H-1)).freqRes(rNumeric);
        }

        extraSensors = new Lst(sensors.sensors);

        BitmapSensor<ArrayBitmap2D> c = new BitmapSensor<>(track.grid, id);
        this.cam = c;
        c.freqRes(camResolution);

        if (targetCam)
            sensors.addSensor(c);

        if (speedControl)
            actionSpeedControl();

        if (controlPosOrVel)
            actionPushButtonMutex_Pos();
        else
		    actionPushButtonMutex_Vel();


        //actionUnipolar();
        //actionSwitch();
        //actionTriState();

        //actionAccelerate();

//        {
//            curiosity.enable.set(false);
//            GraphEdit w = new GraphEdit(RectFloat.X0Y0WH(0, 0, 256, 256));
//
//            w.addAt(new KeyboardChip.ArrowKeysChip() {
//                @Override
//                protected void keyLeft() {
//                    long now = nar.time();
//                    nar().want(0.1f, $.the("left"), now, now+dur, 1f, 0.02f);
//                }
//                @Override
//                protected void keyRight() {
//                    long now = nar.time();
//                    nar().want(0.1f, $.the("right"), now, now+dur, 1f, 0.02f);
//                }
//            }).pos(0, 0, 256, 256);
//
//            window(w, 256, 256);
//        }

//        if (alwaysTrain) {
//            curiosity.enable.set(false);
//            onFrame(x -> {
//                track.act();
//            });
//        } else {

        if (track.H > 1 && rewardSeparateXY) {
            rewardNear($.p("near","x"), () -> 1 - (track.distX() / (track.W - 1)));
            rewardNear($.p("near","y"), () -> 1 - (track.distY() / (track.H - 1)));
        } else {

            FloatSupplier near = () -> 1f - (track.dist() / track.distMax());

            rewardNear($.atomic("near"), near);

//            rewardNear($.the("nearer"),
//                    new FloatDifference(near, this::time) {
//
//                        final FloatNormalizer nn = new FloatNormalizer(
//                                4, 200
//                        ).polar();
//
//                        @Override
//                        public float asFloat() {
//                            var y = super.asFloat();
//                            if (y!=y) return Float.NaN;
//                            return nn.valueOf(y);
//                        }
//                    }
//            );

//            rewardNear($.the("nearer"),
//                new FloatDifference(near, this::time) {
//
//                    final FloatNormalizer nn = new FloatNormalizer(
//                        4, 200
//                    );
//
//                    @Override
//                    public float asFloat() {
//                        var y = super.asFloat();
//                        if (y!=y) return Float.NaN;
//                        return nn.valueOf(Util.min(y, 0));
//                    }
//                }
//            );

//			nar.runLater(()-> {//HACK
//				r.addGuard(true, false);
//			});
        }


//		nar.runLater(()->{//HACK
//			r.concept.resolution(0.1f);
//		});


//        FloatSupplier notLeft  = () -> ( 1f - Util.max(0,track.tx - track.cx) / track.W );
//        FloatSupplier notRight = () -> ( 1f - Util.max(0,track.cx - track.tx) / track.W );
//        reward("notLeft", notLeft).resolution().set(0.1f);
//        reward("notRight", notRight).resolution().set(0.1f);
//
//        if (track.H > 1) {
//            FloatSupplier notBelow  = () -> ( 1f - Util.max(0,track.ty - track.cy) / track.H );
//            FloatSupplier notAbove = () -> ( 1f - Util.max(0,track.cy - track.ty) / track.H );
//            reward("notBelow", notBelow).resolution().set(0.1f);
//            reward("notAbove", notAbove).resolution().set(0.1f);
//        }


        //rewardNormalized($.the("better"), -0.1f,  +0.1f, new FloatFirstOrderDifference(nar::time, nearness) );

//        }

        track.randomize();

        beforeFrame(track::act);

//		if (debug)
//			debugger();
    }

    @Override
    protected void init() {
        super.init();
        if (ui) {
            nar.runLater(() -> {
                Splitting<?, ?> vv = col(Containers.row(new VectorSensorChart(cam, TrackXY.this) {
                    @Override
                    protected void paint(GL2 gl, ReSurface reSurface) {
                        super.paint(gl, reSurface);
                        gl.glColor4f(1, 0, 0, 0.9f);
                        dot(gl, track.cx, track.cy);
                        gl.glColor4f(0, 1, 0, 0.9f);
                        dot(gl, track.tx, track.ty);
                    }

                    private void dot(GL2 gl, float x, float y) {
                        Draw.rect(cellRect(x, y, 0.5f, 0.5f).move(left(), bottom()), gl);
                    }
                }.withControls(), 0.9f, new Gridding(extraSensors.stream().map(x -> new VectorSensorChart((VectorSensor) x, nar)))), 0.1f, new ObjectSurface(track));
                SpaceGraph.window(vv, 640, 480);
            });
        }


    }

    private void rewardNear(Term x, FloatSupplier near) {
        if (rewardAbsolute)
            reward($.inh(this.id, x), near.pow(rewardExp)).resolution(rewardResolution);
//        if (rewardDelta)
//            reward($.inh(this.id, $.p(x, "nearer")), near.diff(nar::time)
//                    .times(100/*dur?*/)
//                    .times(1 / track.controlSpeed.asFloat())
//                    .clamp(-1, +1)
//                    .unpolarize()
//            ).resolution(rewardResolution).strength(0.5f);

//			nar.runLater(()-> {
//				NAL.DEBUG = true;
//				a.addGuard(true, false);
//				b.addGuard(true, false);
//			});

    }

//	private void debugger() {
////		Term L, R, U, D;
////		L = actions.get(0).term();
////		R = actions.get(1).term();
////		if (actions.size()==4) {
////			U = actions.get(2).term();
////			D = actions.get(3).term();
////		} else {
////			U = D = null;
////		}
//
//		NAL.DEBUG = true;
//		NAL.causeCapacity.set(Math.max(NAL.causeCapacity.intValue(), 8));
//
////		float gridThresh = 1f;
//		AbstractAction L = actions.actions.get(0);
//		AbstractAction R = actions.actions.get(1);
//
////		L.cause = (p)->{
////			float needsDirX = track.tx - track.cx;
////			if (L.actionTruthOrganic.is() && L.actionTruthOrganic.POSITIVE()!=needsDirX<0) {
////				if (Math.abs(needsDirX) >= gridThresh / 2f) {
////					if (needsDirX > 0) {
////						//blame wantsLeft
////						System.err.println("bad left");
////						print(p, nar);
////					}
//////						if (xErr) {
//////							Truth lt = L.actionTruthOrganic.ifIs();
//////							Truth rt = R.actionTruthOrganic.ifIs();
//////							if ((lt != null && lt.POSITIVE()) || (rt != null && rt.POSITIVE())) {
//////								System.err.println(L.term + "\t" + lt != null ? lt.detailString() : "null");
//////								System.err.println(R.term + "\t" + rt != null ? rt.detailString() : "null");
//////								wantsLeft.forEach(t -> nar.proofPrint(t));
//////								wantsRight.forEach(t -> nar.proofPrint(t));
//////								System.err.println();
//////							}
//////						}
////				}
////			}
////		};
////		R.cause = (p)-> {
////			float needsDirX = track.tx - track.cx;
////			if (R.actionTruthOrganic.is() && R.actionTruthOrganic.POSITIVE()!=needsDirX>0) {
////				if (Math.abs(needsDirX) >= gridThresh / 2f) {
////					if (needsDirX < 0) {
////						//blame wantsRight
////						System.err.println("bad right");
////						print(p, nar);
////					}
////				}
////			}
////		};
//
//
////				//TODO
////				synchronized (TrackXY_NAR.this) {
////					long s = nowLoop.s, e = nowLoop.e;
////
//////					Set<NALTask> wantsLeft = new UnifiedSet(), wantsRight = new UnifiedSet();
//////					TrackXY_NAR.this.collectActions(L.goals().taskStream(), s, e, wantsLeft, wantsRight, true);
//////					TrackXY_NAR.this.collectActions(R.goals().taskStream(), s, e, wantsLeft, wantsRight, false);
////
////					if (track.H > 1) {
////						float needsDirY = track.ty - track.cy;
////						if (Math.abs(needsDirY) >= gridThresh) {
////						}
////					}
////				}
////			}
////		});
//
//	}

    private static void print(TruthProjection p, NAR nar) {
        for (NALTask x : p)
            nar.proofPrint(x);
        System.out.println();
    }

    private void collectActions(Stream<? extends NALTask> tasks, long s, long e, Set<NALTask> wantsLeft, Set<NALTask> wantsRight, boolean leftOrRight) {
        tasks
                .filter(t -> !(t instanceof SerialTask) && !t.ETERNAL())
//			.filter(t -> t.freq() >= freqThresh)
                .filter(t -> t.intersects(s, e))
                .forEach(t -> {
                    boolean yes = t.POSITIVE();
                    Set<NALTask> target = leftOrRight ? (yes ? wantsLeft : wantsRight) : (yes ? wantsRight : wantsLeft);
                    target.add(t);
                });
    }

    private void actionUnipolar() {

        int W = track.grid.width();
        action($.inh("lr", id), b -> {
            track.cx = Util.clamp(b * (W - 1), 0, W - 1);
            //return track.cx / (track.W-1);
            return b;
        });
        int H = track.grid.height();
        if (H > 1) {
            action($.inh("ud", id), (b) -> {
                track.cy = Util.clamp(b * (H - 1), 0, H - 1);
                //return track.cy / (track.H-1);
                return b;
            });
        }

    }

    private void actionSpeedControl() {
        float p = 1;
        action($.inh(id, "speed"),
                (float z) -> track.controlSpeed.setLerp((float) Math.pow(z, p)));//.resolution(0.2f);
    }

    public void actionBipolarAnalog() {
        throw new TODO();
    }

    public void actionPushButtonMutex_Pos() {

        FloatRange s = track.controlSpeed;
        float thresh = 0.01f;
        int W = track.grid.width();
        actionToggle($.inh(id, "L"), $.inh(id, "R"), () -> {
            float pcx = track.cx;
            track.cx = Util.clamp(pcx - s.floatValue(), 0, W - 1);
            return feedback(track.cx, pcx, thresh);
        }, () -> {
            float pcx = track.cx;
            track.cx = Util.clamp(pcx + s.floatValue(), 0, W - 1);
            return feedback(track.cx, pcx, thresh);
        });

        int H = track.grid.height();
        if (H > 1) {
            actionToggle($.inh(id, "U"), $.inh(id, "D"), () -> {
                float pcy = track.cy;
                track.cy = Util.clamp(pcy + s.floatValue(), 0, H - 1);
                return feedback(track.cy, pcy, thresh);
            }, () -> {
                float pcy = track.cy;
                track.cy = Util.clamp(pcy - s.floatValue(), 0, H - 1);
                return feedback(track.cy, pcy, thresh);
            });
        }


    }

    private boolean feedback(float track, float pcx, float thresh) {
        return !FEEDBACK_QUELL || !Util.equals(track, pcx, thresh);
    }

    public void actionPushButtonMutex_Vel() {
        float[] vx = {0}, vy = {0};
        int W = track.grid.width(), H = track.grid.height();
        action($.inh(id, "vx"), (v) -> {
            vx[0] = Fuzzy.polarize(v);
        });
        if (H > 1) {
            action($.inh(id, "vy"), (v) -> {
                vy[0] = Fuzzy.polarize(v);
            });
        }
        afterFrame(() -> {
            float s = track.controlSpeed.floatValue();
            track.cx = Util.clamp(track.cx + vx[0] * s, 0, W - 1);
            track.cy = Util.clamp(track.cy + vy[0] * s, 0, H - 1);
        });
    }


}
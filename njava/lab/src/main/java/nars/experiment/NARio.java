package nars.experiment;

import jcog.Is;
import jcog.data.list.Lst;
import jcog.math.FloatSupplier;
import jcog.signal.IntRange;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.NAR;
import nars.Player;
import nars.Term;
import nars.experiment.mario.LevelScene;
import nars.experiment.mario.MarioComponent;
import nars.experiment.mario.Scene;
import nars.experiment.mario.level.Level;
import nars.experiment.mario.sprites.Mario;
import nars.game.Game;
import nars.game.action.BiPolarAction;
import nars.game.reward.Reward;
import nars.game.sensor.AbstractSensor;
import nars.game.sensor.SelectorSensor;
import nars.gui.GameUI;
import nars.sensor.PixelBag;
import nars.video.AutoClassifiedBitmap;
import spacegraph.space2d.container.grid.Gridding;

import javax.swing.*;
import java.util.List;

import static nars.$.*;
import static nars.experiment.mario.level.Level.*;
import static nars.experiment.mario.sprites.Mario.KEY_JUMP;
import static nars.game.GameTime.fps;
import static spacegraph.SpaceGraph.window;

public class NARio extends Game {

	@Is({"Afterlife", "Near-death_experience", "Bardo", "Hell", "Womb"})
	public final IntRange afterLifeTime = new IntRange(100, 1, 2000);

	private static final float gameFPS =
		35;
		//25; //normal
	    //30;
	    //20;
		//38;
		//50;  //2x
		//12.5; //1/2x

	private final boolean senseCamera = true;
	private final boolean cameraZoom = false;

	private final boolean senseMotion = true;
	private final boolean senseTiles = true;

	private final MarioComponent game = new MarioComponent(640, 480);
	int lastCoins;
	float lastX;
	private Mario theMario;

	public NARio() {
		this($$("nario"));
	}

	public NARio(Term id) {
		super(id, fps(gameFPS));
	}

	public static void main(String[] args) {

		NARio g = new NARio();
		var p = new Player(g).fps(gameFPS);
		p.ready(n-> {
			window(new Gridding(
				g.s.stream().map(z -> GameUI.col(z, g)
					//new VectorSensorChart((VectorSensor)z, nar).withControls())
				)), 200, 200);
		});
		//p.gameReflex = true;
		//p.subMetaReflex = true;
		p.start();
	}


	private final Lst<AbstractSensor> s = new Lst();
	PixelBag cc;

	@Override
	protected void init() {
		game.fps.set(gameFPS);

		SwingUtilities.invokeLater(()->{
			JFrame f = new JFrame("Infinite NARio");
			f.setIgnoreRepaint(true);
			f.setResizable(false);
			f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			f.setContentPane(game);
			f.pack();
			f.setVisible(true);
		});

		initButton();
		//initBipolar();

		var speed = actionPushButton(inh(id, "speed"), n -> {
			if (game.scene != null) {
				Scene.key(Mario.KEY_SPEED, n);
				return n;
			}
			return false;
		});

		if (senseCamera) {
			cc = new PixelBag(new MonoBufImgBitmap2D(() -> game.image)
				/*.mode(ColorMode.Hue)*/
				,
					//128, 64 //HI RES
					//96,96
					64, 64
					//64, 32
					//32, 16  //LO RES
			);

			if (cameraZoom)
				cc.actions(id, false, false, true, 0.5f);

			int aeStates =
				//8;
				//4;
				6;
				//12;
				//16;
				//7;
				//5;
				//10;
				//9;
				//12;
				//7;
				//12;
				//16;
				//20;

			var superPixelsXY =
				//4;
				8;

			AutoClassifiedBitmap camAE = new AutoClassifiedBitmap(
					inh(id, p(p(varDep(1), varDep(2)), varDep(3))),
					//$.inh(p(varDep(3), id), p(varDep(1), varDep(2))),
					//inh(p(varDep(3), id), p(varDep(1), varDep(2))),
					cc,
					superPixelsXY,superPixelsXY,
					//4,4,
					//(subX, subY) -> new float[]{/*cc.X, cc.Y, *//*cc.Z*/},
					aeStates, this);
			cc.setMinZoom(0.8f);
			if (cameraZoom)
				cc.setMaxZoom(0.3f);
			else
				cc.setZoom(0.5f);

			//camAE.learnRandom = camAE.tileCount();

			camAE.freqRes(
				//0.1f
				//0.01f
				0.04f
				//0.5f
				//0.2f
			);

			nar.runLater(()-> window(GameUI.aeUI(camAE), 500, 500));

		} else {
			cc = null;
		}

		if (senseTiles) {
			Term[] types = {
				$$("bumpable"), $$("block")
			};
			Term[] dir = {
				p(-1, 0), p(+1, 0), p(0, -1), p(0, +1),
				p(-1, -1), p(+1, -1), p(-1, +1), p(+1, +1)
			};
			s.add(
				addSensor(new SelectorSensor(
					inh(id, "tile"), 2, 8, a -> {
						if (!alive()) return -1;
                    return switch (a) {
                        case 0 -> tile(-1, 0);
                        case 1 -> tile(+1, 0);
                        case 2 -> tile(0, -1);
                        case 3 -> tile(0, +1);
                        case 4 -> tile(-1, -1);
                        case 5 -> tile(+1, -1);
                        case 6 -> tile(-1, +1);
                        case 7 -> tile(+1, +1);
                        default -> throw new UnsupportedOperationException();
                    };
                },
					(a, v) ->
						inh(id, p(dir[a], types[v]))
						//$.inh($.p(id, dir[a]), types[v])
					,nar
				))
			);
		}
		if (senseMotion) {

//			Term MOTION = p(the("motion"), id);

			float res = 0.1f;

			final FloatSupplier mx = () -> alive() ? theMario.x : 0;
			final FloatSupplier my = () -> alive() ? theMario.y : 0;

			Term VX = inh(id, p(atomic("move"), atomic("x"), varDep(1)));
			Term VY = inh(id, p(atomic("move"), atomic("y"), varDep(1)));
			var vx =
					senseDiffTri(VX, 8, mx);
//					senseDiffBi(dx,
		//				inh(MOTION, p(the(-1), the(0))),
//						inh(MOTION, p(the(+1), the(0)))
//					);

			var vy =
					senseDiffTri(VY, 8, my);
					//senseDiff(inh(id, $.p("move", "y")), 8, dy);
//				senseDiffBi(8, dy,
//					inh(MOTION, p(the(0), the(-1))),
//					inh(MOTION, p(the(0), the(+1)))
//			);

			List.of(vx, vy).forEach(v -> v.freqRes(res));
			s.addAll(vx, vy);

		}




//        window(NARui.beliefCharts(this.nar, Stream.of(vx, vy).flatMap(x->x.sensors.stream()).collect(toList())), 400, 300);


		//unitize(Math.max(0, (curX - lastX)) / 16f * MoveRight.floatValue());
		final FloatSupplier rightward = () -> {
			float curX = alive() ? theMario.x : Float.NaN;
			int thresh = 2;
			var y = (curX == curX && lastX == lastX) && curX - lastX > thresh ? 1 : 0;
			lastX = curX;
			return y;
		};

		reward("right",
			rightward
			//Reward.attack(rightward, 16)
		).resolution(0.1f);//.usually(0); //.strength(0.1f);


		reward("money", Reward.release(() -> {
			int coins = Mario.coins;
			int deltaCoin = coins - lastCoins;
			if (deltaCoin <= 0)
				return 0;

			float reward = Math.min(1, deltaCoin);// * EarnCoin.floatValue();
			lastCoins = coins;
			return reward;
		}, 1));//.usually(0);
		//getCoins.setDefault($.t(0, 0.75f));

		reward("alive", () -> {
//            if (dead)
//                return -1;
//
			if (theMario == null)
				return Float.NaN;

			return theMario.deathTime > 0 ? 0 : /*Float.NaN*/ +1;
		}).conf(0.5f);//.usually(1);

		beforeFrame(() -> {

			Scene ss = game.scene;

			if (ss instanceof LevelScene level) {
                Mario M = theMario = level.mario;
				if (senseCamera) {
					cc.setXRelative((M.x - level.xCam) / 320);
					cc.setYRelative((M.y - level.yCam) / 240);
				}

				theMario.afterlifeDuration = afterLifeTime.intValue();

			} else {
				theMario = null;
			}

		});

//		nar.add(new BeliefPredict(
//			actions,
//			actions,
//			8,
//			Math.round(2 * nar.dur()),
//			3,
//			new LivePredictor.LSTMPredictor(0.1f, 1),
//			nar,
//			false));



		game.paused = false;
		game.thread.start();
	}

	private boolean alive() {
		return theMario != null && theMario.deathTime <= 0;
	}

	@Override
	protected void stopping(NAR nar) {
		game.paused = true;
		super.stopping(nar);
	}

//	private SelectorSensor tileSwitch(int dx, int dy) {
////		senseSwitch(new Term[] { $$("bumpable"), $$("block") },
////			() -> tile(dx, dy),
////			inh(p(varDep(1),id), p(dx, dy))
////		);
//	}

	int tile(int dx, int dy) {
		if (!(game.scene instanceof LevelScene s)) return -1;

		Level ll = s.level; if (ll == null) return -1;

		Mario mm = s.mario;
		byte block = ll.getBlock(Math.round((mm.x - 8) / 16f) + dx, Math.round((mm.y - 8) / 16f) + dy);
		byte t = TILE_BEHAVIORS[block & 0xff];
		if ((t&BIT_BREAKABLE)!=0 || (t&BIT_BUMPABLE)!=0 || (t&BIT_PICKUPABLE)!=0)
			return 0;
		else if ((t&BIT_BLOCK_ALL)!=0)
			return +1;
		else
			return -1;
	}

	private void initButton() {
//		Term MOVE = the("move");
//		Term left = inh(id, p(MOVE, p(-1, 0)));
//		Term right = inh(id, p(MOVE, p(+1, 0)));
//		Term jump = inh(id, p(MOVE, p(0, +1)));
		Term left = inh(id, "L") /*inh(p(id, MOVE), p(-1, 0))*/;
		Term right = inh(id, "R") /*inh(p(id, MOVE), p(+1, 0))*/;
		Term jump = inh(id, "J")/*inh(p(id, MOVE), p(0, +1))*/;

		actionToggle(left, right,
			n -> {
				var s = game.scene; if (s != null) Scene.key(Mario.KEY_LEFT, n);
			},
			n -> {
				var s = game.scene; if (s != null) Scene.key(Mario.KEY_RIGHT, n);
			}
		);

		var j = actionPushButton(jump, n -> {
				Scene s = game.scene;
				if (s != null) {
					Mario m = this.theMario;
					if (m != null) {
						Scene.key(KEY_JUMP, n);
						return n;
					}
				}
				return false;
			});

		//j.actionDur(1);


//        actionPushButton($$("down"),
//                n -> { game.scene.key(Mario.KEY_DOWN, n); return n; } );


		//s.actionDur(1);

		//bias
//        j.goalDefault($.t(0, 0.01f), nar);
//        ss.goalDefault($.t(0, 0.01f), nar);
	}

	void initTriState() {
		actionTriState(inh("x", id), i -> {
			boolean n, p;
			switch (i) {
				case -1 -> {
					p = false;
					n = true;
				}
				case +1 -> {
					p = true;
					n = false;
				}
				case 0 -> {
					p = false;
					n = false;
				}
				default -> throw new RuntimeException();
			}
			Scene.key(Mario.KEY_LEFT, n);
			Scene.key(Mario.KEY_RIGHT, p);
			return true;
		});
		actionTriState(inh("y", id), i -> {
			boolean n, p;
			switch (i) {
				case -1 -> {
					p = false;
					n = true;
				}
				case +1 -> {
					p = true;
					n = false;
				}
				case 0 -> {
					p = false;
					n = false;
				}
				default -> throw new RuntimeException();
			}
			Scene.key(Mario.KEY_DOWN, n);
			Scene.key(KEY_JUMP, p);
			return true;
		});


	}

	public void initBipolar() {
		float thresh = 0.25f;


		BiPolarAction X = this.actionBipolar(p(id, "x"), (x) -> {
			if (game == null || game.scene == null) return Float.NaN; //HACK

			float boostThresh = 0.75f;
			if (x <= -thresh) {
				Scene.key(Mario.KEY_LEFT, true);
				Scene.key(Mario.KEY_RIGHT, false);
				Scene.key(Mario.KEY_SPEED, x <= -boostThresh);

				return x <= -boostThresh ? -1 : -boostThresh;
			} else if (x >= +thresh) {
				Scene.key(Mario.KEY_RIGHT, true);
				Scene.key(Mario.KEY_LEFT, false);
				Scene.key(Mario.KEY_SPEED, x >= +boostThresh);

				return x >= +boostThresh ? +1 : +boostThresh;
			} else {
				Scene.key(Mario.KEY_LEFT, false);
				Scene.key(Mario.KEY_RIGHT, false);
				Scene.key(Mario.KEY_SPEED, false);


				return 0f;

			}
		});
		BiPolarAction Y = this.actionBipolar(p(id, "y"), (y) -> {
			if (game == null || game.scene == null) return Float.NaN; //HACK

			if (y <= -thresh) {
				Scene.key(Mario.KEY_DOWN, true);
				Scene.key(KEY_JUMP, false);
				return -1f;

			} else if (y >= +thresh) {
				Scene.key(KEY_JUMP, true);
				Scene.key(Mario.KEY_DOWN, false);
				return +1f;

			} else {
				Scene.key(KEY_JUMP, false);
				Scene.key(Mario.KEY_DOWN, false);

				return 0f;

			}
		});/*.forEach(g -> {
            g.resolution(0.1f);
        });*/

		//window(NARui.beliefCharts(nar, List.of(X.pos, X.neg, Y.pos, Y.neg)), 700, 700);
	}

}
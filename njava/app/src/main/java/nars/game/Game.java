package nars.game;

import jcog.Is;
import jcog.Research;
import jcog.TODO;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.thing.Part;
import nars.*;
import nars.control.Pausing;
import nars.focus.PriAmp;
import nars.focus.PriNode;
import nars.game.action.CompoundAction;
import nars.game.action.util.Curiosity;
import nars.game.reward.MultiReward;
import nars.game.reward.Reward;
import nars.game.reward.ScalarReward;
import nars.game.sensor.ScalarSensor;
import nars.game.sensor.Sensor;
import nars.game.sensor.VectorSensor;
import nars.game.util.Perception;
import nars.term.Compound;
import nars.term.Termlike;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.truth.Truther;
import nars.truth.evi.EviInterval;
import nars.util.NARPart;
import nars.util.Timed;

import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static nars.NAL.DEFAULT_GAME_CAPACITY;
import static nars.Op.BELIEF;


/**
 * an integration of sensor concepts and motor functions
 * interfacing with an environment forming a sensori-motor loop.
 * <p>
 * these include all forms of problems including
 * optimization
 * reinforcement learning
 * etc
 * <p>
 * the name 'Game' is used, in the most general sense of the
 * word 'game'.
 *
 * TODO min required vocabulary volume test
 */
@Research
@Is({"General_game_playing", "Game_studies", "Game_theory"})
public class Game extends NARPart /* TODO extends ProxyWhat -> .. and commit when it updates */ implements NSense, NAct, NReward, Timed, Pausing {

	/**
	 * context for processing the game experience
	 */
	private final Focus focus;

	public final Curiosity curiosity = new Curiosity();

	public Perception perception;

	public final Sensors sensors = new Sensors();
	public final Actions actions = new Actions();
	public final Rewards rewards = new Rewards();


	public final AtomicBoolean enable = new AtomicBoolean(true);

	/** frame timing */
	final GameTime clock;

	public final EviInterval time = new EviInterval(Long.MIN_VALUE, Long.MIN_VALUE);

	/** pre-frame */
	private final Topic<Game> beforeFrame = new ListTopic<>();

	/** inner-frame event: called after sensors & reward, before actions */
	private final Topic<Game> inFrame = new ListTopic<>();

	/** after-frame event: called after actions */
	private final Topic<Game> afterFrame = new ListTopic<>();

	/** populated after initialization */
	public final Set<Term> vocabulary = new TreeSet<>();
	private final Set<Term> vocabularyRecursive = new HashSet<>();

	private final Truther truther = new Truther.FreqTruther();

	public Game(String id) {
		this($.$$(id));
	}

	public Game(Term id) {
		this(id, GameTime.durs(
			1
			//2 /* nyquist */
		));
	}

	public Game(String id, GameTime clock) {
		this($.$$(id), clock);
	}

	public Game(Term id, GameTime clock) {
		this(new Focus($.inh(id,GAME), DEFAULT_GAME_CAPACITY/2), clock);
	}

	public Game(Focus focus, GameTime clock) {
		super(focus.id.sub(0));
		this.clock = clock;
		this.focus = focus;
    }

	@Override
	public final Focus focus() {
		return focus;
	}

	/**
	 * dexterity = mean(conf(action))
	 * evidence/confidence in action decisions, current measurement
	 */
	public final double dexterity() {
		return enabled() ? actions.dexterity() : Double.NaN;
	}

	public final double coherency() {
		return enabled() ? actions.coherency(time.s, time.e, time.dur, nar) : Double.NaN;
	}


	/**
	 * avg reward satisfaction, current measurement
	 * happiness metric applied to all reward concepts
	 */
	public final double happiness(long start, long end, float dur) {
		return enabled() ? rewards.happiness(start, end, dur) : Double.NaN;
	}

	public final double happiness() {
		var dur = dur();
		long start = Math.round(time.s + dur * NAL.temporal.GAME_REWARD_SHIFT_DURS);
		long end = Math.round(start + dur);
		return happiness(start, end, nars.NAL.signal.HAPPINESS_DUR_ZERO ? 0 : time.dur);
	}

//	public final double frustration() {
//		return enabled() ? actions.frustration(nowLoop.s, nowLoop.e, nowLoop.dur, nar) : Double.NaN;
//	}

	@Override
	public final <A extends FocusLoop<Game>> A addAction(A c) {
		return actions.addAction(c);
	}

	@Override
	public final <S extends Sensor> S addSensor(S s) {
		return sensors.addSensor(s);
	}

	private void init(Object x) {

		if (x instanceof Controlled c)
			c.controlStart(this);

		if (x instanceof FocusLoop f)
			f.start(this);

		if (x instanceof FocusLoop ss)
			vocabularyAdd(ss);

		if (x instanceof CompoundAction X) {
			for (var xx : X.concepts) {
				init(xx);
				actions.addAction(xx);
			}
		}
	}

	private void priLink(PriNode src, Object x) {
		init(x);
        var p = nar.pri;
		//the order in this switch matters:
        switch (x) {
			case CompoundAction C -> { p.link(C.pri, src); for (var ca : C.concepts) p.link(ca.pri, C.pri); }
			case MultiReward M 	  -> { for (var xr : M.r) p.link(xr.sensor.pri, src); }
            case VectorSensor X   ->   p.link(X.pri, src);
			case ScalarReward R   ->   p.link(R.sensor.pri, src);
            case ScalarSensor S   ->   p.link(S.pri, src);
			case PriNode P 		  ->   p.link(P, src);
            case null, default -> throw new TODO();
        }
	}

	public final IntSummaryStatistics vocabularyComplexityStatistics() {
		return vocabulary.stream().mapToInt(Termlike::complexity).summaryStatistics();
	}

	@Deprecated public RandomGenerator random() {
        var w = this.focus;
		return w != null ? w.random() : ThreadLocalRandom.current();//HACK
	}

	/** "left aligned" */
	@Override public long time() {
		return time.s;
	}

	public String summary() {
		return id +
			" dex=" + /*n4*/(actions.dexterity()) +
			" hapy=" + /*n4*/(happiness()) +

			/*" var=" + n4(varPct(nar)) + */ '\t' + nar.memory.summary() + ' ' +
			nar.emotion.summary();
	}

	/**
	 * registers sensor, action, and reward concepts with the NAR
	 * TODO call this in the constructor
	 */
	protected final void starting(NAR nar) {

		super.starting(nar);

		focus.nar = nar; //HACK early
		focus.local(Game.class, z->this);
		focus.local(NARPart.class, z->this);

		/* the game's focus priority */
        var root = focus.pri;
        var focusID = focus.id;
		var P = nar.pri;
		P.link(actions.pri = new PriAmp($.p(focusID, ACTION)), root);
		P.link(sensors.pri = new PriAmp($.p(focusID, SENSOR)), root);
		P.link(rewards.pri = new PriAmp($.p(focusID, REWARD)), root);

		init();

		//by this point, all sensors, actions, and rewards must have been added.
		// TODO allow dynamic
		for (var s : sensors.sensors)
			priLink(sensors.pri, s);

		for (var r : rewards)
			priLink(rewards.pri, r);

		for (var a : actions)
			priLink(actions.pri, a);

		for (var a : actions._actions)
			priLink(actions.pri, a);

		//EXPERIMENTAL AUTOPRI
		{
			//root.pri(1/10f);

//			//
//			var a = //1 / (1 + vocabulary.size() / 3f);
//					0.25f;
//			actions.pri.amp(a);
//			rewards.pri.amp(a);
//			sensors.pri.amp(a);
		}

		perception = new Perception(this);

		perception.capacity(vocabulary.size());

		clock.start(this);

		focus.dur(clock.dur()); //default/initial dur //HACK

        P.commit(); //HACK force PriTree update

		nar.add(focus);

	}

	protected void stopping(NAR nar) {
		clock.stop();

		sensors.sensors.stream().filter(x -> x instanceof NARPart).forEach(s -> nar.remove((NARPart) s));

		nar.pri.removeAll(actions.pri, sensors.pri, rewards.pri);

		focus.local(Game.class, z->null);
		focus.local(NARPart.class, z->null);

		this.focus.close();
	}


	private void vocabularyAdd(FocusLoop<Game> s) {
		s.components().forEach(x -> {
            var xx = x.term();
            var unique = vocabulary.add(xx);
			if (!unique)
				throw new UnsupportedOperationException("duplicate vocabulary term: " + xx + " in " + s);
			vocabularyAdd(xx);
		});
	}

	private void vocabularyAdd(Term t) {
		if (t instanceof Compound) {
			for (var tt : t.subterms())
				vocabularyAdd(tt);
		} else if (t.ATOM())
			vocabularyRecursive.add(t);
	}



	/**
	 * subclasses can safely override to add sensors, actions, rewards by implementing this.  NAR and What will be initialized prior
	 */
	protected void init() {

	}

	/**
	 * default reward module
	 */
	@Override public final synchronized void reward(Reward r) {
		rewards.add(r);
	}

	@Override
	@Deprecated public final Term rewardTerm(String r) {
		return $.inh(id, $.$$(r));
	}

	/**
	 * loop duration
	 */
	public final float dur() {
		return time.dur;
	}

	public final float durFocus() {
		return focus.dur();
	}

	@Override
	public final void pause(boolean pause) {
		//clock.pause(pause);
		enable(!pause);
	}

	public final void enable(boolean e) {
		enable.setOpaque(e);
	}
	public final boolean enabled() {
		return enable.getOpaque();
	}

	/**
	 * iteration
	 */
	protected final void next() {
        if (isOn() && enabled() && ready()) {
            pre();
            post();
        }
    }

	private void post() {
		queue.removeIf(r -> { r.run(); return true; } );

		inFrame.accept(this);

		actions.act(this);

		afterFrame.accept(this);
	}

	private void pre() {
		setTruther();

		beforeFrame.accept(this);

		perception.commit();

		sense();

		reward();
	}

	private boolean ready() {
		long prev = time(),
			 now = focus.nar.time();
		if (now <= prev)
			return false; //not yet
		else {
			ready(now);
			return true;
		}
	}

	private void ready(long now) {
        var gameDur = clock.dur();
        var iDur = Math.round(gameDur);
		time.when(
			now,
			Math.max(now, now + iDur - 1)
		, nar).dur(gameDur);

		focus.commit(now); //HACK ensure focus is updated, in case running without Derivers
	}

	private void setTruther() {
		((Truther.FreqTruther)truther)
			.freqRes(nar.freqRes.floatValue())
			.conf(nar.confDefault(BELIEF));
	}

	@Override
	public final NAR nar() {
		return nar;
	}

	protected void sense() {
		sensors.sensors.forEach(this::update);
	}

	private void reward() {
		rewards.update(this);
	}

	final void update(FocusLoop s) {
		if (s instanceof Part p && !p.isOn()) return;

		s.accept(this);
	}

	/** add before-phase of the frame callback */
	public Off beforeFrame(Runnable eachFrame) {
		return beforeFrame.on(eachFrame);
	}
	public Off beforeFrame(Consumer<Game> eachFrame) {
		return beforeFrame.on(eachFrame);
	}
	/** add before-phase of the frame callback */
	public Off onFrame(Consumer<Game> eachFrame) {
		return inFrame.on(eachFrame);
	}
	public Off onFrame(Runnable eachFrame) {
		return inFrame.on(eachFrame);
	}

//	public Off onFrameWeak(Runnable eachFrame) {
//		return eventFrame.onWeak(eachFrame);
//	}

	public Off afterFrame(Runnable eachFrame) {
		return afterFrame.on(eachFrame);
	}
	public Off afterFrame(Consumer<Game> eachFrame) {
		return afterFrame.on(eachFrame);
	}

//	public Off logActions() {
//		return onFrame(new Runnable() {
//			DataTable data = null;
//			Object[] row = null;
//
//			{
//				Runtime.getRuntime().addShutdownHook(new Thread(()->{
//					System.out.println(data);
//					String file = "/tmp/" + id + ".actions.csv";
//					try {
//						data.write().csv(file);
//						System.err.println("wrote: " + file + " (" + data.rowCount() + " rows)");
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//					data.columns().forEach(c -> {
//						//StreamingStatistics s = new StreamingStatistics(true);
//						DescriptiveStatistics s = new DescriptiveStatistics(4096);
//						((DoubleColumn)c).forEach(_d -> {
//							double d = _d;
//							if (d == d)
//								s.addValue(d);
//						});
//						System.out.println(c.name() + "\t" + s.toString().replace("\n", ", "));
//
//					});
//				}));
//			}
//
//			@Override
//			public void run() {
//				int n = actions.size();
//				if (data == null || data.columnCount()!=n) {
//					Table t = Table.create(actions.stream().map(x -> DoubleColumn.create(x.term.toString())));
//					row = new Object[n];
//					data = new DataTable(t);
//				}
//				int i = 0;
//				for (AbstractAction a : actions) {
//					row[i++] = a.freq();
//				}
//				data.add(row);
//
//			}
//		});
//	}

	/** sets Game's Focus's capacity */
	public Game capacity(int cap) {
		focus.attn.capacity(cap);
		return this;
	}

	/** streams the NAR's relevant beliefs for this Game */
	public Stream<NALTask> knowledge() {
		return nar().concepts().filter(x -> relevant(x.term())).flatMap(Concept::tasks);
	}

	public boolean relevant(Term x/* TODO , boolean allowOutsiders */) {
		if (x instanceof Compound c) {
			return c.AND(this::relevant);
		} else if (x.ATOM()) {
			return vocabulary.contains(x);
		} else
			return true; //variable or special
	}

	/** default truther used by signals */
	public Truther truther() {
		return truther;
	}

	/** mean reward over entire existence (or until statistics are reset) */
	public final double rewardMean() {
		return rewards.rewardStat.getAverage();
	}

	private static String lClass(Class c) {
		return c.getSimpleName().toLowerCase();
	}

	private static final Atom GAME = Atomic.atom(lClass(Game.class));

	private static final Atom ACTION = Atomic.atom("action");
	private static final Atom SENSOR = Atomic.atom(lClass(Sensor.class));
	private static final Atom REWARD = Atomic.atom(lClass(Reward.class));


	private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

	public void runLater(Runnable r) {
		queue.add(r);
	}
}
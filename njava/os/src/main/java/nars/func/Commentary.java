package nars.func;

import jcog.Is;
import jcog.pri.PLink;
import nars.*;
import nars.func.language.NARHear;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.TermTransform;
import nars.time.part.DurLoop;
import org.jetbrains.annotations.Nullable;
import spacegraph.audio.speech.NativeSpeechDispatcher;

import static nars.Op.*;

/**
 * TODO dont just vocalize concepts but tasks, so that beliefs/goals can be prefixed with NOT if negated.
 * TODO generic async endpoint (ex: for stream of tasks, from task bag etc)
 * TODO abstract to more general commentary triggered by any provided event target
 */
@Is({"Ten-code","Brevity_code"}) public class Commentary extends DurLoop {

	final int volLimit = 16;
	final boolean feedback = false;
	private final NativeSpeechDispatcher out;

	public Commentary(Focus f) {
		super($.p(f.nar.self(), $.the(Commentary.class)));
        var nar = f.nar;
		durs(4);
		out = new NativeSpeechDispatcher() {
			@Override
			public void speak(Object x, float amp, float speed) {
			super.speak(x, amp, speed);
			if (feedback)
				NARHear.hear(f, x.toString(), nar.self().toString(), Math.round(nar.dur() / 2));
			}
		};
	}

	@Override
	public void accept(NAR n) {
		if (out.voicesAvailable() > 0) {
			final var random = nar.random();
			@Nullable PLink<Focus> w = nar.focus.sample(random);
			if (w == null) return;
            var f = w.id;
			var x = f.attn._bag.sample(random);
			if (x == null) return;

			//@Nullable Activate x = nar.concepts.sample(nar.random());
            var xf = x.get().term();
			if (xf.complexity() < volLimit) {
//					try {
				//nar.input("say(" + $.quote(str(x)) + ")! |");
				//v.speak(x, nar.time(), $.tt(1, 0.9f));
				//String s = str(x).trim();
				var s = Described.apply(xf).toString();
				if (!s.isEmpty())
					out.speak(s + " " + eot, x.priElseZero(), Math.min(1, s.length()/64f));
//
//					} catch (Narsese.NarseseException e) {
//						e.printStackTrace();
//					}
			}
		}

	}


//	public void init() {


//		Vocalize v = new Vocalize(nar, 1f, out::speak);

//		//NARSpeak speak = new NARSpeak(nar);
//		//speak.spoken.on(out::speak);
//		nar.setOp(Atomic.atom("say"), (t, n) -> {
//			if (t.start() >= n.time() - n.dur()) {
//				Term x = Functor.args(t.term()).sub(0);
//				if (!x instanceof Variable) {
//					v.speak(x, t.start(), t.truth());
//				}
//			}
//			return t;
//		});


//		nar.on1("str", (Term t) -> t instanceof Variable ? t : $.quote(t.toString()));

//		Term sayWhat = $$("say(?1)");
//		Term saySomething = $$("say(#1)");
//		alwaysQuestion(() -> sayWhat, false);
//		alwaysQuest(() -> sayWhat, false);

//            alwaysWant(saySomething, 1f);

//            Term sayIt = $.$$("(#x &| say(str(#x)))");
//            alwaysWant(()->sayIt, 1f);



//		try {
////                nar.want($.$("say(ready)"), Tense.Present, 1f, 0.9f);
////                nar.believe($.$("(" + happy + " =|> say(happy))"));
////                nar.want($.$("(" + happy + " &| say(happy))"));
////                nar.believe($.$("(" + happy.neg() + " =|> say(sad))"));
////                nar.want($.$("(" + happy.neg() + " &| say(sad))"));
//			//nar.want($.$("(#x &| say(str(#x)))"));
//			nar.believe($.$("($x =|> say(str($x)))"));
//			//nar.want($.$("say(#1)"));
//		} catch (Narsese.NarseseException e) {
//			e.printStackTrace();
//		}

//	}

	private String str(Premise x) {
		if (x instanceof Premise) {
			//Task y = new TaskResolve(new NonEternalTaskOccurenceOrPresentDeriverTiming(), false).get((TaskLink)x, new When<?>(nar.time(), nar.dur(), nar), null);
			return /*"about " +*/ Described.apply(x.from()).toString();
		} else {
            var t = x.task();
			return (punc(t) + " ") +
				Described.apply(t.term()).negIf(t.BELIEF_OR_GOAL() && t.NEGATIVE()).toString() + (char) (t.punc());
		}
	}

	private static String punc(Task t) {
        return switch (t.punc()) {
            case BELIEF -> "";
            case GOAL -> "want";
            case QUESTION -> "question";
            case QUEST -> "quest";
            default -> throw new UnsupportedOperationException();
        };
	}

	/** end of transmission noise */
	static final String eot = "";


	//    /** semanticize, as much as possible, a target so it can enter higher order
//     * TODO merge with NLPGen stuff
//     * */
	@Deprecated
	static final TermTransform Described = new RecursiveTermTransform() {

        private final Atomic and = Atomic.atomic("and");
        //private final Atomic so = Atomic.the("so"); //so, then, thus
        private final Atomic seq = Atomic.atomic("seq"); //so, then, thus
        private final Atomic dt = Atomic.atomic("dt");
        private final Atomic If = Atomic.atomic("if");
		private final Atomic then = Atomic.atomic("then");
        private final Atomic is = Atomic.atomic("is");
        private final Atomic similar = Atomic.atomic("similar"); //similarity
		private final Atomic same = Atomic.atomic("same"); //similarity

        @Override
        public Term applyCompound(Compound x) {
            var dt = x.dt();
            switch (x.op()) {
                case NEG:
                    return $.p("not", apply(x.unneg()));

                case INH: {
                    if (Functor.isFunc(x)) {
                        //preserve functor form
                        return INH.the(PROD.the(transformSubs(x.sub(0).subterms(), this)), x.sub(1));
                    } else {
                        return $.p(apply(x.sub(0)), is, apply(apply(x.sub(1))));
                    }
                }

                case SIM:
					return $.p(x.sub(0), similar, apply(x.sub(1)));
				case DIFF:
					return $.p(x.sub(0), same, apply(x.sub(1)));

                case IMPL:
                    //TODO insert 'dur' for dt()'s
                    if (dt == DTERNAL || dt == XTERNAL) {
                        return $.p(If, apply(x.sub(0)), then, apply(x.sub(1)));
                    } else {
                        return $.p(If, apply(x.sub(0)), then, apply(x.sub(1)),
                                $.func(this.dt, /*Tense.dither(*/Int.i(dt)));
                    }

//                case CONJ:
//
//                    if (Conj.concurrent(dt)) {
//                        return $.func(and, SETe.the(term.subterms().transformSubs(this, SETe)));
//                    } else {
//                        List<Term> ss = new FasterList(3);
//                        final long[] last = {0};
//                        term.eventsWhile((when,what)->{
//                            if (!ss.isEmpty()) {
//                                long interval = when - last[0];
//                                if (interval > 0)
//                                    ss.add($.func(this.dt, /*Tense.dither(*/$.the(interval)));
//                            }
//                            ss.add(apply(what));
//                            last[0] = when;
//                            return true;
//                        },0, false, false, false);
//
//                        return $.func(seq, ss);
//
//                    }
            }
            return x;
        }
    };


}
//
//    public static void main(String[] args) throws Narsese.NarseseException {
//        NAR n = NARS.realtime(10f).get();
//
//        new Deriver(Derivers.nal(n, 1, 8
//                //"curiosity.nal"
//                , "motivation.nal"));
//
////        NARSpeak speak = new NARSpeak(n);
////        speak.spoken.on(new NativeSpeechDispatcher()::speak);
//
//
//        n.startFPS(10f);
//
//        //n.log();
//
//        n.onTask(x -> {
//           if (x.isGoal() && !x.isInput())
//               System.out.println(x.proof());
//        });
//
//        n.input("say(abc)! :|:");
//        while (true) {
//            Util.sleepMS(2500);
//            String word;
//            switch (n.random().nextInt(3)) {
//                default:
//                case 0:
//                    word = "x";
//                    break;
//                case 1:
//                    word = "y";
//                    break;
//                case 2:
//                    word = "z";
//                    break;
//            }
//            n.input("say(" + word + ")! :|:");
//        }
//
//
//    }
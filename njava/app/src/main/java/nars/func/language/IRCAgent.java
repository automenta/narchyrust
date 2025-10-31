//package nars.func.language;
//
//import jcog.data.list.Lst;
//import jcog.exe.Loop;
//import jcog.pri.PriReference;
//import jcog.pri.bag.util.Bagregate;
//import jcog.pri.op.PriMerge;
//import nars.*;
//import nars.action.transform.ExplodeAction;
//import nars.exe.impl.WorkerExec;
//import nars.Focus;
//import nars.focus.util.Questioning;
//import nars.func.Factorize;
//import nars.func.language.util.IRC;
//import nars.NALTask;
//import nars.time.clock.RealTime;
//import org.pircbotx.exception.IrcException;
//import org.pircbotx.hooks.events.MessageEvent;
//import org.pircbotx.hooks.events.PrivateMessageEvent;
//import org.pircbotx.hooks.types.GenericMessageEvent;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.concurrent.ThreadLocalRandom;
//import java.util.function.Consumer;
//
///**
// * $0.9;0.9;0.99$
// * <p>
// * $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
// * $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
// * $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
// * $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
// * $0.9;0.9;0.99$ hear(I, #something)!
// * hear(I,?x)?
// * <p>
// * $0.9$ (($x,"the") <-> ($x,"a")).
// * ((($x --> (/,hear,#c,_)) &&+1 ($y --> (/,hear,#c,_))) ==> bigram($x,$y)).
// */
//public class IRCAgent extends IRC {
////    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);
//
//
//	static final int wordDelayMS = 100;
//	private final Focus f;
//
//	private final Questioning questioning;
//	private final NLPInput nlp;
//
//	/*@Nullable*/ private final String messagePrefix;
//	private final String[] channels;
//
//	private MyLeakOut out;
//	boolean trace = true;
//
//	public IRCAgent(Focus f, String nick, String server, String... channels) {
//		super(nick, server, channels);
//
//		this.f = f;
//		this.nlp = new NLPInput(f.nar);
//		this.channels = channels;
//		this.questioning = new Questioning(f, 8) {
//			@Override protected void answer(NALTask question, NALTask answer) {
//				sendNow(channels, question + " \u2568 " + answer);
//			}
//		};
//
//		messagePrefix =  irc.getNick() + ", ";
//
//		if (trace) {
//			out = new MyLeakOut(f /* HACK */, channels);
//			Loop.of(() -> {
//				out.leak();
//			}).fps(0.05f);
//		}
//	}
//
//	public static void main(String[] args) {
//
//		float durFPS = 1f;
//		NAR nar = new NARS.DefaultNAR(8, true)
//			.exe(new WorkerExec(4, (n)->
//				Derivers.nal(1, 8).core().stm().images()
//                    .temporalInduction().addAll(
//                        new ExplodeAction(),
//                        new Factorize.FactorIntroduction())
//                    .compile(n)))
//			.time(new RealTime.MS().durFPS(durFPS)).get();
//		nar.timeRes.set(100);
//		nar.complexMax.set(96);
//
//
//		IRCAgent bot = new IRCAgent(nar.main(),
//			"narchy", "irc.libera.chat", "#nars"
//		);
//
//		new Thread(() -> {
//			try {
//				bot.start();
//			} catch (IOException | IrcException e) {
//				e.printStackTrace();
//			}
//		}).start();
//
////        n.addOpN("trace", (arg, nn) -> {
////            if (arg.subs() > 0) {
////                switch (arg.sub(0).toString()) {
////                    case "on": bot.setTrace(true); break;
////                    case "off": bot.setTrace(false);  bot.out.clear(); break;
////                }
////            }
////        });
//
//
//        /*
//        n.on("readToUs", (Command) (a, t, nn) -> {
//            if (t.length > 0) {
//                String url = $.unquote(t[0]);
//                if (canReadURL(url)) {
//                    try {
//
//                        Term[] targets;
//                        if (t.length > 1 && t[1] instanceof Compound) {
//                            targets = ((Compound)t[1]).terms();
//                        } else {
//                            targets = null;
//                        }
//
//                        Collection<String> lines = IOUtil.readLines(new URL(url).openStream());
//
//                        new RateIterator<String>(lines.iterator(), 2)
//                                .threadEachRemaining(l -> {
//
//                                    bot.hear(l, nn.self().toString());
//
//                                    if (targets == null) {
//                                        bot.broadcast(l);
//                                    } else {
//                                        for (Term u : targets)
//                                            bot.send($.unquote(u), l);
//                                    }
//
//                                }).start();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        */
//
//
//        /*
//
//        try {
//            new RateIterator<Task>(
//                NQuadsRDF.stream(n,
//                    new File("/home/me/Downloads/nquad")), 500)
//                        .threadEachRemaining(n::inputLater).start();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        */
//
//
////        n.startFPS(10f);
////
////        try {
////            bot.start();
////        } catch (IOException | IrcException e) {
////            e.printStackTrace();
////        }
//
//		nar.log();
//
//		nar.startFPS(1);
//	}
//
//	static boolean canReadURL(String url) {
//		return url.startsWith("https://gist.githubusercontent");
//	}
//
//	public void setTrace(boolean trace) {
//		this.trace = trace;
//	}
//
//	void hear(String text, String src) {
////		boolean hearTwenglish = false;
////		NARHear.hear(f, text, src, hearTwenglish ? wordDelayMS : -1);
//
//		List<Task> parsed = new Lst<>();
//
//		List<Narsese.NarseseException> errors = new Lst<>();
//
//		try {
//			Narsese.tasks(text, parsed, f.nar);
//		} catch (Narsese.NarseseException ignored) { }
//
//		if (!parsed.isEmpty() && errors.isEmpty()) {
//			for (var p : parsed)
//				input(p, src);
//		} else {
//			inputUnparsed(text, src);
//		}
//	}
//
//	private void inputUnparsed(String text, String src) {
//		List<NALTask> t = nlp.parse(text);
//		for (var tt : t)
//			sendNow(channels, tt.toString());
//		f.acceptAll(t);
//	}
//
//	private void input(Task x, String src) {
//		if(x.QUESTION()/*TODO QUEST */) {
//			questioning.ask((NALTask) x);
//		} else {
//			f.accept(x);
//		}
//	}
//
//	@Override
//	public void onPrivateMessage(PrivateMessageEvent event) {
//		onGenericMessage(event);
//	}
//
//	@Override
//	public void onGenericMessage(GenericMessageEvent event) {
//
//		if (event instanceof MessageEvent pevent) {
//
//            if (pevent.getUser().equals(irc.getUserBot()))
//				return; //ignore own output
//
//
//			String msg = pevent.getMessage().trim();
//
//			//        if (channel.equals("unknown")) return;
//			if (msg.startsWith("//"))
//				return; //comment or previous output
//			if (messagePrefix!=null) {
//				if (!msg.startsWith(messagePrefix))
//					return;
//				msg = msg.substring(messagePrefix.length()).trim();
//			}
//
//
//
//			String src = pevent.getUser().getNick();
//			String channel = pevent.getChannel().getName();
//
//			try {
//
//				hear(msg, src);
//
//			} catch (Exception e) {
//				pevent.respond(e.toString());
//			}
//
//
//		}
//
//	}
//
//	public void send(String target, String l) {
//		irc.send().message(target, l);
//	}
//
//    private class MyLeakOut implements Consumer<Task> {
//		final private static int capacity = 16;
//		final Bagregate<NALTask> bag = new Bagregate<>(capacity, PriMerge.max);
//		private final String[] channels;
//
//		MyLeakOut(Focus f, String... channels) {
//			f.onTask(this);
//			this.channels = channels;
//		}
//		public void leak() {
//			bag.commit();
//
//			PriReference<NALTask> _next = bag.bag.sample(ThreadLocalRandom.current());
//			if (_next==null) return;
//			var next = _next.get();
//
//            boolean cmd = next.COMMAND();
//            if (cmd || (trace && !next.isDeleted())) {
//                String s = cmd ? next.term().toString() : next.toString();
//                Runnable r = IRCAgent.this.send(channels, s);
//                if (r!=null)
//                    f.nar.runLater(r);
//            }
//		}
//
//		@Override
//		public void accept(Task task) {
//			if (task instanceof NALTask t)
//				bag.put(t);
//		}
//
//		//
//
////        @Override
////        public boolean filter(@NotNull Task next) {
////            if (trace || next.isCommand())
////                return super.filter(next);
////            return false;
////        }
////
////        @Override
////        public float value() {
////            return 1;
////        }
////    }
////
//	}
//
//}
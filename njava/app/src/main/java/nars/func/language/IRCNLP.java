
package nars.func.language;

import jcog.TODO;
import nars.*;
import nars.func.language.util.IRC;
import nars.term.Compound;
import nars.time.clock.RealTime;
import org.jetbrains.annotations.Nullable;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.OutputEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static nars.Op.*;

/**
 * http:
 * <p>
 * $0.9;0.9;0.99$
 * <p>
 * $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 * $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 * $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 * $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 * $0.9;0.9;0.99$ hear(I, #something)!
 * hear(I,?x)?
 * <p>
 * $0.9$ (($x,"the") <-> ($x,"a")).
 * ((($x --> (/,hear,#c,_)) &&+1 ($y --> (/,hear,#c,_))) ==> bigram($x,$y)).
 */
public class IRCNLP extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCNLP.class);


    private final NAR nar;


//    private final boolean hearTwenglish = true;


    private final String[] channels;
//    private final MyLeakOut outleak;
    final Vocalize speech;

    boolean trace;


    public IRCNLP(NAR nar, String nick, String server, String... channels) {
        super(nick, server, channels);

        this.nar = nar;
        this.channels = channels;
        this.speech = new Vocalize(nar, 1f, this::send);




//        outleak = new MyLeakOut(nar, channels);


        /*
        $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 $0.9;0.9;0.99$ hear(I, #something)!
 hear(I,?x)?

 $0.9$ (($x,"the") <-> ($x,"a")).
         */


    }

//    /**
//     * identical with IRCAgent, TODO share them
//     */
//    private class MyLeakOut extends TaskLeak {
//        public final String[] channels;
//
//        public MyLeakOut(NAR nar, String... channels) {
//            super(8, nar);
//            this.channels = channels;
//        }
//
//        @Override
//        public float value() {
//            return 1;
//        }
//
//        @Override
//        protected float leak(Task next, What what) {
//            boolean cmd = next.isCommand();
//            if (cmd || (trace && !next.isDeleted())) {
//                String s = (!cmd) ? next.toString() : next.term().toString();
//                Runnable r = IRCNLP.this.send(channels, s);
//                if (r != null) {
//                    nar.runLater(r);
////                    if (NAL.DEBUG && !next.isCommand())
////                        logger.info("{}\n{}", next, next.proof());
//                } else {
//
//                }
//                return cmd ? 0 : 1;
//            }
//            return 0;
//        }
//
//        @Override
//        public boolean filter(@NotNull Task next) {
//            if (trace || next.isCommand())
//                return super.filter(next);
//            return false;
//        }
//    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }


    @Override
    public void onPrivateMessage(PrivateMessageEvent event) {

    }

    @Override
    public void onOutput(OutputEvent event){
        List<String> ll = event.getLineParsed();
        if (ll.size() == 3 && ll.get(0).equals("PRIVMSG")) {
            throw new TODO();
            //echo loopback
            //String s = ll.get(2);
            //hearText(f, s, event.getBot().getNick(), (int)nar.dur(), 1);
        }
    }

    @Override
    public void onGenericMessage(GenericMessageEvent event) {

        if (event instanceof MessageEvent pevent) {

//            if (pevent.getUser().equals(irc.getUserBot())) {
//                return;
//            }

            String msg = pevent.getMessage().trim();

            String src = pevent.getUser().getNick();
            String channel = pevent.getChannel().getName();

            try {

                NARHear.hear(msg, src, nar);

            } catch (Exception e) {
                pevent.respond(e.toString());
            }


        }


    }


    public static void main(String[] args) {


        float durFPS = 2f;
        NAR n = new NARS.DefaultNAR(8, true)
                .time(new RealTime.MS().durFPS(durFPS)).get();

        n.freqRes.set(0.05f);
        n.confRes.set(0.02f);

        n.timeRes.set(100);
        n.complexMax.set(30);

//		ReactionModel d = Derivers.core(Derivers.nal(3, 8, "motivation.nal").addAll(
//			new CompoundClustering(BELIEF, 8, 64)
//			//new AnswerQuestionsFromConcepts.AnswerQuestionsFromTaskLinks()
//		)).compile(n);



        /*@NotNull Default n = new Default(new Default.DefaultTermIndex(4096),
            new RealTime.DS(true),
            new TaskExecutor(256, 0.25f));*/


//        new Thread(() -> {
//            try {
//                new TextUI(n, 1024);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();


        IRCNLP bot = new IRCNLP(n,
                "nar" + Math.round(64 * 1024 * Math.random()),
                "irc.freenode.net",
                "#xyz123"
        );
        new Thread(()-> {
            try {
                bot.start();
            } catch (IOException | IrcException e) {
                e.printStackTrace();
            }
        }).start();



        Term HEAR = $.atomic("hear");


        //t.isBeliefOrGoal() /* BOTH */) {
        //long now = n.time();
        //int dur = n.dur();
        //if (taskTime >= now - dur) {
        //}
        n.main().onTask(_t -> {


            NALTask t = (NALTask) _t;
            long taskTime = t.mid();
            if (taskTime != ETERNAL && t.GOAL() && t.POSITIVE()) { //t.isBeliefOrGoal() /* BOTH */) {
                //long now = n.time();
                //int dur = n.dur();
                //if (taskTime >= now - dur) {
                Term tt = t.term();
                if (tt.INH() && HEAR.equals(tt.sub(1))) {
                    if (((Compound) tt).subIs(0, PROD) && tt.sub(0).sub(0).TASKABLE()) {
                        bot.speak(tt.sub(0).sub(0), taskTime, t.truth());
                    }
                }
                //}
            }
        }, GOAL);


        n.synch();

        //NARHear.readURL(n);
        //NARHear.readURL(n, "http://w3c.org");


        n.log();



        n.startFPS(25f);
//        n.loop.throttle.set(0.5f);

//        while (true) {
//          n.run(1000);
//          Util.sleepMS(10);
//        }


        //Thread.currentThread().setDaemon(true);
    }


    private void speak(Term word, long when, @Nullable Truth truth) {
        speech.speak(word, when, truth);
    }


    String s = "";
    int minSendLength = 1;

    protected float send(Term o) {
        Runnable r = null;
        synchronized (this) {
            String w = $.unquote(o);
            this.s += w;
            boolean punctuation = List.of(".", "!", "?").contains(w);
            if (!punctuation)
                s += " ";
            if ((!s.isEmpty() && punctuation) || this.s.length() >= minSendLength) {

                r = IRCNLP.this.send(channels, this.s.trim());
                this.s = "";
            }
        }


        if (r != null) {
            r.run();

        }

        return 1;
    }

}
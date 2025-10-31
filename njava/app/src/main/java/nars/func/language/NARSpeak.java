package nars.func.language;

import jcog.event.ListTopic;
import jcog.event.Topic;
import nars.NAR;
import nars.func.java.Opjects;

/**
 * TODO make extend NARService and support start/re-start
 */
public class NARSpeak {
    private final NAR nar;

    private final Opjects op;

    /**
     * emitted on each utterance
     */
    public final Topic<Object> spoken = new ListTopic();
    public final SpeechControl speech;

    public NARSpeak(NAR nar) {
        this.nar = nar;


        this.op = new Opjects(nar.main());

        speech = op.the(nar.self(), new SpeechControl(), this);

        op.alias("say", nar.self(), "speak");

    }

    public class SpeechControl {

        public SpeechControl() {
            chatty();
        }

        public void speak(Object... text) {

            spoken.emitAsync(text, nar.exe);


        }

        public void quiet() {
            op.exeThresh.set(1f);
        }

        public void normal() {
            op.exeThresh.set(0.75f);
        }

        public void chatty() {
            op.exeThresh.set(0.51f);
        }
    }


}
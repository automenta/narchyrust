package nars.experiment;

import nars.NALTask;
import nars.Player;
import nars.game.Game;

import java.util.concurrent.ThreadLocalRandom;

import static nars.$.$$;

/**
 * 2-ary simplified "Pong"
 */
public class NObvious2 extends Game {

    /** how fast target may change */
    float period = 16;

    private boolean target;
    //private float choice = Float.NaN;
    private float choiceL = 0, choiceR = 0;
    float choiceThresh = 0.5f;

    public NObvious2() {
        super("n");

        change();

//        actionBipolar($$("x"), (x)->{
//           return choice = x;
//        });
        action($$("l"), (x)->{
            choiceL = x;
        });
        action($$("r"), (x)->{
            choiceR = x;
        });
        sense("a", ()->{
           return target ? 1 : 0;
        });
//        sense("b", ()->{
//            return !target ? 1 : 0;
//        });
        reward($$("g"), ()-> {
            //boolean choice = choiceR >= choiceL;
            //return (target && choice > choiceThresh) || (!target && choice < -choiceThresh) ? 1 : 0;
            return (target && /*choiceL >= choiceThresh &&*/ choiceR < choiceL) ||
                   (!target && /*choiceR >= choiceThresh &&*/ choiceL < choiceR)
                        ? 1 : 0;
        });
        afterFrame(g->{
            //System.out.println(choice + "\t" + target);
           if (g.rng().nextBoolean(1f/period))
               change();

        });

    }

    private void change() {
        target = ThreadLocalRandom.current().nextBoolean();
    }
    public static void main(String[] args) {
        Player p = new Player(new NObvious2());
        p.fps(40);
        p.complexMax = 8;
        p.freqRes = 0.25f;
        //p.timeRes = 100;
        p.nalStructural = false;
        p.meta = false;
        p.nalDiff = true;
        p.nalDelta = false;
        //p.confMin = 0.01f;
        p.threads = 2;
        p.start();

        p.games().forEach(g->{
            g.focus().onTask((t)->{
//                if (t.toString().contains("x"))
                if (t.GOAL() && ((NALTask)t).stamp().length!=1)
//                if (t.GOAL())
//                if (t.BELIEF())

//                if (t.BELIEF() && ((NALTask)t).stamp().length!=1)
//                    if (t.term().IMPL())
//                        if (t.term().sub(1)
//                            .ATOM())
//                                //.equals($$("g")))

                            System.out.println(t);
            });
        });
    }
}

package nars.experiment.minicraft;

import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.$;
import nars.Player;
import nars.Term;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.game.Game;
import nars.sensor.PixelBag;
import nars.video.AutoClassifiedBitmap;
import spacegraph.SpaceGraph;

import static nars.$.$$;


/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends Game {

    private final TopDownMinicraft craft = new TopDownMinicraft();
    protected final AutoClassifiedBitmap camAE;
    float prevScore;

    public TopCraft() {
        super("cra");

        TopDownMinicraft.start(craft);
        //craft.changeLevel(1);

        PixelBag p = new PixelBag(new MonoBufImgBitmap2D(() -> craft.image), 64, 64)
                .actions(id, true, true, true, 0.25f);
        p.maxZoom = 0.5f;
        int nx = 8;
        //return new float[]{p.X, p.Y, p.Z};
        int states = 12;
        camAE = new AutoClassifiedBitmap($$("cae"), p, nx, nx, states, this);
        camAE.freqRes(0.1f);
        //camAE.learnRandom = states;
        //camAE.controlStart(this);
        beforeFrame(() -> {
            craft.frameImmediate();
            p.updateBitmap();
        });

        senseSwitch($.func("dir", this.id, $.varDep(1)),
            new Term[] { $$("dirUp"), $$("dirDown"), $$("dirLeft"), $$("dirRight") /* TODO check these */},
            () -> craft.player.dir);

        sense($.inh(id, "stamina"), () -> (craft.player.stamina) / ((float) craft.player.maxStamina));
        //sense($.func("health", id), () -> (craft.player.health) / ((float) craft.player.maxHealth));

        int tileMax = 13;

        //TODO combine like NARio
        senseSwitch("tile:here", () -> craft.player.tile().id, 0, tileMax);
        senseSwitch("tile:up", () -> craft.player.tile(0, 1).id, 0, tileMax);
        senseSwitch("tile:down", () -> craft.player.tile(0, -1).id, 0, tileMax);
        senseSwitch("tile:right", () -> craft.player.tile(1, 0).id, 0, tileMax);
        senseSwitch("tile:left", () -> craft.player.tile(-1, 0).id, 0, tileMax);

        InputHandler input = craft.input;
        actionPushButton($.inh(id, "fire"), input.attack::press/*, 16*/);
        actionToggle($.inh(id, $.p("move", "l")), $.inh(id,$.p("move", "r")),
                input.left::press, input.right::press);
        actionToggle($.inh(id, $.p("move", "u")), $.inh(id, $.p("move", "d")),
                input.up::press, input.down::press);
        actionPushButton($.inh(id, "next"), (i) -> {
            if (craft.menu != null) {
                input.up.press(false);
                input.down.pressIfUnpressed();
                return true;
            }
            return false;
        });
        actionPushButton($.inh(id, "menu"),
            debounce(
                input.menu::press
                //input.menu::pressIfUnpressed
            , 32f)
        );

        rewardNormalizedPolar($.inh(id, "score"), () -> {
            if (!alive()) return 0;

            float nextScore = craft.player.score;
//            return nextScore;
            float ds = nextScore - prevScore;
//            if (ds == 0)
//                return Float.NaN;
            this.prevScore = nextScore;
//            //System.out.println("score delta:" + ds);
            return ds;
        });
        reward("alive", ()-> {
            return alive() ? 1 : 0;
        });
        reward("health", ()-> {
//            float nextHealth = craft.player.health;
//            float dh = nextHealth - prevHealth;
//            if (dh == 0)
//                return Float.NaN;
//            this.prevHealth = nextHealth;
//            //System.out.println("health delta: " + dh);
//            return dh;
            return (craft.player.health / ((float) craft.player.maxHealth));
        });

    }

    private boolean alive() {
        return craft.player.health > 0;
    }

    public static void main(String[] args) {

        TopCraft tc = new TopCraft() {
            @Override
            protected void init() {
                super.init();
                nar.runLater(()-> SpaceGraph.window(nars.gui.GameUI.aeUI(camAE), 500, 500));
            }
        };

        new Player(60, n -> n.add(tc)).start();


    }


}
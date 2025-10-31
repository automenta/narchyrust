package nars.experiment.minicraft;

import jcog.Util;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.Player;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.game.Game;
import nars.sensor.BitmapSensor;
import nars.sensor.PixelBag;
import nars.video.AutoClassifiedBitmap;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.SpaceGraph;

import java.awt.image.BufferedImage;

import static nars.$.$$;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends Game {

    private final SideScrollMinicraft craft;

    public static void main(String[] args) {

        new Player(30f, n -> n.add(new SideCraft())).start();

    }

    public SideCraft() {
        super("cra");

        this.craft = new SideScrollMinicraft();


    }

    @Override
    protected void init() {
        super.init();
        BufferedImage camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;

        PixelBag cam = new PixelBag(new MonoBufImgBitmap2D(() -> camBuffer), 48, 32)
                .actions($$("cra"), true, true, true, 0.1f);


        AutoClassifiedBitmap camAE = new AutoClassifiedBitmap($$("cra"), cam.pixels, 8, 8, 32, this);
        camAE.alpha(0.1f);
        SpaceGraph.window(nars.gui.GameUI.aeUI(camAE), 500, 500);


        BitmapSensor<PixelBag> pixels = senseCamera("cra", cam);


        /* slow */


        actionPushButton($$("cra(key,left)"), (b4) -> {
            if (b4) craft.player.startLeft(false /* slow */);
            else craft.player.stopLeft();
        });

        /* slow */


        actionPushButton($$("cra(key,right)"), (b3) -> {
            if (b3) craft.player.startRight(false /* slow */);
            else craft.player.stopRight();
        });


        actionPushButton($$("cra(key,up)"), (b2) -> {
            if (b2) craft.player.startClimb();
            else craft.player.stopClimb();
        });


        actionPushButton($$("cra(key,mouseL)"), (BooleanProcedure) (b1) -> craft.leftClick = b1);


        actionPushButton($$("cra(key,mouseR)"), (BooleanProcedure) (b) -> craft.rightClick = b);

        float mSpeed = 45f;
        actionBipolar($$("cra(mouse,X)"), (v) -> {
            int x = craft.screenMousePos.x;
            int xx = Util.clamp(Math.round(x + v * mSpeed), 0, camBuffer.getWidth() - 1);

            craft.screenMousePos.x = xx;
            return v;

        });
        actionBipolar($$("cra(mouse,Y)"), (v) -> {
            int y = craft.screenMousePos.y;
            int yy = Util.clamp(Math.round(y + v * mSpeed), 0, camBuffer.getHeight() - 1);
            craft.screenMousePos.y = yy;
            return v;
        });


        reward(() -> {

            float nextScore = 0;
            for (int i = 0; i < gameFramesPerCycle; i++)
                nextScore += craft.frame();
            float ds = nextScore - prevScore;
            this.prevScore = nextScore;
            return ds;
        });

        craft.startGame(false, 512);

    }

    float prevScore;
    static final int gameFramesPerCycle = 1;


}
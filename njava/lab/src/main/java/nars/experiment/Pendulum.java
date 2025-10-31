/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.math.v2;
import nars.Player;
import nars.game.Game;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.video.Draw;

import java.util.List;

import static nars.$.$$;
import static nars.game.GameTime.fps;
import static spacegraph.SpaceGraph.window;

/**
 * @author thorsten
 */
public class Pendulum extends Game {

    private static final float fps = 15;
    private final Physics2D phy;
    private final Physics2D.Point agentPoint;
    private final Physics2D.Point pendulumPoint;
    private final PhysicsRenderer physicsRenderer;

    private static final double VX_MAX = 128;

    double dt = 0.8;
    int substeps = 4;

    double speed = 2;


    double gravity = 0.2;

    int maxX = 800;
    int minAgentX = 50;
    int maxAgentX = 750;
    double poleLength = 175;
    double decay = 0.995;
    double decay2 = 0.995;
    double agentY = 300;


    public Pendulum() {
        super("p", fps(fps));
        phy = new Physics2D(gravity, 300);

        physicsRenderer = new PhysicsRenderer(phy);
        window(physicsRenderer, 800, 600);
//        JFrame j = new JFrame();
//        j.setSize(800, 600);
//        j.getContentPane().add(physicsRenderer);
//        j.setVisible(true);

        agentPoint = new Physics2D.Point((minAgentX + maxAgentX) / 2.0f, (float)agentY, 0, 0, decay2, 0);
        pendulumPoint = new Physics2D.Point((minAgentX + maxAgentX) / 2.0f, (float)(agentY - poleLength), 0, 0, decay, decay);
    }

    @Override
    protected void init() {
        super.init();

        Physics2D.Connection c = new Physics2D.Connection(poleLength, agentPoint, pendulumPoint);
        phy.points.add(agentPoint);
        phy.points.add(pendulumPoint);
        phy.connections.add(c);
        sense("(a,x)", ()-> agentPoint.x / maxX);
        sense("(p,x)", ()-> pendulumPoint.x / maxX);
        //sense("y", ()->(float)((pendulumPoint.y - agentPoint.y) / (poleLength * 1.1)));
        senseAngle($$("r"), ()->(float)Math.atan2(pendulumPoint.y, pendulumPoint.x), 4);
        reward("good", ()->0.5f + 0.5f * (float)((agentPoint.y - pendulumPoint.y) / poleLength));
        actionBipolar($$("(move,x)"), (dx)->{
            agentPoint.vx = Util.clamp(agentPoint.vx + (speed * dt) * dx, -VX_MAX, VX_MAX);
            return dx;
        });
        onFrame(()->{

            for (int i = 0; i < substeps; i++) {
                phy.step(dt/substeps);

                if (agentPoint.x < minAgentX) {
                    agentPoint.x = minAgentX;
                    agentPoint.vx = 0;
                }
                if (agentPoint.x > maxAgentX) {
                    agentPoint.x = maxAgentX;
                    agentPoint.vx = 0;
                }
            }

//            physicsRenderer.repaint();
            //SwingUtilities.invokeLater(physicsRenderer::repaint);
        });
    }

    public static void main(String[] args) {

        new Player(fps * 2, n -> {
                    Game p = new Pendulum();
                    n.add(p);
        }).start();

    }

    /**
     * @author thorsten
     */
    private static class Physics2D {

        public final List<Point> points = new Lst<>();
        public final List<Connection> connections = new Lst<>();
        public double gravity;
        public double floor;


        Physics2D(double gravity, double floor) {
            this.gravity = gravity;
            this.floor = floor;
        }

        public void step(double dt) {
            for (Connection c : connections) {
                final Point a = c.a, b = c.b;
                double dx = a.x - b.x;
                double dy = a.y - b.y;
                double l = Math.sqrt(Util.sqr(dx) + Util.sqr(dy));
                double dl = c.length - l;
                double nx = dt * dl * dx / l / 2;
                double ny = dt * dl * dy / l / 2;
                a.vx += nx;
                b.vx -= nx;
                a.vy += ny;
                b.vy -= ny;
            }

            for (Point p : points) {
                p.vy += dt * gravity;
                p.vx *= p.decayx;
                p.vy *= p.decayy;
                p.x += p.vx;
                p.y += p.vy;
            }


        }

        /**
         * @author thorsten
         */
        private static class Point extends v2 {
            public double vx, vy;
            public double decayx, decayy;

            Point(float x, float y, double vx, double vy, double decayx, double decayy) {
                super(x, y);
                this.vx = vx;
                this.vy = vy;
                this.decayx = decayx;
                this.decayy = decayy;
            }
        }

        /**
         *
         * @author thorsten
         */
        static class Connection {

            public final double length;
            public final Point a;
            public final Point b;

            Connection(double length, Point p1, Point p2) {
                this.length = length;
                this.a = p1;
                this.b = p2;
            }
        }
    }

    /**
     *
     * @author thorsten
     */
    private static class PhysicsRenderer extends Surface {

        final Physics2D physics2D;

        /**
         * Creates new form PhysicsRenderer
         * @param physics2D
         */
        PhysicsRenderer(Physics2D physics2D) {
            this.physics2D = physics2D;
        }

//        float motionBlur = 0.75f;

        @Override
        protected void render(ReSurface r) {
            //g.setColor(new Color(0, 0, 0, 1-motionBlur));
            //g.fillRect(0, 0, getWidth(), getHeight());

            //Graphics2D g2 = (Graphics2D)g;
            //g2.setStroke(st);

            GL2 g = r.gl;
            g.glColor3f(1,1,1);
            g.glLineWidth(4);
            Draw.linf(0, (float)physics2D.floor, r.w, (float)physics2D.floor, g);

            float h = r.h;
            //g.setColor(Color.orange);
            for (Physics2D.Connection c : physics2D.connections)
                Draw.linf(c.a.x, h-c.a.y, c.b.x, h-c.b.y, g);

            ///g.setColor(Color.green);
            for (Physics2D.Point p : physics2D.points)
                Draw.circle(g, new v2(p.x, h-p.y), false, 20, 12);
        }
    }
}
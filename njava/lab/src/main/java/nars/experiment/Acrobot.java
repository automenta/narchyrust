package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.math.normalize.FloatNormalized;
import nars.Player;
import nars.game.Game;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.video.Draw;

import java.util.Arrays;
import java.util.stream.IntStream;

import static nars.$.$$;

/** https://raw.githubusercontent.com/openai/gym/master/gym/envs/classic_control/acrobot.py */
public class Acrobot extends Game  {
    private static final double GRAVITY = 9.8;
    private static final double LINK_LENGTH_1 = 1.0; // meters
    private static final double LINK_LENGTH_2 = 1.0; // meters
    private static final double LINK_MASS_1 = 1.0; // kg
    private static final double LINK_MASS_2 = 1.0; // kg
    private static final double LINK_COM_POS_1 = 0.5; // center of mass position of link 1
    private static final double LINK_COM_POS_2 = 0.5; // center of mass position of link 2
    private static final double LINK_MOI_1 = LINK_MASS_1 * LINK_LENGTH_1 * LINK_LENGTH_1 / 12.0; // moment of inertia for link 1
    private static final double LINK_MOI_2 = LINK_MASS_2 * LINK_LENGTH_2 * LINK_LENGTH_2 / 12.0; // moment of inertia for link 2
    private static final double MAX_VEL_1 = 4 * Math.PI;
    private static final double MAX_VEL_2 = 9 * Math.PI;

    private final double[] state = {Math.PI, 0, 0, 0}; // theta1, theta2, omega1, omega2
    float torque;

    public static void main(String[] args) {
        new Player(new Acrobot()).fps(30).start();
    }

    float speed = 0.05f;
    double dt = 0.1;

    public static boolean ui = true;


    public Acrobot() {
        super("acro");

        if (ui)
            SpaceGraph.window(new Display(), 400,400);

        senseAngle($$("(acro-->(a,#1))"), ()->(float)state[0]);
        senseAngle($$("(acro-->(b,#1))"), ()->(float)state[1]);
        sense($$("(acro-->(a,v))"), new FloatNormalized(()->(float)state[2]).polar());
        sense($$("(acro-->(b,v))"), new FloatNormalized(()->(float)state[3]).polar());
//        action($$("(acro-->torque)"), (x)->{
//            this.torque = (x * 2 - 1) * speed;
//            return x;
//        });
        actionBipolar($$("(acro-->(torque,#1))"), (x)->{
            this.torque = x * speed;
            return x;
        });
        reward(this::step);
    }

    private class Display extends PaintSurface {

        static int SCREEN_DIM=300;

        @Override
        protected void paint(GL2 gl, ReSurface reSurface) {
            double bound = LINK_LENGTH_1 + LINK_LENGTH_2 + 0.2;
            double scale = SCREEN_DIM / (bound * 2);
            float offset = SCREEN_DIM / 2f;

            // Coordinates of the first link
            float x1 = offset;
            float y1 = offset;
            float x2 = (float) (x1 + LINK_LENGTH_1 * Math.cos(state[0]) * scale);
            float y2 = (float) (y1 + LINK_LENGTH_1 * Math.sin(state[0]) * scale);

            // Coordinates of the second link
            float x3 = (float) (x2 + LINK_LENGTH_2 * Math.cos(state[0] + state[1]) * scale);
            float y3 = (float) (y2 + LINK_LENGTH_2 * Math.sin(state[0] + state[1]) * scale);

            gl.glLineWidth(4);

            // Draw the horizontal line
            Draw.colorHash(gl, 29428937);
            Draw.linf((float) (-2.2 * scale + offset), offset, (float) (2.2 * scale + offset), offset, gl);

            // Draw first link
            Draw.colorHash(gl, -82357);
            Draw.linf(x1, y1, x2, y2, gl);
            Draw.circle(gl, new jcog.math.v2(x1, y1), true, 5, 7); // Yellow joint circle at base
            Draw.circle(gl, new jcog.math.v2(x2, y2), true, 5, 7); // Yellow joint circle at first link's end

            // Draw second link
            Draw.colorHash(gl, +82357);
            Draw.linf(x2, y2, x3, y3, gl);
            Draw.circle(gl, new jcog.math.v2(x3, y3), true, 5, 7); // Yellow joint circle at first link's end

        }
    }

    public float step() {
        double[] nextState = rk4(this.derivs(this.state, torque), this.state, dt);
        this.state[0] = wrap(nextState[0], -Math.PI, Math.PI);
        this.state[1] = wrap(nextState[1], -Math.PI, Math.PI);
        this.state[2] = Math.max(-MAX_VEL_1, Math.min(MAX_VEL_1, nextState[2]));
        this.state[3] = Math.max(-MAX_VEL_2, Math.min(MAX_VEL_2, nextState[3]));
        double r = (Math.sin(state[0])/2+0.5 + Math.sin(state[0] + state[1])/2+0.5)/2;
        //System.out.println(state[0] + " " + state[1] + " " + r);
        return (float) r;
    }

    private double[] derivs(double[] state, double torque) {
        double theta1 = state[0];
        double theta2 = state[1];
        double omega1 = state[2];
        double omega2 = state[3];

        double d1 = LINK_MOI_1 + LINK_MOI_2 + LINK_MASS_2 * LINK_LENGTH_1 * LINK_LENGTH_1 + 2 * LINK_MASS_2 * LINK_LENGTH_1 * LINK_COM_POS_2 * Math.cos(theta2);
        double d2 = LINK_MOI_2 + LINK_MASS_2 * LINK_LENGTH_1 * LINK_COM_POS_2 * Math.cos(theta2);
        double phi2 = LINK_MASS_2 * LINK_COM_POS_2 * GRAVITY * Math.cos(theta1 + theta2 - Math.PI / 2);
        double phi1 = -LINK_MASS_2 * LINK_LENGTH_1 * LINK_COM_POS_2 * omega2 * omega2 * Math.sin(theta2) - 2 * LINK_MASS_2 * LINK_LENGTH_1 * LINK_COM_POS_2 * omega2 * omega1 * Math.sin(theta2) + (LINK_MASS_1 * LINK_COM_POS_1 + LINK_MASS_2 * LINK_LENGTH_1) * GRAVITY * Math.cos(theta1 - Math.PI / 2) + phi2;

        double dtheta2 = (torque + d2 / d1 * phi1 - LINK_MOI_2 * phi2) / (d1 * d2 - d2 * d2);
        double dtheta1 = -(dtheta2 * d2 + phi1) / d1;
        return new double[]{omega1, omega2, dtheta1, dtheta2};
    }

    private static double wrap(double x, double m, double M) {
        double intervalLength = M - m;
        while (x > M) x -= intervalLength;
        while (x < m) x += intervalLength;
        return x;
    }


    public static double[] rk4(double[] derivs, double[] y, double dt) {
        double[] k1 = derivs;
        double[] k2 = Arrays.stream(derivs).map(k -> k * dt / 2).toArray();
        double[] k3 = Arrays.stream(derivs).map(k -> k * dt / 2).toArray();
        double[] k4 = Arrays.stream(derivs).map(k -> k * dt).toArray();

        return IntStream.range(0, derivs.length)
                .mapToDouble(i -> y[i] + dt / 6.0 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]))
                .toArray();
    }


    public static double bound(double x, double m, double M) {
        return Math.min(Math.max(x, m), M);
    }}

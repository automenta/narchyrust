package nars.experiment;

import jcog.Fuzzy;
import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.math.normalize.FloatNormalized;
import jcog.math.v2;
import jcog.signal.FloatRange;
import nars.$;
import nars.Player;
import nars.Term;
import nars.game.Game;
import nars.game.reward.LambdaScalarReward;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.phys.Dynamics2DView;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.BodyDef;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.FixtureDef;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJoint;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJointDef;

import java.util.function.Supplier;

import static java.lang.Math.PI;
import static jcog.Util.PHI;
import static spacegraph.space2d.phys.dynamics.BodyType.DYNAMIC;
import static spacegraph.space2d.phys.dynamics.BodyType.STATIC;

/**
 * A streamlined 2D physics-based game superclass with ready-to-use helpers.
 * Specific games (like Arm2D) can extend this and implement their logic.
 */
public abstract class Phys2DGame extends Game {

    protected final Dynamics2D world = new Dynamics2D();
    protected float worldDT = 1/16f;
    protected boolean gravity;

    public Phys2DGame(String id) {
        super(id);
        if (!gravity) world.setGravity(new v2());
        beforeFrame(() -> world.step(worldDT, 2, 2));
    }

    public Phys2DGame window() {
        SpaceGraph.window(vis(), 1024, 800);
        return this;
    }

    public Surface vis() {
        var v = new Dynamics2DView(world);
        var c = config();
        if (c!=null) {
            var s = new ObjectSurface(c);
            return new Splitting<>(v, 0.9f, s).resizeable().horizontal();
        }
        return v;
    }

    /** Sense a joint's current angle in normalized [0..1]. */
    protected void senseJoint(RevoluteJoint j, String label) {
        sense($.inh(id, $.quote(label)), /*new FloatNormalized*/(() -> {
            double a =
                    //j.getJointAngle();
                    j.getJointAngle() + j.A().getAngle();
            while (a < 0)    a += 2 * PI;
            while (a > 2*PI) a -= 2 * PI;
            return (float) (a / (2 * PI));
        }));
    }

    /** Reward for proximity: rewardPow>1 emphasizes being close; maxDist sets scale. */
    protected LambdaScalarReward rewardNear(Term label, Supplier<v2> a, Supplier<v2> b,
                                            float rewardPow, float maxDist) {
        FloatSupplier distNorm = () -> a.get().distance(b.get()) / maxDist;
        return reward(label, () -> (float) Math.pow(1 - distNorm.asFloat(), rewardPow));
    }

    @Nullable public Object config() { return null; }

    /** Direct drive: input in [-1..+1], sets a joint motor speed. */
    static class DirectDrive implements FloatToFloatFunction {
        private final RevoluteJoint joint;
        private final FloatSupplier motorSpeed;

        /** TODO FloatSupplier torque */
        public DirectDrive(RevoluteJoint j, FloatSupplier motorSpeed) {
            joint = j;
            this.motorSpeed = motorSpeed;
            joint.setMaxMotorTorque(1f);
        }

        @Override
        public float valueOf(float x) {
            float s = Fuzzy.polarize(x);
            //joint.setMotorSpeed(s * motorSpeed.asFloat());
            //float s = x;
            //float a = (float) (s * PI * 1); joint.setLimits(a,a);
            float a = Util.clamp((s * motorSpeed.asFloat()) + joint.getLowerLimit(), 0, (float) +PI); joint.setLimits(a, a);
            return x;
        }
    }

    // ----------------------------------------------------------------------
    // Example Implementation: Arm2D
    // ----------------------------------------------------------------------
    public static class Arm2D extends Phys2DGame {

        static class Config {
            public final FloatRange motorSpeed = new FloatRange(0.05f, 0.001f, 0.1f);
        }

        private static final float density = 0.001f;

        final RevoluteJoint rotShoulder, rotElbow, rotFingerA, rotFingerB;
        final Body2D fingerA;
        float ballRad = 2;
        double ballSpeed = 4;
        float rewardPow = 1;
        boolean actionShoulder = true, actionElbow = true, actionFinger;

        public final Config cfg = new Config();

        public Arm2D(String id) {
            super(id);

            float shoulderRad = 2;
            float upperLen = (float)(shoulderRad * Math.pow(PHI, 1));
            float upperThick = (float)(upperLen * Math.pow(PHI, -2));
            float lowerLen = (float) (upperLen * PHI);
            float lowerThick = (float) (upperThick / PHI);

            var shoulderBody = world.addBody(new BodyDef(STATIC),
                    new FixtureDef(PolygonShape.box(shoulderRad, shoulderRad), density, 0));

            var upperArm = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(upperLen, upperThick), density, 0));
            var lowerArm = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(lowerLen, lowerThick), density, 0));

            var sDef = new RevoluteJointDef(shoulderBody, upperArm, true)
                    .localB(+upperLen*1.75f - shoulderRad, 0);
            //shoulderBody.angle(-(float) PI/2);
            //sDef.limit((float)((0*PI)), (float)((0.5*PI)));
            sDef.enableLimit(false);
            rotShoulder = world.addJoint(sDef);

            var eDef = new RevoluteJointDef(upperArm, lowerArm, true)
                    .localA(-upperLen*0.9f, 0)
                    .localB(+lowerLen*0.9f, 0);
            eDef.enableLimit = true;
            eDef.lowerAngle = (float)(-Util.PHI_min_1f/2 * PI);
            eDef.upperAngle = (float)(+Util.PHI_min_1f/2 * PI);
            rotElbow = world.addJoint(eDef);

            {
                float f = lowerThick;
                float fingerLen  = (float)(f * Math.pow(PHI, 3));
                float thumbLen   = (float)(f * Math.pow(PHI, 2));
                float fingerThick= (float)(f * Math.pow(PHI, -1));

                var fingerShape = new PolygonShape(0, fingerThick/2, fingerLen, 0, 0, -fingerThick/2f);
                fingerA = world.addBody(new BodyDef(DYNAMIC), new FixtureDef(fingerShape, density, 0));
                var fingerB = world.addBody(new BodyDef(DYNAMIC),
                        new FixtureDef(new PolygonShape(0, fingerThick/2, thumbLen, 0, 0, -fingerThick/2f), density, 0));

                var rA = new RevoluteJointDef(lowerArm, fingerA, true).localA(-lowerLen, -lowerThick);
                var rB = new RevoluteJointDef(lowerArm, fingerB, true).localA(-lowerLen, +lowerThick);

                rA.enableLimit = rA.enableMotor = true;
                rB.enableLimit = rB.enableMotor = true;
                //rA.maxMotorTorque = rB.maxMotorTorque = 5000;
                // Lock them in place by matching lowerAngle=upperAngle:
                rA.lowerAngle = rA.upperAngle = (float) PI;
                rB.lowerAngle = rB.upperAngle = (float) PI;

                rotFingerA = world.addJoint(rA);
                rotFingerB = world.addJoint(rB);
            }
        }

        @Override
        public Object config() { return cfg; }

        @Override
        protected void init() {
            float res = 0.01f;

            if (actionShoulder) {
                action($.inh(id, "s"), new DirectDrive(rotShoulder, cfg.motorSpeed)).freqRes(res);
                senseJoint(rotShoulder, "shoulder");
            } else {
                rotShoulder.setLimits((float) PI, (float) PI);
            }

            if (actionElbow) {
                action($.inh(id, "e"), new DirectDrive(rotElbow, cfg.motorSpeed)).freqRes(res);
                senseJoint(rotElbow, "elbow");
            }

            if (actionFinger) {
                action($.inh(id, $.p("f", 1)), new DirectDrive(rotFingerA, cfg.motorSpeed)).freqRes(res);
                action($.inh(id, $.p("f", 2)), new DirectDrive(rotFingerB, cfg.motorSpeed)).freqRes(res);
            }

            var ball = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.regular(6, ballRad), 1, 0));

            sense($.inh(id, $.p("finger","x")), new FloatNormalized(() -> rotFingerA.A().pos.x));
            sense($.inh(id, $.p("finger","y")), new FloatNormalized(() -> rotFingerA.A().pos.y));
            sense($.inh(id, $.p("ball","x")),   new FloatNormalized(() -> ball.pos.x));
            sense($.inh(id, $.p("ball","y")),   new FloatNormalized(() -> ball.pos.y));

            beforeFrame(()->updateBall(ball));
            // Reward for bringing finger close to the ball, which moves in a simple pattern
            rewardNear($.inh(id,"target"),
                () -> fingerA.pos,
                () -> ball.pos,
                rewardPow, 40);
            reward($.inh(id,"still"), ()->1 / (1 + rotElbow.B().vel.length())).weight(0.1f);
        }

        private void updateBall(Body2D ball) {
            double t = world.time/10.0 * ballSpeed;
            float bx = (ballRad*8)*1.25f;
            float by = (float) Math.sin(t/10f)*(ballRad*8);
            ball.setTransform(new v2(bx,by), 0);
            ball.setLinearVelocity(new v2());
        }

        public static void main(String[] args) {
            new Player(new Arm2D("demo").window()).fps(25f).start();
        }
    }


    public static class BalanceBot extends Phys2DGame {

        static class Config {
            public final FloatRange jointStrength = new FloatRange(10f, 5f, 50f);
            public final FloatRange maxForce = new FloatRange(100f, 50f, 200f);
        }

        private final Config cfg = new Config();
        private RevoluteJoint hipJoint, kneeJoint, ankleJoint;
        private Body2D platform;
        private Body2D torso, thigh, shin, foot;

        public BalanceBot(String id) {
            super(id);
        }

        @Override
        public Object config() { return cfg; }

        @Override
        protected void init() {
            // Create moving platform
            platform = world.addBody(new BodyDef(STATIC),
                    new FixtureDef(PolygonShape.box(5, 0.5f), 0, 0));
            platform.pos.set(new v2(0, 25));

            // Create robot parts
            torso = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(0.5f, 1f), 5f, 0.5f));
            torso.pos.set(new v2(0, 0));

            thigh = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(0.3f, 1f), 3f, 0.3f));
            thigh.pos.set(new v2(0, -1.5f));

            shin = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(0.2f, 1f), 2f, 0.2f));
            shin.pos.set(new v2(0, -2.5f));

            foot = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(0.4f, 0.2f), 1f, 0.1f));
            foot.pos.set(new v2(0, -3.5f));

            // Connect torso to platform via hip joint
            hipJoint = world.addJoint(new RevoluteJointDef(platform, torso, true)
                    .localB(0, 1f)
                    .enableMotor(true)
                    .maxMotorTorque(cfg.jointStrength.asFloat()));

            // Connect thigh to torso via knee joint
            kneeJoint = world.addJoint(new RevoluteJointDef(torso, thigh, true)
                    .localA(0, -1f)
                    .localB(0, 1f)
                    .enableMotor(true)
                    .maxMotorTorque(cfg.jointStrength.asFloat()));

            // Connect shin to thigh via ankle joint
            ankleJoint = world.addJoint(new RevoluteJointDef(thigh, shin, true)
                    .localA(0, -1f)
                    .localB(0, 1f)
                    .enableMotor(true)
                    .maxMotorTorque(cfg.jointStrength.asFloat()));

            // Connect foot to shin
            world.addJoint(new RevoluteJointDef(shin, foot, true)
                    .localA(0, -1f)
                    .localB(0, 0.2f)
                    .enableMotor(true)
                    .maxMotorTorque(cfg.jointStrength.asFloat()));

            // Define platform movement
            beforeFrame(() -> movePlatform());

            // Player actions to control joints
            action($.inh(id, "move_hip"), (float x) -> hipJoint.setMotorSpeed(Fuzzy.polarize(x)*cfg.maxForce.asFloat()));
            action($.inh(id, "move_knee"), (float x) -> kneeJoint.setMotorSpeed(Fuzzy.polarize(x)*cfg.maxForce.asFloat()));
            action($.inh(id, "move_ankle"), (float x) -> ankleJoint.setMotorSpeed(Fuzzy.polarize(x)*cfg.maxForce.asFloat()));

            // Sense robot's upright status
            sense($.inh(id, "torso_angle"), new FloatNormalized(() -> torso.getAngle() / (float) Math.PI));
            sense($.inh(id, "platform_angle"), new FloatNormalized(() -> platform.getAngle() / (float) Math.PI));

            // Define reward based on balance duration
            reward($.inh(id, "balance_time"), () -> 1f / (1f + Math.abs(torso.getAngle())));
        }

        private void movePlatform() {
            // Simple oscillating platform
            double time = world.time;
            float angle = (float) Math.sin(time) * 0.1f; // Small tilts
            platform.setTransform(platform.pos, angle);
        }

        public static void main(String[] args) {
            new Player(new BalanceBot("balance_bot").window()).fps(60f).start();
        }
    }

    /**
     * CartPole Game: Balance a pole on a moving cart.
     */
    static class CartPole extends Phys2DGame {

        private final float cartFriction = 0.01f;

        static class Config {
            public final FloatRange motorForce = new FloatRange(0.003f, 1e-5f, 0.005f);
        }

        private static final float cartWidth = 4f;
        private static final float cartHeight = 2f;
        private static final float poleLength = 6f;
        private static final float poleThickness = 0.5f;
        private static final float density = 1f;

        final RevoluteJoint poleJoint;
        final Body2D cart, pole;
        final Config cfg = new Config();

        public CartPole(String id) {
            super(id);

            world.setGravity(new v2(0, -10));

            // Create the ground (static)
            Body2D ground = world.addBody(new BodyDef(STATIC),
                    new FixtureDef(PolygonShape.box(50, 1), 0, 0));

            // Create the cart (dynamic)
            cart = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(cartWidth, cartHeight), density, cartFriction));

            // Position the cart above the ground
            cart.setTransform(new v2(0, cartHeight + 1), 0);

            // Create the pole (dynamic)
            pole = world.addBody(new BodyDef(DYNAMIC),
                    new FixtureDef(PolygonShape.box(poleThickness, poleLength), density, 0));

            // Position the pole on top of the cart
            pole.setTransform(new v2(0, cartHeight + poleLength), 0);

            // Create a revolute joint to connect the pole to the cart
            RevoluteJointDef jointDef = new RevoluteJointDef(cart, pole, true)
                    .localA(0, cartHeight)
                    .localB(0, -poleLength);
//            jointDef.enableLimit = true;
//            jointDef.lowerAngle = (float) (-PI / 12); // -15 degrees
//            jointDef.upperAngle = (float) (PI / 12);  // +15 degrees
            poleJoint = world.addJoint(jointDef);
        }

        @Override
        public Object config() {
            return cfg;
        }

        @Override
        protected void init() {
            float res = 0.01f;

            // Action: Apply force to the cart (left or right)
            action($.inh(id, "move"), (float x) -> {
                cart.applyForceToCenter(new v2(Fuzzy.polarize(x)*cfg.motorForce.asFloat(), 0));
            }).freqRes(res);

            // Sensors: Pole angle and cart position
            sense($.inh(id, "pole_angle"), new FloatNormalized(() -> {
                double angle = poleJoint.getJointAngle();
                // Normalize angle to [-0.5, +0.5] corresponding to [-PI/2, +PI/2]
                return (float) (angle / PI);
            }));

            sense($.inh(id, "cart_position"), new FloatNormalized(() -> {
                float pos = cart.pos.x;
                // Assuming the environment width is 100 units
                return pos / 50f; // Normalize to [-1, +1]
            }));

            // Reward: Penalize the absolute pole angle and cart position
            reward($.inh(id, "reward"), () -> {
                float angleNorm = Math.abs((float) (poleJoint.getJointAngle() / (PI / 2)));
                float posNorm = Math.abs(cart.pos.x / 50f);
                // Combine penalties
                return 1 - (angleNorm + posNorm) * 0.5f;
            });

//            // Termination condition: pole angle exceeds limits or cart goes out of bounds
//            terminate(() -> {
//                float angle = (float) poleJoint.getJointAngle();
//                float pos = cart.pos.x;
//                return Math.abs(angle) > PI / 12 || Math.abs(pos) > 50f;
//            });

            // Optionally, visualize additional elements
        }

        public static void main(String[] args) {
            new Player(new CartPole("cartpole").window()).fps(60f).start();
        }
    }

}

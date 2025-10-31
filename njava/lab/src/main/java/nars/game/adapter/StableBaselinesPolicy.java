package nars.game.adapter;

import jcog.TODO;
import jcog.agent.Policy;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Random;

/**
 *  adapter to a pytorch StableBaselines agent
 *  https://github.com/DLR-RM/stable-baselines3
 *  TODO
 */
public class StableBaselinesPolicy implements Policy {

    static {
        /* TODO run pip to install stablebaselines dependencies if library is missing */
    }

    final int inputs, actions;

    private boolean needsClear, initialized;
    @Nullable private AIGymGame.PyShell shell;

    public StableBaselinesPolicy(int inputs, int actions) {
        this.inputs = inputs;
        this.actions = actions;
    }

    @Override
    public void clear(Random rng) {
        needsClear = true;
    }

    public String program() {
        return String.format("""
import gym

from stable_baselines3 import PPO

%1$s

env = gym.make(MyEnv())

model = PPO("MlpPolicy", env, verbose=1)

vec_env = model.get_env()
obs = vec_env.reset()
for i in range(1000):
    model.learn(total_timesteps=1)
    action, _states = model.predict(obs, deterministic=True)
    obs, reward, done, info = vec_env.step(action)
    #vec_env.render()
    # VecEnv resets automatically
    # if done:
    #   obs = env.reset()

env.close()       
        """, env());
    }

    /** generates a gym environment */
    public String env() {
        return String.format("""

class MyEnv(gym.Env):              
  def __init__(self):
    super(MyEnv, self).__init__()

    # The observation will be the coordinate of the agent
    # this can be described both by Discrete and Box space
    self.observation_space = spaces.Box(low=0, high=1,shape=(%1$d,), dtype=np.float32)

    n_actions = 2
    #self.action_space = spaces.Discrete(%2$s)
    self.action_space = spaces.Box(low=0, high=1,shape=(%2$d,), dtype=np.float32)

  def reset(self):      
    # here we convert to float32 to make it more general (in case we want to use continuous actions)
    return self.obs().astype(np.float32)

  def step(self, action):

    reward = 1

    # Optionally we can pass additional info, we are not using that for now
    info = {}

    return self.obs().astype(np.float32), reward, done, info

  def obs(self):
    # TODO fetch next observation
    return self.x
    
  def close(self):
    pass              
    
        """, inputs, actions);
    }

    @Override
    public double[] learn(@Nullable double[] xPrev, double[] actionPrev, double reward, double[] x, float pri) {
        if (needsClear) {
            if (shell!=null) {
                shell.close();
                shell = null;
            }
        }
        if (shell == null) {
            try {
                shell = init();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new TODO();
    }

    private AIGymGame.PyShell init() throws IOException {
        AIGymGame.PyShell s = new AIGymGame.PyShell();
        String p = program();
        s.input(p);
        return s;
    }

}

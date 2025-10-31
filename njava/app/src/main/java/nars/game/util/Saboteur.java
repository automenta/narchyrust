package nars.game.util;

import jcog.Is;

/**
 * learns how to subconsciously undermine game play, in order to strengthen the player.
 * it can be modeled as a player of a virtual anti-game,
 * in which case its reward is the negation of the game's reward (zero-sum).
 * its actions may include:
 *      affecting game's hidden parameters
 *      affecting player's runtime parameters (those not under direct control of the player itself)
 *
 * qualities of its learned policies may be invertible for beneficial effect in future play. */

@Is({"Generative_adversarial_network", "Actor_critic_model", "False_flag", "Adaptive_immune_system", "Fuzzing"})
public class Saboteur {

    //TODO

}
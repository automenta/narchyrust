package nars.game.action.util;

import nars.game.Game;

import java.util.function.Consumer;

/** TODO expand to integrate with multiple games, as a supervisor for multiple games, if
 *      necessary
 *      */
public interface ActionReinforcement extends Consumer<Game> {
}
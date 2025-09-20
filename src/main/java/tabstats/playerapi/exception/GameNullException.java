package tabstats.playerapi.exception;

import tabstats.playerapi.api.games.HypixelGames;

public class GameNullException extends Exception {

    public GameNullException(HypixelGames game) {
        System.out.println(game.getGameName() + " data returned as null");
    }
}

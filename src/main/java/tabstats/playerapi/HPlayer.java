package tabstats.playerapi;

import tabstats.playerapi.api.games.HGameBase;
import tabstats.playerapi.api.stats.Stat;
import tabstats.util.ChatColor;
import tabstats.util.NickDetector;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* Hypixel Player */
public class HPlayer {
    private HashMap<String, HGameBase> gameMap;
    private String playerUUID, playerName, nickName, playerRank;
    private boolean nicked;

    /**
     * @param playerUUID Player's UUID
     * @param playerName Player's Name
     * @param gameBase All HGameBase's you would like the HPlayer to contain
     * (Generally you would like all HGameBases which are complete to be added)
     */
    public HPlayer(String playerUUID, String playerName, HGameBase... gameBase) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;

        this.gameMap = new HashMap<>();
        for (HGameBase game : gameBase) {
            this.gameMap.put(game.getGame().getGameName(), game);
        }
    }

    /* Meant for nicked players */
    public HPlayer(String playerUUID, String playerName, String nickName, HGameBase... gameBase) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.nickName = nickName;
        this.nicked = true;

        this.gameMap = new HashMap<>();
        for (HGameBase game : gameBase) {
            this.gameMap.put(game.getGame().getGameName(), game);
        }
    }

    /* Meant for null api players */
    /* I use this to remove boilerplate as well */
    public HPlayer(String playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.playerRank = ChatColor.GRAY.toString();

        this.gameMap = new HashMap<>();
    }

    public void addGames(HGameBase... gameBases) {
        if (this.gameMap == null) {
            this.gameMap = new HashMap<>();
        }

        for (HGameBase game : gameBases) {
            this.gameMap.put(game.getGame().getGameName(), game);
        }
    }

    public String getPlayerUUID() {
        return this.playerUUID;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public String getNickname() {
        return "Nickname Test";
//        return nickName == null ? playerName : nickName;
    }

    public List<Stat> getFormattedGameStats(String gameName) {
        return this.gameMap.get(gameName) == null ? new ArrayList<>() : this.gameMap.get(gameName).getFormattedStatList();
    }

    public void setNicked(boolean nicked) { this.nicked = nicked; }

    public boolean isNicked() { 
        // Use the precomputed nick flag set during player processing (UUID v1 + known skin)
        return this.nicked;
    }

    public void setPlayerRank(String playerRank) {
        this.playerRank = playerRank;
    }

    public void setPlayerRank(JsonObject playerObject) {
        String s = ChatColor.GRAY.toString();  // Default to gray for non-ranked players
        String staff = "NOT STAFF", rank = "", rankColour = "RED", mvpPlusPlus = "NEVER BROUGHT";
        JsonObject player = playerObject.getAsJsonObject();

        // Get staff rank
        try {
            staff = player.get("rank").getAsString();
        } catch (Exception ignored) {
            staff = "NOT STAFF";
        }
        
        // Get monthly package rank (MVP++)
        try {
            mvpPlusPlus = player.get("monthlyPackageRank").getAsString();
        } catch (Exception ignored) {
            mvpPlusPlus = "NEVER BROUGHT";
        }
        
        // Get regular package rank (VIP, MVP, etc.)
        try {
            rank = player.get("newPackageRank").getAsString();
        } catch (Exception ignored) {
            rank = "";  // No rank for non-ranked players
        }
        
        // Get rank color
        try {
            rankColour = player.get("rankPlusColor").getAsString();
        } catch (Exception ignored) {
            rankColour = "RED";
        }
        
        // Check for staff ranks first (highest priority)
        if (staff.equalsIgnoreCase("HELPER")) {
            s = ChatColor.BLUE + "[HELPER] ";
        } else if (staff.equalsIgnoreCase("MODERATOR")) {
            s = ChatColor.DARK_GREEN + "[MODERATOR] ";
        } else if (staff.equalsIgnoreCase("ADMIN")) {
            s = ChatColor.RED + "[ADMIN] ";
        } else if (staff.equalsIgnoreCase("YOUTUBER")) {
            s = ChatColor.RED + "[" + ChatColor.WHITE + "YOUTUBE" + ChatColor.RED + "] ";
        }
        // Check for MVP++ (superstar)
        else if (mvpPlusPlus.equalsIgnoreCase("SUPERSTAR")) {
            s = ChatColor.GOLD + "[MVP" + ChatColor.valueOf(rankColour) + "++" + ChatColor.GOLD + "] ";
        }
        // Check for other ranks
        else if (rank.equalsIgnoreCase("MVP_PLUS")) {
            s = ChatColor.AQUA + "[MVP" + ChatColor.valueOf(rankColour) + "+" + ChatColor.AQUA + "] ";
        } else if (rank.equalsIgnoreCase("MVP")) {
            s = ChatColor.AQUA + "[MVP] ";
        } else if (rank.equalsIgnoreCase("VIP_PLUS")) {
            s = ChatColor.GREEN + "[VIP" + ChatColor.GOLD + "+" + ChatColor.GREEN + "] ";
        } else if (rank.equalsIgnoreCase("VIP")) {
            s = ChatColor.GREEN + "[VIP] ";
        }
        // If no rank matches, keep default gray color (s = "§7")
        
        this.playerRank = s;
    }

    public String getPlayerRank() {
        String baseRank = this.playerRank == null || this.playerRank.isEmpty() ? ChatColor.GRAY.toString() : this.playerRank;
        
        // If player is detected as nicked, prepend nick indicator
        if (isNicked()) {
            return ChatColor.WHITE + "[" + ChatColor.RED + "NICKED" + ChatColor.WHITE + "] " + baseRank;
        }
        
        return baseRank;
    }

    public String getPlayerRankColor() {
        return this.playerRank == null || this.playerRank.isEmpty() ? ChatColor.GRAY.toString() : this.playerRank.substring(0, 2);
    }

    public HGameBase getGame(String gameName) {
        return this.gameMap.get(gameName);
    }
}

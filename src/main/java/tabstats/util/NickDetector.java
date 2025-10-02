package tabstats.util;

/**
 * Nick detection: as of Oct 2025 every UUID v1 on Hypixel is a nicked player.
 * Only the UUID version nibble is inspected.
 */
public class NickDetector {

    /**
     * Return true if the supplied UUID string is version 1 (time-based) which Hypixel now reserves
     * exclusively for nicked players.
     * Supports both canonical UUID (xxxxxxxx-xxxx-1xxx-xxxx-xxxxxxxxxxxx) and the 32-char compact form.
     */
    public static boolean isNickedUuid(String uuid) {
        if (uuid == null) return false;
        String u = uuid;
        int idx;
        if (u.indexOf('-') != -1) {
            if (u.length() < 15) return false; // malformed
            idx = 14; // version nibble
        } else {
            if (u.length() < 13) return false; // malformed
            idx = 12; // version nibble in compact form
        }
        return u.charAt(idx) == '1';
    }
}
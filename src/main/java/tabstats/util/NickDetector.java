package tabstats.util;

/**
 * Simplified nick detection: as of Oct 2025 every UUID v1 on Hypixel represents a nicked player.
 * We therefore only need to test the UUID version nibble. All previous skin-hash based heuristics
 * have been removed to save memory and CPU.
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
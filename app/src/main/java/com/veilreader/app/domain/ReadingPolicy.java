package com.veilreader.app.domain;

import java.util.LinkedHashSet;
import java.util.Set;

/** Dependency-free reading rules shared by Android and the executable regression suite. */
public final class ReadingPolicy {
    private ReadingPolicy() {}
    public static double fontSizePercent(double scale) {
        if (!Double.isFinite(scale)) return 100.0;
        return Math.max(.75, Math.min(1.8, scale)) * 100.0;
    }
    public static int ritualTarget(String path, int rank) {
        int r = Math.max(0, Math.min(5, rank));
        switch (path) {
            case "dreamwalker": return 30 + r * 15;
            case "vanguard": return 30 + r * 15;
            case "nocturne": return 20 + r * 10;
            case "archivist": case "artificer": return 3 + r;
            default: return 5 + r * 2;
        }
    }
    public static String ritualDescription(String path, int rank) {
        int n = ritualTarget(path, rank);
        switch (path) {
            case "dreamwalker": return "Explore your worlds for " + n + " active reading minutes.";
            case "vanguard": return "Read " + n + " paced pages. Fast tapping does not count.";
            case "nocturne": return "Read for " + n + " minutes between 8 PM and 6 AM, in your device's local time.";
            case "archivist": return "Write " + n + " new passage notes of at least 40 characters.";
            case "artificer": return "Capture " + n + " new concept notes of at least 40 characters.";
            default: return "Highlight " + n + " new passages that reveal clues, themes or hidden structure.";
        }
    }
    public static boolean acceptsEvent(String path, String event) {
        switch (path) {
            case "dreamwalker": return event.equals("minute");
            case "vanguard": return event.equals("page");
            case "nocturne": return event.equals("nightMinute");
            case "archivist": case "artificer": return event.equals("note");
            default: return event.equals("highlight");
        }
    }
    public static boolean isNight(int hour) { return hour >= 0 && hour < 24 && (hour >= 20 || hour < 6); }
    /** Formatting, whitespace and standalone combining marks are not note content. */
    public static boolean qualifiesNote(String note) {
        if (note == null) return false;
        int visible = 0;
        for (int offset = 0; offset < note.length();) {
            int codePoint = note.codePointAt(offset);
            offset += Character.charCount(codePoint);
            int type = Character.getType(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
                || Character.isISOControl(codePoint) || type == Character.FORMAT
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK) continue;
            if (++visible >= 40) return true;
        }
        return false;
    }

    /** Per-session uniqueness plus dwell time, with a daily budget tied to reading minutes. */
    public static final class PageGate {
        private static final long MIN_INTERVAL_MS = 8_000L;
        private static final int MAX_SEEN_LOCATIONS = 2_048;
        private static final long MAX_PAGES_PER_READING_MINUTE = 6L;
        private static final long DAILY_PAGE_HEADROOM = 6L;

        private final Set<String> seen = new LinkedHashSet<>();
        private long lastSeenAt = -1;
        public boolean visit(String key, long elapsedMs, int todayPages, int todayMinutes) {
            if (key == null || key.isEmpty() || elapsedMs < 0) return false;
            boolean first = lastSeenAt < 0;
            long dwell = first ? 0 : elapsedMs - lastSeenAt;
            boolean unique = seen.add(key);
            lastSeenAt = elapsedMs;
            if (seen.size() > MAX_SEEN_LOCATIONS) seen.remove(seen.iterator().next());
            long budget = (long) Math.max(0, todayMinutes) * MAX_PAGES_PER_READING_MINUTE
                + DAILY_PAGE_HEADROOM;
            return !first && unique && dwell >= MIN_INTERVAL_MS && todayPages < budget;
        }
        /** Re-establishes the dwell-time baseline after resume or an intentional reader reset. */
        public void rebase(long elapsedMs) { lastSeenAt = elapsedMs >= 0 ? elapsedMs : -1; }
        public void pause() { lastSeenAt = -1; }
    }
}

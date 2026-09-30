import com.veilreader.app.domain.ReadingPolicy;

public final class ReadingPolicyTest {
    private static int checks;
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        check(ReadingPolicy.ritualTarget("oracle", 0) == 5, "oracle starting target");
        check(ReadingPolicy.ritualTarget("oracle", 5) == 15, "oracle scaled target");
        check(ReadingPolicy.ritualTarget("vanguard", -1) == 30, "negative rank clamps");
        check(ReadingPolicy.ritualTarget("dreamwalker", 99) == 105, "rank upper clamp");
        check(ReadingPolicy.acceptsEvent("oracle", "highlight"), "oracle highlights");
        check(!ReadingPolicy.acceptsEvent("oracle", "minute"), "oracle rejects minutes");
        check(ReadingPolicy.acceptsEvent("archivist", "note"), "archivist notes");
        check(!ReadingPolicy.acceptsEvent("artificer", "highlight"), "artificer rejects highlights");
        check(ReadingPolicy.acceptsEvent("dreamwalker", "minute"), "dreamwalker minutes");
        check(ReadingPolicy.acceptsEvent("vanguard", "page"), "vanguard pages");
        check(ReadingPolicy.acceptsEvent("nocturne", "nightMinute"), "nocturne night");
        check(!ReadingPolicy.acceptsEvent("nocturne", "minute"), "nocturne rejects daytime");
        check(!ReadingPolicy.isNight(19) && ReadingPolicy.isNight(20), "night starts at 20");
        check(ReadingPolicy.isNight(5) && !ReadingPolicy.isNight(6), "night ends at 6");
        check(!ReadingPolicy.isNight(-1) && !ReadingPolicy.isNight(24), "invalid hours");
        check(!ReadingPolicy.qualifiesNote(" ".repeat(100)), "blank padding rejected");
        check(!ReadingPolicy.qualifiesNote("a".repeat(39)), "short note rejected");
        check(ReadingPolicy.qualifiesNote("a".repeat(40)), "40 character note accepted");
        check(!ReadingPolicy.qualifiesNote("\u200c".repeat(80)), "invisible half-spaces earn nothing");
        check(!ReadingPolicy.qualifiesNote("\u00a0".repeat(80)), "Unicode space padding earns nothing");
        check(!ReadingPolicy.qualifiesNote("a".repeat(39) + "\u200c".repeat(80)), "format marks cannot complete a short note");
        check(!ReadingPolicy.qualifiesNote("a".repeat(20) + " ".repeat(80) + "a".repeat(19)), "interior spacing cannot complete a short note");
        check(!ReadingPolicy.qualifiesNote("\u0301".repeat(80)), "combining marks alone earn nothing");
        check(!ReadingPolicy.qualifiesNote("\ud83d\ude00".repeat(20)), "surrogate pairs count as one visible character");
        check(ReadingPolicy.qualifiesNote("\u0628".repeat(40)), "Persian letters qualify");
        check(ReadingPolicy.fontSizePercent(1.0) == 100.0, "default font is 100 percent");
        check(ReadingPolicy.fontSizePercent(.1) == 75.0, "small font clamps");
        check(ReadingPolicy.fontSizePercent(3.0) == 180.0, "large font clamps");
        check(ReadingPolicy.fontSizePercent(Double.NaN) == 100.0, "NaN font recovers");
        check(ReadingPolicy.fontSizePercent(Double.POSITIVE_INFINITY) == 100.0, "infinite font recovers");
        ReadingPolicy.PageGate gate = new ReadingPolicy.PageGate();
        check(!gate.visit("a", 0, 0, 0), "opening does not earn page XP");
        check(!gate.visit("b", 100, 0, 0), "fast tap rejected");
        check(gate.visit("c", 8100, 0, 0), "paced turn accepted");
        check(!gate.visit("a", 16100, 1, 0), "revisit rejected");
        check(!gate.visit("d", 24100, 6, 0), "daily page budget enforced");
        check(gate.visit("e", 32100, 6, 1), "reading minute expands budget");
        gate.pause();
        check(!gate.visit("f", 90000, 7, 1), "background dwell earns nothing");
        check(!gate.visit("g", 89999, 7, 1), "clock rollback rejected");
        check(!gate.visit("", 100000, 7, 1), "empty key rejected");
        check(!gate.visit(null, 100000, 7, 1), "null key rejected");
        for (String path : new String[]{"oracle", "dreamwalker", "archivist", "vanguard", "nocturne", "artificer"}) {
            check(!ReadingPolicy.ritualDescription(path, 1).isBlank(), "description for " + path);
        }
        System.out.println("PASS: " + checks + " reading-policy checks");
    }
}

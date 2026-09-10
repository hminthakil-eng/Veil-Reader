import com.veilreader.app.domain.GamificationEngine

fun main() {
    check(GamificationEngine.readingXp(10, 10) == 50)
    check(GamificationEngine.levelFor(349) == 1)
    check(GamificationEngine.levelFor(350) == 2)
    check(GamificationEngine.progressInsideLevel(350).first == 0)
    println("Gamification smoke tests passed")
}

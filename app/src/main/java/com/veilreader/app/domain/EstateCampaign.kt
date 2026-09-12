package com.veilreader.app.domain

/** Authored offline campaign. Currency comes only from engaged reading and one-time chapters. */
data class EstateState(
    val stage: Int = 0,
    val spent: Int = 0,
    val claimedChapters: Set<String> = emptySet(),
    val name: String = "The Lantern House",
    val palette: EstatePalette = EstatePalette.MOONSTONE,
    val grounds: EstateGrounds = EstateGrounds.WILDFLOWERS,
    val sky: EstateSky = EstateSky.MOONLIT
)

enum class EstatePalette(val label: String) { MOONSTONE("Moonstone"), AMBER("Amber oak"), WISTERIA("Wisteria") }
enum class EstateGrounds(val label: String) { WILDFLOWERS("Wildflowers"), LANTERNS("Lantern walk"), FOUNTAIN("Courtyard fountain") }
enum class EstateSky(val label: String) { MOONLIT("Moonlight"), DAWN("Daybreak"), MIST("Silver mist") }

data class EstateStage(val name: String, val minutes: Int, val cost: Int, val description: String)
data class StoryChapter(
    val id: String, val title: String, val npcId: String,
    val minutes: Int, val stage: Int, val reward: Int,
    val request: String, val loreTitle: String, val lore: String
)
data class EstateNpc(val id: String, val name: String, val role: String, val stage: Int, val greeting: String)

object EstateCampaign {
    val stages = listOf(
        EstateStage("Wayside shack", 0, 0, "A leaking roof. A borrowed lamp. The beginning of a home."),
        EstateStage("Lamplit cottage", 15, 15, "A sound roof, a warm window, and room for another chair."),
        EstateStage("Reader’s lodge", 60, 45, "A second storey rises. Travelers begin to leave books at your door."),
        EstateStage("Wisteria manor", 150, 90, "An open courtyard and a library with room for other lives."),
        EstateStage("Moonwatch keep", 300, 150, "Your first tower watches over the lost road."),
        EstateStage("Lantern castle", 600, 300, "Two towers, an arched gate, and a light the valley can follow."),
        EstateStage("Citadel of stories", 1200, 600, "A sanctuary made from the hours you chose to give to stories.")
    )

    val residents = listOf(
        EstateNpc("mara", "Mara", "The lantern keeper", 0,
            "You can stay. The roof complains, but the lamp still works. Read a little; I’ll keep the rain away from your chair."),
        EstateNpc("oren", "Oren", "The wandering builder", 1,
            "I don’t build monuments to hurry. I build places people want to return to. Shall we make this one yours?"),
        EstateNpc("sable", "Sable", "The mapmaker", 2,
            "The road has been missing from every map for years. Yet here you are, and here is your house. Curious."),
        EstateNpc("iona", "Iona", "The keeper of seeds", 3,
            "A garden needs somewhere to be quiet. Your courtyard will do. Tell me what you’ve been reading while I work."),
        EstateNpc("vesper", "Vesper", "The last archivist", 4,
            "The castle was never lost. It was waiting for someone who would leave its doors open.")
    )

    val chapters = listOf(
        StoryChapter("first_light", "A light in the rain", "mara", 5, 0, 5,
            "Spend five active minutes with any book. Mara has found a dry place to sit.",
            "The Borrowed Lamp", "Before the valley forgot its name, every house kept a lamp in its window. Mara carries the last one. She says it was lent to her, and refuses to say by whom. Tonight she leaves it beside your book."),
        StoryChapter("sound_roof", "A roof that remembers", "mara", 15, 0, 10,
            "Reach fifteen reading minutes. Every quiet return makes this place less abandoned.",
            "A Mark in the Timber", "Beneath a loose roof tile is the outline of a doorway, carved by a child. The same mark appears on the lamp. Mara turns the tile over, carefully. ‘We will need a builder,’ she says."),
        StoryChapter("builder", "One more chair", "oren", 30, 1, 15,
            "Build the cottage and reach thirty reading minutes. Oren is waiting at the gate.",
            "The Empty Chair", "Oren arrives carrying two chairs and no tools. One chair is for you; the other, he explains, is for whoever comes next. His tools have been hidden under your floorboards all along."),
        StoryChapter("lost_road", "The road between lines", "sable", 60, 2, 20,
            "Build the lodge and reach one hour of reading. A mapmaker notices your light.",
            "The Unmapped Road", "Sable’s map shows every house in the valley except yours. When she draws your lodge, a pale road appears in the paper by itself. It ends at a gate labeled: ‘Open from the inside.’"),
        StoryChapter("letters", "Letters with no address", "sable", 120, 2, 25,
            "Reach two reading hours. There is no deadline; Sable will keep the letters safe.",
            "The Post Without a Town", "The letters are addressed to people who have not arrived yet: a gardener, an archivist, and someone called the Returning Reader. None asks for rescue. All ask whether the house has room."),
        StoryChapter("garden", "What the courtyard keeps", "iona", 180, 3, 30,
            "Build the manor and reach three reading hours. Iona brings a pocketful of seeds.",
            "The Patient Garden", "Iona plants seeds in the cracks, not the flowerbeds. ‘Those are the places the house tried to mend itself,’ she says. By morning, small white flowers trace the outlines of rooms that once stood here."),
        StoryChapter("tower", "The bell with no voice", "vesper", 300, 4, 40,
            "Build the keep and reach five reading hours. A silent bell hangs above the road.",
            "The Silence Agreement", "Vesper brings a bell without a clapper. The old keep promised to shelter anyone who wished to read undisturbed. Its silent bell announced that promise. No alarm, no summons. Only a place to return."),
        StoryChapter("names", "A valley of names", "vesper", 480, 4, 50,
            "Reach eight reading hours. Vesper has opened the first cabinet.",
            "The Register of Guests", "The register lists no kings or battles, only the books visitors carried and where they liked to sit. The margins hold jokes, recipes, apologies. A kingdom, Vesper suggests, may be made of smaller things."),
        StoryChapter("open_gate", "Open from the inside", "mara", 720, 5, 60,
            "Build the castle and reach twelve reading hours. You have the key already.",
            "The Returning Reader", "The gate has no lock. On the inside is a message in Mara’s hand: ‘Build nothing that makes you afraid to leave.’ The valley did not need a ruler. It needed a reader willing to come home."),
        StoryChapter("beacon", "A thousand quiet lights", "vesper", 1200, 6, 100,
            "Raise the citadel and reach twenty reading hours. The valley is ready to remember.",
            "A House, Still", "Lights appear along the road, one window at a time. Your citadel is not the end of the story. It is the place where other stories begin. Mara takes her old chair by the window. The lamp, she tells you, is yours now.")
    )

    fun chapterReady(state: EstateState, minutes: Int, chapter: StoryChapter): Boolean {
        val index = chapters.indexOfFirst { it.id == chapter.id }
        return index >= 0 && chapter.id !in state.claimedChapters &&
            minutes >= chapter.minutes && state.stage >= chapter.stage &&
            (index == 0 || chapters[index - 1].id in state.claimedChapters)
    }

    fun balance(state: EstateState, minutes: Int): Int =
        (minutes.coerceAtLeast(0).toLong() + chapters.filter { it.id in state.claimedChapters }.sumOf { it.reward } -
            state.spent.coerceAtLeast(0)).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    fun nextStage(state: EstateState): EstateStage? = stages.getOrNull(state.stage + 1)

    fun build(state: EstateState, minutes: Int): EstateState? {
        val next = nextStage(state) ?: return null
        if (minutes < next.minutes || balance(state, minutes) < next.cost) return null
        return state.copy(stage = state.stage + 1, spent = state.spent + next.cost)
    }

    fun claim(state: EstateState, minutes: Int, id: String): EstateState? {
        val chapter = chapters.firstOrNull { it.id == id } ?: return null
        if (!chapterReady(state, minutes, chapter)) return null
        return state.copy(claimedChapters = state.claimedChapters + id)
    }

    fun groundsUnlocked(grounds: EstateGrounds, stage: Int): Boolean = when (grounds) {
        EstateGrounds.WILDFLOWERS -> true
        EstateGrounds.LANTERNS -> stage >= 1
        EstateGrounds.FOUNTAIN -> stage >= 3
    }

    fun dialogue(npc: EstateNpc, state: EstateState, subject: String): String = when (subject) {
        "home" -> when (npc.id) {
            "oren" -> "${stages[state.stage].name}, built one return at a time. Your materials follow your active reading minutes. Change the stone, the sky, or the grounds whenever you like; decoration costs nothing."
            "iona" -> "Leave it for a week, a month, a year. The garden will wait. Nothing you have built will be taken away."
            "sable" -> "I have marked ${state.name} on my map. I left space around it for the places you haven’t imagined yet."
            "vesper" -> "A home has no final form. Even a citadel must keep a small, comfortable place to read."
            else -> "${state.name}. It has a name now. That matters more than the size of its walls."
        }
        "mystery" -> chapters.lastOrNull { it.npcId == npc.id && it.id in state.claimedChapters }?.lore
            ?: "There is more to tell, but it belongs to a chapter you haven’t reached. For now: ${npc.greeting}"
        else -> npc.greeting
    }
}

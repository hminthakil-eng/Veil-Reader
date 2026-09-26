package com.veilreader.app.data

import com.veilreader.app.domain.*

object SampleData {
    val paths = listOf(
        ReadingPath(
            id = "oracle",
            name = "The Oracle Path",
            epithet = "Seek what hides between the lines",
            description = "For mystery, deduction, layered worlds, and readers who live for clues.",
            ranks = listOf("Observer", "Cipher", "Investigator", "Augur", "Oracle", "Veilseer")
        ),
        ReadingPath(
            id = "dreamwalker",
            name = "The Dreamwalker Path",
            epithet = "Cross worlds no map can hold",
            description = "For fantasy, myth, wonder, and impossible journeys.",
            ranks = listOf("Wanderer", "Wayfarer", "Storybinder", "Dreamwalker", "Worldweaver", "Mythborne")
        ),
        ReadingPath(
            id = "archivist",
            name = "The Archivist Path",
            epithet = "Nothing learned is ever lost",
            description = "For history, science, philosophy, and knowledge-heavy reading.",
            ranks = listOf("Novice", "Scribe", "Scholar", "Curator", "Archivist", "Keeper")
        ),
        ReadingPath(
            id = "vanguard",
            name = "The Vanguard Path",
            epithet = "Turn every page like a battlefield",
            description = "For action, adventure, progression fantasy, and relentless momentum.",
            ranks = listOf("Recruit", "Strider", "Vanguard", "Champion", "Paragon", "Legend")
        ),
        ReadingPath(
            id = "nocturne",
            name = "The Nocturne Path",
            epithet = "Read where the lantern fades",
            description = "For horror, gothic fiction, dark fantasy, and unsettling mysteries.",
            ranks = listOf("Listener", "Watcher", "Shade", "Nocturne", "Nightwarden", "Eclipse")
        ),
        ReadingPath(
            id = "artificer",
            name = "The Artificer Path",
            epithet = "Understand the machinery beneath the miracle",
            description = "For technology, engineering, hard science fiction, and systems thinkers.",
            ranks = listOf("Tinkerer", "Analyst", "Maker", "Artificer", "Architect", "Prime Mechanist")
        )
    )

    val currentPath = paths.first()

    val profile = ReaderProfile(
        level = 17,
        xp = 2460,
        xpForNextLevel = 2850,
        streakDays = 23,
        pagesRead = 12_840,
        minutesRead = 8_920,
        booksFinished = 37,
        path = currentPath,
        rankIndex = 2,
        ritualProgress = 3,
        ritualTarget = 5
    )

    val books = listOf(
        Book("b1", "The City Beneath Glass", "Mira Vale", .68f, "Chapter 19 · The Bell", 612, 416),
        Book("b2", "A Memory of Smoke", "R. S. Hale", .31f, "Chapter 8 · Ash Letters", 488, 151),
        Book("b3", "The Starless Archive", "N. Orin", .92f, "Chapter 42 · The Final Door", 734, 675),
        Book("b4", "Clockwork Saints", "J. Corven", .12f, "Chapter 4 · Brass Rain", 526, 63),
        Book("b5", "Axiom Garden", "Lena Sor", .47f, "Chapter 15 · Recursive Dawn", 402, 189),
        Book("b6", "The Hollow Crown", "T. Arden", .04f, "Chapter 1 · Invitation", 815, 33)
    )

    val rooms = listOf(
        CastleRoom("library", "Grand Library", "Your books, collections, finished tomes, and reading history.", 0, "📚"),
        CastleRoom("ritual", "Ritual Chamber", "Perform advancement challenges when your Path is ready.", 1, "🕯️"),
        CastleRoom("observatory", "Observatory", "A private Memory Atlas of recorded relations between your books and preserved passages.", 2, "🔭"),
        CastleRoom("archive", "Hidden Archive", "Highlights, notes, entities, maps, and personal lore.", 3, "🗝️"),
        CastleRoom("treasury", "Treasury", "Themes, profile frames, sigils, page effects, and collectibles.", 4, "💎"),
        CastleRoom("sanctum", "Inner Sanctum", "Endgame challenges and the deepest Castle customizations.", 5, "👁️")
    )

    val quests = listOf(
        Quest("q1", "Read for 20 minutes", 14, 20, 90),
        Quest("q2", "Finish 2 chapters", 1, 2, 120),
        Quest("q3", "Mark 3 intriguing passages", 2, 3, 75)
    )
}

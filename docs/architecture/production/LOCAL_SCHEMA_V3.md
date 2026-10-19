# Local database — observed Room schema v3

This is the existing device database, not a proposed cloud migration.

Source commit: `30998d1945dda6a9afd639f71fdf42436f36e52a`.
Schema file: `app/schemas/com.veilreader.app.data.db.VeilDatabase/3.json`; SHA-256: `646c51ba85b3346ac2aa9eef01d47ca5d8a7d9817f2ba69116ad4d8fb706344d`.

Room identity hash: `8708cb0ba06535d4adb6a412c09321d0`. Tables: 15.

Reader preferences remain in DataStore; local files and regenerable caches are not SQL blobs.

## books

```sql
CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT NOT NULL, `progress` REAL NOT NULL, `currentChapter` TEXT NOT NULL, `totalPages` INTEGER NOT NULL, `pagesRead` INTEGER NOT NULL, `format` TEXT NOT NULL, `sourceUri` TEXT, `mediaType` TEXT, `locatorJson` TEXT, `addedAtEpochMs` INTEGER NOT NULL, `lastOpenedAtEpochMs` INTEGER NOT NULL, `finished` INTEGER NOT NULL, `favorite` INTEGER NOT NULL, `coverCachePath` TEXT, `contentFingerprint` TEXT, `seriesName` TEXT, `seriesIndex` REAL, `language` TEXT, PRIMARY KEY(`id`));
```

## highlights

```sql
CREATE TABLE IF NOT EXISTS `highlights` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `quote` TEXT NOT NULL, `locatorJson` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `colorArgb` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_highlights_bookId` ON `highlights` (`bookId`);
```

## bookmarks

```sql
CREATE TABLE IF NOT EXISTS `bookmarks` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `label` TEXT NOT NULL, `locatorJson` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_bookmarks_bookId` ON `bookmarks` (`bookId`);
```

## collections

```sql
CREATE TABLE IF NOT EXISTS `collections` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `normalizedName` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE UNIQUE INDEX IF NOT EXISTS `index_collections_normalizedName` ON `collections` (`normalizedName`);
```

## book_collection

```sql
CREATE TABLE IF NOT EXISTS `book_collection` (`bookId` TEXT NOT NULL, `collectionId` TEXT NOT NULL, PRIMARY KEY(`bookId`, `collectionId`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_book_collection_collectionId` ON `book_collection` (`collectionId`);
```

## reading_sessions

```sql
CREATE TABLE IF NOT EXISTS `reading_sessions` (`id` TEXT NOT NULL, `bookId` TEXT, `startedAtEpochMs` INTEGER NOT NULL, `endedAtEpochMs` INTEGER, `activeMillis` INTEGER NOT NULL, `pacedPageTurns` INTEGER NOT NULL, `highlightCount` INTEGER NOT NULL, `noteCount` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL );
CREATE INDEX IF NOT EXISTS `index_reading_sessions_bookId` ON `reading_sessions` (`bookId`);
CREATE INDEX IF NOT EXISTS `index_reading_sessions_startedAtEpochMs` ON `reading_sessions` (`startedAtEpochMs`);
```

## reading_cycles

```sql
CREATE TABLE IF NOT EXISTS `reading_cycles` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `cycleIndex` INTEGER NOT NULL, `titleSnapshot` TEXT NOT NULL, `authorSnapshot` TEXT NOT NULL, `startedAtEpochMs` INTEGER, `completedAtEpochMs` INTEGER NOT NULL, `finalLocatorJson` TEXT NOT NULL, `sessionCount` INTEGER NOT NULL, `totalActiveMillis` INTEGER NOT NULL, `pacedPageTurns` INTEGER NOT NULL, `highlightCount` INTEGER NOT NULL, `noteCount` INTEGER NOT NULL, `bookmarkCount` INTEGER NOT NULL, `sealCode` TEXT NOT NULL, `timelineJson` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_reading_cycles_bookId` ON `reading_cycles` (`bookId`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_reading_cycles_bookId_cycleIndex` ON `reading_cycles` (`bookId`, `cycleIndex`);
CREATE INDEX IF NOT EXISTS `index_reading_cycles_completedAtEpochMs` ON `reading_cycles` (`completedAtEpochMs`);
```

## passage_visits

```sql
CREATE TABLE IF NOT EXISTS `passage_visits` (`id` TEXT NOT NULL, `highlightId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `locatorJson` TEXT NOT NULL, `viewedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`highlightId`) REFERENCES `highlights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_passage_visits_highlightId` ON `passage_visits` (`highlightId`);
CREATE INDEX IF NOT EXISTS `index_passage_visits_bookId` ON `passage_visits` (`bookId`);
CREATE INDEX IF NOT EXISTS `index_passage_visits_viewedAtEpochMs` ON `passage_visits` (`viewedAtEpochMs`);
```

## reading_milestones

```sql
CREATE TABLE IF NOT EXISTS `reading_milestones` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `kind` TEXT NOT NULL, `reachedAtEpochMs` INTEGER NOT NULL, `progression` REAL NOT NULL, `locatorJson` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_reading_milestones_bookId` ON `reading_milestones` (`bookId`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_reading_milestones_bookId_kind` ON `reading_milestones` (`bookId`, `kind`);
CREATE INDEX IF NOT EXISTS `index_reading_milestones_reachedAtEpochMs` ON `reading_milestones` (`reachedAtEpochMs`);
```

## manga_chapters

```sql
CREATE TABLE IF NOT EXISTS `manga_chapters` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `readingOrder` INTEGER NOT NULL, `cacheKey` TEXT NOT NULL, `title` TEXT, `normalizedTitle` TEXT, `volume` REAL, `number` REAL, `languageTag` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_manga_chapters_bookId` ON `manga_chapters` (`bookId`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_manga_chapters_bookId_readingOrder` ON `manga_chapters` (`bookId`, `readingOrder`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_manga_chapters_id_bookId` ON `manga_chapters` (`id`, `bookId`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_manga_chapters_cacheKey` ON `manga_chapters` (`cacheKey`);
```

## manga_source_links

```sql
CREATE TABLE IF NOT EXISTS `manga_source_links` (`bookId` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `mangaKey` TEXT NOT NULL, `publicUrl` TEXT, PRIMARY KEY(`bookId`, `sourceId`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_manga_source_links_sourceId` ON `manga_source_links` (`sourceId`);
```

## manga_chapter_sources

```sql
CREATE TABLE IF NOT EXISTS `manga_chapter_sources` (`chapterId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `mangaKey` TEXT NOT NULL, `chapterKey` TEXT NOT NULL, PRIMARY KEY(`chapterId`, `sourceId`), FOREIGN KEY(`chapterId`, `bookId`) REFERENCES `manga_chapters`(`id`, `bookId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`bookId`, `sourceId`) REFERENCES `manga_source_links`(`bookId`, `sourceId`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_manga_chapter_sources_chapterId_bookId` ON `manga_chapter_sources` (`chapterId`, `bookId`);
CREATE INDEX IF NOT EXISTS `index_manga_chapter_sources_bookId_sourceId` ON `manga_chapter_sources` (`bookId`, `sourceId`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_manga_chapter_sources_sourceId_mangaKey_chapterKey` ON `manga_chapter_sources` (`sourceId`, `mangaKey`, `chapterKey`);
```

## manga_progress

```sql
CREATE TABLE IF NOT EXISTS `manga_progress` (`bookId` TEXT NOT NULL, `chapterId` TEXT NOT NULL, `pageIndex` INTEGER NOT NULL, `pageCount` INTEGER, `chapterProgression` REAL NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`bookId`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`chapterId`, `bookId`) REFERENCES `manga_chapters`(`id`, `bookId`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_manga_progress_chapterId_bookId` ON `manga_progress` (`chapterId`, `bookId`);
```

## manga_offline_chapters

```sql
CREATE TABLE IF NOT EXISTS `manga_offline_chapters` (`chapterId` TEXT NOT NULL, `originSourceId` TEXT NOT NULL, `originChapterKey` TEXT NOT NULL, `completed` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`chapterId`), FOREIGN KEY(`chapterId`) REFERENCES `manga_chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
```

## manga_offline_pages

```sql
CREATE TABLE IF NOT EXISTS `manga_offline_pages` (`chapterId` TEXT NOT NULL, `pageIndex` INTEGER NOT NULL, `relativePath` TEXT NOT NULL, `byteSize` INTEGER NOT NULL, `contentSha256` TEXT, PRIMARY KEY(`chapterId`, `pageIndex`), FOREIGN KEY(`chapterId`) REFERENCES `manga_offline_chapters`(`chapterId`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE UNIQUE INDEX IF NOT EXISTS `index_manga_offline_pages_relativePath` ON `manga_offline_pages` (`relativePath`);
```

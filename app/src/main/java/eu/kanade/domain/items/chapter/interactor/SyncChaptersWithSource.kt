package eu.kanade.domain.items.chapter.interactor

import eu.kanade.domain.entries.manga.interactor.UpdateManga
import eu.kanade.domain.entries.manga.model.toSManga
import eu.kanade.domain.items.chapter.model.copyFromSChapter
import eu.kanade.domain.items.chapter.model.toSChapter
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadProvider
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import tachiyomi.data.items.chapter.ChapterSanitizer
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.interactor.GetChapterByMangaId
import tachiyomi.domain.items.chapter.interactor.ShouldUpdateDbChapter
import tachiyomi.domain.items.chapter.interactor.UpdateChapter
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.model.NoChaptersException
import tachiyomi.domain.items.chapter.model.toChapterUpdate
import tachiyomi.domain.items.chapter.repository.ChapterRepository
import tachiyomi.domain.items.chapter.service.ChapterRecognition
import tachiyomi.source.local.entries.manga.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.lang.Long.max
import java.util.Date
import java.util.TreeSet

class SyncChaptersWithSource(
    private val downloadManager: MangaDownloadManager = Injekt.get(),
    private val downloadProvider: MangaDownloadProvider = Injekt.get(),
    private val chapterRepository: ChapterRepository = Injekt.get(),
    private val shouldUpdateDbChapter: ShouldUpdateDbChapter = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val updateChapter: UpdateChapter = Injekt.get(),
    private val getChapterByMangaId: GetChapterByMangaId = Injekt.get(),
) {

    /**
     * Method to synchronize db chapters with source ones
     *
     * @param rawSourceChapters the chapters from the source.
     * @param manga the manga the chapters belong to.
     * @param source the source the manga belongs to.
     * @return Newly added chapters
     */
    suspend fun await(
        rawSourceChapters: List<SChapter>,
        manga: Manga,
        source: MangaSource,
    ): List<Chapter> {
        if (rawSourceChapters.isEmpty() && !source.isLocal()) {
            throw NoChaptersException()
        }

        val sourceChapters = rawSourceChapters
            .distinctBy { it.url }
            .mapIndexed { i, sChapter ->
                Chapter.create()
                    .copyFromSChapter(sChapter)
                    .copy(name = with(ChapterSanitizer) { sChapter.name.sanitize(manga.title) })
                    .copy(mangaId = manga.id, sourceOrder = i.toLong())
            }

        // Chapters from db.
        val dbChapters = getChapterByMangaId.await(manga.id)

        // Chapters from the source not in db.
        val toAdd = mutableListOf<Chapter>()

        // Chapters whose metadata have changed.
        val toChange = mutableListOf<Chapter>()

        // Chapters from the db not in source.
        val toDelete = dbChapters.filterNot { dbChapter ->
            sourceChapters.any { sourceChapter ->
                dbChapter.url == sourceChapter.url
            }
        }

        val rightNow = Date().time

        // Used to not set upload date of older chapters
        // to a higher value than newer chapters
        var maxSeenUploadDate = 0L

        val sManga = manga.toSManga()
        for (sourceChapter in sourceChapters) {
            var chapter = sourceChapter

            // Update metadata from source if necessary.
            if (source is HttpSource) {
                val sChapter = chapter.toSChapter()
                source.prepareNewChapter(sChapter, sManga)
                chapter = chapter.copyFromSChapter(sChapter)
            }

            // Recognize chapter number for the chapter.
            val chapterNumber = ChapterRecognition.parseChapterNumber(manga.title, chapter.name, chapter.chapterNumber)
            chapter = chapter.copy(chapterNumber = chapterNumber)

            val dbChapter = dbChapters.find { it.url == chapter.url }

            if (dbChapter == null) {
                val toAddChapter = if (chapter.dateUpload == 0L) {
                    val altDateUpload = if (maxSeenUploadDate == 0L) rightNow else maxSeenUploadDate
                    chapter.copy(dateUpload = altDateUpload)
                } else {
                    maxSeenUploadDate = max(maxSeenUploadDate, sourceChapter.dateUpload)
                    chapter
                }
                toAdd.add(toAddChapter)
            } else {
                if (shouldUpdateDbChapter.await(dbChapter, chapter)) {
                    val shouldRenameChapter = downloadProvider.isChapterDirNameChanged(dbChapter, chapter) &&
                        downloadManager.isChapterDownloaded(dbChapter.name, dbChapter.scanlator, manga.title, manga.source)

                    if (shouldRenameChapter) {
                        downloadManager.renameChapter(source, manga, dbChapter, chapter)
                    }
                    var toChangeChapter = dbChapter.copy(
                        name = chapter.name,
                        chapterNumber = chapter.chapterNumber,
                        scanlator = chapter.scanlator,
                        sourceOrder = chapter.sourceOrder,
                    )
                    if (chapter.dateUpload != 0L) {
                        toChangeChapter = toChangeChapter.copy(dateUpload = chapter.dateUpload)
                    }
                    toChange.add(toChangeChapter)
                }
            }
        }

        // Return if there's nothing to add, delete or change, avoiding unnecessary db transactions.
        if (toAdd.isEmpty() && toDelete.isEmpty() && toChange.isEmpty()) {
            return emptyList()
        }

        val reAdded = mutableListOf<Chapter>()

        val deletedChapterNumbers = TreeSet<Float>()
        val deletedReadChapterNumbers = TreeSet<Float>()
        val deletedBookmarkedChapterNumbers = TreeSet<Float>()

        toDelete.forEach { chapter ->
            if (chapter.read) deletedReadChapterNumbers.add(chapter.chapterNumber)
            if (chapter.bookmark) deletedBookmarkedChapterNumbers.add(chapter.chapterNumber)
            deletedChapterNumbers.add(chapter.chapterNumber)
        }

        val deletedChapterNumberDateFetchMap = toDelete.sortedByDescending { it.dateFetch }
            .associate { it.chapterNumber to it.dateFetch }

        // Date fetch is set in such a way that the upper ones will have bigger value than the lower ones
        // Sources MUST return the chapters from most to less recent, which is common.
        var itemCount = toAdd.size
        var updatedToAdd = toAdd.map { toAddItem ->
            var chapter = toAddItem.copy(dateFetch = rightNow + itemCount--)

            if (chapter.isRecognizedNumber.not() || chapter.chapterNumber !in deletedChapterNumbers) return@map chapter

            chapter = chapter.copy(
                read = chapter.chapterNumber in deletedReadChapterNumbers,
                bookmark = chapter.chapterNumber in deletedBookmarkedChapterNumbers,
            )

            // Try to to use the fetch date of the original entry to not pollute 'Updates' tab
            deletedChapterNumberDateFetchMap[chapter.chapterNumber]?.let {
                chapter = chapter.copy(dateFetch = it)
            }

            reAdded.add(chapter)

            chapter
        }

        if (toDelete.isNotEmpty()) {
            val toDeleteIds = toDelete.map { it.id }
            chapterRepository.removeChaptersWithIds(toDeleteIds)
        }

        if (updatedToAdd.isNotEmpty()) {
            updatedToAdd = chapterRepository.addAllChapters(updatedToAdd)
        }

        if (toChange.isNotEmpty()) {
            val chapterUpdates = toChange.map { it.toChapterUpdate() }
            updateChapter.awaitAll(chapterUpdates)
        }

        // Set this manga as updated since chapters were changed
        // Note that last_update actually represents last time the chapter list changed at all
        updateManga.awaitUpdateLastUpdate(manga.id)

        val reAddedUrls = reAdded.map { it.url }.toHashSet()

        return updatedToAdd.filterNot { it.url in reAddedUrls }
    }
}

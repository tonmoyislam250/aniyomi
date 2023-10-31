package eu.kanade.domain.entries.manga.interactor

import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import tachiyomi.domain.entries.manga.model.MangaUpdate
import tachiyomi.domain.entries.manga.repository.MangaRepository

class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetMangaReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.updateManga(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReadingModeType.MASK.toLong()),
            ),
        )
    }

    suspend fun awaitSetOrientationType(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.updateManga(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, OrientationType.MASK.toLong()),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}

package eu.kanade.tachiyomi.data.coil

import coil.key.Keyer
import coil.request.Options
import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import tachiyomi.domain.entries.anime.model.AnimeCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.entries.anime.model.Anime as DomainAnime

class AnimeKeyer : Keyer<DomainAnime> {
    override fun key(data: DomainAnime, options: Options): String {
        return if (data.hasCustomCover()) {
            "anime;${data.id};${data.coverLastModified}"
        } else {
            "anime;${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class AnimeCoverKeyer(
    private val coverCache: AnimeCoverCache = Injekt.get(),
) : Keyer<AnimeCover> {
    override fun key(data: AnimeCover, options: Options): String {
        return if (coverCache.getCustomCoverFile(data.animeId).exists()) {
            "anime;${data.animeId};${data.lastModified}"
        } else {
            "anime;${data.url};${data.lastModified}"
        }
    }
}

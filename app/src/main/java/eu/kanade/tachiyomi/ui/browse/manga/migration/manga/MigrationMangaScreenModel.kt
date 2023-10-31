package eu.kanade.tachiyomi.ui.browse.manga.migration.manga

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.tachiyomi.source.MangaSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.manga.interactor.GetMangaFavorites
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationMangaScreenModel(
    private val sourceId: Long,
    private val sourceManager: MangaSourceManager = Injekt.get(),
    private val getFavorites: GetMangaFavorites = Injekt.get(),
) : StateScreenModel<MigrateMangaState>(MigrateMangaState()) {

    private val _events: Channel<MigrationMangaEvent> = Channel()
    val events: Flow<MigrationMangaEvent> = _events.receiveAsFlow()

    init {
        coroutineScope.launch {
            mutableState.update { state ->
                state.copy(source = sourceManager.getOrStub(sourceId))
            }

            getFavorites.subscribe(sourceId)
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(MigrationMangaEvent.FailedFetchingFavorites)
                    mutableState.update { state ->
                        state.copy(titleList = emptyList())
                    }
                }
                .map { manga ->
                    manga.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                }
                .collectLatest { list ->
                    mutableState.update { it.copy(titleList = list) }
                }
        }
    }
}

sealed class MigrationMangaEvent {
    object FailedFetchingFavorites : MigrationMangaEvent()
}

@Immutable
data class MigrateMangaState(
    val source: MangaSource? = null,
    private val titleList: List<Manga>? = null,
) {

    val titles: List<Manga>
        get() = titleList.orEmpty()

    val isLoading: Boolean
        get() = source == null || titleList == null

    val isEmpty: Boolean
        get() = titles.isEmpty()
}

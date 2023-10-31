package eu.kanade.tachiyomi.ui.browse.anime.migration.sources

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.source.anime.interactor.GetAnimeSourcesWithFavoriteCount
import eu.kanade.domain.source.service.SetMigrateSorting
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.source.anime.model.AnimeSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateAnimeSourceScreenModel(
    preferences: SourcePreferences = Injekt.get(),
    private val getSourcesWithFavoriteCount: GetAnimeSourcesWithFavoriteCount = Injekt.get(),
    private val setMigrateSorting: SetMigrateSorting = Injekt.get(),
) : StateScreenModel<MigrateAnimeSourceState>(MigrateAnimeSourceState()) {

    private val _channel = Channel<Event>(Int.MAX_VALUE)
    val channel = _channel.receiveAsFlow()

    init {
        coroutineScope.launchIO {
            getSourcesWithFavoriteCount.subscribe()
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _channel.send(Event.FailedFetchingSourcesWithCount)
                }
                .collectLatest { sources ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = sources,
                        )
                    }
                }
        }

        preferences.migrationSortingDirection().changes()
            .onEach { mutableState.update { state -> state.copy(sortingDirection = it) } }
            .launchIn(coroutineScope)

        preferences.migrationSortingMode().changes()
            .onEach { mutableState.update { state -> state.copy(sortingMode = it) } }
            .launchIn(coroutineScope)
    }

    fun toggleSortingMode() {
        with(state.value) {
            val newMode = when (sortingMode) {
                SetMigrateSorting.Mode.ALPHABETICAL -> SetMigrateSorting.Mode.TOTAL
                SetMigrateSorting.Mode.TOTAL -> SetMigrateSorting.Mode.ALPHABETICAL
            }

            setMigrateSorting.await(newMode, sortingDirection)
        }
    }

    fun toggleSortingDirection() {
        with(state.value) {
            val newDirection = when (sortingDirection) {
                SetMigrateSorting.Direction.ASCENDING -> SetMigrateSorting.Direction.DESCENDING
                SetMigrateSorting.Direction.DESCENDING -> SetMigrateSorting.Direction.ASCENDING
            }

            setMigrateSorting.await(sortingMode, newDirection)
        }
    }

    sealed class Event {
        object FailedFetchingSourcesWithCount : Event()
    }
}

data class MigrateAnimeSourceState(
    val isLoading: Boolean = true,
    val items: List<Pair<AnimeSource, Long>> = emptyList(),
    val sortingMode: SetMigrateSorting.Mode = SetMigrateSorting.Mode.ALPHABETICAL,
    val sortingDirection: SetMigrateSorting.Direction = SetMigrateSorting.Direction.ASCENDING,
) {
    val isEmpty = items.isEmpty()
}

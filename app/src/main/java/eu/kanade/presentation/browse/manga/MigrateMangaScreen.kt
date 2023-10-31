package eu.kanade.presentation.browse.manga

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.entries.manga.components.BaseMangaListItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.manga.migration.manga.MigrateMangaState
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun MigrateMangaScreen(
    navigateUp: () -> Unit,
    title: String?,
    state: MigrateMangaState,
    onClickItem: (Manga) -> Unit,
    onClickCover: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = title,
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        if (state.isEmpty) {
            EmptyScreen(
                textResource = R.string.empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }

        MigrateMangaContent(
            contentPadding = contentPadding,
            state = state,
            onClickItem = onClickItem,
            onClickCover = onClickCover,
        )
    }
}

@Composable
private fun MigrateMangaContent(
    contentPadding: PaddingValues,
    state: MigrateMangaState,
    onClickItem: (Manga) -> Unit,
    onClickCover: (Manga) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(state.titles) { manga ->
            MigrateMangaItem(
                manga = manga,
                onClickItem = onClickItem,
                onClickCover = onClickCover,
            )
        }
    }
}

@Composable
private fun MigrateMangaItem(
    modifier: Modifier = Modifier,
    manga: Manga,
    onClickItem: (Manga) -> Unit,
    onClickCover: (Manga) -> Unit,
) {
    BaseMangaListItem(
        modifier = modifier,
        manga = manga,
        onClickItem = { onClickItem(manga) },
        onClickCover = { onClickCover(manga) },
    )
}

package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun AnimeExtensionFilterScreen(
    navigateUp: () -> Unit,
    state: AnimeExtensionFilterState.Success,
    onClickToggle: (String) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(R.string.label_extensions),
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
        AnimeExtensionFilterContent(
            contentPadding = contentPadding,
            state = state,
            onClickLang = onClickToggle,
        )
    }
}

@Composable
private fun AnimeExtensionFilterContent(
    contentPadding: PaddingValues,
    state: AnimeExtensionFilterState.Success,
    onClickLang: (String) -> Unit,
) {
    val context = LocalContext.current
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(state.languages) { language ->
            SwitchPreferenceWidget(
                modifier = Modifier.animateItemPlacement(),
                title = LocaleHelper.getSourceDisplayName(language, context),
                checked = language in state.enabledLanguages,
                onCheckedChanged = { onClickLang(language) },
            )
        }
    }
}

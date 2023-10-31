package eu.kanade.presentation.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.EntryDownloadDropdownMenu
import eu.kanade.presentation.components.OverflowMenu
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.theme.active

@Composable
fun EntryToolbar(
    modifier: Modifier = Modifier,
    title: String,
    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float = titleAlphaProvider,
    hasFilters: Boolean,
    onBackClicked: () -> Unit,
    onClickFilter: () -> Unit,
    onClickShare: (() -> Unit)?,
    onClickDownload: ((DownloadAction) -> Unit)?,
    onClickEditCategory: (() -> Unit)?,
    onClickRefresh: () -> Unit,
    onClickMigrate: (() -> Unit)?,
    onClickSettings: (() -> Unit)?,
    // Anime only
    changeAnimeSkipIntro: (() -> Unit)?,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    isManga: Boolean,
) {
    Column(
        modifier = modifier,
    ) {
        val isActionMode = actionModeCounter > 0
        TopAppBar(
            title = {
                Text(
                    text = if (isActionMode) actionModeCounter.toString() else title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (isActionMode) 1f else titleAlphaProvider()),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = if (isActionMode) Icons.Outlined.Close else Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.abc_action_bar_up_description),
                    )
                }
            },
            actions = {
                if (isActionMode) {
                    IconButton(onClick = onSelectAll) {
                        Icon(
                            imageVector = Icons.Outlined.SelectAll,
                            contentDescription = stringResource(R.string.action_select_all),
                        )
                    }
                    IconButton(onClick = onInvertSelection) {
                        Icon(
                            imageVector = Icons.Outlined.FlipToBack,
                            contentDescription = stringResource(R.string.action_select_inverse),
                        )
                    }
                } else {
                    if (onClickDownload != null) {
                        val (downloadExpanded, onDownloadExpanded) = remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { onDownloadExpanded(!downloadExpanded) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = stringResource(R.string.manga_download),
                                )
                            }
                            val onDismissRequest = { onDownloadExpanded(false) }
                            EntryDownloadDropdownMenu(
                                expanded = downloadExpanded,
                                onDismissRequest = onDismissRequest,
                                onDownloadClicked = onClickDownload,
                                isManga = isManga,
                            )
                        }
                    }

                    val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
                    IconButton(onClick = onClickFilter) {
                        Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.action_filter), tint = filterTint)
                    }

                    OverflowMenu { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.action_webview_refresh)) },
                            onClick = {
                                onClickRefresh()
                                closeMenu()
                            },
                        )
                        if (onClickEditCategory != null) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_edit_categories)) },
                                onClick = {
                                    onClickEditCategory()
                                    closeMenu()
                                },
                            )
                        }
                        if (onClickMigrate != null) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_migrate)) },
                                onClick = {
                                    onClickMigrate()
                                    closeMenu()
                                },
                            )
                        }
                        if (changeAnimeSkipIntro != null) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_change_intro_length)) },
                                onClick = {
                                    changeAnimeSkipIntro()
                                    closeMenu()
                                },
                            )
                        }
                        if (onClickShare != null) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_share)) },
                                onClick = {
                                    onClickShare()
                                    closeMenu()
                                },
                            )
                        }
                        if (onClickSettings != null) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.settings)) },
                                onClick = {
                                    onClickSettings()
                                    closeMenu()
                                },
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme
                    .surfaceColorAtElevation(3.dp)
                    .copy(alpha = if (isActionMode) 1f else backgroundAlphaProvider()),
            ),
        )
    }
}

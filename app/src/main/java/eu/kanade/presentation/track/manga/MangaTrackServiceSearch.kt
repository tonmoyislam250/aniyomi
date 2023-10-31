package eu.kanade.presentation.track.manga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.ItemCover
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.runOnEnterKeyPressed
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun MangaTrackServiceSearch(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onDispatchQuery: () -> Unit,
    queryResult: Result<List<MangaTrackSearch>>?,
    selected: MangaTrackSearch?,
    onSelectedChange: (MangaTrackSearch) -> Unit,
    onConfirmSelection: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val dispatchQueryAndClearFocus: () -> Unit = {
        onDispatchQuery()
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    title = {
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .runOnEnterKeyPressed(action = dispatchQueryAndClearFocus),
                            textStyle = MaterialTheme.typography.bodyLarge
                                .copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { dispatchQueryAndClearFocus() }),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = {
                                if (query.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.action_search_hint),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                it()
                            },
                        )
                    },
                    actions = {
                        if (query.text.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onQueryChange(TextFieldValue())
                                    focusRequester.requestFocus()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
                Divider()
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selected != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                Button(
                    onClick = { onConfirmSelection() },
                    modifier = Modifier
                        .padding(12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .fillMaxWidth(),
                    elevation = ButtonDefaults.elevatedButtonElevation(),
                ) {
                    Text(text = stringResource(R.string.action_track))
                }
            }
        },
    ) { innerPadding ->
        if (queryResult == null) {
            LoadingScreen(modifier = Modifier.padding(innerPadding))
        } else {
            val availableTracks = queryResult.getOrNull()
            if (availableTracks != null) {
                if (availableTracks.isEmpty()) {
                    EmptyScreen(
                        modifier = Modifier.padding(innerPadding),
                        textResource = R.string.no_results_found,
                    )
                } else {
                    ScrollbarLazyColumn(
                        contentPadding = innerPadding + PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = availableTracks,
                            key = { it.hashCode() },
                        ) {
                            SearchResultItem(
                                title = it.title,
                                coverUrl = it.cover_url,
                                type = it.publishing_type.toLowerCase(Locale.current).capitalize(Locale.current),
                                startDate = it.start_date,
                                status = it.publishing_status.toLowerCase(Locale.current).capitalize(Locale.current),
                                description = it.summary.trim(),
                                selected = it == selected,
                                onClick = { onSelectedChange(it) },
                            )
                        }
                    }
                }
            } else {
                EmptyScreen(
                    modifier = Modifier.padding(innerPadding),
                    message = queryResult.exceptionOrNull()?.message
                        ?: stringResource(R.string.unknown_error),
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    title: String,
    coverUrl: String,
    type: String,
    startDate: String,
    status: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.outline else Color.Transparent
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = shape,
            )
            .selectable(selected = selected, onClick = onClick)
            .padding(12.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column {
            Row {
                ItemCover.Book(
                    data = coverUrl,
                    modifier = Modifier.height(96.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        modifier = Modifier.padding(end = 28.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (type.isNotBlank()) {
                        SearchResultItemDetails(
                            title = stringResource(R.string.track_type),
                            text = type,
                        )
                    }
                    if (startDate.isNotBlank()) {
                        SearchResultItemDetails(
                            title = stringResource(R.string.label_started),
                            text = startDate,
                        )
                    }
                    if (status.isNotBlank()) {
                        SearchResultItemDetails(
                            title = stringResource(R.string.track_status),
                            text = status,
                        )
                    }
                }
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    modifier = Modifier
                        .paddingFromBaseline(top = 24.dp)
                        .secondaryItemAlpha(),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun SearchResultItemDetails(
    title: String,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.tiny)) {
        Text(
            text = title,
            maxLines = 1,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = text,
            modifier = Modifier
                .weight(1f)
                .secondaryItemAlpha(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

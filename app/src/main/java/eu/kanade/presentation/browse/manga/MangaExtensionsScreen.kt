package eu.kanade.presentation.browse.manga

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.BaseBrowseItem
import eu.kanade.presentation.browse.manga.components.MangaExtensionIcon
import eu.kanade.presentation.entries.DotSeparatorNoSpaceText
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun MangaExtensionScreen(
    state: MangaExtensionsState,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (MangaExtension) -> Unit,
    onClickItemCancel: (MangaExtension) -> Unit,
    onInstallExtension: (MangaExtension.Available) -> Unit,
    onUninstallExtension: (MangaExtension) -> Unit,
    onUpdateExtension: (MangaExtension.Installed) -> Unit,
    onTrustExtension: (MangaExtension.Untrusted) -> Unit,
    onOpenExtension: (MangaExtension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))
            state.isEmpty -> {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    R.string.no_results_found
                } else {
                    R.string.empty_screen
                }
                EmptyScreen(
                    textResource = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            }
            else -> {
                ExtensionContent(
                    state = state,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onInstallExtension = onInstallExtension,
                    onUninstallExtension = onUninstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onTrustExtension = onTrustExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                )
            }
        }
    }
}

@Composable
private fun ExtensionContent(
    state: MangaExtensionsState,
    contentPadding: PaddingValues,
    onLongClickItem: (MangaExtension) -> Unit,
    onClickItemCancel: (MangaExtension) -> Unit,
    onInstallExtension: (MangaExtension.Available) -> Unit,
    onUninstallExtension: (MangaExtension) -> Unit,
    onUpdateExtension: (MangaExtension.Installed) -> Unit,
    onTrustExtension: (MangaExtension.Untrusted) -> Unit,
    onOpenExtension: (MangaExtension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
) {
    var trustState by remember { mutableStateOf<MangaExtension.Untrusted?>(null) }

    FastScrollLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        state.items.forEach { (header, items) ->
            item(
                contentType = "header",
                key = "extensionHeader-${header.hashCode()}",
            ) {
                when (header) {
                    is MangaExtensionUiModel.Header.Resource -> {
                        val action: @Composable RowScope.() -> Unit =
                            if (header.textRes == R.string.ext_updates_pending) {
                                {
                                    Button(onClick = { onClickUpdateAll() }) {
                                        Text(
                                            text = stringResource(R.string.ext_update_all),
                                            style = LocalTextStyle.current.copy(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        )
                                    }
                                }
                            } else {
                                {}
                            }
                        ExtensionHeader(
                            textRes = header.textRes,
                            modifier = Modifier.animateItemPlacement(),
                            action = action,
                        )
                    }
                    is MangaExtensionUiModel.Header.Text -> {
                        ExtensionHeader(
                            text = header.text,
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                }
            }

            items(
                items = items,
                contentType = { "item" },
                key = { "extension-${it.hashCode()}" },
            ) { item ->
                ExtensionItem(
                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    onClickItem = {
                        when (it) {
                            is MangaExtension.Available -> onInstallExtension(it)
                            is MangaExtension.Installed -> onOpenExtension(it)
                            is MangaExtension.Untrusted -> { trustState = it }
                        }
                    },
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onClickItemAction = {
                        when (it) {
                            is MangaExtension.Available -> onInstallExtension(it)
                            is MangaExtension.Installed -> {
                                if (it.hasUpdate) {
                                    onUpdateExtension(it)
                                } else {
                                    onOpenExtension(it)
                                }
                            }

                            is MangaExtension.Untrusted -> {
                                trustState = it
                            }
                        }
                    },
                )
            }
        }
    }
    if (trustState != null) {
        ExtensionTrustDialog(
            onClickConfirm = {
                onTrustExtension(trustState!!)
                trustState = null
            },
            onClickDismiss = {
                onUninstallExtension(trustState!!)
                trustState = null
            },
            onDismissRequest = {
                trustState = null
            },
        )
    }
}

@Composable
private fun ExtensionItem(
    modifier: Modifier = Modifier,
    item: MangaExtensionUiModel.Item,
    onClickItem: (MangaExtension) -> Unit,
    onLongClickItem: (MangaExtension) -> Unit,
    onClickItemCancel: (MangaExtension) -> Unit,
    onClickItemAction: (MangaExtension) -> Unit,
) {
    val (extension, installStep) = item
    BaseBrowseItem(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val idle = installStep.isCompleted()
                if (!idle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.dp,
                    )
                }

                val padding by animateDpAsState(targetValue = if (idle) 0.dp else 8.dp)
                MangaExtensionIcon(
                    extension = extension,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(padding),
                )
            }
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemAction = onClickItemAction,
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExtensionItemContent(
    extension: MangaExtension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        // Won't look good but it's not like we can ellipsize overflowing content
        FlowRow(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                if (extension is MangaExtension.Installed && extension.lang.isNotEmpty()) {
                    Text(
                        text = LocaleHelper.getSourceDisplayName(extension.lang, LocalContext.current),
                    )
                }

                if (extension.versionName.isNotEmpty()) {
                    Text(
                        text = extension.versionName,
                    )
                }

                val warning = when {
                    extension is MangaExtension.Untrusted -> R.string.ext_untrusted
                    extension is MangaExtension.Installed && extension.isUnofficial -> R.string.ext_unofficial
                    extension is MangaExtension.Installed && extension.isObsolete -> R.string.ext_obsolete
                    extension.isNsfw -> R.string.ext_nsfw_short
                    else -> null
                }
                if (warning != null) {
                    Text(
                        text = stringResource(warning).uppercase(),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!installStep.isCompleted()) {
                    DotSeparatorNoSpaceText()
                    Text(
                        text = when (installStep) {
                            InstallStep.Pending -> stringResource(R.string.ext_pending)
                            InstallStep.Downloading -> stringResource(R.string.ext_downloading)
                            InstallStep.Installing -> stringResource(R.string.ext_installing)
                            else -> error("Must not show non-install process text")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtensionItemActions(
    extension: MangaExtension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (MangaExtension) -> Unit = {},
    onClickItemAction: (MangaExtension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()
    Row(modifier = modifier) {
        if (isIdle) {
            TextButton(
                onClick = { onClickItemAction(extension) },
            ) {
                Text(
                    text = when (installStep) {
                        InstallStep.Installed -> stringResource(R.string.ext_installed)
                        InstallStep.Error -> stringResource(R.string.action_retry)
                        InstallStep.Idle -> {
                            when (extension) {
                                is MangaExtension.Installed -> {
                                    if (extension.hasUpdate) {
                                        stringResource(R.string.ext_update)
                                    } else {
                                        stringResource(R.string.action_settings)
                                    }
                                }
                                is MangaExtension.Untrusted -> stringResource(R.string.ext_trust)
                                is MangaExtension.Available -> stringResource(R.string.ext_install)
                            }
                        }
                        else -> error("Must not show install process text")
                    },
                )
            }
        } else {
            IconButton(onClick = { onClickItemCancel(extension) }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                )
            }
        }
    }
}

@Composable
fun ExtensionHeader(
    @StringRes textRes: Int,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    ExtensionHeader(
        text = stringResource(textRes),
        modifier = modifier,
        action = action,
    )
}

@Composable
fun ExtensionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.header,
        )
        action()
    }
}

@Composable
fun ExtensionTrustDialog(
    onClickConfirm: () -> Unit,
    onClickDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.untrusted_extension))
        },
        text = {
            Text(text = stringResource(R.string.untrusted_extension_message))
        },
        confirmButton = {
            TextButton(onClick = onClickConfirm) {
                Text(text = stringResource(R.string.ext_trust))
            }
        },
        dismissButton = {
            TextButton(onClick = onClickDismiss) {
                Text(text = stringResource(R.string.ext_uninstall))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

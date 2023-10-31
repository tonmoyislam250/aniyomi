package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.EnhancedMangaTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.data.track.bangumi.BangumiApi
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi
import eu.kanade.tachiyomi.data.track.shikimori.ShikimoriApi
import eu.kanade.tachiyomi.data.track.simkl.SimklApi
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.presentation.core.components.material.padding
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsTrackingScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    @StringRes
    override fun getTitleRes() = R.string.pref_category_tracking

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(onClick = { uriHandler.openUri("https://aniyomi.org/help/guides/tracking/") }) {
            Icon(
                imageVector = Icons.Outlined.HelpOutline,
                contentDescription = stringResource(R.string.tracking_guide),
            )
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val trackPreferences = remember { Injekt.get<TrackPreferences>() }
        val trackManager = remember { Injekt.get<TrackManager>() }
        val sourceManager = remember { Injekt.get<MangaSourceManager>() }

        var dialog by remember { mutableStateOf<Any?>(null) }
        dialog?.run {
            when (this) {
                is LoginDialog -> {
                    TrackingLoginDialog(
                        service = service,
                        uNameStringRes = uNameStringRes,
                        onDismissRequest = { dialog = null },
                    )
                }
                is LogoutDialog -> {
                    TrackingLogoutDialog(
                        service = service,
                        onDismissRequest = { dialog = null },
                    )
                }
            }
        }

        val enhancedMangaTrackers = trackManager.services
            .filter { it is EnhancedMangaTrackService }
            .partition { service ->
                val acceptedMangaSources = (service as EnhancedMangaTrackService).getAcceptedSources()
                sourceManager.getCatalogueSources().any { it::class.qualifiedName in acceptedMangaSources }
            }
        var enhancedMangaTrackerInfo = stringResource(R.string.enhanced_tracking_info)
        if (enhancedMangaTrackers.second.isNotEmpty()) {
            val missingMangaSourcesInfo = stringResource(
                R.string.enhanced_services_not_installed,
                enhancedMangaTrackers.second
                    .map { stringResource(it.nameRes()) }
                    .joinToString(),
            )
            enhancedMangaTrackerInfo += "\n\n$missingMangaSourcesInfo"
        }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = trackPreferences.autoUpdateTrack(),
                title = stringResource(R.string.pref_auto_update_manga_sync),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = trackPreferences.trackOnAddingToLibrary(),
                title = stringResource(R.string.pref_track_on_add_library),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = trackPreferences.showNextEpisodeAiringTime(),
                title = stringResource(R.string.pref_show_next_episode_airing_time),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.services),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TrackingPreference(
                        title = stringResource(trackManager.myAnimeList.nameRes()),
                        service = trackManager.myAnimeList,
                        login = { context.openInBrowser(MyAnimeListApi.authUrl(), forceDefaultBrowser = true) },
                        logout = { dialog = LogoutDialog(trackManager.myAnimeList) },
                    ),
                    Preference.PreferenceItem.TrackingPreference(
                        title = stringResource(trackManager.aniList.nameRes()),
                        service = trackManager.aniList,
                        login = { context.openInBrowser(AnilistApi.authUrl(), forceDefaultBrowser = true) },
                        logout = { dialog = LogoutDialog(trackManager.aniList) },
                    ),
                    Preference.PreferenceItem.TrackingPreference(
                        title = stringResource(trackManager.kitsu.nameRes()),
                        service = trackManager.kitsu,
                        login = { dialog = LoginDialog(trackManager.kitsu, R.string.email) },
                        logout = { dialog = LogoutDialog(trackManager.kitsu) },
                    ),
                    Preference.PreferenceItem.TrackingPreference(
                        title = stringResource(trackManager.mangaUpdates.nameRes()),
                        service = trackManager.mangaUpdates,
                        login = { dialog = LoginDialog(trackManager.mangaUpdates, R.string.username) },
                        logout = { dialog = LogoutDialog(trackManager.mangaUpdates) },
                    ),
                    Preference.PreferenceItem.TrackingPreference(
                        title = stringResource(trackManager.shikimori.nameRes()),
                        service = trackManager.shikimori,
                        login = { context.openInBrowser(ShikimoriApi.authUrl(), forceDefaultBrowser = true) },
                        logout = { dialog = LogoutDialog(trackManager.shikimori) },
                    ),
                    Preference.PreferenceItem.TrackingPreference(
                        title = stringResource(trackManager.simkl.nameRes()),
                        service = trackManager.simkl,
                        login = { context.openInBrowser(SimklApi.authUrl(), forceDefaultBrowser = true) },
                        logout = { dialog = LogoutDialog(trackManager.simkl) },
                    ),
                    Preference.PreferenceItem.TrackingPreference(
                        title = stringResource(trackManager.bangumi.nameRes()),
                        service = trackManager.bangumi,
                        login = { context.openInBrowser(BangumiApi.authUrl(), forceDefaultBrowser = true) },
                        logout = { dialog = LogoutDialog(trackManager.bangumi) },
                    ),
                    Preference.PreferenceItem.InfoPreference(stringResource(R.string.tracking_info)),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.enhanced_services),
                preferenceItems = enhancedMangaTrackers.first
                    .map { service ->
                        Preference.PreferenceItem.TrackingPreference(
                            title = stringResource(service.nameRes()),
                            service = service,
                            login = { (service as EnhancedMangaTrackService).loginNoop() },
                            logout = service::logout,
                        )
                    } + listOf(Preference.PreferenceItem.InfoPreference(enhancedMangaTrackerInfo)),

            ),
        )
    }

    @Composable
    private fun TrackingLoginDialog(
        service: TrackService,
        @StringRes uNameStringRes: Int,
        onDismissRequest: () -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var username by remember { mutableStateOf(TextFieldValue(service.getUsername())) }
        var password by remember { mutableStateOf(TextFieldValue(service.getPassword())) }
        var processing by remember { mutableStateOf(false) }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.login_title, stringResource(service.nameRes())),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(text = stringResource(uNameStringRes)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        isError = inputError && !processing,
                    )

                    var hidePassword by remember { mutableStateOf(true) }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(text = stringResource(R.string.password)) },
                        trailingIcon = {
                            IconButton(onClick = { hidePassword = !hidePassword }) {
                                Icon(
                                    imageVector = if (hidePassword) {
                                        Icons.Filled.Visibility
                                    } else {
                                        Icons.Filled.VisibilityOff
                                    },
                                    contentDescription = null,
                                )
                            }
                        },
                        visualTransformation = if (hidePassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        singleLine = true,
                        isError = inputError && !processing,
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !processing && username.text.isNotBlank() && password.text.isNotBlank(),
                    onClick = {
                        scope.launchIO {
                            processing = true
                            val result = checkLogin(
                                context = context,
                                service = service,
                                username = username.text,
                                password = password.text,
                            )
                            inputError = !result
                            if (result) onDismissRequest()
                            processing = false
                        }
                    },
                ) {
                    val id = if (processing) R.string.loading else R.string.login
                    Text(text = stringResource(id))
                }
            },
        )
    }

    private suspend fun checkLogin(
        context: Context,
        service: TrackService,
        username: String,
        password: String,
    ): Boolean {
        return try {
            service.login(username, password)
            withUIContext { context.toast(R.string.login_success) }
            true
        } catch (e: Throwable) {
            service.logout()
            withUIContext { context.toast(e.message.toString()) }
            false
        }
    }

    @Composable
    private fun TrackingLogoutDialog(
        service: TrackService,
        onDismissRequest: () -> Unit,
    ) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.logout_title, stringResource(service.nameRes())),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.tiny)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest,
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            service.logout()
                            onDismissRequest()
                            context.toast(R.string.logout_success)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(text = stringResource(R.string.logout))
                    }
                }
            },
        )
    }
}

private data class LoginDialog(
    val service: TrackService,
    @StringRes val uNameStringRes: Int,
)

private data class LogoutDialog(
    val service: TrackService,
)

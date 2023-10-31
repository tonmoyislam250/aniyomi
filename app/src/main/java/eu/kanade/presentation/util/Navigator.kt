package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

/**
 * For invoking back press to the parent activity
 */
val LocalBackPress: ProvidableCompositionLocal<(() -> Unit)?> = staticCompositionLocalOf { null }

abstract class Tab : cafe.adriel.voyager.navigator.tab.Tab {

    override val key: ScreenKey = uniqueScreenKey
    open suspend fun onReselect(navigator: Navigator) {}
}

// TODO: this prevents crashes in nested navigators with transitions not being disposed
// properly. Go back to using vanilla Voyager Screens once fixed upstream.
abstract class Screen : Screen {

    override val key: ScreenKey = uniqueScreenKey
}

/**
 * A variant of ScreenModel.coroutineScope except with the IO dispatcher instead of the
 * main dispatcher.
 */
val ScreenModel.ioCoroutineScope: CoroutineScope
    get() = ScreenModelStore.getOrPutDependency(
        screenModel = this,
        name = "ScreenModelIoCoroutineScope",
        factory = { key -> CoroutineScope(Dispatchers.IO + SupervisorJob()) + CoroutineName(key) },
        onDispose = { scope -> scope.cancel() },
    )

interface AssistContentScreen {
    fun onProvideAssistUrl(): String?
}

@Composable
fun DefaultNavigatorScreenTransition(navigator: Navigator) {
    val slideDistance = rememberSlideDistance()
    ScreenTransition(
        navigator = navigator,
        transition = {
            materialSharedAxisX(
                forward = navigator.lastEvent != StackEvent.Pop,
                slideDistance = slideDistance,
            )
        },
    )
}

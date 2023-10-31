package eu.kanade.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import tachiyomi.core.preference.CheckboxState

@Composable
fun DeleteLibraryEntryDialog(
    containsLocalEntry: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit,
    isManga: Boolean,
) {
    var list by remember {
        mutableStateOf(
            buildList<CheckboxState.State<Int>> {
                val checkbox1 = if (isManga) R.string.manga_from_library else R.string.anime_from_library
                add(CheckboxState.State.None(checkbox1))
                if (!containsLocalEntry) {
                    val checkbox2 = if (isManga) R.string.downloaded_chapters else R.string.downloaded_episodes
                    add(CheckboxState.State.None(checkbox2))
                }
            },
        )
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = list.any { it.isChecked },
                onClick = {
                    onDismissRequest()
                    onConfirm(
                        list[0].isChecked,
                        list.getOrElse(1) { CheckboxState.State.None(0) }.isChecked,
                    )
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        title = {
            Text(text = stringResource(R.string.action_remove))
        },
        text = {
            Column {
                list.forEach { state ->
                    val onCheck = {
                        val index = list.indexOf(state)
                        if (index != -1) {
                            val mutableList = list.toMutableList()
                            mutableList[index] = state.next() as CheckboxState.State<Int>
                            list = mutableList.toList()
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCheck() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state.isChecked,
                            onCheckedChange = { onCheck() },
                        )
                        Text(text = stringResource(state.value))
                    }
                }
            }
        },
    )
}

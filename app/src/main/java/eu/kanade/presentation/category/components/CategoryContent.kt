package eu.kanade.presentation.category.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.material.padding

@Composable
fun CategoryContent(
    categories: List<Category>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickRename: (Category) -> Unit,
    onClickHide: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onMoveUp: (Category) -> Unit,
    onMoveDown: (Category) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        itemsIndexed(
            items = categories,
            key = { _, category -> "category-${category.id}" },
        ) { index, category ->
            CategoryListItem(
                modifier = Modifier.animateItemPlacement(),
                category = category,
                canMoveUp = index != 0,
                canMoveDown = index != categories.lastIndex,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRename = { onClickRename(category) },
                onHide = { onClickHide(category) },
                onDelete = { onClickDelete(category) },
            )
        }
    }
}

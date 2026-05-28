package codes.shubham.grocerymanagement.ui.screens.audit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codes.shubham.grocerymanagement.domain.model.Product
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuditViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.feedbackMessage) {
        if (state.feedbackMessage != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (state.isReviewing) "Review Audit" else "Audit Inventory")
                        if (state.items.isNotEmpty()) {
                            Text(
                                "${state.checkedCount + state.skippedCount}/${state.items.size} processed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.isReviewing) viewModel.returnToAudit() else onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (state.isReviewing) {
                        TextButton(
                            onClick = viewModel::saveAudit,
                            enabled = state.hasAuditedItems && state.invalidCount == 0
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                    } else {
                        TextButton(
                            onClick = viewModel::showReview,
                            enabled = state.hasAuditedItems
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Review")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            EmptyAuditState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            return@Scaffold
        }

        if (state.isReviewing) {
            AuditReviewContent(
                state = state,
                onEditItem = viewModel::editProduct,
                modifier = Modifier.padding(padding)
            )
        } else {
            AuditSessionContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun AuditSessionContent(
    state: AuditUiState,
    viewModel: AuditViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.feedbackMessage?.let { message ->
            item { FeedbackCard(message) }
        }

        item {
            AuditProgressCard(state = state)
        }

        state.currentItem?.let { item ->
            item {
                GuidedAuditCard(
                    item = item,
                    position = state.currentIndex + 1,
                    total = state.items.size,
                    onCountedQuantityChange = { viewModel.updateCountedQuantity(item.product, it) },
                    onNotesChange = { viewModel.updateNotes(item.product, it) },
                    onSame = { viewModel.markSame(item.product) },
                    onEmpty = { viewModel.markEmpty(item.product) },
                    onIncrement = { viewModel.adjustQuantity(item.product, 1.0) },
                    onDecrement = { viewModel.adjustQuantity(item.product, -1.0) },
                    onSkip = { viewModel.skipProduct(item.product.id) },
                    onReset = { viewModel.resetDraft(item.product.id) },
                    onPrevious = viewModel::goPrevious,
                    onNext = viewModel::goNext,
                    canGoPrevious = state.currentIndex > 0,
                    canGoNext = state.currentIndex < state.items.lastIndex
                )
            }
        }
    }
}

@Composable
private fun AuditReviewContent(
    state: AuditUiState,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val checkedItems = state.items.filter { it.isChecked }
    val skippedItems = state.items.filter { it.isSkipped }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.feedbackMessage?.let { message ->
            item { FeedbackCard(message) }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Audit Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryChip("Checked", state.checkedCount.toString(), Icons.Default.CheckCircle)
                        SummaryChip("Changed", state.changedCount.toString(), Icons.Default.Edit)
                        SummaryChip("Skipped", state.skippedCount.toString(), Icons.Default.SkipNext)
                    }
                    if (state.invalidCount > 0) {
                        Text(
                            "Fix ${state.invalidCount} invalid quantity before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (checkedItems.isNotEmpty()) {
            item { SectionTitle("Checked Items") }
            items(checkedItems, key = { it.product.id }) { item ->
                ReviewItemCard(item = item, onEditItem = onEditItem)
            }
        }

        if (skippedItems.isNotEmpty()) {
            item { SectionTitle("Skipped Items") }
            items(skippedItems, key = { it.product.id }) { item ->
                ReviewItemCard(item = item, onEditItem = onEditItem)
            }
        }
    }
}

@Composable
private fun AuditProgressCard(state: AuditUiState) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Stock check session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Item ${state.currentIndex + 1} of ${state.items.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip("Checked", state.checkedCount.toString(), Icons.Default.CheckCircle)
                SummaryChip("Changed", state.changedCount.toString(), Icons.Default.Edit)
                SummaryChip("Skipped", state.skippedCount.toString(), Icons.Default.SkipNext)
            }
        }
    }
}

@Composable
private fun GuidedAuditCard(
    item: AuditItemUiState,
    position: Int,
    total: Int,
    onCountedQuantityChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSame: () -> Unit,
    onEmpty: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Check $position / $total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    ProductSubtext(item.product)
                }
                StatusIcon(item)
            }

            QuantitySummary(product = item.product)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease counted quantity")
                }
                OutlinedTextField(
                    value = item.countedQuantity,
                    onValueChange = onCountedQuantityChange,
                    label = { Text("Counted") },
                    suffix = { Text(item.product.unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = item.errorMessage != null,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = "Increase counted quantity")
                }
            }

            if (item.errorMessage != null) {
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSame, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Same")
                }
                OutlinedButton(onClick = onEmpty, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Empty")
                }
            }

            OutlinedTextField(
                value = item.notes,
                onValueChange = onNotesChange,
                label = { Text("Note") },
                placeholder = { Text("Optional") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onReset, enabled = item.isChecked || item.isSkipped) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPrevious, enabled = canGoPrevious) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = null)
                    }
                    OutlinedButton(onClick = onSkip) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Skip")
                    }
                    Button(onClick = onNext, enabled = canGoNext) {
                        Icon(Icons.Default.NavigateNext, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(item: AuditItemUiState, onEditItem: (Long) -> Unit) {
    val statusColor = when {
        item.errorMessage != null -> MaterialTheme.colorScheme.error
        item.isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
        item.hasChanges -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when {
                    item.errorMessage != null -> Icons.Default.WarningAmber
                    item.isSkipped -> Icons.Default.SkipNext
                    item.hasChanges -> Icons.Default.Edit
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = statusColor
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ProductSubtext(item.product)
                Text(
                    reviewQuantityText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = { onEditItem(item.product.id) }) {
                Text("Edit")
            }
        }
    }
}

@Composable
private fun ProductSubtext(product: Product) {
    val details = listOfNotNull(
        product.brand?.takeIf { it.isNotBlank() },
        product.category?.takeIf { it.isNotBlank() }
    ).joinToString(" - ")

    if (details.isNotBlank()) {
        Text(
            text = details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuantitySummary(product: Product) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FactCheck,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = "Recorded quantity",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formatQuantity(product.quantity)} ${product.unit}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusIcon(item: AuditItemUiState) {
    val icon: ImageVector
    val color: Color
    val label: String

    when {
        item.errorMessage != null -> {
            icon = Icons.Default.WarningAmber
            color = MaterialTheme.colorScheme.error
            label = "Needs fix"
        }
        item.isSkipped -> {
            icon = Icons.Default.SkipNext
            color = MaterialTheme.colorScheme.onSurfaceVariant
            label = "Skipped"
        }
        item.hasChanges -> {
            icon = Icons.Default.Edit
            color = MaterialTheme.colorScheme.tertiary
            label = "Changed"
        }
        item.isChecked -> {
            icon = Icons.Default.CheckCircle
            color = MaterialTheme.colorScheme.primary
            label = "Checked"
        }
        else -> {
            icon = Icons.Default.Inventory2
            color = MaterialTheme.colorScheme.onSurfaceVariant
            label = "Pending"
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun SummaryChip(label: String, value: String, icon: ImageVector) {
    AssistChip(
        onClick = {},
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        label = { Text("$label $value") }
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun FeedbackCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun EmptyAuditState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FactCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No items available to audit.",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun reviewQuantityText(item: AuditItemUiState): String =
    if (item.isSkipped) {
        "Skipped"
    } else {
        "Recorded ${formatQuantity(item.product.quantity)} ${item.product.unit}, counted ${item.countedQuantity} ${item.product.unit}"
    }

private fun formatQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) {
        quantity.toLong().toString()
    } else {
        "%.2f".format(quantity)
    }
